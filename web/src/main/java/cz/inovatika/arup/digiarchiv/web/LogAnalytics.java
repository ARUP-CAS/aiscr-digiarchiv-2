package cz.inovatika.arup.digiarchiv.web;

import java.io.IOException;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.JsonMapResponseParser;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.json.JSONObject;

/**
 *
 * @author alber
 */
public class LogAnalytics {

    public static final Logger LOGGER = Logger.getLogger(LogAnalytics.class.getName());

    public static void log(HttpServletRequest request, String ident_cely, String type) {
        try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
            JSONObject user = LoginServlet.user(request);
            String ip = request.getRemoteAddr();
            SolrInputDocument idoc = new SolrInputDocument();
            idoc.addField("id", ident_cely + "_" + Instant.now().toEpochMilli());
            idoc.addField("ident_cely", ident_cely);
            idoc.addField("user", user.optString("ident_cely", "anonym"));
            idoc.addField("ip", ip);
            idoc.addField("type", type);
            LOGGER.log(Level.FINE, "user:{0}; ip:{1}; ident_cely: {2}; type: {3}",
                    new String[]{user.optString("ident_cely", "anonym"),
                        ip, ident_cely, type});
            client.add("logs", idoc, 1000);  
        } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    public static JSONObject stats(HttpServletRequest request) {
            NoOpResponseParser dontMessWithSolr = new NoOpResponseParser();
            dontMessWithSolr.setWriterType("json");
        try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost"))
                .withResponseParser(dontMessWithSolr).build()) {
            // request.getParameter("id"), request.getParameter("type")
            SolrQuery query = new SolrQuery()
                    .setQuery("ident_cely:\"" + request.getParameter("ident_cely") + "\"")
                    .setFacet(true)
                    .setFacetMinCount(1)
                    .addFacetField("user")
                    .addFacetField("ip")
                    .addFacetField("type");
            JSONObject ret = json(query, client, "logs");
            // client.close();
            return ret;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return new JSONObject().put("error", ex);
        }
    }
    
    public static JSONObject json(SolrQuery query, Http2SolrClient client, String core) { 
        query.setRequestHandler("/select");
        String qt = query.get("qt");
        query.set("wt", "json");
        String jsonResponse;
        try {
            QueryRequest qreq = new QueryRequest(query);
            if (qt != null) {
                qreq.setPath(qt);
            }
            NamedList<Object> qresp = client.request(qreq, core);
            jsonResponse = (String) qresp.get("response");
            return new JSONObject(jsonResponse);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return new JSONObject().put("error", ex);
        }
    }
}
