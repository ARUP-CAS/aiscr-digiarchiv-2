/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web4.index;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public interface ComponentSearcher {
  public void getRelated(JSONObject jo, HttpJdkSolrClient client, HttpServletRequest request);
  public boolean isRelatedSearchable();
}
