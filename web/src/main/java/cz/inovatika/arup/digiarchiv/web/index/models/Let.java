/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index.models;

import com.alibaba.fastjson.annotation.JSONField;
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
public class Let implements Entity {

  static final Logger LOGGER = Logger.getLogger(Let.class.getName());

  @Field
  public String ident_cely;

  // @JSONField(format = "yyyy-MM-dd")  
  @Field
  public Date datum;
  
  @Field
  public String hodina_zacatek;
  
  @Field
  public String hodina_konec;
  
  @Field
  public String pozorovatel;
  
  @Field
  public String organizace;
  
  @Field
  public String fotoaparat;
  
  @Field
  public String pilot;
  
  @Field
  public String typ_letounu;
  
  @Field
  public String ucel_letu;
  
  @Field
  public String letiste_start;
  
  @Field
  public String letiste_cil;
  
  @Field
  public String pocasi;
  
  @Field
  public String dohlednost;
  
  @Field
  public String uzivatelske_oznaceni;
  
  @Field
  public List<String> child_dokument;
  
  @Override
  public void fillFields(SolrInputDocument idoc) {
    idoc.setField("searchable", true);
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
