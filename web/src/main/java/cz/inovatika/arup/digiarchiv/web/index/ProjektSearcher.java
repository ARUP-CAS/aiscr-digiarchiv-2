package cz.inovatika.arup.digiarchiv.web.index;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


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
public class ProjektSearcher implements EntitySearcher {

  public static final Logger LOGGER = Logger.getLogger(ProjektSearcher.class.getName());
  final String ENTITY = "projekt";
  
  @Override
  public String[] getChildSearchFields(String pristupnost) {
    return this.getSearchFields(pristupnost);
  }
  
  @Override
  public void getChilds(JSONObject jo, HttpSolrClient client, HttpServletRequest request) {
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      if (LoginServlet.userId(request) != null) {
        SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
      }
      String fields = "ident_cely,katastr,okres,vedouci_akce,specifikace_data,datum_zahajeni,datum_ukonceni,je_nz,pristupnost,organizace.dalsi_katastry,lokalizace,pian:[json]";
      SolrSearcher.addChildField(client, doc, "child_akce", "akce", fields);

      fields = "ident_cely,katastr,okres,nalezce,datum_nalezu,typ_dokumentu,material_originalu,rada,pristupnost,obdobi,presna_datace,druh,specifikace,soubor_filepath";
      SolrSearcher.addChildField(client, doc, "child_samostatny_nalez", "samostatny_nalez", fields);
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
  
  @Override
  public String[] getSearchFields(String pristupnost) {
    return new String[]{"*,akce:[json],pian:[json]","katastr","okres","f_katastr:katastr","f_okres:okres"};
  }

  public void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
    SolrSearcher.addCommonParams(request, query, ENTITY);
    String pristupnost = LoginServlet.pristupnost(request.getSession());
    if ("E".equals(pristupnost)) {
      pristupnost = "D";
    }
    query.set("df", "text_all_" + pristupnost);
    query.setFields("*,akce:[json],pian:[json]","katastr","okres","f_katastr:katastr","f_okres:okres");
    if (Boolean.parseBoolean(request.getParameter("mapa"))) {
      SolrSearcher.addLocationParams(request, query);
    }
    SolrSearcher.addFilters(request, query, pristupnost);
  }

}
