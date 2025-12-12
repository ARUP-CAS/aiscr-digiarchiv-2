package cz.inovatika.arup.digiarchiv.web4.oai;

import cz.inovatika.arup.digiarchiv.web4.AuthService;
import cz.inovatika.arup.digiarchiv.web4.Options;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@WebServlet(name = "OAIServlet", urlPatterns = {"/oai/*"})
public class OAIServlet extends HttpServlet {

    public static final Logger LOGGER = Logger.getLogger(OAIServlet.class.getName());

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
        response.setContentType("text/xml;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String version = request.getPathInfo();
        if ("/v2".equals(version)) {
            version = "/v2.2";
        }
        try {
            String actionNameParam = request.getParameter("verb");
            if (actionNameParam != null) {
                Actions actionToDo = Actions.valueOf(actionNameParam);
                final String authorization = request.getHeader("Authorization");
                if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
                    // Authorization: Basic base64credentials
                    String base64Credentials = authorization.substring("Basic".length()).trim();
                    byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
                    String credentials = new String(credDecoded, StandardCharsets.UTF_8); 
                    // credentials = username:password
                    final String[] values = credentials.split(":", 2);
                    JSONObject jo = AuthService.login(values[0], values[1]);
                    if (!jo.has("error")) {
                        request.getSession().setAttribute("user", jo);
                    } else {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        out.print("Invalid credentials");
                        return;
                    }
                    
                }
                String xml = actionToDo.doPerform(request, response, version);
                out.println(xml);
            } else {
                String xml = OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                        + "<request>" + Options.getInstance().getJSONObject("OAI").getString("baseUrl") + "</request>"
                        + "<error code=\"badVerb\">verb is absent</error>"
                        + "</OAI-PMH>";
                out.print(xml);
            }
        } catch (IllegalArgumentException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            String xml = OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                    + "<request>" + Options.getInstance().getJSONObject("OAI").getString("baseUrl") + "</request>"
                    + "<error code=\"badVerb\">Illegal OAI verb</error>"
                    + "</OAI-PMH>"; 
            out.print(xml);
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
        Identify {
            @Override
            String doPerform(HttpServletRequest req, HttpServletResponse response, String version) throws Exception {
                return OAIRequest.identify(req, version);
            }
        },
        ListSets {
            @Override
            String doPerform(HttpServletRequest req, HttpServletResponse response, String version) throws Exception {
                return OAIRequest.listSets(req, version);
            }
        },
        ListMetadataFormats {
            @Override
            String doPerform(HttpServletRequest req, HttpServletResponse response, String version) throws Exception {
                return OAIRequest.metadataFormats(req, version);
            }
        },
        GetRecord {
            @Override
            String doPerform(HttpServletRequest req, HttpServletResponse response, String version) throws Exception {
                return OAIRequest.getRecord(req, version);
            }
        },
        ListIdentifiers {
            @Override
            String doPerform(HttpServletRequest req, HttpServletResponse response, String version) throws Exception {
                return OAIRequest.listRecords(req, true, version);
            }
        },
        ListRecords {
            @Override
            String doPerform(HttpServletRequest req, HttpServletResponse response, String version) throws Exception {
                return OAIRequest.listRecords(req, false, version);
            }
        };

        abstract String doPerform(HttpServletRequest request, HttpServletResponse response, String version) throws Exception;
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
