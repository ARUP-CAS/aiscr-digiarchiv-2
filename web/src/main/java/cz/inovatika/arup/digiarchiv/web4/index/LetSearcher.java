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
public class LetSearcher implements EntitySearcher {

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
    public void checkRelations(JSONObject jo, SolrClient client, HttpServletRequest request) {
    }

    @Override
    public String[] getRelationsFields() {
        return new String[]{"dokument", "projekt"};
    }

    @Override
    public void getChilds(JSONObject jo, SolrClient client, HttpServletRequest request) {
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
    public void processAsChild(HttpServletRequest request, JSONObject jo) {

    }

    @Override
    public JSONObject search(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        try (SolrClient client = new HttpJettySolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
            SolrQuery query = new SolrQuery()
                    .setFacet(true);
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

    @Override
    public JSONObject export(HttpServletRequest request) {
        return new JSONObject();
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
