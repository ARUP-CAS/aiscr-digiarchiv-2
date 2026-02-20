package cz.inovatika.arup.digiarchiv.web4;

import cz.inovatika.arup.digiarchiv.web4.index.SolrClientFactory;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.impl.InputStreamResponseParser;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CoreAdminParams;
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
//        NoOpResponseParser dontMessWithSolr = new NoOpResponseParser();
//        dontMessWithSolr.setWriterType("json");
        try (SolrClient client = new HttpJdkSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
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
                    .setParam("f.indextime.facet.range.other", "before")
                    .setParam("f.indextime.facet.range.end", "NOW")
                    .setParam("f.indextime.facet.range.gap",
                            "+7DAYS");
            JSONArray ips = Options.getInstance().getJSONArray("statsIpFilter");
            for (int i = 0; i < ips.length(); i++) {
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
                    query.setParam("f.indextime.facet.range.start", "2024-11-01T00:00:00Z");
                } else {
                    from = from + "T00:00:00Z";
                    query.setParam("f.indextime.facet.range.start", from);
                }
                String to = parts[1];
                if ("null".equals(to)) {
                    to = "*";
                } else {
                    to = to + "T23:59:59Z";
                }
                String fq = "indextime:[" + from + " TO " + to + "]";
                query.addFilterQuery(fq);
            } else {
                // query.setParam("f.indextime.facet.range.start", "NOW/YEAR-1YEAR");
                // prvni zaznam "2024-11-08T11:53:40.718Z"
                query.setParam("f.indextime.facet.range.start", "2024-11-01T00:00:00Z");
            }

            if (request.getParameter("entity") != null) {
                // query.addFilterQuery("{!join fromIndex=entities to=ident_cely from=ident_cely}entity:\"" + request.getParameter("entity") + "\"");
                query.addFilterQuery("{!tag=entityF}entity:\"" + request.getParameter("entity") + "\"");
            }

            JSONObject ret = json(query, client, "logs");
            if (LoginServlet.pristupnost(request.getSession()).compareToIgnoreCase("C") > 0) {
                JSONObject r = entities(request, client);
                ret.put("index_entities", r.getJSONObject("facet_counts")
                        .getJSONObject("facet_pivot").getJSONArray("entity,stav"));
                ret.put("ruian", ruian(request, client).getJSONObject("facet_counts")
                        .getJSONObject("facet_fields").getJSONArray("entity"));
                ret.put("cores", cores(request, client).getJSONObject("status"));
            }
            return ret;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return new JSONObject().put("error", ex);
        }
    }

    public static JSONObject statsIndex(HttpServletRequest request) {
        JSONObject ret = new JSONObject();
        try (SolrClient client = new HttpJdkSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
            if (LoginServlet.pristupnost(request.getSession()).compareToIgnoreCase("C") > 0) {
            JSONObject r = entities(request, client);
            ret.put("index_entities", r.getJSONObject("facet_counts")
                    .getJSONObject("facet_pivot").getJSONArray("entity,stav"));
            ret.put("ruian", ruian(request, client).getJSONObject("facet_counts")
                    .getJSONObject("facet_fields").getJSONArray("entity"));
            ret.put("cores", cores(request, client).getJSONObject("status"));
            }
            return ret;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return new JSONObject().put("error", ex);
        }
    }

    private static JSONObject entities(HttpServletRequest request, SolrClient client) {
        // request.getParameter("id"), request.getParameter("type")
        SolrQuery query = new SolrQuery()
                .setQuery("*")
                .setFacet(true)
                .setRows(0)
                .setFacetMinCount(1)
                .addFacetField("entity")
                .addFacetPivotField("entity,stav");

        if (!Boolean.parseBoolean(request.getParameter("show_deleted"))) {
            query.addFilterQuery("-is_deleted:true");
        }

        if (Boolean.parseBoolean(request.getParameter("only_visible"))) {
            query.addFilterQuery("-is_deleted:true");
            query.addFilterQuery("searchable:true");
        }

        JSONObject ret = json(query, client, "entities");
        return ret;
    }

    private static JSONObject ruian(HttpServletRequest request, SolrClient client) {
        // request.getParameter("id"), request.getParameter("type")
        SolrQuery query = new SolrQuery()
                .setQuery("*")
                .setFacet(true)
                .setRows(0)
                .setFacetMinCount(1)
                .addFacetField("entity")
                .setFacetSort("index asc");

        query.set("json.nl", "arrntv");
        query.set("wt", "json");
        JSONObject ret = json(query, client, "ruian");
        return ret;
    }

    private static JSONObject cores(HttpServletRequest request, SolrClient client) {
        try {
            CoreAdminRequest arequest = new CoreAdminRequest();
            arequest.setIndexInfoNeeded(true);
            arequest.setAction(CoreAdminParams.CoreAdminAction.STATUS);

            arequest.setResponseParser(new InputStreamResponseParser("json"));
            NamedList<Object> resp = client.request(arequest);
            InputStream is = (InputStream) resp.get("stream");
            return new JSONObject(IOUtils.toString(is, "UTF-8"));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return new JSONObject().put("error", ex);
        }
    }

    public static JSONObject json(SolrQuery query, SolrClient client, String core) {
        query.set("wt", "json");
        try {

            QueryRequest req = new QueryRequest(query);
            req.setPath("/select");

            req.setResponseParser(new InputStreamResponseParser("json"));
            NamedList<Object> resp = client.request(req, core);
            InputStream is = (InputStream) resp.get("stream");
            return new JSONObject(IOUtils.toString(is, "UTF-8"));
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
