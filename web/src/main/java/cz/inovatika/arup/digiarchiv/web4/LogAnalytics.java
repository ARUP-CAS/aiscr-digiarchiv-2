package cz.inovatika.arup.digiarchiv.web4;

import cz.inovatika.arup.digiarchiv.web4.index.SolrClientFactory;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrClient;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CursorMarkParams; 
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author alber
 */
public class LogAnalytics {

    public static final Logger LOGGER = Logger.getLogger(LogAnalytics.class.getName());

    public static void log(HttpServletRequest request, String ident_cely, String type, String entity) {
        try {
            SolrClient solr = SolrClientFactory.getSolrClient();
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
            solr.add("logs", idoc, 1000);
        } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public static JSONObject stats(HttpServletRequest request) {
        NoOpResponseParser dontMessWithSolr = new NoOpResponseParser();
        dontMessWithSolr.setWriterType("json");
        try (SolrClient client = new HttpJdkSolrClient.Builder(Options.getInstance().getString("solrhost"))
                .withResponseParser(dontMessWithSolr).build()) {
            // request.getParameter("id"), request.getParameter("type")
            SolrQuery query = new SolrQuery()
                    .setQuery("*")
                    .setFacet(true)
                    .setRows(0)
                    .setFacetMinCount(1)
                    .addFacetField("type")
                    .addFacetField("{!ex=entityF}entity")
                    .addFacetField("ident_cely")
                    .setParam("stats.field", "{!countDistinct=true}ident_cely")
                    .setParam("json.nl", "arrntv")
                    .setParam("facet.range", "indextime")
                    .setParam("f.indextime.facet.range.start", "NOW/YEAR-1YEAR")
                    .setParam("f.indextime.facet.range.end", "NOW")
                    .setParam("f.indextime.facet.range.gap", 
                            "+1DAY");
            JSONArray ips = Options.getInstance().getJSONArray("statsIpFilter");
            for(int i =0; i<ips.length(); i++) {
                query.addFilterQuery("-ip:" + ips.getString(i)
                        .replaceAll(":", "\\\\:")
                        .replaceAll("\\.", "\\\\."));
            }

            if (LoginServlet.pristupnost(request.getSession()).compareToIgnoreCase("C") > 0) {
                query.addFacetField("user")
                        .addFacetField("ip");
            }

            query.set("stats", "true");
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
                query.addFilterQuery("user:" + request.getParameter("user").replaceAll("-", "\\-").replaceAll("/", "%2F") + "");
            }
            if (request.getParameter("ip") != null) {
                query.addFilterQuery("ip:" + request.getParameter("ip").replaceAll(":", "\\\\:") + "");
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

    public static JSONObject json(SolrQuery query, SolrClient client, String core) {
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

    public static JSONObject fixEntity() {
        LOGGER.log(Level.INFO, "Fix logs entity started");
        Instant start = Instant.now();
        // Date start = new Date();
        int totalDocs = 0;
        int rows = 100;
        JSONObject jo = new JSONObject();
        try (SolrClient client = new HttpJdkSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) { 

            SolrQuery query = new SolrQuery("*")
                    //.addFilterQuery("-entity:*") 
                    .setRows(rows)
                    .setSort("id", SolrQuery.ORDER.asc);
            boolean done = false;
            QueryResponse rsp = null;
            SolrDocumentList docs;
            String cursorMark = CursorMarkParams.CURSOR_MARK_START;
            while (!done) {
                query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
                try {
                    rsp = client.query("logs_old", query);
                    docs = rsp.getResults();
                    for (SolrDocument doc : docs) { 
                        String ident_cely = (String) doc.getFirstValue("ident_cely");
                        String typ = (String) doc.getFirstValue("type");

                        SolrQuery query2 = new SolrQuery("*")
                                .setFields("entity")
                                .setRows(1)
                                .addFilterQuery("is_deleted:false");
                        switch (typ) {
                            case "file":
                                query2.addFilterQuery("soubor_filepath:\"" + ident_cely + "\"");
                                break;
                            default:
                                query2.addFilterQuery("ident_cely:\"" + ident_cely + "\"");
                        }

                        SolrDocumentList docs2 = client.query("entities", query2).getResults();
                        if (docs2.getNumFound() > 0) {
                            SolrInputDocument idoc = new SolrInputDocument();
                            idoc.addField("id", doc.getFirstValue("id"));
                            idoc.addField("ident_cely", ident_cely);
                            idoc.addField("user", doc.getFirstValue("user"));
                            idoc.addField("ip", doc.getFirstValue("ip"));
                            idoc.addField("indextime", doc.getFirstValue("indextime"));
                            idoc.addField("type", doc.getFirstValue("type"));
                            idoc.addField("entity", docs2.get(0).getFirstValue("entity"));
                            // client.add("logs", idoc);
                            client.add("logs", idoc);
                        }
                        totalDocs++;
                    }
                    client.commit("logs"); 
                    LOGGER.log(Level.INFO, "Currently {0} files processed", totalDocs);

                    String nextCursorMark = rsp.getNextCursorMark();
                    if (cursorMark.equals(nextCursorMark) || rsp.getResults().size() < rows) {
                        done = true;
                    } else {
                        cursorMark = nextCursorMark;
                    }
                
//                    if (docs.size() < rows) {
//                        done = true;
//                    }

                } catch (SolrServerException e) {
                    LOGGER.log(Level.SEVERE, null, e);

                    Date end = new Date();
                    String msg = String.format("fixEntity finished with error. Thumbs :%1$d", totalDocs);
                    LOGGER.log(Level.INFO, msg);
                    jo.put("result", "error");
                    jo.put("error", e.toString());
                    jo.put("total docs", totalDocs);
                    jo.put("ellapsed time", FormatUtils.formatInterval(end.getTime() - start.toEpochMilli()));
                    return jo;
                }
            }

            Date end = new Date();
            String msg = String.format("fixEntity finished. Total: %1$d. Time: %2$tF",
                    totalDocs, end);
            LOGGER.log(Level.INFO, msg);
            jo.put("total", totalDocs);
            jo.put("ellapsed time", FormatUtils.formatInterval(end.getTime() - start.toEpochMilli()));
            return jo;

        } catch (IOException | JSONException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return jo;
        }

    }
}
