/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index.models;

import com.alibaba.fastjson.JSON;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author alberto
 */
public interface Entity {
  public static <T> Entity parseJson(String record, Class<T> clazz){
    return (Entity) JSON.parseObject(record, clazz);
  };
  
  public void fillFields(SolrInputDocument idoc);
  public void addRelations(HttpSolrClient client, SolrInputDocument idoc);
  public void setFullText(SolrInputDocument idoc);
  public void secondRound(HttpSolrClient client, SolrInputDocument idoc);
  public boolean isEntity();
  
}
