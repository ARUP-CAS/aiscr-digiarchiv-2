package cz.inovatika.arup.digiarchiv.web;

import cz.inovatika.arup.digiarchiv.web.index.ComponentSearcher;
import cz.inovatika.arup.digiarchiv.web.index.DokumentSearcher;
import cz.inovatika.arup.digiarchiv.web.index.EntitySearcher;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web.index.PIANSearcher;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@WebServlet(name = "SearchServlet", urlPatterns = {"/search/*"})
public class SearchServlet extends HttpServlet {

    public static final Logger LOGGER = Logger.getLogger(SearchServlet.class.getName());

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0
        response.setDateHeader("Expires", 0); // Proxies.
        PrintWriter out = response.getWriter();

        request.getSession().setMaxInactiveInterval(Options.getInstance().getInt("sessionTimeout", 300));
        try {
            String action = request.getPathInfo().substring(1);
            if (action != null) {
                Actions actionToDo = Actions.valueOf(action.toUpperCase());
                String json = actionToDo.doPerform(request, response);
                out.println(json);
            } else {
                out.print("action -> " + action);
            }
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.toString());
            out.print(e1.toString());
        } catch (SecurityException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.toString());
            out.print(e1.toString());
        }
    }

    enum Actions {
        HOME {
            @Override
            String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {

                JSONObject json = new JSONObject();
                try {
                    Http2SolrClient client = IndexUtils.getClientNoOp();
                    SolrQuery query = new SolrQuery("*")
                            .setRequestHandler("/search")
                            .setRows(0)
                            .setFacet(true).addFacetField("dokument_kategorie_dokumentu", "dokument_rada")
                            .setParam("json.nl", "map");
                    JSONObject jo = SearchUtils.json(query, client, "entities");
                    json = jo.getJSONObject("facet_counts").getJSONObject("facet_fields").getJSONObject("entity");
                    json.put("kategorie", jo.getJSONObject("facet_counts").getJSONObject("facet_fields").getJSONObject("dokument_kategorie_dokumentu"));

                } catch (Exception ex) {
                    json.put("error", ex);
                }
                return json.toString();
            }
        },
        CHECK_RELATIONS {
            @Override
            String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {

                JSONObject json = new JSONObject();
                try {
                    Http2SolrClient client = IndexUtils.getClientNoOp();
                    String entity = request.getParameter("entity");
                    SolrQuery query = new SolrQuery("ident_cely:\"" + request.getParameter("id") + "\"")
                            .setFacet(false);
                    query.setRequestHandler("/search");
                    if (entity == null) {
                        query.setFields("entity");
                        JSONObject jo = SearchUtils.json(query, client, "entities");
                        if (jo.getJSONObject("response").optInt("numFound", 0) == 0) {
                            return jo.toString();
                        }
                        entity = jo.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("entity");
                    }
                    String pristupnost = LoginServlet.pristupnost(request.getSession());
                    if ("E".equals(pristupnost)) {
                        pristupnost = "D";
                    }
                    EntitySearcher searcher = SearchUtils.getSearcher(entity);
                    query.setFields(searcher.getRelationsFields());

                    JSONObject jo = SearchUtils.json(query, client, "entities");
                    if (jo.getJSONObject("response").optInt("numFound", 0) > 0) {
                        searcher.checkRelations(jo, client, request);
                        JSONObject doc = jo.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
                        return doc.toString();
                    } else {
                        return "{}";
                    }

                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                    json.put("error", ex);
                }
                return json.toString();
            }
        },
        ID {
            @Override
            String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {

                JSONObject json = new JSONObject();
                try {
                    String entity = request.getParameter("entity");
                    SolrQuery query = new SolrQuery("ident_cely:\"" + request.getParameter("id") + "\"")
                            .setFacet(false);
                    query.setRequestHandler("/search");
                    if (entity == null) {
                        query.setFields("entity");
                        JSONObject jo = SearchUtils.json(query, IndexUtils.getClientNoOp(), "entities");
                        if (jo.getJSONObject("response").optInt("numFound", 0) == 0) {
                            return jo.toString();
                        }
                        entity = jo.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("entity");
                    }
                    String pristupnost = LoginServlet.pristupnost(request.getSession());
                    if ("E".equals(pristupnost)) {
                        pristupnost = "D";
                    }
                    EntitySearcher searcher = SearchUtils.getSearcher(entity);
                    if (searcher != null) {
                        query.setFields(searcher.getSearchFields(pristupnost));
                    } else {
                        query.setFields("*,dok_jednotka:[json],pian:[json],adb:[json],jednotka_dokumentu:[json],nalez_dokumentu:[json],"
                                + "ext_odkaz:[json],ext_zdroj:[json],vazba_projekt_akce:[json],akce:[json],soubor:[json],let:[json],nalez:[json],vyskovy_bod:[json],"
                                + "dokument:[json],projekt:[json],samostatny_nalez:[json],obdobi:[json],komponenta:[json],komponenta_dokument:[json],neident_akce:[json],aktivita:[json]");
                    }
                    JSONObject jo = SearchUtils.json(query, IndexUtils.getClientNoOp(), "entities");
                    if (jo.getJSONObject("response").optInt("numFound", 0) > 0) {
                        if (searcher != null) {
                            //if ("pian".equals(entity) || "adb".equals(entity) || "ext_zdroj".equals(entity)) {
                            searcher.getChilds(jo, IndexUtils.getClientNoOp(), request);
                            //}
                            searcher.checkRelations(jo, IndexUtils.getClientNoOp(), request);
                            searcher.filter(jo, pristupnost, LoginServlet.organizace(request.getSession()));
                        }
                        ComponentSearcher cs = SearchUtils.getComponentSearcher(entity);
                        if (cs != null) {
                            cs.getRelated(jo, IndexUtils.getClientNoOp(), request);
                            if (!cs.isRelatedSearchable()) {
                                jo.getJSONObject("response").put("numFound", 0).put("docs", new JSONArray());
                            }
                        }
                        // json = jo.getJSONObject("response").getJSONArray("docs").getJSONObject(0);

                    }
                    // Remove stats
                    jo.getJSONObject("stats").getJSONObject("stats_fields").remove("lat");
                    jo.getJSONObject("stats").getJSONObject("stats_fields").remove("lng");
                    return jo.toString();

                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                    json.put("error", ex);
                }
                return json.toString();
            }
        },
        ID_AS_CHILD {
            @Override
            String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {

                JSONObject json = new JSONObject();
                try {
                    Http2SolrClient client = IndexUtils.getClientNoOp();
                    String entity = request.getParameter("entity");
                    SolrQuery query = new SolrQuery("ident_cely:(\"" + String.join("\" OR \"", request.getParameterValues("id")) + "\")")
                            .addFilterQuery("entity:" + entity)
                            .setFacet(false);
                    query.setRequestHandler("/search");
//          if (entity == null) {
//            query.setFields("entity");
//            JSONObject jo = SearchUtils.json(query, client, "entities");
//            if (jo.getJSONObject("response").optInt("numFound", 0) == 0) {
//              return jo.toString();
//            }
//            entity = jo.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("entity");
//          }
                    String pristupnost = LoginServlet.pristupnost(request.getSession());
                    if ("E".equals(pristupnost)) {
                        pristupnost = "D";
                    }
                    EntitySearcher searcher = SearchUtils.getSearcher(entity);
                    if (searcher != null) {
                        query.setFields(searcher.getChildSearchFields(pristupnost));
                    } else {
                        query.setFields("*,dok_jednotka:[json],pian:[json],adb:[json],jednotka_dokumentu:[json],nalez_dokumentu:[json],"
                                + "ext_zdroj:[json],ext_odkaz:[json],vazba_projekt_akce:[json],akce:[json],soubor:[json],let:[json],nalez:[json],vyskovy_bod:[json],"
                                + "dokument:[json],projekt:[json],samostatny_nalez:[json],komponenta:[json],komponenta_dokument:[json],neident_akce:[json],aktivita:[json]");
                    }
                    JSONObject jo = SearchUtils.json(query, client, "entities");
                    if (searcher != null) {
                        searcher.filter(jo, pristupnost, LoginServlet.organizace(request.getSession()));
                    }
                    if (jo.getJSONObject("response").optInt("numFound", 0) > 0) {
//            String entityById = jo.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("entity");
//            if (!entityById.equals(entity)) {
//              return new JSONObject().put("error", "invalid request").toString();
//            }

                        ComponentSearcher cs = SearchUtils.getComponentSearcher(entity);
                        if (cs != null) {
                            cs.getRelated(jo, client, request);
                            if (!cs.isRelatedSearchable()) {
                                jo.getJSONObject("response").put("numFound", 0).put("docs", new JSONArray());
                            }
                        }
                    }
                    jo.getJSONObject("stats").getJSONObject("stats_fields").remove("lat");
                    jo.getJSONObject("stats").getJSONObject("stats_fields").remove("lng");
                    return jo.toString();

                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                    json.put("error", ex);
                }
                return json.toString();
            }
        },
        GML {
            @Override
            String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {

                JSONObject json = new JSONObject();
                try {
                    Http2SolrClient client = IndexUtils.getClientNoOp();
                    SolrQuery query = new SolrQuery("ident_cely:\"" + request.getParameter("id") + "\"")
                            .setFacet(false);
                    query.setRequestHandler("/search");
                    query.setFields("geom_gml:[json]");
                    JSONObject jo = SearchUtils.json(query, client, "entities").getJSONObject("response").getJSONArray("docs").getJSONObject(0);
                    return jo.toString();

                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                    json.put("error", ex);
                }
                return json.toString();
            }
        },
        WKT {
            @Override
            String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {

                JSONObject json = new JSONObject();
                try {
                    Http2SolrClient client = IndexUtils.getClientNoOp();
                    SolrQuery query = new SolrQuery("ident_cely:\"" + request.getParameter("id") + "\"")
                            .setFacet(false);
                    query.setRequestHandler("/search");
                    query.setFields("geom_wkt,chranene_udaje:[json]");

                    JSONObject jo = SearchUtils.json(query, client, "entities").getJSONObject("response").getJSONArray("docs").getJSONObject(0);
                    if (jo.has("geom_wkt")) {
                        jo.put("geom_wkt_c", jo.getString("geom_wkt"));
                        // jo.put("geom_wkt_c", GPSconvertor.convertGeojson(jo.getString("geom_wkt")));
                    } else {
                        jo.put("geom_wkt_c", jo.getJSONObject("chranene_udaje").getJSONObject("geom_wkt").getString("value"));
                        // jo.put("geom_wkt_c", GPSconvertor.convertGeojson(jo.getJSONObject("chranene_udaje").getJSONObject("geom_wkt").getString("value")));
                    }

                    return jo.toString();

                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                    json.put("error", ex);
                }
                return json.toString();
            }
        },
        GEOMETRIE {
            @Override
            String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {

                JSONObject json = new JSONObject();
                try {
                    SolrQuery query = new SolrQuery("ident_cely:\"" + request.getParameter("id") + "\"")
                            .setFacet(false);
                    query.setRequestHandler("/search");
                    String format = request.getParameter("format");
                    switch (format) {
                        case "GML":
                            query.setFields("geometrie:geom_gml");
                            break;
                        default:
                            query.setFields("geometrie:geom_wkt");
                    }

                    JSONObject jo = SearchUtils.json(query, IndexUtils.getClientNoOp(), "entities").getJSONObject("response").getJSONArray("docs").getJSONObject(0);

                    if ("GeoJSON".equals(format)) {
                        jo.put("geometrie", GPSconvertor.convertGeojson(jo.getString("geometrie")));
                    }
                    return jo.toString();

                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                    json.put("error", ex);
                }
                return json.toString();
            }
        },
        QUERY {
            @Override
            String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {
                try {
                    String entity = "" + request.getParameter("entity");
                    EntitySearcher searcher = SearchUtils.getSearcher(entity);
                    if (searcher == null) {
                        searcher = new DokumentSearcher();
                    }
                    JSONObject jo = searcher.search(request);
                    // searcher.checkRelations(jo, IndexUtils.getClientNoOp(), request);

                    // Remove stats in case of one result, without access
                    int numFound = jo.getJSONObject("response").getInt("numFound");
                    if (numFound == 1) {
                        String docPr = jo.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("pristupnost");
                        String pristupnost = LoginServlet.pristupnost(request.getSession());
                        if (docPr.compareTo(pristupnost) > 0) {
                            jo.getJSONObject("stats").getJSONObject("stats_fields").remove("lat");
                            jo.getJSONObject("stats").getJSONObject("stats_fields").remove("lng");
                        }
                    }
                    return jo.toString();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                    return new JSONObject().put("error", ex).toString();
                }

            }
        },
        EXPORT_MAPA {
            @Override
            String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {

                String entity = "" + request.getParameter("entity");
                EntitySearcher searcher = SearchUtils.getSearcher(entity);
                if (searcher == null) {
                    searcher = new DokumentSearcher();
                }
                return searcher.export(request);
            }
        },
        PIANS {
            @Override
            String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {

                PIANSearcher searcher = new PIANSearcher();
                JSONObject jo = searcher.getMapPians(request);
                return jo.toString();

//        PIANSearcher searcher = new PIANSearcher();
//        JSONObject ret = new JSONObject();
//        JSONArray docs = searcher.getMapPians(request).getJSONObject("response").getJSONArray("docs");
//        for (int i = 0; i < docs.length(); i++) {
//          JSONObject doc = docs.getJSONObject(i);
//          JSONObject pian = doc.getJSONObject("pian");
//          String pianId =  pian.getString("ident_cely");
//          if (ret.has(pianId)) {
//            
//          }
//          ret.put(pianId, pian);
//        }
//        return ret.toString();
            }
        },
        GETHESLAR {
            @Override
            String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {

                JSONObject json = new JSONObject();
                try {
                    Http2SolrClient client = IndexUtils.getClientNoOp();
                    String pristupnost = LoginServlet.pristupnost(request.getSession());
                    SolrQuery query = new SolrQuery("heslar:\"" + request.getParameter("id") + "\"")
                            .setRows(1000);

                    QueryRequest req = new QueryRequest(query);

                    NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
                    rawJsonResponseParser.setWriterType("json");
                    req.setResponseParser(rawJsonResponseParser);

                    NamedList<Object> resp = client.request(req, "translations");
                    return (String) resp.get("response");

                } catch (Exception ex) {
                    json.put("error", ex);
                }
                return json.toString();
            }
        },
        THESAURI {
            @Override
            String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {

                JSONObject ret = new JSONObject();
                try {
                    Http2SolrClient client = IndexUtils.getClientNoOp();
                    String pristupnost = LoginServlet.pristupnost(request.getSession());
                    SolrQuery query = new SolrQuery("*")
                            .setFields("id,heslar,pole,heslo,poradi")
                            .setRows(5000);

                    QueryRequest req = new QueryRequest(query);

                    NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
                    rawJsonResponseParser.setWriterType("json");
                    req.setResponseParser(rawJsonResponseParser);

                    NamedList<Object> resp = client.request(req, "translations");
                    JSONArray docs = new JSONObject((String) resp.get("response"))
                            .getJSONObject("response").getJSONArray("docs");
                    // return (String) resp.get("response");

                    JSONObject heslarToPole = Options.getInstance().getClientConf().getJSONObject("heslarToPole");
                    for (int i = 0; i < docs.length(); i++) {
                        // ret.put(docs.getJSONObject(i).getString("id"), docs.getJSONObject(i).getInt("poradi"));
                        String heslar = docs.getJSONObject(i).getString("heslar");
                        LOGGER.log(Level.FINE, heslar);
                        // String pole = heslar;
                        ret.put(heslar + "_" + docs.getJSONObject(i).getString("heslo"), docs.getJSONObject(i).getInt("poradi"));

                        if (heslarToPole.has(heslar)) {
                            Object obj = heslarToPole.get(heslar);
                            if (obj instanceof JSONArray) {
                                JSONArray poli = (JSONArray) obj;
                                for (int p = 0; p < poli.length(); p++) {
                                    ret.put(poli.getString(p) + "_" + docs.getJSONObject(i).getString("heslo"), docs.getJSONObject(i).getInt("poradi"));
                                }
                            } else {
                                heslar = (String) obj;
                                ret.put(heslar + "_" + docs.getJSONObject(i).getString("heslo"), docs.getJSONObject(i).getInt("poradi"));
                            }
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                    ret.put("error", ex);
                }
                return ret.toString();
            }
        },
        OBDOBI {
            @Override
            String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    Http2SolrClient client = IndexUtils.getClientNoOp();
                    SolrQuery query = new SolrQuery("*").setFilterQueries("heslar_name:obdobi_prvni")
                            .setRows(1000)
                            .setSort("poradi", SolrQuery.ORDER.asc)
                            .setParam("stats", true)
                            .setParam("stats.field", "poradi");
                    QueryRequest req = new QueryRequest(query);

                    NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
                    rawJsonResponseParser.setWriterType("json");
                    req.setResponseParser(rawJsonResponseParser);

                    NamedList<Object> resp = client.request(req, "heslar");
                    return (String) resp.get("response");
                } catch (Exception ex) {
                    json.put("error", ex);
                }
                return json.toString();
            }
        },
        MAPA {
            @Override
            String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    Http2SolrClient client = IndexUtils.getClientNoOp();
                    String entity = request.getParameter("entity");
                    if (entity == null) {
                        entity = "dokument";
                    }
                    // String handler = LoginServlet.isLogged(request.getSession()) ? "/full_logged" : "/full_notlogged";
                    SolrQuery query = new SolrQuery("*:*")
                            //        .setRequestHandler(handler)
                            //.setParam("facet", "true")
                            //.setParam("json.nl", "map")
                            .setFacet(true)
                            .setParam("rows", "100")
                            .addFilterQuery("entity:" + entity)
                            .setFields("pian:[json],loc_rpt,centroid_n,centroid_e,organizace");
                    String pristupnost = LoginServlet.pristupnost(request.getSession());
                    if ("E".equals(pristupnost)) {
                        pristupnost = "D";
                    }
                    // SolrSearcher.setQuery(request, query, pristupnost);
                    SolrSearcher.addLocationParams(request, query);

                    QueryRequest req = new QueryRequest(query);

                    NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
                    rawJsonResponseParser.setWriterType("json");
                    req.setResponseParser(rawJsonResponseParser);

                    NamedList<Object> resp = client.request(req, "entities");
                    return (String) resp.get("response");

                } catch (Exception ex) {
                    json.put("error", ex);
                }
                return json.toString();
            }
        };

        abstract String doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception;
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
