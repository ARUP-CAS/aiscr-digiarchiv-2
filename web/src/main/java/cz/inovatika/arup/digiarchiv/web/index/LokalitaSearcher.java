/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class LokalitaSearcher implements EntitySearcher {

  public static final Logger LOGGER = Logger.getLogger(LokalitaSearcher.class.getName());

  final String ENTITY = "lokalita";
  
  private final List<String> allowedFields = Arrays.asList(new String[]{"ident_cely", "entity", "pristupnost", "okres","typ_lokality",
    "druh", "datum_zahajeni", "datum_ukonceni", "child_dokument", "organizace", "datestamp"});
  
  @Override
  public void filter(JSONObject jo, String pristupnost, String org) {
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      if (doc.getString("pristupnost").compareTo(pristupnost) > 0) {
        Object[] keys =  doc.keySet().toArray();
        for (Object key : keys) {
          if (!allowedFields.contains((String)key)) {
            doc.remove((String)key);
          }
          
        }
      }
    }
  }
  
  @Override
  public String[] getChildSearchFields(String pristupnost) {
    return new String[]{"ident_cely,entity,pristupnost,okres,typ_lokality,druh,pristupnost",
            "nazev:f_nazev_" + pristupnost, 
            "popis:f_popis_" + pristupnost, 
            "katastr:f_katastr_" + pristupnost,  
            "dalsi_katastry:f_dalsi_katastry_" + pristupnost};
  }
  
  @Override
  public void getChilds(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
    if (jo.has("response")) {
      JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
      for (int i = 0; i < ja.length(); i++) {
        JSONObject doc = ja.getJSONObject(i);
        if (LoginServlet.userId(request) != null) {
          SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
        }
        String fields = "ident_cely,entity,katastr,okres,autor,rok_vzniku,typ_dokumentu,material_originalu,pristupnost,rada,material_originalu,organizace,popis,soubor_filepath";
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
    try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
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
    try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
      SolrQuery query = new SolrQuery();
      setQuery(request, query);
      return SearchUtils.csv(query, client, "entities");
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return ex.toString();
    }
  }
  
  @Override
  public String[] getSearchFields(String pristupnost) {
    return new String[]{"*,dok_jednotka:[json],pian:[json],adb:[json],ext_zdroj:[json],dokument:[json]", "f_katastr:katastr",
            "nazev:f_nazev_" + pristupnost, 
            "popis:f_popis_" + pristupnost, 
            "katastr:f_katastr_" + pristupnost,  
            "dalsi_katastry:f_dalsi_katastry_" + pristupnost};
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
      query.setFields("ident_cely,entity,nazev,organizace,pristupnost,loc_rpt,pian:[json],katastr,okres,child_dokument");
    } else {
      query.setFields(getSearchFields(pristupnost));
     }
    
  }

}
