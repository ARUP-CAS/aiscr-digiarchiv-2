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
public interface EntitySearcher {
  public JSONObject search(HttpServletRequest request);
  public String export(HttpServletRequest request);
  public String[] getSearchFields(String pristupnost);
  public void filter(JSONObject jo, String pristupnost, String org);
  public void processAsChild(HttpServletRequest request, JSONObject jo);
  public void getChilds(JSONObject jo, HttpJdkSolrClient client, HttpServletRequest request);
  public String[] getChildSearchFields(String pristupnost);
  public String[] getRelationsFields();
  public void checkRelations(JSONObject jo, HttpJdkSolrClient client, HttpServletRequest request);
}
