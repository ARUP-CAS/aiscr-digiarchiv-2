/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index.models;

import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class ExtZdroj implements Entity {

  static final Logger LOGGER = Logger.getLogger(ExtZdroj.class.getName());

  @Field
  public String ident_cely;

  @Field
  public int stav;

  @Field
  public String typ;

  @Field
  public String sysno;

  @Field
  public String autori;

  @Field
  public String nazev;

  @Field
  public String podnazev;

  @Field
  public String edice_rada;

  @Field
  public String rok_vydani_vzniku;

  @Field
  public String isbn;
  ;

  @Field
  public String issn;

  @Field
  public String vydavatel;

  @Field
  public String sbornik_editor;

  @Field
  public String sbornik_nazev;

  @Field
  public String misto;

  @Field
  public String casopis_denik_nazev;

  @Field
  public String casopis_rocnik;

  @Field
  public Date datum_rd;

  @Field
  public String paginace_titulu;

  @Field
  public String link;

  @Field
  public String typ_dokumentu;

  @Field
  public String organizace;

  @Field
  public String oznaceni;

  @Field
  public String poznamka;

  @Field
  public Date datum_vlozeni;

  @Field
  public String odpovedny_pracovnik_vlozeni;

  @Field
  public List<String> ext_odkaz;

  @Override
  public void fillFields(SolrInputDocument idoc) {
    idoc.setField("searchable", true);
    if (ext_odkaz != null) {
      for (String eo : ext_odkaz) {
        JSONObject json = new JSONObject(eo);
        addJSONFields(json, "odkaz", idoc);
      }
    }
  }

  @Override
  public void addRelations(HttpSolrClient client, SolrInputDocument idoc) {

  }

  @Override
  public void setFullText(SolrInputDocument idoc) {
  }

  @Override
  public boolean isEntity() {
    return true;
  }

  private void addJSONFields(JSONObject doc, String prefix, SolrInputDocument idoc) {
    for (String s : doc.keySet()) {
      switch (s) {
        case "_version_":
        case "_root_":
        case "indextime":
        case "externi_zdroj":
          break;
        default:
          SolrSearcher.addFieldNonRepeat(idoc, prefix + "_" + s, doc.optString(s));
      }
    }
  }
  
  @Override
  public void secondRound(HttpSolrClient client, SolrInputDocument idoc) {
  }

}
