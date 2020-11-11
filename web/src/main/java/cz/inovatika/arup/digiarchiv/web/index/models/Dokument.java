/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index.models;

import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.imagging.ImageSupport;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class Dokument implements Entity {

  static final Logger LOGGER = Logger.getLogger(Dokument.class.getName());
  String[] textFields = new String[]{"text_all_A", "text_all_B", "text_all_C", "text_all_D"};
  String[] katastrFields = new String[]{"f_katastr_A", "f_katastr_B", "f_katastr_C", "f_katastr_D"};

  String[] facetFields = new String[]{"f_typ_vyzkumu"};

  @Field
  public String ident_cely;

  @Field
  public int stav;

  @Field
  public String let;

  @Field
  public String typ_dokumentu;

  @Field
  public String material_originalu;

  @Field
  public String rada;

  @Field
  public List<String> typ_dokumentu_posudek;

  @Field
  public List<String> autor;

  @Field
  public String autor_sort;

  @Field
  public int rok_vzniku;

  @Field
  public String organizace;

  @Field
  public String pristupnost;

  @Field
  public Date datum_zverejneni;

  @Field
  public String jazyk_dokumentu;

  @Field
  public String ulozeni_originalu;

  @Field
  public String oznaceni_originalu;

  @Field
  public String popis;

  @Field
  public String poznamka;

  @Field
  public Date datum_vlozeni;

  @Field
  public String odpovedny_pracovnik_vlozeni;

  @Field
  public Date datum_archivace;

  @Field
  public String odpovedny_pracovnik_archivace;

  @Field
  public List<String> extra_data;

  @Field
  public List<String> tvar;

  @Field
  public List<String> jednotka_dokumentu;

  @Field
  public List<String> child_soubor;

  @Field
  public List<String> odkaz;

  @Override
  public void fillFields(SolrInputDocument idoc) {

    boolean searchable = stav == 3 && !"zl".equals(rada.toLowerCase()) && !"za".equals(rada.toLowerCase());
    idoc.setField("searchable", searchable);
    String kategorie = Options.getInstance().getJSONObject("kategoriet").optString(typ_dokumentu);
    idoc.addField("kategorie_dokumentu", kategorie);

    if (extra_data != null) {
      for (String extra : extra_data) {
        JSONObject doc = new JSONObject(extra);
        if (doc.has("northing")) { 
          String loc = doc.optString("northing") + "," + doc.optString("easting");
          idoc.addField("lat", doc.optString("northing"));
          idoc.addField("lng", doc.optString("easting"));
          idoc.addField("loc", loc);
          idoc.addField("loc_rpt", loc); 
        }
        for (String s : doc.keySet()) {
          idoc.addField("extra_data_" + s, doc.optString(s));
        }
      }
    }
    if (tvar != null) {
      for (String js : tvar) {
        JSONObject doc = new JSONObject(js);
        for (String s : doc.keySet()) {
          idoc.addField("tvar_" + s, doc.optString(s));
        }
      }
    }

    if (autor != null) {
      List<String> l = new ArrayList<>();
      for (String v : autor) {
        l.addAll(Arrays.asList(v.split(";")));
      }
      autor.clear();
      autor.addAll(l);
      autor_sort = autor.get(0);
      idoc.addField("autor_sort", autor.get(0));
    }

    if (typ_dokumentu_posudek != null) {
      List<String> l = new ArrayList<>();
      for (String v : typ_dokumentu_posudek) {
        l.addAll(Arrays.asList(v.split(";")));
      }
      typ_dokumentu_posudek.clear();
      typ_dokumentu_posudek.addAll(l);
      idoc.setField("typ_dokumentu_posudek", typ_dokumentu_posudek);
    }

    if (organizace != null) {
      idoc.addField("organizace_sort", organizace);
    }
  }

  private void processAkce(HttpSolrClient client, SolrInputDocument idoc, String field, String id) {

    SolrQuery query = new SolrQuery("ident_cely:\"" + id + "\"")
            .setFields("katastr,okres");
    for (String f : facetFields) {
      query.addField(f);
    }
    JSONObject json = SearchUtils.json(query, client, "entities");
    if (json.getJSONObject("response").getInt("numFound") > 0) {
      JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
      for (String f : facetFields) {
        if (doc.has(f)) {
          idoc.addField(f, doc.get(f));
        }
      }
      if (doc.has("katastr")) {
        idoc.addField(field + "_katastr", doc.getString("katastr"));
        idoc.addField(field + "_okres", doc.getString("okres"));
      }
    }
  }

  private JSONObject addDokJednotka(HttpSolrClient client, SolrInputDocument idoc, String field, String id) {

    SolrQuery query = new SolrQuery(field + ":\"" + id + "\"")
            .setFields("*,komponenta:[json],pian:[json],adb:[json]")
            .addFilterQuery("entity:dok_jednotka");
    JSONObject json = SearchUtils.json(query, client, "entities");
    if (json.getJSONObject("response").getInt("numFound") > 0) {
      JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
      idoc.addField("dok_jednotka", doc.toString());

      if (doc.has("katastr")) {
        idoc.addField(field + "_katastr", doc.getString("katastr"));
        idoc.addField(field + "_okres", doc.getString("okres"));
      }
      if (doc.has("komponenta")) {
        JSONArray ja = doc.getJSONArray("komponenta");
        for (int i = 0; i < ja.length(); i++) {
          JSONObject k = ja.getJSONObject(i);
          for (String fk : k.keySet()) {
            idoc.addField("komponenta_" + fk, k.optString(fk));
            if (fk.contains("aktivita") && k.optString(fk).equals("1")) {
              idoc.addField("f_aktivita", fk.substring("aktivita_".length()));
            } else if ("nalez".equals(fk)) {
              addNalez(idoc, k);
            }
          }
        }

      }

      if (doc.has("pian")) {
        // addGeometry(client, idoc, doc.getJSONArray("pian").getJSONObject(0).getString("ident_cely"));
        JSONObject pianDoc;
        Object p = doc.get("pian");
        if (p instanceof JSONArray) {
          JSONArray pians = (JSONArray) p;
          pianDoc = pians.getJSONObject(0);
          for (int pian = 0; pian < pians.length(); pian++) {
            idoc.addField("pian_id", pians.getJSONObject(pian).optString("ident_cely"));
            idoc.addField("pian_typ", pians.getJSONObject(pian).optString("typ"));
            idoc.addField("pian_presnost", pians.getJSONObject(pian).optString("presnost"));
            idoc.addField("pian_zm10", pians.getJSONObject(pian).optString("zm10"));
          }

        } else {
          pianDoc = (JSONObject) p;
          idoc.addField("pian_id", pianDoc.optString("ident_cely"));
          idoc.addField("pian_typ", pianDoc.optString("typ"));
          idoc.addField("pian_presnost", pianDoc.optString("presnost"));
          idoc.addField("pian_zm10", pianDoc.optString("zm10"));
        }
        if (pianDoc.has("centroid_n")) {
          String loc = pianDoc.optString("centroid_n") + "," + pianDoc.optString("centroid_e");
          idoc.addField("lat", pianDoc.optString("centroid_n"));
          idoc.addField("lng", pianDoc.optString("centroid_e"));
          idoc.addField("loc", loc);
          idoc.addField("loc_rpt", loc);
        }
        idoc.addField("pian", pianDoc.toString());
      }
      return doc;
    }
    return json;
  }

  private void addNalez(SolrInputDocument idoc, JSONObject json) {
    Object nalez = json.get("nalez");
    if (nalez instanceof JSONArray) {
      JSONArray ja = (JSONArray) nalez;
      for (int i = 0; i < ja.length(); i++) {
        idoc.addField("nalez_druh_nalezu", ja.getJSONObject(i).optString("druh_nalezu"));
        idoc.addField("nalez_typ_nalezu", ja.getJSONObject(i).optString("typ_nalezu"));
        idoc.addField("nalez_kategorie", ja.getJSONObject(i).optString("kategorie"));
        idoc.addField("nalez_specifikace", ja.getJSONObject(i).optString("specifikace"));
      }
    } else {
      idoc.addField("nalez_druh_nalezu", ((JSONObject) nalez).optString("druh_nalezu"));
      idoc.addField("nalez_typ_nalezu", ((JSONObject) nalez).optString("typ_nalezu"));
      idoc.addField("nalez_kategorie", ((JSONObject) nalez).optString("kategorie"));
      idoc.addField("nalez_specifikace", ((JSONObject) nalez).optString("specifikace"));
    }
  }

  private void addKomponentaDokument(HttpSolrClient client, JSONObject doc, SolrInputDocument idoc) {

    JSONArray aks = new JSONArray();
    for (String s : doc.keySet()) {

      if (s.contains("aktivita") && doc.optString(s).equals("1")) {
        idoc.addField("f_aktivita", s.substring("aktivita_".length()));
        aks.put(s.substring("aktivita_".length()));
      } else if ("nalez".equals(s)) {
        addNalez(idoc, doc);
      }
      switch (s) {
        case "nalez_dokumentu":
          Object obj = doc.get("nalez_dokumentu");
          if (obj instanceof JSONArray) {
            JSONArray ja = (JSONArray) obj;
            for (int i = 0; i < ja.length(); i++) {
              addJSONFields(ja.getJSONObject(i), "nalez_dokumentu", idoc);
              idoc.addField("nalez_dokumentu", ja.getJSONObject(i).toString());
            }
          } else if (obj instanceof JSONObject) {
            addJSONFields((JSONObject) obj, "nalez_dokumentu", idoc);
            idoc.addField("nalez_dokumentu", ((JSONObject) obj).toString());
          }
          break;
        default:
          idoc.addField("komponenta_dokument_" + s, doc.optString(s));
      }
    }
    if (!aks.isEmpty()) {
      doc.put("aktivita", aks);
    }
    idoc.addField("komponenta_dokument", doc.toString());

    addAsEntity(client, doc, "komponenta_dokument");
    if (doc.has("obdobi")) {
      idoc.addField("obdobi_poradi", SearchUtils.getObdobiPoradi(doc.getString("obdobi")));
    }
  }

  private void addJSONFields(JSONObject doc, SolrInputDocument idoc) {
    for (String s : doc.keySet()) {
      switch (s) {
        case "_version_":
        case "_root_":
        case "indextime":
          break;
        default:
          idoc.addField(s, doc.optString(s));
      }
    }
  }

  private void addJSONFields(JSONObject doc, String prefix, SolrInputDocument idoc) {
    for (String s : doc.keySet()) {
      switch (s) {
        case "_version_":
        case "_root_":
        case "indextime":
          break;
        case "vedouci":
          idoc.addField(prefix + "_" + s, doc.optString(s).split(";"));
          break;
        default:
          idoc.addField(prefix + "_" + s, doc.optString(s));
      }
    }
  }

  private void addAsEntity(HttpSolrClient client, JSONObject doc, String entity) {
    try {
      SolrInputDocument idoc = new SolrInputDocument();
      idoc.setField("entity", entity);
      idoc.setField("searchable", true);
      addJSONFields(doc, idoc);
      client.add("entities", idoc);
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, "Cant add {0}", entity);
    }
  }

  @Override
  public void addRelations(HttpSolrClient client, SolrInputDocument idoc) {
    if (this.child_soubor != null) {
      addSoubor(client, idoc);
    } else {
      idoc.setField("hasThumb", true);
    }
    addLet(client, idoc);

    if (jednotka_dokumentu != null) {
      for (String js : jednotka_dokumentu) {
        JSONObject doc = new JSONObject(js);
        JSONObject dok_jednotka = null;
        for (String s : doc.keySet()) {
          switch (s) {
            case "neident_akce":
              JSONObject naDoc = doc.getJSONObject("neident_akce");
              idoc.addField("neident_akce", naDoc.toString());
              addJSONFields(naDoc, "neident_akce", idoc);

              int start = naDoc.optInt("rok_zahajeni", 0);
              if (start != 0) {
                int end = naDoc.optInt("rok_ukonceni", 0);
                String ukonceni = "*";
                Calendar c1 = Calendar.getInstance();
                c1.set(start, 1, 1);
                idoc.addField("datum_provedeni_od", c1.toInstant().toString());
                if (end != 0) {
                  Calendar c2 = Calendar.getInstance();
                  c2.set(end, 12, 31);
                  idoc.addField("datum_provedeni_do", c2.toInstant().toString());
                  if (c2.after(c1)) {
                    ukonceni = c2.toInstant().toString();
                  }
                }
                idoc.addField("datum_provedeni", "[" + c1.toInstant().toString() + " TO " + ukonceni + "]");
              }
              break;

            case "komponenta_dokument":
              Object obj = doc.get("komponenta_dokument");
              if (obj instanceof JSONArray) {
                JSONArray ja = (JSONArray) obj;
                for (int i = 0; i < ja.length(); i++) {
                  addKomponentaDokument(client, ja.getJSONObject(i), idoc);
                }
              } else if (obj instanceof JSONObject) {
                addKomponentaDokument(client, (JSONObject) obj, idoc);
              }

              break;
            case "vazba_akce":
              // Chceme pian
              //String vazba_akce = (String) doc.get("vazba_akce");
              idoc.addField("jednotka_dokumentu_vazba_akce", doc.optString(s));
              dok_jednotka = addDokJednotka(client, idoc, "parent_akce", doc.optString(s));
              processAkce(client, idoc, "parent_akce", doc.optString(s));
              break;
            case "vazba_lokalita":
              // Chceme pian
              idoc.addField("jednotka_dokumentu_vazba_lokalita", doc.optString(s));
              dok_jednotka = addDokJednotka(client, idoc, "parent_lokalita", (String) doc.get("vazba_lokalita"));
              processAkce(client, idoc, "parent_lokalita", doc.optString(s));
              break;
            default:
              idoc.addField("jednotka_dokumentu_" + s, doc.optString(s));
          }
        }

        doc.put("dok_jednotka", dok_jednotka);
        addAsEntity(client, doc, "jednotka_dokumentu");
      }
    }
  }

  private void addSoubor(HttpSolrClient client, SolrInputDocument idoc) {

    for (int i = 0; i < child_soubor.size(); i++) {
      String soubor = this.child_soubor.get(i);
      SolrQuery query = new SolrQuery("filepath:\"" + soubor + "\"")
              .setFields("filepath,nazev,uzivatelske_oznaceni,mimetype,rozsah,"
                      + "size_bytes,vytvoreno,stav,dokument,projekt,samostatny_nalez");
      JSONObject json = SearchUtils.json(query, client, "soubor");
      if (json.getJSONObject("response").getInt("numFound") > 0) {
        JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
        idoc.addField("soubor", doc.toString());
        addJSONFields(doc, "soubor", idoc);

        if (i == 0) {
          String filepath = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0).optString("filepath");
          String dest = ImageSupport.getDestDir(filepath);
          String fname = dest + filepath + "_thumb.jpg";
          idoc.setField("hasThumb", new File(fname).exists() || new File(dest + "_thumb.jpg").exists());
        }
      }
    }

  }

  private void addLet(HttpSolrClient client, SolrInputDocument idoc) {
    SolrQuery query = new SolrQuery("child_dokument:\"" + ident_cely + "\"");
    query.addFilterQuery("entity:let");
    // JSONObject json = SearchUtils.json(query, client, "let");
    JSONObject json = SearchUtils.json(query, client, "entities");
    JSONArray docs = json.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < docs.length(); i++) {
      idoc.setField("let", docs.getJSONObject(i).toString());
      addJSONFields(docs.getJSONObject(i), "let", idoc);
//       addAsEntity(client, docs.getJSONObject(i), "let");
    }
  }

  @Override
  public void setFullText(SolrInputDocument idoc) {
    Object[] fields = idoc.getFieldNames().toArray();
    List<Object> indexFields = Options.getInstance().getJSONObject("indexFieldsByType").getJSONArray("dokument").toList();
    String pr = (String) idoc.getFieldValue("pristupnost");
    pr = "A";
    List<String> prSufix = new ArrayList<>();

    if ("A".compareTo(pr) >= 0) {
      prSufix.add("A");
    }
    if ("B".compareTo(pr) >= 0) {
      prSufix.add("B");
    }
    if ("C".compareTo(pr) >= 0) {
      prSufix.add("C");
    }
    if ("D".compareTo(pr) >= 0) {
      prSufix.add("D");
    }

    for (Object f : fields) {
      String s = (String) f;
      
      SolrSearcher.addCommonFieldFacets(s, idoc, prSufix);
      
      if (indexFields.contains(s)) {
        for (String sufix : prSufix) {
          idoc.addField("text_all_" + sufix, idoc.getFieldValues(s));
        }
      } 
      
    }
  }

  @Override
  public boolean isSearchable() {
    return true;
  }
  
  @Override
  public void secondRound(HttpSolrClient client, SolrInputDocument idoc) {
  }
}
