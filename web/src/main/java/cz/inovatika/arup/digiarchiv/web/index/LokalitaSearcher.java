/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class LokalitaSearcher implements EntitySearcher {

  public static final Logger LOGGER = Logger.getLogger(LokalitaSearcher.class.getName());

  final String ENTITY = "lokalita";
  
  @Override
  public void getChilds(JSONObject jo, HttpSolrClient client, HttpServletRequest request) {
    if (jo.has("response")) {
      JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
      for (int i = 0; i < ja.length(); i++) {
        JSONObject doc = ja.getJSONObject(i);
        if (LoginServlet.userId(request) != null) {
          SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
        }
        String fields = "ident_cely,katastr,okres,autor,rok_vzniku,typ_dokumentu,material_originalu,pristupnost,rada,material_originalu,organizace,popis,soubor_filepath";
        SolrSearcher.addChildField(client, doc, "child_dokument", "dokument", fields);
      }
    } else {
        JSONObject doc = jo.getJSONObject("doc");
        if (LoginServlet.userId(request) != null) {
          SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
        }
        String fields = "ident_cely,katastr,okres,autor,rok_vzniku,typ_dokumentu,material_originalu,pristupnost,rada,material_originalu,organizace,popis,soubor_filepath";
        SolrSearcher.addChildField(client, doc, "child_dokument", "dokument", fields);
    }
  }

  @Override
  public JSONObject search(HttpServletRequest request) {
    JSONObject json = new JSONObject();
    try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
      SolrQuery query = new SolrQuery();
      setQuery(request, query);
      JSONObject jo = SearchUtils.json(query, client, "entities");
      SolrSearcher.addFavorites(jo, client, request);
      return jo;
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      json.put("error", ex);
    }
    return json;
  }
  

  @Override
  public String export(HttpServletRequest request) {
    try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
      SolrQuery query = new SolrQuery();
      setQuery(request, query);
      return SearchUtils.csv(query, client, "entities");
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return ex.toString();
    }
  }

  public void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
    SolrSearcher.addCommonParams(request, query, ENTITY);
    // query.addFilterQuery("{!tag=entityF}stav:3");
    String pristupnost = LoginServlet.pristupnost(request.getSession());
    if ("E".equals(pristupnost)) {
      pristupnost = "D";
    }
    query.set("df", "text_all_" + pristupnost);
               
    SolrSearcher.addFilters(request, query, pristupnost);
    
    if (Boolean.parseBoolean(request.getParameter("mapa"))) {
      SolrSearcher.addLocationParams(request, query);
    } 
    
    if (Boolean.parseBoolean(request.getParameter("mapa")) && request.getParameter("format") == null) {
      query.setFields("ident_cely,entity,nazev,organizace,pristupnost,loc_rpt,pian:[json],katastr,okres");
    } else {
      query.setFields("*,dok_jednotka:[json],pian:[json],adb:[json],ext_zdroj:[json],dokument:[json]","katastr", "f_katastr:katastr");
     }
    
  }

}
