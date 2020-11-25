/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index.models;

import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class DokJednotka implements Entity {
  
  static final Logger LOGGER = Logger.getLogger(DokJednotka.class.getName());
  
  @Field
  public String ident_cely;
  
  @Field
  public String parent_lokalita;
  
  @Field
  public String parent_akce;
  
  @Field
  public String lokalita_stav;
  
  @Field
  public String akce_stav;
  
  @Field
  public String pristupnost;
  
  @Field
  public String nazev;
  
  @Field
  public String typ;
  
  @Field
  public String pian;
  
  @Field
  public String datum;
  
  @Field
  public int negativni_jednotka;
  
  @Field
  public String child_adb;
  
  @Field
  public List<String> komponenta;

  @Override
  public void fillFields(SolrInputDocument idoc) {
    idoc.setField("searchable", true);
  }
  
  @Override
  public void addRelations(HttpSolrClient client, SolrInputDocument idoc) {
    if (komponenta != null) {
      try {
        // Musime indexovat jako entita
        List<SolrInputDocument> idocs = new ArrayList<>();
        for (String s : komponenta) {
          SolrInputDocument vbdoc = new SolrInputDocument();
          JSONObject vbJson = new JSONObject(s);
          vbdoc.setField("entity", "komponenta");
          vbdoc.setField("searchable", true);
          addJSONFields(vbJson, vbdoc);
          idocs.add(vbdoc);
        }
        client.add("entities", idocs);
      } catch (SolrServerException | IOException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
    }
    if (pian != null) {
      addPian(client, idoc);
    }
    
    if (child_adb != null) {
      addADB(client, idoc);
    }
  }
  
  private void addADB(HttpSolrClient client, SolrInputDocument idoc) {
    SolrQuery query = new SolrQuery("ident_cely:\"" + child_adb + "\"")
            .setFields("*,vyskovy_bod:[json]");
    JSONObject json = SearchUtils.json(query, client, "entities");
    if (json.getJSONObject("response").getInt("numFound") > 0) {
      idoc.setField("adb", json.getJSONObject("response").getJSONArray("docs").getJSONObject(0).toString());
    }
  }
  
  private void addPian(HttpSolrClient client, SolrInputDocument idoc) {
    SolrQuery query = new SolrQuery("ident_cely:\"" + pian + "\"")
            .setFields("ident_cely,centroid_n,centroid_e,typ,presnost,zm10,pristupnost")
            .addFilterQuery("entity:pian");
    JSONObject json = SearchUtils.json(query, client, "entities");
    if (json.getJSONObject("response").getInt("numFound") > 0) {
      JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
      if (doc.has("centroid_n")) {
          String loc = doc.optString("centroid_n") + "," + doc.optString("centroid_e");
          SolrSearcher.addFieldNonRepeat(idoc, "loc", loc);
          SolrSearcher.addFieldNonRepeat(idoc, "loc_rpt", loc);
      }
      idoc.setField("pian", doc.toString());
    } else {
      idoc.setField("pian", "{}");
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
          SolrSearcher.addFieldNonRepeat(idoc, s, doc.optString(s));
      }
    }
  }

  @Override
  public void setFullText(SolrInputDocument idoc) {
  }
  
  @Override
  public boolean isSearchable() {
    return true;
  }
  
  @Override
  public void secondRound(HttpSolrClient client, SolrInputDocument idoc) {
  }
  
}
