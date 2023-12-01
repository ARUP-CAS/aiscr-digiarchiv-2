/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web;

import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/user/*"})
public class LoginServlet extends HttpServlet {

    public static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

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

    public static JSONObject user(HttpServletRequest req) {
        JSONObject ses = (JSONObject) req.getSession().getAttribute("user");
        if (ses != null && !ses.has("error")) {
            return ses;
        } else {
            return new JSONObject();
        }

    }

    public static String userId(HttpServletRequest req) {
        return (String) req.getSession().getAttribute("userid");
    }

    public static String pristupnost(HttpSession session) {
        System.out.println(session);
        JSONObject ses = (JSONObject) session.getAttribute("user");
        String pristupnost = "A";
        if (ses != null && !ses.has("error")) {
            pristupnost = ses.getString("pristupnost");
        }
        return pristupnost;
    }

    public static String organizace(HttpSession session) {
        JSONObject ses = (JSONObject) session.getAttribute("user");
        if (ses != null && !ses.has("error")) {
            return ses.getJSONObject("organizace").getString("id");
        }
        return "";
    }

    public static boolean isLogged(HttpSession session) {
        return session.getAttribute("user") != null;
    }

    enum Actions {
        
        TOKEN { 
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                JSONObject jo = new JSONObject();
                String user = req.getParameter("user");
                String pwd = req.getParameter("pwd");
                jo.put("token", AuthService.getToken(user, pwd));
                return jo;
            }
        },
        ISLOGGED {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                int left = (int) Math.round(req.getSession().getMaxInactiveInterval() - (Instant.now().toEpochMilli() - req.getSession().getLastAccessedTime()) * .001);
                boolean expired = left < 0;
                JSONObject jo = new JSONObject();
                try {
                    if (expired || req.getSession(false) == null || req.getSession(false).getAttribute("user") == null) {
                        jo.put("error", "nologged");
                        req.getSession();
                    } else {
                        req.getSession().setMaxInactiveInterval(left);
                        if (Boolean.parseBoolean(req.getParameter("wantsUser"))) {
                            jo = (JSONObject) req.getSession().getAttribute("user");
                            jo.put("remaining", left);
                        } else {
                            jo.put("remaining", left);
                        }
                    }
                } catch (Exception ex) {
                    jo.put("error", ex.toString());
                }
                return jo;
            }
        },
        LOGIN { 
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                JSONObject jo = new JSONObject();
                try {

                    String user = req.getParameter("user");
                    String pwd = req.getParameter("pwd");
                    if (req.getMethod().equals("POST")) {
                        JSONObject js = new JSONObject(IOUtils.toString(req.getInputStream(), "UTF-8"));
                        user = js.getString("user");
                        pwd = js.getString("pwd");
                    }

                    if (user != null) {
                        
                        jo = AuthService.login(user, pwd);
                        LOGGER.log(Level.FINE, jo.toString(2));
                        req.getSession().setAttribute("user", jo);
                        req.getSession().setAttribute("userid", jo.getString("ident_cely"));
                        req.getSession().setMaxInactiveInterval(Options.getInstance().getInt("sessionTimeout", 300));

                    } else {
                        req.getSession().setAttribute("user", null);
                        req.getSession().setAttribute("userid", null);
                        jo.put("error", "no name");
                    }

                } catch (Exception ex) {
                    req.getSession().setAttribute("user", null);
                    req.getSession().setAttribute("userid", null);
                    jo.put("error", ex.toString());
                    LOGGER.log(Level.SEVERE, null, ex);
                }
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
                return jo;

            }
        },
        LOGIN_2 {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                JSONObject jo = new JSONObject();
                try {

                    String user = req.getParameter("user");
                    String pwd = req.getParameter("pwd");
                    if (req.getMethod().equals("POST")) {
                        JSONObject js = new JSONObject(IOUtils.toString(req.getInputStream(), "UTF-8"));
                        user = js.getString("user");
                        pwd = js.getString("pwd");
                    }

                    if (user != null) {
                        AmcrAPI amcr = new AmcrAPI();
                        jo = amcr.login(user, pwd);
                        if (jo.has("error")) {
                            return jo;
                        }
                        Iterator it = jo.keys();
                        if (it.hasNext()) {
                            String userid = (String) it.next();
                            JSONObject userJo = jo.getJSONObject(userid);
                            int auth = userJo.getInt("auth");

//Jméno uživ. skupiny    kód   kód binárně  Označení v hesláři Přístupnost
//anonym                  0     0                     A 
//badatel                 1     1                     B 
//archeolog               2     10                    C
//archivář                16    10000                 D
//administrátor           4     100                   E
// ((auth & kodSkupinyBinarne) == kodSkupinyBinarne)
                            if (((auth & 4) == 4)) {
                                userJo.put("pristupnost", "E");
                            } else if (((auth & 16) == 16)) {
                                userJo.put("pristupnost", "D");
                            } else if (((auth & 2) == 2)) {
                                userJo.put("pristupnost", "C");
                            } else if (((auth & 1) == 1)) {
                                userJo.put("pristupnost", "B");
                            } else if (((auth) == 0)) {
                                userJo.put("pristupnost", "A");
                            }

                            // Pridame organizace z heslaru
                            int organizaceId = userJo.getInt("organizace");
                            try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
                                SolrQuery query = new SolrQuery("id:" + organizaceId)
                                        .setRows(1)
                                        .addFilterQuery("heslar_name:organizace");
                                JSONObject res = SearchUtils.json(query, client, "heslar");
                                if (res.getJSONObject("response").optInt("numFound", 0) > 0) {
                                    String organizace = res.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("nazev_zkraceny");
                                    userJo.put("organizaceNazev", organizace);
                                } else {
                                    LOGGER.log(Level.WARNING, "Uzivatel bez organizace");
                                    userJo.put("organizaceNazev", "");
                                }

                            } catch (Exception ex) {
                                LOGGER.log(Level.WARNING, "Uzivatel bez organizace");
                                userJo.put("organizaceNazev", "");
                            }

                            LOGGER.log(Level.FINE, jo.toString(2));
                            req.getSession().setAttribute("user", jo);
                            req.getSession().setAttribute("userid", userid);
                            req.getSession().setMaxInactiveInterval(Options.getInstance().getInt("sessionTimeout", 300));
                        }

                    } else {
                        req.getSession().setAttribute("user", null);
                        req.getSession().setAttribute("userid", null);
                        jo.put("error", "no name");
                    }

                } catch (Exception ex) {
                    req.getSession().setAttribute("user", null);
                    req.getSession().setAttribute("userid", null);
                    jo.put("error", ex.toString());
                    LOGGER.log(Level.SEVERE, null, ex);
                }

                return jo;

            }
        },
        LOGOUT {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                JSONObject jo = new JSONObject();
                try {
                    req.getSession().invalidate();
                    jo.put("msg", "logged out");

                } catch (Exception ex) {
                    jo.put("error", ex.toString());
                }
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
                return jo;
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
