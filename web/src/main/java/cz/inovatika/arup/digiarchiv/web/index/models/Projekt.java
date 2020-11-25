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
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
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
public class Projekt implements Entity {

  static final Logger LOGGER = Logger.getLogger(Projekt.class.getName());

  @Field
  public String ident_cely;

  @Field
  public int stav;

  @Field
  public String typ_projektu;

  @Field
  public String datetime_born;

  @Field
  public String okres;

  @Field
  public String katastr;

  @Field
  public String dalsi_katastry;

  @Field
  public String podnet;

  @Field
  public String lokalita;

  @Field
  public String parcelni_cislo;

  @Field
  public String planovane_zahajeni;

  @Field
  public String objednatel;

  @Field
  public String odpovedna_osoba;

  @Field
  public String adresa;

  @Field
  public String telefon;

  @Field
  public String email;

  @Field
  public String vedouci_projektu;

  @Field
  public String organizace_zapisu;

  @Field
  public String organizace_prihlaseni;

  @Field
  public String uzivatelske_oznaceni;

  @Field
  public String kulturni_pamatka;

  @Field
  public String nkp_cislo;

  @Field
  public String nkp_popis;

  @Field
  public Date datum_zahajeni;

  @Field
  public Date datum_ukonceni;

  @Field
  public Date datum_zapisu;

  @Field
  public String odpovedny_pracovnik_zapisu;

  @Field
  public Date datum_prihlaseni;

  @Field
  public String odpovedny_pracovnik_prihlaseni;

  @Field
  public Date datum_zapisu_zahajeni;

  @Field
  public String odpovedny_pracovnik_zahajeni;

  @Field
  public Date datum_zapisu_ukonceni;

  @Field
  public String odpovedny_pracovnik_ukonceni;

  @Field
  public Date datum_navrhu_archivace;

  @Field
  public String odpovedny_pracovnik_navrhu_archivace;

  @Field
  public Date datum_archivace;

  @Field
  public String odpovedny_pracovnik_archivace;

  @Field
  public Date termin_odevzdani_nz;

  @Field
  public String geometry_e;

  @Field
  public String geometry_n;

  @Field
  public String loc;

  @Field
  public String loc_rpt;

  @Field
  public List<String> child_akce;

  @Field
  public List<String> child_samostatny_nalez;

  @Field
  public String pristupnost;
  
  String[] facetFields = new String[]{"komponenta_areal","komponenta_obdobi","f_aktivita","nalez_kategorie","nalez_druh_nalezu","nalez_specifikace","f_typ_vyzkumu"};

  @Override
  public void fillFields(SolrInputDocument idoc) {

    if (this.geometry_n != null) {
      this.loc = this.geometry_n + "," + this.geometry_e;
              SolrSearcher.addFieldNonRepeat(idoc, "lat", this.geometry_n);
              SolrSearcher.addFieldNonRepeat(idoc, "lng", this.geometry_e);
      SolrSearcher.addFieldNonRepeat(idoc, "loc", this.loc);
      SolrSearcher.addFieldNonRepeat(idoc, "loc_rpt", this.loc);
    }

    if (organizace_prihlaseni != null) {
      SolrSearcher.addFieldNonRepeat(idoc, "organizace_sort", organizace_prihlaseni);
    }
    if (vedouci_projektu != null) {
      SolrSearcher.addFieldNonRepeat(idoc, "autor_sort", vedouci_projektu);
    }
    if (okres != null) {
      String okres_sort = okres;
      if (katastr != null) {
        okres_sort += " " + katastr;
      }
      SolrSearcher.addFieldNonRepeat(idoc, "okres_sort", okres_sort);
    }

    if (dalsi_katastry != null) {
      String[] kats = dalsi_katastry.split(";");
      for (String kat : kats) {
        String[] parts = kat.split("\\(");
        String kt = parts[0];
        String ok = parts[1].substring(0, parts[1].length()-1);
        
        SolrSearcher.addFieldNonRepeat(idoc, "dalsi_katastr", kt.trim());
        SolrSearcher.addFieldNonRepeat(idoc, "dalsi_okres",ok.trim());
      }
      idoc.setField("dalsi_katastry", kats);
    } 

    if (datum_zahajeni != null) {
      SolrSearcher.addFieldNonRepeat(idoc, "datum_provedeni_od", datum_zahajeni);
      String ukonceni = "*";
      if (datum_ukonceni != null) {
        SolrSearcher.addFieldNonRepeat(idoc, "datum_provedeni_do", datum_ukonceni);
        if (datum_ukonceni.after(datum_zahajeni)) {
          ukonceni = datum_ukonceni.toInstant().toString();
        }
      }
      SolrSearcher.addFieldNonRepeat(idoc, "datum_provedeni", "[" + datum_zahajeni.toInstant().toString() + " TO " + ukonceni + "]");
    }
  }

  @Override
  public void addRelations(HttpSolrClient client, SolrInputDocument idoc) {
    idoc.setField("searchable", false);
    boolean searchable = false;
    pristupnost = "A";
    if (this.child_akce != null) {
      searchable = addAkce(client, idoc);
    }
    if (!searchable && child_samostatny_nalez != null) {
      searchable = samostatnyNalez(client);
    } 
    idoc.setField("pristupnost", pristupnost);
    idoc.setField("searchable", searchable);
  }
  
  private boolean samostatnyNalez(HttpSolrClient client) {
    for (String sn : this.child_samostatny_nalez) {
      SolrQuery query = new SolrQuery("ident_cely:\"" + sn + "\"")
              .setRequestHandler("/search");
      JSONObject json = SearchUtils.json(query, client, "entities");
      if (json.getJSONObject("response").getInt("numFound") > 0) {
        JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
        if (pristupnost.compareTo(doc.getString("pristupnost")) < 0) {
          pristupnost = doc.getString("pristupnost");
        }
        return true;
      }
    }
    return false;
  }

  private boolean addAkce(HttpSolrClient client, SolrInputDocument idoc) {
    boolean searchable = false;
     
    for (String akce : this.child_akce) {
      SolrQuery query = new SolrQuery("ident_cely:\"" + akce + "\"")
              .setFields("pian:[json],pian_id,pristupnost")
              .setRequestHandler("/search");
      for (String f : facetFields) {
        query.addField(f);
      }
      JSONObject json = SearchUtils.json(query, client, "entities");

      if (json.getJSONObject("response").getInt("numFound") > 0) {
        searchable = true;
        JSONArray docs = json.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < docs.length(); i++) {
          
          JSONObject doc = docs.getJSONObject(i); 
          // SolrSearcher.addFieldNonRepeat(idoc, "akce", doc.toString());
          
          if (pristupnost.compareTo(doc.getString("pristupnost")) < 0) {
            pristupnost = doc.getString("pristupnost");
          }
          for (String f : facetFields) {
            if (doc.has(f)) {
              SolrSearcher.addFieldNonRepeat(idoc, f, doc.get(f));
            } 
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
              SolrSearcher.addFieldNonRepeat(idoc, "loc_rpt", loc);
            }
            SolrSearcher.addFieldNonRepeat(idoc, "pian", pianDoc.toString());

          }
        }
      }
    }
    return searchable;
  }

  @Override
  public void setFullText(SolrInputDocument idoc) {
    
    List<Object> indexFields = Options.getInstance().getJSONObject("indexFieldsByType").getJSONArray("projekt").toList();
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
  }

  @Override
  public boolean isSearchable() {
    return true;
  }
  
  @Override
  public void secondRound(HttpSolrClient client, SolrInputDocument idoc) {
  }
}
