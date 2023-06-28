/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@WebServlet(name = "FavoritesServlet", urlPatterns = {"/fav/*"})
public class FavoritesServlet extends HttpServlet {

  public static final Logger LOGGER = Logger.getLogger(FavoritesServlet.class.getName());

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
    try {
      String action = request.getPathInfo().substring(1);
      if (action != null) {
        Actions actionToDo = Actions.valueOf(action.toUpperCase());
        JSONObject json = actionToDo.doPerform(request, response);
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
    ADD {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        JSONObject json = new JSONObject();

        try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {

          JSONObject ses = (JSONObject) req.getSession().getAttribute("user");

          if (ses == null || ses.has("error")) {
            json.put("error", "not logged");
            return json;
          }
          String username = (String) req.getSession().getAttribute("userid");

          String docid = req.getParameter("docid");
          String entity = req.getParameter("entity");
          if (req.getMethod().equals("POST")) {
            JSONObject js = new JSONObject(IOUtils.toString(req.getInputStream(), "UTF-8"));
            docid = js.getString("docid");
            entity = js.getString("entity");
          }
          SolrInputDocument idoc = new SolrInputDocument();
          idoc.addField("uniqueid", username + "_" + docid);
          idoc.addField("docid", docid);
          idoc.addField("username", username);
          idoc.addField("entity", entity);
          client.add("favorites", idoc);
          client.commit("favorites");

          json.put("message", "Added success: " + username + docid);

        } catch (Exception ex) {
          json.put("error", ex.toString());
        }

        return json;
      }
    },
    REMOVE {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        JSONObject json = new JSONObject();

        try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {

          JSONObject ses = (JSONObject) req.getSession().getAttribute("user");
          if (ses == null) {
            json.put("error", "not logged");
          } else if (ses.has("error")) {
            json.put("error", "not logged");
          } else {
            String username = (String) req.getSession().getAttribute("userid");
            String docid = req.getParameter("docid");
            if (req.getMethod().equals("POST")) {
              JSONObject js = new JSONObject(IOUtils.toString(req.getInputStream(), "UTF-8"));
              docid = js.getString("docid");
            }
            client.deleteById("favorites", username + "_" + docid);
            client.commit("favorites");

            json.put("message", "Removed success");

          }
        } catch (Exception ex) {
          json.put("error", ex.toString());
        }

        return json;
      }
    },
    GET {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        JSONObject json = new JSONObject();

        try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {

          JSONObject ses = (JSONObject) req.getSession().getAttribute("user");
          if (ses == null) {
            json.put("error", "not logged");
          } else if (ses.has("error")) {
            json.put("error", "not logged");
          } else {
            String username = (String) req.getSession().getAttribute("userid");
            String docid = req.getParameter("docid");

            SolrQuery query = new SolrQuery("uniqueid:" + username + "_" + docid).setRows(1);

            QueryRequest qreq = new QueryRequest(query);

            NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
            rawJsonResponseParser.setWriterType("json");
            qreq.setResponseParser(rawJsonResponseParser);

            NamedList<Object> sresp = client.request(qreq, "favorites");
            return new JSONObject((String) sresp.get("response"));

          }
        } catch (Exception ex) {
          json.put("error", ex.toString());
        }

        return json;
      }
    },
    ALL {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        JSONObject json = new JSONObject();

        try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {

          JSONObject ses = (JSONObject) req.getSession().getAttribute("user");
          if (ses == null) {
            json.put("error", "not logged");
          } else if (ses.has("error")) {
            json.put("error", "not logged");
          } else {
            String username = (String) req.getSession().getAttribute("userid");

            SolrQuery query = new SolrQuery("username:" + username).setRows(100);

            QueryRequest qreq = new QueryRequest(query);

            NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
            rawJsonResponseParser.setWriterType("json");
            qreq.setResponseParser(rawJsonResponseParser);

            NamedList<Object> sresp = client.request(qreq, "favorites");
            return new JSONObject((String) sresp.get("response"));

          }
        } catch (Exception ex) {
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
