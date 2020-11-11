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
public class ExtZdrojSearcher implements EntitySearcher{
  
  public static final Logger LOGGER = Logger.getLogger(ExtZdrojSearcher.class.getName());
  
  final String ENTITY = "ext_zdroj";
  
  @Override
  public void getChilds(JSONObject jo, HttpSolrClient client, HttpServletRequest request) {
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      if (LoginServlet.userId(request) != null) {
        SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
      }
      String fields = "ident_cely,katastr,okres,vedouci_akce,specifikace_data,datum_zahajeni,datum_ukonceni,je_nz,pristupnost,organizace,dalsi_katastry,lokalizace";
      SolrSearcher.addChildField(client, doc, "odkaz_vazba_akce", "akce", fields);
      fields = "ident_cely,katastr,okres,nazev,typ_lokality,druh,pristupnost,dalsi_katastry,popis";
      SolrSearcher.addChildField(client, doc, "odkaz_vazba_lokalita", "lokalita", fields);
    }
  }

  @Override
  public JSONObject search(HttpServletRequest request) {
    JSONObject json = new JSONObject();
    try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
      String handler = LoginServlet.isLogged(request.getSession()) ? "/search" : "/search";
      SolrQuery query = new SolrQuery()
              .setFacet(true)
              .setRequestHandler(handler);
      setQuery(request, query);
      JSONObject jo = SearchUtils.json(query, client, "entities");
      getChilds(jo, client, request);
      return jo;

    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      json.put("error", ex);
    }
    return json;
  }

  public void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
    SolrSearcher.addCommonParams(request, query);
    query.addFilterQuery("{!tag=entityF}entity:" + ENTITY);
    String pristupnost = LoginServlet.pristupnost(request.getSession());
    if ("E".equals(pristupnost)) {
      pristupnost = "D";
    }
    query.set("df", "text_all_" + pristupnost);
    if (Boolean.parseBoolean(request.getParameter("mapa"))) {
      SolrSearcher.addLocationParams(request, query);
    }
    
    SolrSearcher.addFilters(request, query, pristupnost);
  }
  
}
