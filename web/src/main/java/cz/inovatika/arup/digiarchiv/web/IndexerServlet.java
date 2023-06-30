/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web;

import cz.inovatika.arup.digiarchiv.web.fedora.FedoraHarvester;
import cz.inovatika.arup.digiarchiv.web.index.HeslarIndexer;
import cz.inovatika.arup.digiarchiv.web.index.IndexerTranslations;
import cz.inovatika.arup.digiarchiv.web.index.OAIIndexer;
import cz.inovatika.arup.digiarchiv.web.index.OAIUtils;
import cz.inovatika.arup.digiarchiv.web.index.models.ADB;
import cz.inovatika.arup.digiarchiv.web.index.models.Akce;
import cz.inovatika.arup.digiarchiv.web.index.models.DokJednotka;
import cz.inovatika.arup.digiarchiv.web.index.models.Dokument;
import cz.inovatika.arup.digiarchiv.web.index.models.ExtZdroj;
import cz.inovatika.arup.digiarchiv.web.index.models.Let;
import cz.inovatika.arup.digiarchiv.web.index.models.Lokalita;
import cz.inovatika.arup.digiarchiv.web.index.models.Pian;
import cz.inovatika.arup.digiarchiv.web.index.models.Projekt;
import cz.inovatika.arup.digiarchiv.web.index.models.SamostatniNalez;
import cz.inovatika.arup.digiarchiv.web.index.models.Soubor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONArray;
import org.json.JSONException;
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

    SHOWID {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        try {
          json = OAIUtils.getId(req.getParameter("id"));

        } catch (Exception ex) {
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    BYID { 
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        try {
          json = OAIUtils.indexId(req.getParameter("id"));

        } catch (Exception ex) {
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    HESLARE {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        try {

          HeslarIndexer indexer = new HeslarIndexer();
          if (Boolean.parseBoolean(req.getParameter("clean"))) {
            json.put("clean", indexer.clean());
          }
          json = indexer.indexHeslare();
        } catch (IOException ex) {
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    AKCE {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        try {
          json = OAIIndexer.full("akce", Akce.class, Boolean.parseBoolean(req.getParameter("clean")));
        } catch (JSONException ex) {
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    FULL {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        Date start = new Date();
        try {
          boolean oldIndexing = Options.setIndexingFlag(true);

          if (oldIndexing) {
            json.put("error", "Indexing already in progress.");
          } else {
            JSONArray ja = new JSONArray();
            Options.resetInstance();
//            HeslarIndexer h = new HeslarIndexer();
//            boolean clean = req.getParameter("clean") == null || Boolean.parseBoolean(req.getParameter("clean"));
//            if (clean) {
//              h.clean();
//            }
//            ja.put(h.indexHeslare());

            ja.put(IndexerTranslations.fromCSV());

            String[] actions = new String[]{"SOUBOR", "ENTITIES"};
            for (String action : actions) {
              ja.put(Actions.valueOf(action).doPerform(req, resp));
            }
            json.put("full", ja);

            if (!Options.setIndexingFlag(oldIndexing)) {
              // shouldn't happen
              LOGGER.log(Level.WARNING, "indexing flag updates crossed");
            }
          }
        } catch (IOException ex) {
          Options.setIndexingFlag(false);
          json.put("error", ex.toString());
        }
        Date end = new Date();
        json.put("ellapsed time", FormatUtils.formatInterval(end.getTime() - start.getTime()));
        return json;
      }
    },
    TRANSLATIONS {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        try {
          json = IndexerTranslations.fromCSV();
          // json = indexer.fromCSV();
        } catch (JSONException ex) {
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    ENTITIES {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        Date start = new Date();
        try {
          JSONArray ja = new JSONArray();
          Options.resetInstance();
          String[] actions = req.getParameterValues("entity");
          if (actions == null) {
            actions = new String[]{"ADB", "EXT_ZDROJ", "PIAN", "LET", "DOK_JEDNOTKA", "LOKALITA", "AKCE", "DOKUMENT", "SAMOSTATNY_NALEZ", "PROJEKT", "LOKALITA2"};
          }
          for (String action : actions) {
            ja.put(Actions.valueOf(action.toUpperCase()).doPerform(req, resp));
          }
          json.put("entities", ja);

          Date end = new Date();
          String ellapsed = FormatUtils.formatInterval(end.getTime() - start.getTime());
          LOGGER.log(Level.INFO, "Indexing FINISHED. Time: {0}", ellapsed);
        } catch (Exception ex) {
          Options.setIndexingFlag(false);
          json.put("error", ex.toString());
        }
        Date end = new Date();
        json.put("ellapsed time", FormatUtils.formatInterval(end.getTime() - start.getTime()));
        return json;
      }
    },
    DOKUMENT {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        try {
          json = OAIIndexer.full("dokument", Dokument.class, Boolean.parseBoolean(req.getParameter("clean")));
        } catch (JSONException ex) {
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    LOKALITA {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        try {
          json = OAIIndexer.full("lokalita", Lokalita.class, Boolean.parseBoolean(req.getParameter("clean")));
        } catch (JSONException ex) {
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    LOKALITA2 {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        try {
          json = OAIIndexer.full("lokalita", Lokalita.class, Boolean.parseBoolean(req.getParameter("clean")), true);
        } catch (JSONException ex) {
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    SAMOSTATNY_NALEZ {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        try {
          json = OAIIndexer.full("samostatny_nalez", SamostatniNalez.class, Boolean.parseBoolean(req.getParameter("clean")));
        } catch (JSONException ex) {
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    ADB {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        try {
          json = OAIIndexer.full("adb", ADB.class, Boolean.parseBoolean(req.getParameter("clean")));
        } catch (JSONException ex) {
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    PIAN {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        try {
          json = OAIIndexer.full("pian", Pian.class, Boolean.parseBoolean(req.getParameter("clean")));
        } catch (JSONException ex) {
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    EXT_ZDROJ {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        try {
          json = OAIIndexer.full("ext_zdroj", ExtZdroj.class, Boolean.parseBoolean(req.getParameter("clean")));
        } catch (JSONException ex) {
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    LET {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        try {
          json = OAIIndexer.full("let", Let.class, Boolean.parseBoolean(req.getParameter("clean")));
        } catch (JSONException ex) {
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    PROJEKT {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        try {
          json = OAIIndexer.full("projekt", Projekt.class, Boolean.parseBoolean(req.getParameter("clean")));
        } catch (JSONException ex) {
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    SOUBOR {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        try {

          json = OAIIndexer.full("soubor", Soubor.class, Boolean.parseBoolean(req.getParameter("clean")));
        } catch (JSONException ex) {
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    DOK_JEDNOTKA {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JSONObject json = new JSONObject();
        try {
          json = OAIIndexer.full("dok_jednotka", DokJednotka.class, Boolean.parseBoolean(req.getParameter("clean")));
        } catch (JSONException ex) {
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    FEDORA { 
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
