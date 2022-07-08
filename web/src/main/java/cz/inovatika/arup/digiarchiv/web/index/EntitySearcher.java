/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index;

import javax.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public interface EntitySearcher {
  public JSONObject search(HttpServletRequest request);
  public String export(HttpServletRequest request);
  public String[] getSearchFields(String pristupnost);
  public void getChilds(JSONObject jo, HttpSolrClient client, HttpServletRequest request);
  public String[] getChildSearchFields(String pristupnost);
}
