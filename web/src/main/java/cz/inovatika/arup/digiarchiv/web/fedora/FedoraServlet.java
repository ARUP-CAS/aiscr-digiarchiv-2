/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.fedora;

import cz.inovatika.arup.digiarchiv.web.*;
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
          String[] entities = new String[]{"ext_zdroj", "let","pian", "adb", "archeologicky_zaznam","samostatny_nalez", "projekt", "dokument"};
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
          json = fh.indexId(req.getParameter("id"));
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
