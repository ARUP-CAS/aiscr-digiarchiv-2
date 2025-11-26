package cz.inovatika.arup.digiarchiv.web4.fedora;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inovatika.arup.digiarchiv.web4.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@WebServlet(name = "FedoraServlet", urlPatterns = {"/fedora/*"})
public class FedoraServlet extends HttpServlet {

    public static final Logger LOGGER = Logger.getLogger(FedoraServlet.class.getName());

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
        List<Object> allowedIP = Options.getInstance().getJSONArray("allowedIP").toList();
        // System.out.println(request.getRemoteAddr());
        // System.out.println(request.getHeader("X-Forwarded-For"));
        boolean isAllowedIP = allowedIP.contains(request.getRemoteAddr());
        // boolean isAllowedIP = false;
        PrintWriter out = response.getWriter();
        try {
            String action = request.getPathInfo().substring(1);
            if (action != null) {
                boolean isLocalhost = request.getRemoteAddr().startsWith("127.0.0.1");
                String pristupnost = LoginServlet.pristupnost(request.getSession());
                String confLevel = Options.getInstance().getString("indexSecLevel", "E");
                LOGGER.log(Level.FINE,
                        "pristupnost -> {0}. confLevel -> {1}. isLocalhost -> {2}. isAllowedIP -> {3}. request -> {4}",
                        new Object[]{pristupnost, confLevel, isLocalhost, isAllowedIP, request.getRequestURL().toString()});
                if (isAllowedIP || isLocalhost || pristupnost.compareTo(confLevel) >= 0) {
                    Actions actionToDo = Actions.valueOf(action.toUpperCase());
                    if (actionToDo.equals(Actions.REQUEST_RAW)) {
                        // FedoraUtils.requestFile(request.getParameter("file"), InitServlet.CONFIG_DIR + File.separator + request.getParameter("file"), "application/pdf");
                        InputStream is = FedoraUtils.requestInputStream(StringEscapeUtils.escapeHtml4(request.getParameter("file")) + "/orig");
                        String path = InitServlet.CONFIG_DIR + File.separator + "1.pdf";
                        File targetFile = new File(path);
                        FileUtils.copyInputStreamToFile(is, targetFile);

                        return;
                    }
                    response.setContentType("application/json;charset=UTF-8");
                    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
                    JSONObject json = actionToDo.doPerform(request, response);
                    if (actionToDo.equals(Actions.GET_ID)) {
                        response.setContentType("application/xml;charset=UTF-8");
                        out.println(json.getString("model"));

//          } else if (actionToDo.equals(Actions.GET_ID_RAW)) {
//              response.setContentType("plain/text;charset=UTF-8");
//              out.println(json.getString("raw"));
                    } else {
                        out.println(json.toString(2));
                    }

                } else {
                    out.print("Insuficient rights");
                }
            } else {
                out.print("action -> " + StringEscapeUtils.escapeHtml4(action));
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

    public static String pristupnost(HttpSession session) {
        JSONObject ses = (JSONObject) session.getAttribute("user");
        String userid = (String) session.getAttribute("userid");
        String pristupnost = "A";
        if (ses != null && !ses.has("error")) {
            pristupnost = ses.getJSONObject(userid).getString("pristupnost");
        }
        return pristupnost;
    }

    enum Actions {

        INDEX_FULL {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    FedoraHarvester fh = new FedoraHarvester();
                    json = fh.harvest();
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        STOP_INDEX {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    FedoraHarvester fh = new FedoraHarvester();
                    json = fh.stopIndex();
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        INDEX_UPDATE {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    String from = req.getParameter("from");
                    FedoraHarvester fh = new FedoraHarvester();
                    json = fh.update(from);
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        STOP_UPDATE {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    FedoraHarvester fh = new FedoraHarvester();
                    json = fh.stopUpdate();
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        INDEX_ENTITIES {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    FedoraHarvester fh = new FedoraHarvester();
                    String[] entities = new String[]{"adb", "pian", "ext_zdroj", "let", "archeologicky_zaznam", "samostatny_nalez", "dokument", "projekt"}; 
                    json = fh.indexModels(entities);
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        INDEX_MODEL {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    FedoraHarvester fh = new FedoraHarvester();
                    if (req.getParameter("offset") != null) {
                        fh.setOffset(Integer.parseInt(req.getParameter("offset")));
                    }
                    json = fh.indexModels(req.getParameterValues("model"));
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        INDEX_ID {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    FedoraHarvester fh = new FedoraHarvester();
                    for (String id : req.getParameterValues("id")) {
                        json.put(id, fh.indexId(id));
                    }

                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        REINDEX_FILTER {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    FedoraHarvester fh = new FedoraHarvester();
                    json = fh.reindexByFilter(req.getParameter("fq"));

                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        
        
        GET_ID {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    FedoraHarvester fh = new FedoraHarvester();
                    json.put("model", fh.getId(req.getParameter("id")));
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        
        
        GET_ID_PARSED {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    FedoraHarvester fh = new FedoraHarvester();
                    FedoraModel o = fh.getIdParsed(req.getParameter("id"));
                    ObjectMapper objectMapper = new ObjectMapper();
                    return new JSONObject(objectMapper.writeValueAsString(o));
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        GET_ID_RAW {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    String xml = FedoraUtils.requestXml("record/" + req.getParameter("id"));
                    json.put("raw", xml);
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        GET_ID_METADATA {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    FedoraHarvester fh = new FedoraHarvester();
                    json.put("model", fh.getIdMetadata(req.getParameter("id")));
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        GET_ID_JSON {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    FedoraHarvester fh = new FedoraHarvester();
                    json.put("model", fh.getIdJSON(req.getParameter("id")));
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        REQUEST {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    json.put("resp", FedoraUtils.request(StringEscapeUtils.escapeHtml4(req.getParameter("url"))));
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        SEARCH {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    String search_fedora_id_prefix = Options.getInstance().getJSONObject("fedora").getString("search_fedora_id_prefix"); 
                    String baseQuery = "condition=" + URLEncoder.encode("fedora_id=" + search_fedora_id_prefix + "record/*/metadata", "UTF8")
                + "&condition=" + URLEncoder.encode("modified>" + req.getParameter("url"), "UTF8");
                    
                    
                    json.put("resp", FedoraUtils.search(baseQuery));
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        SEARCH_FILE {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    String search_fedora_id_prefix = Options.getInstance().getJSONObject("fedora").getString("search_fedora_id_prefix"); 
                    String baseQuery = "condition=" + URLEncoder.encode("fedora_id=" + search_fedora_id_prefix + "record/*/file/*", "UTF8")
                + "&condition=" + URLEncoder.encode("modified>" + req.getParameter("from"), "UTF8");
                    
                    
                    json.put("resp", FedoraUtils.search(baseQuery));
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        CHECK_DATESTAMP {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    FedoraHarvester fh = new FedoraHarvester();
                    if (req.getParameter("offset") != null) {
                        fh.setOffset(Integer.parseInt(req.getParameter("offset")));
                    }
                    json = fh.checkDatestamp(req.getParameterValues("model"));
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        REQUEST_RAW {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    json.put("resp", FedoraUtils.request(req.getParameter("url")));
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        };

        abstract JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception;
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
