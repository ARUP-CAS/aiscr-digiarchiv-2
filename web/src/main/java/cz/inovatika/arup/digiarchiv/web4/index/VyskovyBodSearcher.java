package cz.inovatika.arup.digiarchiv.web4.index;

import cz.inovatika.arup.digiarchiv.web4.LoginServlet;
import cz.inovatika.arup.digiarchiv.web4.Options;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.jetty.HttpJettySolrClient;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class VyskovyBodSearcher implements ComponentSearcher, EntitySearcher {

  public static final Logger LOGGER = Logger.getLogger(VyskovyBodSearcher.class.getName());

  final String ENTITY = "vyskovy_bod";
  private boolean parentSearchable;

  @Override
  public void getRelatedInHandle(JSONObject jo, SolrClient client, HttpServletRequest request) {

    String pristupnost = LoginServlet.pristupnost(request.getSession());
    if ("E".equals(pristupnost)) {
        pristupnost = "D";
    }
    String org = LoginServlet.organizace(request.getSession());
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    //String fields = "*";
    ADBSearcher as = new ADBSearcher();
    String[] adbFields = as.getChildSearchFields(pristupnost);
    String fields = String.join(",", adbFields);
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      if (doc.has("vyskovy_bod_parent")) {
        String p = doc.getString("vyskovy_bod_parent");
        JSONObject sub = SolrSearcher.getById(client, p, fields, false);
        if (sub != null) {
          as.filterOne(sub, pristupnost, org);
          doc.append(sub.getString("entity"), sub);
          doc.put("datestamp", sub.getString("datestamp"));
          parentSearchable = true;
        }
      }
    }
  }

  @Override
  public boolean isRelatedSearchable() {
    return parentSearchable;
  }

    @Override
    public void processAsChild(HttpServletRequest request, JSONObject jo) {

    }

    @Override
    public JSONObject search(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        try (SolrClient client = new HttpJettySolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
            SolrQuery query = new SolrQuery();
            setQuery(request, query);
            JSONObject jo = SearchUtils.json(query, client, "entities");
            SolrSearcher.addFavorites(jo, client, request);
            return jo;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            json.put("error", ex);
        }
        return json;
    }
    
    public void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
        SolrSearcher.addCommonParams(request, query, ENTITY);
        String pristupnost = LoginServlet.pristupnost(request.getSession());
        if ("E".equals(pristupnost)) {
            pristupnost = "D";
        }
        query.set("df", "text_all_" + pristupnost);

        SolrSearcher.addFilters(request, query, pristupnost);
    }

    @Override
    public JSONObject export(HttpServletRequest request) {
        return new JSONObject();
    }

    @Override
    public String[] getSearchFields(String pristupnost) {
        return new String[]{"*,vyskovy_bod_geom_wkt:[json]"};
    }

    @Override
    public void filter(JSONObject jo, String pristupnost, String org) {
        
    }

    @Override
    public void getChilds(JSONObject jo, SolrClient client, HttpServletRequest request) {
        
    }

    @Override
    public String[] getChildSearchFields(String pristupnost) {
        return getSearchFields(pristupnost);
    }

    @Override
    public String[] getRelationsFields() {
        return new String[]{"adb"};
    }

    @Override
    public void checkRelations(JSONObject jo, SolrClient client, HttpServletRequest request) {
        
    }

}
