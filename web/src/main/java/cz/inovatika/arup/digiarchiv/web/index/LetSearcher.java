package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;
import static cz.inovatika.arup.digiarchiv.web.index.LokalitaSearcher.LOGGER;
import java.io.IOException;
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
public class LetSearcher implements EntitySearcher{
  
  public static final Logger LOGGER = Logger.getLogger(LetSearcher.class.getName());
  
  final String ENTITY = "let";
  
  @Override
  public void filter(JSONObject jo, String pristupnost, String org) {
    
  }
  
  @Override
  public String[] getChildSearchFields(String pristupnost) {
    return this.getSearchFields(pristupnost);
  }

    @Override
    public void checkRelations(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
    }

    @Override
    public String[] getRelationsFields() {
        return new String[]{"dokument", "projekt"};
    }
  
  @Override
  public void getChilds(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      if (LoginServlet.userId(request) != null) {
        SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
      }
      DokumentSearcher ds = new DokumentSearcher("dokument");
      String dfs = String.join(",", ds.getChildSearchFields("D"));
      SolrSearcher.addChildField(client, doc, "let_dokument", "dokument", dfs);
      
    }
  }

  @Override
  public JSONObject search(HttpServletRequest request) {
    JSONObject json = new JSONObject();
            try {            Http2SolrClient client = IndexUtils.getClientNoOp();
      SolrQuery query = new SolrQuery()
              .setFacet(true);
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
            try {            Http2SolrClient client = IndexUtils.getClientNoOp();
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
    return new String[]{"*"};
  }

  public void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
    SolrSearcher.addCommonParams(request, query, ENTITY);
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
