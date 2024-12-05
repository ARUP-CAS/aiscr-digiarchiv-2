package cz.inovatika.arup.digiarchiv.web;

import cz.inovatika.arup.digiarchiv.web.fedora.FedoraUtils;
import cz.inovatika.arup.digiarchiv.web.imagging.ImageSupport;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@WebServlet(name = "HandleServlet", urlPatterns = {"/id/*"})
public class HandleServlet extends HttpServlet {

    public static final Logger LOGGER = Logger.getLogger(HandleServlet.class.getName());

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

        String id = request.getPathInfo().substring(1);
        if (id.contains("file")) {
            // response.getWriter().println(id);
            // RequestDispatcher rd = request.getRequestDispatcher("/img/" + request.getPathInfo());
//            RequestDispatcher rd = request.getRequestDispatcher("/img");
//            rd.include(request, response);
            try {
                //Logger.getLogger(HandleServlet.class.getName()).log(Level.INFO, "getFile started");
                // Check if IP call could run by time limits
                String ip = request.getRemoteAddr();
                long retryTime = AppState.canGetFileInterval(ip, id);
                    if (retryTime > 0) {
                        response.setStatus(429); // 429 Too Many Requests
                        response.addHeader("Retry-After", retryTime + "");
                        response.getWriter().print("Try in " + retryTime + " seconds.");
                        return;
                    } else if (retryTime == -1){
                        response.setStatus(429); // 429 Too Many Requests
                        response.getWriter().print("Downloading file still in progress. Try later.");
                        return;
                    }
                AppState.writeGetFileStarted(ip, id);
                boolean success = getFile(id, request, response);
                // Logs IP ends time
                AppState.writeGetFileFinished(ip, id, success);
                //Logger.getLogger(HandleServlet.class.getName()).log(Level.INFO, "getFile end");
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        } else {
            try (PrintWriter out = response.getWriter()) {
                response.setContentType("text/html;charset=UTF-8");
                String url = "http://localhost:4000/id/" + id;
                try (InputStream inputStream = RESTHelper.inputStream(url)) {
                    out.println(org.apache.commons.io.IOUtils.toString(inputStream, "UTF-8"));
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error processing {0}", request.getRequestURI());
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    private static void getPdfPage(JSONObject soubor, String page, HttpServletRequest request, HttpServletResponse response) throws Exception {

        try (OutputStream out = response.getOutputStream()) {
            String id = soubor.getString("id");
            String filename = soubor.getString("nazev");
            if (id != null && !id.equals("")) {
                try {
                    // Tady stranky zacinaji na 1. Generovane na 0 
                    int pageIndex = Integer.parseInt(page) - 1;
                    String fname = ImageSupport.getDestDir(id) + id + File.separator + pageIndex + ".jpg";
                    File f = new File(fname);
                    if (f.exists()) {
                        response.setContentType("image/jpeg");
                        response.setHeader("Content-Disposition", "attachment; filename=" + filename + "_" + page + ".jpg");
                        BufferedImage bi = ImageIO.read(f);
                        ImageIO.write(bi, "jpg", out);
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        LOGGER.log(Level.WARNING, "File does not exist in {0}. ", fname);
                    }

                } catch (Exception ex) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private static boolean getFile(String id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject user = LoginServlet.user(request);
        final String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
            // Authorization: Basic base64credentials
            String base64Credentials = authorization.substring("Basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            // credentials = username:password
            final String[] values = credentials.split(":", 2);
            user = AuthService.login(values[0], values[1]);
            if (!user.has("error")) {
                request.getSession().setAttribute("user", user);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().print("Invalid credentials");
                return false;
            }
        }

        if (id != null && !id.equals("")) {
            File f = File.createTempFile("img-", "-orig", new File(InitServlet.TEMP_DIR));
            try {
                JSONObject doc = getDocument(id, user);
                if (doc == null) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    LOGGER.log(Level.WARNING, "{0} not found", id);
                    return false;
                }

                if (doc.optBoolean("not_found")) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    // LOGGER.log(Level.WARNING, "{0} not found", id);
                    return false;
                }

                if (doc.optBoolean("invalid")) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    // LOGGER.log(Level.WARNING, "{0} not allowed", id);
                    return false;
                }

                if (doc.optBoolean("not_allowed")) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    LOGGER.log(Level.WARNING, "{0} not allowed", id);
                    return false;
                }

                String mime = doc.optString("mimetype");
                if (id.contains("page") && mime.contains("pdf")) {
                    String page = id.substring(id.lastIndexOf("/") + 1);
                    getPdfPage(doc, page, request, response);
                    return false;
                }

                String filename = doc.getString("nazev");

                String url = doc.getString("path");
                if (id.contains("page")) {
                    url += "/thumb-large";
                    response.setContentType("image/png");
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + ".png\"");
                } else if (id.contains("thumb")) {
                    url += "/thumb";
                    response.setContentType("image/png");
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + ".png\"");
                } else {
                    url += "/orig"; 
                    response.setContentType(mime);
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                }
                url = url.substring(url.indexOf("record"));
                InputStream is = FedoraUtils.requestInputStream(url);
                FileUtils.copyInputStreamToFile(is, f);
                LOGGER.log(Level.FINE, "bytes received: {0}", f.length());
                IOUtils.copy(new FileInputStream(f), response.getOutputStream());
                if (!id.contains("thumb")) {
                    LogAnalytics.log(request, doc.getString("path"), "file", doc.getString("entity"));
                }
                is.close();
                return true;

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            } finally {
                f.delete();
            }
        }
        return true;
    }

    private static boolean isAllowed(String id, JSONObject doc, JSONObject user) {
        if (id.contains("thumb") && !id.contains("page")) {
            return true;
        }
        
        String entity = doc.optString("entity");
        int stav = doc.optInt("stav");
        String docPr = doc.getString("pristupnost");

        String userPr = user.optString("pristupnost", "A");
        String userId = user.optString("ident_cely", "A");
        String userOrg = "none";
        if (user.has("organizace")) {
            userOrg = user.getJSONObject("organizace").optString("id", "");
        }

        switch (entity) {
            case "projekt":
//-- A-B: nikdy
//-- C: projekt/stav = 1 OR (projekt/stav >= 2 AND projekt/stav <= 6 AND projekt/organizace = {user}.organizace)
//-- D-E: bez omezení
                String docOrg = doc.optString("projekt_organizace");
                boolean sameOrg = userOrg.toLowerCase().equals(docOrg.toLowerCase());
                if (userPr.equalsIgnoreCase("C")
                        && ((stav == 1) || (sameOrg && stav <= 6))) {
                    return true;
                } else {
                    return userPr.compareToIgnoreCase("D") >= 0;
                }
            case "dokument":
            case "knihovna_3d":
//-- A: dokument/pristupnost = A AND dokument/stav = 3
//-- B: (dokument/pristupnost <= B AND dokument/stav = 3) OR dokument/historie[typ_zmeny='D01']/uzivatel = {user}
//-- C: (dokument/pristupnost <= C AND dokument/stav = 3) OR dokument/historie[typ_zmeny='D01']/uzivatel.organizace = {user}.organizace
//-- D-E: bez omezení
                if (userPr.equalsIgnoreCase("A") && docPr.equalsIgnoreCase("A") && stav == 3) {
                    return true;
                } else if (userPr.equalsIgnoreCase("B")) {
                    if (docPr.compareToIgnoreCase("B") <= 0 && stav == 3) {
                        return true; 
                    }

                    JSONArray h = doc.getJSONArray("historie");
                    String uzivatel = null;
                    for (int i = 0; i < h.length(); i++) {
                        JSONObject hi = h.getJSONObject(i);
                        if ("D01".equals(hi.optString("typ_zmeny"))) {
                            uzivatel = hi.getJSONObject("uzivatel").getString("id");
                        }
                    }
                    return (userId.equals(uzivatel));

                } else if (userPr.equalsIgnoreCase("C")) {
                    if (docPr.compareToIgnoreCase("C") <= 0 && stav == 3) {
                        return true;
                    }

                    JSONArray h = doc.getJSONArray("historie");
                    String uzivatel = "KKK";
                    for (int i = 0; i < h.length(); i++) {
                        JSONObject hi = h.getJSONObject(i);
                        if ("D01".equals(hi.optString("typ_zmeny"))) {
                            uzivatel = hi.getJSONObject("uzivatel").getString("id");
                        }
                    }
                    return (userOrg.equals(SolrSearcher.getOrganizaceUzivatele(uzivatel)));

                } else {
                    return userPr.compareToIgnoreCase("D") >= 0;
                }
            case "samostatny_nalez":
//-- A: samostatny_nalez/pristupnost = A AND samostatny_nalez/stav = 4
//-- B: (samostatny_nalez/pristupnost <= B AND samostatny_nalez/stav = 4) OR samostatny_nalez/historie[typ_zmeny='SN01']/uzivatel = {user}
//-- C: (samostatny_nalez/pristupnost <= B AND samostatny_nalez/stav = 4) OR samostatny_nalez/historie[typ_zmeny='SN01']/uzivatel = {user} OR projekt/organizace = {user}.organizace
//-- D-E: bez omezení
                if (userPr.equalsIgnoreCase("A") && stav == 4) {
                    return true;
                } else if (userPr.equalsIgnoreCase("B")) {
                    if (docPr.compareToIgnoreCase("B") <= 0 && stav == 4) {
                        return true;
                    }

                    JSONArray h = doc.getJSONArray("historie");
                    String uzivatel = null;
                    for (int i = 0; i < h.length(); i++) {
                        JSONObject hi = h.getJSONObject(i);
                        if ("SN01".equals(hi.optString("typ_zmeny"))) {
                            uzivatel = hi.getJSONObject("uzivatel").getString("id");
                        }
                    }
                    return (userId.equals(uzivatel));

                } else if (userPr.equalsIgnoreCase("C")) {
                    if (docPr.compareToIgnoreCase("C") <= 0 && stav == 4) {
                        return true;
                    }

                    JSONArray h = doc.getJSONArray("historie");
                    String uzivatel = "KKK";

                    for (int i = 0; i < h.length(); i++) {
                        JSONObject hi = h.getJSONObject(i);
                        if ("D01".equals(hi.optString("typ_zmeny"))) {
                            uzivatel = hi.getJSONObject("uzivatel").getString("id");
                        }
                    }
                    if (userOrg.equals(SolrSearcher.getOrganizaceUzivatele(uzivatel))) {
                        return true;
                    }

                    String projektId = doc.optString("samostatny_nalez_projekt");
                    String projektOrg = null;
                    SolrQuery query = new SolrQuery("ident_cely:\"" + projektId + "\"")
                            .setFields("projekt_organizace");
                    JSONObject json = SearchUtils.searchById(query, "entities", projektId, false);

                    if (json.getJSONObject("response").getInt("numFound") > 0) {
                        projektOrg = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("projekt_organizace");
                    }

                    return (userOrg.equals(projektOrg));

                } else {
                    return userPr.compareToIgnoreCase("D") >= 0;
                }
            default:
                return false;
        }
    }

    private static JSONObject getDocument(String id, JSONObject user) throws Exception {

//-- C-202300529/file/3a1a5793-535a-4352-884f-69756d51d9b2
        String soubor_filepath = "rest/AMCR/record/" + id;
        
        
        if (id.contains("thumb") && !id.contains("page") && !id.endsWith("thumb")) { 
//-- C-202300529/file/3a1a5793-535a-4352-884f-69756d51d9b2/thumb/1            
            LOGGER.log(Level.WARNING, "{0} is invalid", id);
            return new JSONObject().put("invalid", true);
        }
        
        if (id.contains("thumb")) {
//-- C-202300529/file/3a1a5793-535a-4352-884f-69756d51d9b2/thumb
//-- C-202300529/file/3a1a5793-535a-4352-884f-69756d51d9b2/thumb/page/1            
            soubor_filepath = soubor_filepath.substring(0, soubor_filepath.indexOf("/thumb"));
        }

        SolrQuery query = new SolrQuery("*")
                .addSort("datestamp", SolrQuery.ORDER.desc)
                .setFields("entity,pristupnost,stav,samostatny_nalez_projekt,projekt_organizace,soubor:[json],historie:[json]")
                .addFilterQuery("soubor_filepath:\"" + soubor_filepath + "\"");

        JSONObject json = SolrSearcher.jsonSelect(IndexUtils.getClientNoOp(), "entities", query);
        if (json.getJSONObject("response").getJSONArray("docs").length() == 0) {
            LOGGER.log(Level.WARNING, "{0} not found", id);
            return new JSONObject().put("not_found", true);
        }
        JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
        
        
        if (!isAllowed(id, doc, user)) {
            
            LOGGER.log(Level.WARNING, "{0} not allowed", id);
            return new JSONObject().put("not_allowed", true);
        }

        JSONArray soubor = doc.getJSONArray("soubor");
        for (int i = 0; i < soubor.length(); i++) {
            JSONObject sdoc = soubor.getJSONObject(i);
            if (soubor_filepath.equals(sdoc.optString("path"))) {
//                doc.put("id", sdoc.optString("id"));
//                doc.put("mimetype", sdoc.optString("mimetype"));
//                doc.put("path", sdoc.optString("path"));
//                doc.put("nazev", sdoc.optString("nazev"));
                return sdoc;
            }
        }
        return new JSONObject().put("not_found", true);

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
