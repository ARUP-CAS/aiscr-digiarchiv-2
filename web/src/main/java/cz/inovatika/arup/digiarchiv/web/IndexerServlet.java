package cz.inovatika.arup.digiarchiv.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@WebServlet(name = "IndexerServlet", urlPatterns = {"/index/*"})
public class IndexerServlet extends HttpServlet {

  public static final Logger LOGGER = Logger.getLogger(IndexerServlet.class.getName());

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
          JSONObject json = actionToDo.doPerform(request, response);
          out.println(json.toString(2));
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

    HESLARE {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        
        return json;
      }
    },
    FIX_LOG {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        return LogAnalytics.fixEntity();
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
