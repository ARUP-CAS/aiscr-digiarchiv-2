/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index.models;

import java.util.Date;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author alberto
 */
public class Soubor implements Entity {

  static final Logger LOGGER = Logger.getLogger(Soubor.class.getName());

  @Field
  public String filepath;

  @Field
  public Date vytvoreno;
  
  @Field
  public String dokument;
  
  @Field
  public String samostatny_nalez;
  
  @Field
  public String projekt;
  
  @Field
  public String nazev;
  
  @Field
  public String uzivatelske_oznaceni;
  
  @Field
  public int stav;
  
  @Field
  public String mimetype;
  
  @Field
  public int rozsah;
  
  @Field
  public int size_bytes;

  @Override
  public void fillFields(SolrInputDocument idoc) {
  }
  
  @Override
  public void addRelations(HttpSolrClient client, SolrInputDocument idoc) {
  }


  @Override
  public void setFullText(SolrInputDocument idoc) {
  }
  
  @Override
  public boolean isEntity() {
    return false;
  }
  
  @Override
  public void secondRound(HttpSolrClient client, SolrInputDocument idoc) {
  }
}
