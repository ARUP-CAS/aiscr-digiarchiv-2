/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index.models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class ADB implements Entity {

  static final Logger LOGGER = Logger.getLogger(ADB.class.getName());

  @Field
  public String ident_cely;

  @Field
  public String parent;
  
  @Field
  public String uzivatelske_oznaceni_sondy;

  @Field
  public String datum;

  @Field
  public String pristupnost;

  @Field
  public int akce_stav;

  @Field
  public String typ_sondy;

  @Field
  public String trat;

  @Field
  public String cislo_popisne;

  @Field
  public String parcelni_cislo;

  @Field
  public String podnet;

  @Field
  public String stratigraficke_jednotky;

  @Field
  public String autor_popisu;

  @Field
  public String rok_popisu;

  @Field
  public String autor_revize;

  @Field
  public int rok_revize;

  @Field
  public String poznamka;

  @Field
  public List<String> vyskovy_bod;

  @Override
  public void fillFields(SolrInputDocument idoc) {
    idoc.setField("searchable", true);
  }

  @Override
  public void addRelations(Http2SolrClient client, SolrInputDocument idoc) {
    if (vyskovy_bod != null) {
      try {
        // Musime indexovat jako entita
        List<SolrInputDocument> idocs = new ArrayList<>();
        for (String vb : vyskovy_bod) {
          SolrInputDocument vbdoc = new SolrInputDocument();
          JSONObject vbJson = new JSONObject(vb);
          vbdoc.setField("entity", "vyskovy_bod");
          vbdoc.setField("searchable", true);
          addJSONFields(vbJson, vbdoc);
          vbdoc.setField("parent", ident_cely);
          idocs.add(vbdoc);
          // addAsEntity(client, vbdoc, "vyskovy_bod");
        }
        client.add("entities", idocs);
      } catch (SolrServerException | IOException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
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

  @Override
  public void setFullText(SolrInputDocument idoc) {
  }
  
  @Override
  public boolean isEntity() {
    return true;
  }
  
  @Override
  public void secondRound(Http2SolrClient client, SolrInputDocument idoc) {
  }

}
