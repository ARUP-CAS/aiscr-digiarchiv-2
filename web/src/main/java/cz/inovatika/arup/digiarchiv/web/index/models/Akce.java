/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index.models;

import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class Akce implements Entity {

  static final Logger LOGGER = Logger.getLogger(Akce.class.getName());

  @Field
  public String ident_cely;

  @Field
  public int stav;

  @Field
  public int je_nz;

  @Field
  public String okres;

  @Field
  public String katastr;

  @Field
  public String dalsi_katastry;

  @Field
  public String vedouci_akce;

  @Field
  public String organizace;

  @Field
  public List<String> organizace_ostatni;

  @Field
  public List<String> vedouci_akce_ostatni;

  @Field
  public String uzivatelske_oznaceni;

  @Field
  public String specifikace_data;

  @Field
  public String datum_zahajeni;

  @Field
  public Date datum_zahajeni_v;

  @Field
  public String datum_ukonceni;

  @Field
  public Date datum_ukonceni_v;

  @Field
  public String hlavni_typ;

  @Field
  public String vedlejsi_typ;

  @Field
  public String lokalizace;

  @Field
  public String poznamka;

  @Field
  public String ulozeni_nalezu;

  @Field
  public String pristupnost;

  @Field
  public Date datum_zapisu;

  @Field
  public String odpovedny_pracovnik_zapisu;

  @Field
  public Date datum_autorizace;

  @Field
  public String odpovedny_pracovnik_autorizace;

  @Field
  public Date datum_archivace_zaa;

  @Field
  public String odpovedny_pracovnik_archivace_zaa;

  @Field
  public Date datum_odlozeni_nz;

  @Field
  public String odpovedny_pracovnik_odlozeni_nz;

  @Field
  public Date datum_vraceni_zaa;

  @Field
  public String odpovedny_pracovnik_vraceni_zaa;

  @Field
  public Date datum_podani_nz;

  @Field
  public String odpovedny_pracovnik_podani_nz;

  @Field
  public Date datum_archivace;

  @Field
  public String odpovedny_pracovnik_archivace;

  @Field
  public Date datum_zamitnuti;

  @Field
  public String odpovedny_pracovnik_zamitnuti;

  @Field
  public String vazba_projekt_akce;

  @Field
  public List<String> dok_jednotka;

  @Field
  public List<String> dokument;

  @Field
  public List<String> ext_zdroj;

  @Field
  public List<String> child_dok_jednotka;

  @Field
  public List<String> child_dokument;

  @Field
  public List<String> child_ext_zdroj;

  final String[] ADB_FIELDS = new String[]{
    "ident_cely", "pristupnost", "typ_sondy", "uzivatelske_oznaceni_sondy", "trat", "cislo_popisne",
    "parcelni_cislo", "podnet", "stratigraficke_jednotky", "autor_popisu", "rok_popisu",
    "autor_revize", "rok_revize", "poznamka"
  };

  @Override
  public void fillFields(SolrInputDocument idoc) {

    // query.addFilterQuery("{!tag=entityF}(stav:4 OR stav:5 OR stav:8)");
    boolean searchable = stav == 4 || stav == 5 || stav == 8;
    idoc.setField("searchable", searchable);

    if (vedouci_akce_ostatni != null) {
      List<String> l = new ArrayList<>();
      for (String v : vedouci_akce_ostatni) {
        l.addAll(Arrays.asList(v.split(";")));
      }
      vedouci_akce_ostatni.clear();
      vedouci_akce_ostatni.addAll(l);
    }

    if (organizace_ostatni != null) {
      List<String> l = new ArrayList<>();
      for (String v : organizace_ostatni) {
        l.addAll(Arrays.asList(v.split(";")));
      }
      organizace_ostatni.clear();
      organizace_ostatni.addAll(l);
    }

    if (dalsi_katastry != null) {
      String[] kats = dalsi_katastry.split(";");
      for (String kat : kats) {
        String[] parts = kat.split("\\(");
        String kt = parts[0];
        String ok = parts[1].substring(0, parts[1].length() - 1);

        SolrSearcher.addFieldNonRepeat(idoc, "dalsi_katastr", kt.trim());
        SolrSearcher.addFieldNonRepeat(idoc, "dalsi_okres", ok.trim());
      }
      idoc.setField("dalsi_katastry", kats);
    }

    if (vazba_projekt_akce != null) {
      JSONObject j = new JSONObject(vazba_projekt_akce);
      SolrSearcher.addFieldNonRepeat(idoc, "vazba_projekt", j.getString("id_projekt"));
    }

    if (organizace != null) {
      SolrSearcher.addFieldNonRepeat(idoc, "organizace_sort", organizace);
    }

    if (hlavni_typ != null) {
      SolrSearcher.addFieldNonRepeat(idoc, "f_typ_vyzkumu", hlavni_typ);
    }
    if (vedlejsi_typ != null) {
      SolrSearcher.addFieldNonRepeat(idoc, "f_typ_vyzkumu", vedlejsi_typ);
    }

    if (vedouci_akce != null) {
      SolrSearcher.addFieldNonRepeat(idoc, "autor_sort", vedouci_akce);
    }

    if (okres != null) {
      String okres_sort = okres;
      if (katastr != null) {
        okres_sort += " " + katastr;
      } 
      SolrSearcher.addFieldNonRepeat(idoc, "okres_sort", okres_sort);
    }

    if (datum_zahajeni_v != null) {
      SolrSearcher.addFieldNonRepeat(idoc, "datum_provedeni_od", datum_zahajeni_v);
      String ukonceni = "*";
      if (datum_ukonceni_v != null) {
        SolrSearcher.addFieldNonRepeat(idoc, "datum_provedeni_do", datum_ukonceni_v);
        if (!datum_ukonceni_v.before(datum_zahajeni_v)) {
          ukonceni = datum_ukonceni_v.toInstant().toString();
        }
      }
      SolrSearcher.addFieldNonRepeat(idoc, "datum_provedeni", "[" + datum_zahajeni_v.toInstant().toString() + " TO " + ukonceni + "]");
    }

  }

  @Override
  public void addRelations(HttpSolrClient client, SolrInputDocument idoc) {
    if (this.child_dok_jednotka != null) {
      addDokJednotka(client, idoc);
    }

    if (this.child_ext_zdroj != null) {
      addExterniZdroj(client, idoc);
    }
  }

  private void addExterniZdroj(HttpSolrClient client, SolrInputDocument idoc) {
    for (String dok : this.child_ext_zdroj) {

      SolrQuery query = new SolrQuery("ident_cely:\"" + dok + "\"")
              .setFields("*,ext_odkaz:[json]");
      JSONObject json = SearchUtils.json(query, client, "entities");
      if (json.getJSONObject("response").getInt("numFound") > 0) {
        JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
        SolrSearcher.addFieldNonRepeat(idoc, "ext_zdroj", doc.toString());
        if (doc.has("rok_vydani_vzniku")) {
          String r = doc.getString("rok_vydani_vzniku");
          if (StringUtils.isNumeric(r)) {
            SolrSearcher.addFieldNonRepeat(idoc, "rok_vydani", doc.optString("rok_vydani_vzniku"));
          }

        }
      }
    }
  }

  private void addNalez(SolrInputDocument idoc, JSONObject json) {
    Object nalez = json.get("nalez");
    if (nalez instanceof JSONArray) {
      JSONArray ja = (JSONArray) nalez;
      for (int i = 0; i < ja.length(); i++) {
        SolrSearcher.addFieldNonRepeat(idoc, "nalez_druh_nalezu", ja.getJSONObject(i).optString("druh_nalezu"));
        SolrSearcher.addFieldNonRepeat(idoc, "nalez_typ_nalezu", ja.getJSONObject(i).optString("typ_nalezu"));
        SolrSearcher.addFieldNonRepeat(idoc, "nalez_kategorie", ja.getJSONObject(i).optString("kategorie"));
        SolrSearcher.addFieldNonRepeat(idoc, "nalez_specifikace", ja.getJSONObject(i).optString("specifikace"));
      }
    } else {
      SolrSearcher.addFieldNonRepeat(idoc, "nalez_druh_nalezu", ((JSONObject) nalez).optString("druh_nalezu"));
      SolrSearcher.addFieldNonRepeat(idoc, "nalez_typ_nalezu", ((JSONObject) nalez).optString("typ_nalezu"));
      SolrSearcher.addFieldNonRepeat(idoc, "nalez_kategorie", ((JSONObject) nalez).optString("kategorie"));
      SolrSearcher.addFieldNonRepeat(idoc, "nalez_specifikace", ((JSONObject) nalez).optString("specifikace"));
    }
  } 

  private void addDokJednotka(HttpSolrClient client, SolrInputDocument idoc) {
    this.dok_jednotka = new ArrayList<>();
    for (String dok : this.child_dok_jednotka) {

      SolrQuery query = new SolrQuery("ident_cely:\"" + dok + "\"")
              .setFields("*,komponenta:[json],pian:[json],adb:[json]");
      JSONObject json = SearchUtils.json(query, client, "entities");
      if (json.getJSONObject("response").getInt("numFound") > 0) {
        for (int d = 0; d < json.getJSONObject("response").getJSONArray("docs").length(); d++) {
          JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(d);
          SolrSearcher.addFieldNonRepeat(idoc, "dok_jednotka", doc.toString());
          for (String s : doc.keySet()) {
            if ("komponenta".equals(s)) {
              JSONArray ja = doc.getJSONArray("komponenta");
              for (int i = 0; i < ja.length(); i++) {
                JSONObject k = ja.getJSONObject(i);
                for (String fk : k.keySet()) {
                  SolrSearcher.addFieldNonRepeat(idoc, "komponenta_" + fk, k.optString(fk));
                  if (fk.contains("aktivita") && k.optString(fk).equals("1")) {
                    SolrSearcher.addFieldNonRepeat(idoc, "f_aktivita", fk.substring("aktivita_".length()));
                  } else if ("nalez".equals(fk)) {
                    addNalez(idoc, k);
                  }
                }
              }

            } else {
              SolrSearcher.addFieldNonRepeat(idoc, "dok_jednotka_" + s, doc.optString(s));
            }
          }
          if (doc.has("child_adb")) {
            addADB(client, idoc, doc.optString("child_adb"));
          }
          if (doc.has("pian")) {
            // addGeometry(client, idoc, doc.getJSONArray("pian").getJSONObject(0).getString("ident_cely"));
            JSONObject pianDoc;
            Object p = doc.get("pian");
            if (p instanceof JSONArray) {
              // System.out.println(field + "; " + id + "; " + doc.getJSONArray("pian"));
              JSONArray pians = (JSONArray) p;
              pianDoc = pians.getJSONObject(0);
              for (int pian = 0; pian < pians.length(); pian++) {
                SolrSearcher.addFieldNonRepeat(idoc, "pian_id", pians.getJSONObject(pian).optString("ident_cely"));
                SolrSearcher.addFieldNonRepeat(idoc, "pian_typ", pians.getJSONObject(pian).optString("typ"));
                SolrSearcher.addFieldNonRepeat(idoc, "pian_presnost", pians.getJSONObject(pian).optString("presnost"));
                SolrSearcher.addFieldNonRepeat(idoc, "pian_zm10", pians.getJSONObject(pian).optString("zm10"));
              }

            } else {
              pianDoc = (JSONObject) p;
              SolrSearcher.addFieldNonRepeat(idoc, "pian_id", pianDoc.optString("ident_cely"));
              SolrSearcher.addFieldNonRepeat(idoc, "pian_typ", pianDoc.optString("typ"));
              SolrSearcher.addFieldNonRepeat(idoc, "pian_presnost", pianDoc.optString("presnost"));
              SolrSearcher.addFieldNonRepeat(idoc, "pian_zm10", pianDoc.optString("zm10"));
            }

            if (pianDoc.has("centroid_n")) {
              String loc = pianDoc.optString("centroid_n") + "," + pianDoc.optString("centroid_e");
              SolrSearcher.addFieldNonRepeat(idoc, "loc", loc);
              SolrSearcher.addFieldNonRepeat(idoc, "lat", pianDoc.optString("centroid_n"));
              SolrSearcher.addFieldNonRepeat(idoc, "lng", pianDoc.optString("centroid_e"));
              SolrSearcher.addFieldNonRepeat(idoc, "loc_rpt", loc);
            }
            SolrSearcher.addFieldNonRepeat(idoc, "pian", pianDoc.toString());

          }
        }
      }
    }
  }

  private void addADB(HttpSolrClient client, SolrInputDocument idoc, String child_adb) {
    SolrQuery query = new SolrQuery("ident_cely:\"" + child_adb + "\"")
            .setFields(ADB_FIELDS).addField("vyskovy_bod:[json]");
    JSONObject json = SearchUtils.json(query, client, "entities");
    if (json.getJSONObject("response").getInt("numFound") > 0) {
      JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
      SolrSearcher.addFieldNonRepeat(idoc, "adb", doc.toString());
      for (String field : ADB_FIELDS) {
        SolrSearcher.addFieldNonRepeat(idoc, "adb_" + field, doc.optString(field));
      }
      if (doc.has("vyskovy_bod")) {
        JSONArray ja = doc.getJSONArray("vyskovy_bod");
        for (int i = 0; i < ja.length(); i++) {
          JSONObject vb = ja.getJSONObject(i);
          SolrSearcher.addFieldNonRepeat(idoc, "vyskovy_bod_ident_cely", vb.optString("ident_cely"));
          SolrSearcher.addFieldNonRepeat(idoc, "vyskovy_bod_typ", vb.optString("typ"));
          SolrSearcher.addFieldNonRepeat(idoc, "vyskovy_bod_niveleta", vb.optString("niveleta"));
          SolrSearcher.addFieldNonRepeat(idoc, "vyskovy_bod_easting", vb.optString("easting"));
          SolrSearcher.addFieldNonRepeat(idoc, "vyskovy_bod_northing", vb.optString("northing"));
//          String loc = vb.optString("northing") + "," + vb.optString("easting");
//          SolrSearcher.addFieldNonRepeat(idoc, "loc", loc);
//          SolrSearcher.addFieldNonRepeat(idoc, "loc_rpt", loc);
        }

      }
    }

  }

  @Override
  public void setFullText(SolrInputDocument idoc) {
    List<Object> indexFields = Options.getInstance().getJSONObject("indexFieldsByType").getJSONArray("akce").toList();
    String pristupnost = (String) idoc.getFieldValue("pristupnost");
    List<String> prSufix = new ArrayList<>();

    if ("A".compareTo(pristupnost) >= 0) {
      prSufix.add("A");
    }
    if ("B".compareTo(pristupnost) >= 0) {
      prSufix.add("B");
    }
    if ("C".compareTo(pristupnost) >= 0) {
      prSufix.add("C");
    }
    if ("D".compareTo(pristupnost) >= 0) {
      prSufix.add("D");
    }

    Object[] fields = idoc.getFieldNames().toArray();
    for (Object f : fields) {
      String s = (String) f;
      
      SolrSearcher.addCommonFieldFacets(s, idoc, prSufix);
      
      if (indexFields.contains(s)) {
        for (String sufix : prSufix) {
          SolrSearcher.addFieldNonRepeat(idoc, "text_all_" + sufix, idoc.getFieldValues(s));
        }
      } 

    }

    // Fields allways searchable
    String[] defFields = new String[]{"ident_cely", "organizace",
      "vedouci_akce", "specifikace_data", "datum_zahajeni", "okres",
      "datum_ukonceni", "je_nz", "pristupnost"};
    for (String field : defFields) {
      SolrSearcher.addFieldNonRepeat(idoc, "text_all_A", idoc.getFieldValues(field));
      SolrSearcher.addFieldNonRepeat(idoc, "text_all_B", idoc.getFieldValues(field));
      SolrSearcher.addFieldNonRepeat(idoc, "text_all_C", idoc.getFieldValues(field));
    }
  }

  @Override
  public boolean isEntity() {
    return true;
  }
  
  @Override
  public void secondRound(HttpSolrClient client, SolrInputDocument idoc) {
  }

}
