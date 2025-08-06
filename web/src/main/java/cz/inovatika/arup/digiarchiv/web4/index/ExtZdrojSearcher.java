package cz.inovatika.arup.digiarchiv.web4.index;

import cz.inovatika.arup.digiarchiv.web4.LoginServlet;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class ExtZdrojSearcher implements EntitySearcher {

    public static final Logger LOGGER = Logger.getLogger(ExtZdrojSearcher.class.getName());

    final String ENTITY = "ext_zdroj";

    @Override
    public void filter(JSONObject jo, String pristupnost, String org) {

    }

    @Override
    public String[] getChildSearchFields(String pristupnost) {
        return this.getSearchFields(pristupnost);
    }

    @Override
    public void checkRelations(JSONObject jo, HttpJdkSolrClient client, HttpServletRequest request) {
    }

    @Override
    public String[] getRelationsFields() {
        return new String[]{"dokument", "projekt"};
    }

    @Override
    public void getChilds(JSONObject jo, HttpJdkSolrClient client, HttpServletRequest request) {
//        String pristupnost = LoginServlet.pristupnost(request.getSession());
//        if ("E".equals(pristupnost)) {
//            pristupnost = "D";
//        }
//
//        AkceSearcher as = new AkceSearcher();
//        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
//        for (int i = 0; i < ja.length(); i++) {
//            try {
//                JSONObject doc = ja.getJSONObject(i);
//                SolrQuery query = new SolrQuery("*")
//                        .addFilterQuery("az_ext_zdroj:\"" + doc.getString("ident_cely") + "\"")
//                        .setFields(as.getChildSearchFields(pristupnost))
//                        .setRows(1000);
//                JSONObject r = SolrSearcher.json(client, "entities", query);
//                JSONArray cdjs = r.getJSONObject("response").getJSONArray("docs");
//                for (int j = 0; j < cdjs.length(); j++) {
//                    JSONObject cdj = cdjs.getJSONObject(j);
//                    doc.append(cdj.getString("entity"), cdj);
//                }
//            } catch (SolrServerException ex) {
//                Logger.getLogger(ExtZdrojSearcher.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (IOException ex) {
//                Logger.getLogger(ExtZdrojSearcher.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
    }

    @Override
    public void processAsChild(HttpServletRequest request, JSONObject jo) {

    }

    @Override
    public JSONObject search(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        try {
            HttpJdkSolrClient client = IndexUtils.getClientNoOp();
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
        try {
            HttpJdkSolrClient client = IndexUtils.getClientNoOp();
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
        return new String[]{"*,ext_zdroj_ext_odkaz:[json]"};
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
