/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index.models;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author alberto
 */
public class Pian implements Entity {

  static final Logger LOGGER = Logger.getLogger(Pian.class.getName());

  @Field
  public String ident_cely;

  @Field
  public String typ;

  @Field
  public int presnost;

  @Field
  public String pristupnost;

  @Field
  public String zm10;

  @Field
  public String vymezil;

  @Field
  public Date datum_vymezeni;

  @Field
  public String potvrdil;

  @Field
  public Date datum_potvrzeni;

  @Field
  public String geom_gml;

  @Field
  public String geom_wkt;

  @Field
  public String centroid_e;

  @Field
  public String centroid_n;

  @Field
  public List<String> child_dok_jednotka;

  @Field
  public String loc;

  @Field
  public String loc_rpt;

  @Override
  public void fillFields(SolrInputDocument idoc) {
    idoc.setField("searchable", true);
    if (this.centroid_n != null) {
      this.loc = this.centroid_n + "," + this.centroid_e;
      this.loc_rpt = this.loc;
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

  @Override
  public void secondRound(HttpSolrClient client, SolrInputDocument idoc) {
  }
}
