package cz.inovatika.arup.digiarchiv.web;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.json.JSONObject;

/**
 *
 * @author alber
 */
public class LogAnalytics {

    public static final Logger LOGGER = Logger.getLogger(LogAnalytics.class.getName());

    public static void log(HttpServletRequest request, String ident_cely, String type, String entity) {
        try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
            JSONObject user = LoginServlet.user(request);
            String ip = request.getRemoteAddr();
            SolrInputDocument idoc = new SolrInputDocument();
            idoc.addField("id", ident_cely + "_" + Instant.now().toEpochMilli());
            idoc.addField("ident_cely", ident_cely);
            idoc.addField("user", user.optString("ident_cely", "anonym"));
            idoc.addField("ip", ip);
            idoc.addField("type", type);
            idoc.addField("entity", entity);
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
                    .setQuery("*")
                    .setFacet(true)
                    .setRows(0)
                    .setFacetMinCount(1)
                    .addFacetField("user")
                    .addFacetField("ip")
                    .addFacetField("type")
                    .addFacetField("{!ex=entityF}entity")
                    .addFacetField("ident_cely")
                    .setParam("json.nl", "arrntv")
                    .setParam("facet.range", "indextime")
                    .setParam("f.indextime.facet.range.start", "NOW/YEAR-1YEAR")
                    .setParam("f.indextime.facet.range.end", "NOW")
                    .setParam("f.indextime.facet.range.gap", "+1DAY");

            if (request.getParameter("interval") != null) {
                String gap = request.getParameter("interval");
                if ("WEEK".equals(gap)) {
                    gap = "7DAYS";
                } else {
                    gap = "1" + gap;
                }
                query.setParam("f.indextime.facet.range.gap", "+" + gap);
            } 

            if (request.getParameter("ident_cely") != null) {
                query.addFilterQuery("ident_cely:" + request.getParameter("ident_cely").replaceAll("-", "\\-") + "");
            }
            if (request.getParameter("type") != null) {
                query.addFilterQuery("type:\"" + request.getParameter("type") + "\"");
            }
            if (request.getParameter("user") != null) {
                query.addFilterQuery("user:" + request.getParameter("user").replaceAll("-", "\\-") + "");
            }
            if (request.getParameter("ip") != null) {
                query.addFilterQuery("ip:" + request.getParameter("ip") + "");
            }
            if (request.getParameter("date") != null) {
                String[] parts = request.getParameter("date").split(",");
                String from = parts[0];
                if ("null".equals(from)) {
                    from = "*";
                } else {
                    from = from + "T00:00:00Z";
                }
                String to = parts[1];
                if ("null".equals(to)) {
                    to = "*";
                } else {
                    to = to + "T23:59:59Z";
                }
                String fq = "indextime:[" + from + " TO " + to + "]";
                query.addFilterQuery(fq);
            }

            if (request.getParameter("entity") != null) {
                // query.addFilterQuery("{!join fromIndex=entities to=ident_cely from=ident_cely}entity:\"" + request.getParameter("entity") + "\"");
                query.addFilterQuery("{!tag=entityF}entity:\"" + request.getParameter("entity") + "\"");
            }

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
