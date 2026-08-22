package cz.inovatika.arup.digiarchiv.web4.museion;

import cz.inovatika.arup.digiarchiv.web4.index.SolrSearcher;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@WebServlet(name = "MuseionServlet", urlPatterns = {"/mus/*"})
public class MuseionServlet extends HttpServlet {

    public static final Logger LOGGER = Logger.getLogger(MuseionServlet.class.getName());

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
                    Actions actionToDo = Actions.valueOf(action.toUpperCase());
                    response.setContentType("application/json;charset=UTF-8");
                    //response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
                    JSONObject json = actionToDo.doPerform(request, response);
                    out.println(json.toString(2));
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
        PREDMETY_BY_ID {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    MuseionClient m = new MuseionClient();
                    String id = req.getParameter("id");
                    String typ = req.getParameter("typ");
                    if (typ == null) {
                      String entity = SolrSearcher.getEntityById(id);
//museionTypes: { [entity: string]: string } = {
//    projekt: 'P',
//    akce: 'A',
//    samostatny_nalez: 'N'
//}
                      switch(entity){
                        case "projekt": typ = "P"; break;
                        case "akce": typ = "A"; break;
                        case "samostatny_nalez": typ = "N"; break;
                      }
                    }
                    json.put("predmetyDleAmcrId", m.predmetyDleAmcrId(id, typ)); 
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        STATISTIKA {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    MuseionClient m = new MuseionClient();
                    json.put("statistika", m.predmetyStatistikaAsJSON()); 
                } catch (JSONException ex) {
                    json.put("error", ex.toString());
                }
                return json;
            }
        },
        INDEX {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                JSONObject json = new JSONObject();
                try {
                    MuseionClient m = new MuseionClient();
                    json.put("statistika", m.indexStatistika());  
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
