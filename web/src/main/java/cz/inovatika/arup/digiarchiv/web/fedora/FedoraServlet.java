package cz.inovatika.arup.digiarchiv.web.fedora;

import cz.inovatika.arup.digiarchiv.web.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
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
        PrintWriter out = response.getWriter();
        try {
            String action = request.getPathInfo().substring(1);
            if (action != null) {
                boolean isLocalhost = request.getRequestURL().toString().startsWith("http://digiarchiv:8080");
                String pristupnost = LoginServlet.pristupnost(request.getSession());
                String confLevel = Options.getInstance().getString("indexSecLevel", "E");
                LOGGER.log(Level.INFO,
                        "pristupnost -> {0}. confLevel -> {1}. isLocalhost -> {2}. request -> {3}",
                        new Object[]{pristupnost, confLevel, isLocalhost, request.getRequestURL().toString()});
                if (isLocalhost || pristupnost.compareTo(confLevel) >= 0) {
                    Actions actionToDo = Actions.valueOf(action.toUpperCase());
                    if (actionToDo.equals(Actions.REQUEST_RAW)) {
                        // FedoraUtils.requestFile(request.getParameter("file"), InitServlet.CONFIG_DIR + File.separator + request.getParameter("file"), "application/pdf");
                        InputStream is = FedoraUtils.requestInputStream(request.getParameter("file") + "/orig");
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
        INDEX_UPDATE {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    FedoraHarvester fh = new FedoraHarvester();
                    json = fh.update();
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
                    String[] entities = new String[]{"adb", "pian", "ext_zdroj", "let", "archeologicky_zaznam", "projekt", "samostatny_nalez", "dokument"};
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
                    json.put("resp", FedoraUtils.request(req.getParameter("url")));
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
