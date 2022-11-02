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
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class SamostatniNalez implements Entity {

  static final Logger LOGGER = Logger.getLogger(SamostatniNalez.class.getName());

  @Field
  public String ident_cely;

  @Field
  public int stav;

  @Field
  public String typ;

  @Field
  public String inv_cislo;

  @Field
  public String projekt_id;

  @Field
  public String okres;

  @Field
  public String katastr;

  @Field
  public String lokalizace;

  @Field
  public String centroid_e;

  @Field
  public String centroid_n;

//  @Field
//  public String geom_gml;
  @Field
  public int hloubka;

  @Field
  public String poznamka;

  @Field
  public String nalezove_okolnosti;

  @Field
  public String pristupnost;

  @Field
  public String obdobi;

  @Field
  public String presna_datace;

  @Field
  public String druh;

  @Field
  public String specifikace;

  @Field
  public String pocet;

  @Field
  public String nalezce;

  @Field
  public Date datum_nalezu;

  @Field
  public String predano;

  @Field
  public String predano_organizace;

  @Field
  public String odpovedny_pracovnik_vlozeni;

  @Field
  public Date datum_vlozeni;

  @Field
  public String odpovedny_pracovnik_archivace;

  @Field
  public Date datum_archivace;

  @Field
  public List<String> child_soubor;

  String[] facetFields = new String[]{"komponenta_areal", "f_aktivita", "nalez_kategorie"};

  List<String> prSufixAll = new ArrayList<>();

  @Override
  public void fillFields(SolrInputDocument idoc) {

    prSufixAll.add("A");
    prSufixAll.add("B");
    prSufixAll.add("C");
    prSufixAll.add("D");
    boolean searchable = stav == 4;
    idoc.setField("searchable", searchable);

    if (nalezce != null) {
      SolrSearcher.addFieldNonRepeat(idoc, "autor_sort", nalezce);
    }
    if (druh != null) {
      SolrSearcher.addFieldNonRepeat(idoc, "druh_nalezu", druh);
    }
    if (okres != null) {
      String okres_sort = okres;
      if (katastr != null) {
        okres_sort += " " + katastr;
        SolrSearcher.addFieldNonRepeat(idoc, "katastr_sort", katastr);
      }
      SolrSearcher.addFieldNonRepeat(idoc, "okres_sort", okres_sort);
    }
  }

  @Override
  public void addRelations(HttpSolrClient client, SolrInputDocument idoc) {
    if (this.child_soubor != null) {
      addSoubor(client, idoc);
    }
    if (idoc.containsKey("obdobi") && idoc.getFieldValue("obdobi") != null) {
      SolrSearcher.addFieldNonRepeat(idoc, "obdobi_poradi", SearchUtils.getObdobiPoradi((String) idoc.getFieldValue("obdobi")));
    }
    if (druh != null) {
      addKategorie(client, idoc);
    }

    // addPian(client, idoc);
  }

  private void addKategorie(HttpSolrClient client, SolrInputDocument idoc) {
      SolrQuery query = new SolrQuery("heslo:\"" + druh + "\"")
              .addFilterQuery("heslar:predmet_druh")
              .setFields("predmet_kategorie");
      JSONObject json = SearchUtils.json(query, client, "translations");
      if (json.getJSONObject("response").getInt("numFound") > 0) {
        String predmet_kategorie = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("predmet_kategorie");
        SolrSearcher.addFieldNonRepeat(idoc, "predmet_kategorie", predmet_kategorie);
      }
  }

  private void addSoubor(HttpSolrClient client, SolrInputDocument idoc) {
    for (int i = 0; i < child_soubor.size(); i++) {
      String soubor = this.child_soubor.get(i);
//      SolrQuery query = new SolrQuery("samostatny_nalez:\"" + this.ident_cely + "\"")
//              .setFields("filepath,nazev,uzivatelske_oznaceni,mimetype,rozsah,"
//                      + "size_bytes,vytvoreno,stav,dokument,projekt,samostatny_nalez");
      SolrQuery query = new SolrQuery("filepath:\"" + soubor + "\"")
              .setFields("filepath,nazev,uzivatelske_oznaceni,mimetype,rozsah,"
                      + "size_bytes,vytvoreno,stav,dokument,projekt,samostatny_nalez");
      JSONObject json = SearchUtils.json(query, client, "soubor");
      if (json.getJSONObject("response").getInt("numFound") > 0) {
        JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
        SolrSearcher.addFieldNonRepeat(idoc, "soubor", doc.toString());
        addJSONFields(doc, "soubor", idoc);
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
        default:
          SolrSearcher.addFieldNonRepeat(idoc, prefix + "_" + s, doc.optString(s));
      }
    }
  }

  @Override
  public void setFullText(SolrInputDocument idoc) {
    List<Object> indexFields = Options.getInstance().getJSONObject("indexFieldsByType").getJSONArray("samostatny_nalez").toList();

    String pristupnost = (String) idoc.getFieldValue("pristupnost");
    List<String> prSufix = new ArrayList<>();

    if ("A".compareTo(pristupnost) == 0) {
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

      if (s.equals("katastr")) {
        SolrSearcher.addSecuredFieldFacets(s, idoc, prSufix);
      } else {
        SolrSearcher.addSecuredFieldFacets(s, idoc, prSufixAll);
      }

      if (indexFields.contains(s)) {
        for (String sufix : prSufixAll) {
        //for (String sufix : prSufix) {
          SolrSearcher.addFieldNonRepeat(idoc, "text_all_" + sufix, idoc.getFieldValues(s));
        }
      }
    }

    if (this.centroid_n != null) {
      String loc = this.centroid_n + "," + this.centroid_e;
      SolrSearcher.addSecuredFieldNonRepeat(idoc, "lat", this.centroid_n, prSufix);
      SolrSearcher.addSecuredFieldNonRepeat(idoc, "lng", this.centroid_e, prSufix);
      SolrSearcher.addSecuredFieldNonRepeat(idoc, "loc", loc, prSufix);
      SolrSearcher.addSecuredFieldNonRepeat(idoc, "loc_rpt", loc, prSufix);
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
