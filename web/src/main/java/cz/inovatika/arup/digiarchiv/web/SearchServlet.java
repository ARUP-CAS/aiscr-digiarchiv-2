/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web;

import cz.inovatika.arup.digiarchiv.web.index.DokumentSearcher;
import cz.inovatika.arup.digiarchiv.web.index.EntitySearcher;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;
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
        try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
          SolrQuery query = new SolrQuery("*")
                  .setRequestHandler("/search")
                  .setRows(0)
                  .setFacet(true).addFacetField("kategorie_dokumentu", "rada")
                  .setParam("json.nl", "map");
          JSONObject jo = SearchUtils.json(query, client, "entities");
          json = jo.getJSONObject("facet_counts").getJSONObject("facet_fields").getJSONObject("entity");
          json.put("kategorie", jo.getJSONObject("facet_counts").getJSONObject("facet_fields").getJSONObject("kategorie_dokumentu"));

        } catch (Exception ex) {
          json.put("error", ex);
        }
        return json.toString();
      }
    },
    ID {
      @Override
      String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {

        JSONObject json = new JSONObject();
        try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
           SolrQuery query = new SolrQuery("ident_cely:\"" + request.getParameter("id") + "\"")
                   .setFacet(false);
           query.setRequestHandler("/search");
          query.setFields("*,dok_jednotka:[json],pian:[json],adb:[json],jednotka_dokumentu:[json],nalez_dokumentu:[json],"
                  + "ext_zdroj:[json],vazba_projekt_akce:[json],akce:[json],soubor:[json],let:[json],nalez:[json],vyskovy_bod:[json],"
                  + "dokument:[json],projekt:[json],samostatny_nalez:[json],komponenta:[json],komponenta_dokument:[json],neident_akce:[json]");
          JSONObject jo = SearchUtils.json(query, client, "entities");
          if (jo.getJSONObject("response").optInt("numFound", 0) > 0) {
            String entity = jo.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("entity");
            EntitySearcher searcher = SearchUtils.getSearcher(entity);
            if (searcher != null) {
              searcher.getChilds(jo, client, request);
            }
            // json = jo.getJSONObject("response").getJSONArray("docs").getJSONObject(0);

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

        String entity = "" + request.getParameter("entity");
        EntitySearcher searcher = SearchUtils.getSearcher(entity);
        if (searcher == null) {
          searcher = new DokumentSearcher();
        }
        JSONObject jo = searcher.search(request);
        return jo.toString();
      }
    },
    GETHESLAR {
      @Override
      String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {

        JSONObject json = new JSONObject();
        try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
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
    OBDOBI {
      @Override
      String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
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
        try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
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
