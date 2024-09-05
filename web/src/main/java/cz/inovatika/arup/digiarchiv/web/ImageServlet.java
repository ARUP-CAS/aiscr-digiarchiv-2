
package cz.inovatika.arup.digiarchiv.web;

import cz.inovatika.arup.digiarchiv.web.fedora.FedoraUtils;
import cz.inovatika.arup.digiarchiv.web.imagging.ImageAccess;
import cz.inovatika.arup.digiarchiv.web.imagging.ImageSupport;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@WebServlet(name = "ImageServlet", urlPatterns = {"/img/*"})
public class ImageServlet extends HttpServlet {

    public static final Logger LOGGER = Logger.getLogger(ImageServlet.class.getName());

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

        try {
            String action = request.getPathInfo().substring(1);
            if (action != null) {
                Actions actionToDo = Actions.valueOf(action.toUpperCase());
                actionToDo.doPerform(request, response, getServletContext());
                return;
            } else {
                response.getWriter().print("action -> " + action);
                return;
            }
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.toString());
            response.getWriter().print(e1.toString());
        } catch (SecurityException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.toString());
            response.getWriter().print(e1.toString());
        }

//        String id = request.getParameter("id");
//        boolean full = Boolean.parseBoolean(request.getParameter("full"));
//
//        if (!ImageAccess.isAllowed(request, full)) {
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            response.getWriter().println("insuficient rights!!");
//            return;
//        }
//
//        Options opts = Options.getInstance();
//        if (full) {
//            String imagesDir = opts.getString("imagesDir");
//            File f = new File(imagesDir + id);
//            String mime = getServletContext().getMimeType(f.getName());
//            if (mime != null) {
//                response.setContentType(mime);
//            } else {
//                response.setContentType("image/jpeg");
//            }
//            response.setHeader("Content-Disposition", "filename=" + f.getName());
//            IOUtils.copy(new FileInputStream(f), response.getOutputStream());
//            return;
//        }
//
//        if (id != null && !id.equals("")) {
//            String size = request.getParameter("size");
//            if (size == null) {
//                size = "thumb";
//            }
//
//            try {
//                //String fname = Options.getInstance().getString("thumbsDir") + id + ".jpg";
//                String dest = ImageSupport.getDestDir(id);
//                String fname = dest + id + "_" + size + ".jpg";
//                File f = new File(fname);
//                if (!f.exists() && size.equals("thumb")) {
//                    //a bug in PDFThumbsGenerator write name of thumb without id
//                    // check if exists _thumb.jpg in that directory
//                    if (new File(dest + "_thumb.jpg").exists()) {
//                        fname = dest + "_thumb.jpg";
//                        f = new File(fname);
//                    }
//                }
//                if (f.exists()) {
//                    String mime = getServletContext().getMimeType(f.getName());
//                    if (mime != null) {
//                        response.setContentType(mime);
//                    } else {
//                        response.setContentType("image/jpeg");
//                    }
//                    response.setHeader("Content-Disposition", "filename=" + f.getName());
//
//                    BufferedImage bi = ImageIO.read(f);
//                    ImageSupport.addWatermark(bi, logoImg(response, response.getOutputStream(), getServletContext()), (float) Options.getInstance().getDouble("watermark.alpha", 0.2f));
//                    ImageIO.write(bi, "jpg", response.getOutputStream());
//                } else {
//                    //LOG to file
//                    File file = new File(opts.getString("thumbsDir") + File.separator + "missed.txt");
//                    FileUtils.writeStringToFile(file, fname + System.getProperty("line.separator"), "UTF-8", true);
//                    LOGGER.log(Level.WARNING, "File does not exist in {0}. ", fname);
//                    emptyImg(response, response.getOutputStream(), getServletContext());
//                }
//            } catch (Exception ex) {
//                LOGGER.log(Level.SEVERE, null, ex);
//                emptyImg(response, response.getOutputStream(), getServletContext());
//            }
//        } else {
//            LOGGER.info("no id");
//            emptyImg(response, response.getOutputStream(), getServletContext());
//        }

    }

    private static BufferedImage logoImg(HttpServletResponse response, OutputStream out, ServletContext ctx) throws IOException {
        String empty = ctx.getRealPath(File.separator) + "/assets/img/logo-watermark-white.png";
        return ImageIO.read(new File(empty));

    }

    private static void emptyImg(HttpServletResponse response, OutputStream out, ServletContext ctx) throws IOException {
        String empty = ctx.getRealPath(File.separator) + "/assets/img/empty.png";
        response.setContentType("image/png");
        BufferedImage bi = ImageIO.read(new File(empty));
        ImageIO.write(bi, "png", out);
    }
    
    private static JSONObject getDocument(String id) throws Exception {
        SolrQuery query = new SolrQuery();
        query.setQuery("id:\"" + id + "\"");

        JSONObject json = SolrSearcher.json(IndexUtils.getClientNoOp(), "soubor", query);
        if (json.getJSONObject("response").getJSONArray("docs").length() == 0) {
            LOGGER.log(Level.WARNING, "{0} not found", id);
            return null;
        }
        return json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
        
    }

    private static InputStream getFromFedora(String id, String imgSize) throws Exception {
        SolrQuery query = new SolrQuery();
        query.setQuery("id:\"" + id + "\"");
        
        JSONObject doc = getDocument(id);
        if (doc == null) {
            return null;
        }
                
        String path = doc.getString("nazev");
        String url = doc.getString("path") + "/" + imgSize;
        url = url.substring(url.indexOf("record"));

        //String mime = doc.getString("mimetype");
        LOGGER.log(Level.FINE, "Requesting from {0}. ", url); 
        return FedoraUtils.requestInputStream(url);

    }

    private static void writeImg(HttpServletResponse response, String id, String imgSize, ServletContext ctx) throws Exception {
        
        
        SolrQuery query = new SolrQuery();
        query.setQuery("id:\"" + id + "\"");
        
        JSONObject doc = getDocument(id);
        if (doc == null) {
            emptyImg(response, response.getOutputStream(), ctx);
            return;
        }

        String mime = doc.optString("mimetype", "image/jpeg");
        if (imgSize.equals("thumb")) {
            mime = "image/png";
        } else {
            mime = "image/jpeg";
        }
        InputStream is = getFromFedora(id, imgSize);

        if (is != null) {
            // String mime = getServletContext().getMimeType(f.getName());
            response.setContentType(mime);
            
            response.setHeader("Content-Disposition", "filename=" + id);

            // BufferedImage bi = ImageIO.read(f);
            BufferedImage bi = ImageIO.read(is); 
            if (bi != null) {
                ImageSupport.addWatermark(bi, logoImg(response, response.getOutputStream(), ctx), (float) Options.getInstance().getDouble("watermark.alpha", 0.2f));
                ImageIO.write(bi, mime.split("/")[1], response.getOutputStream());
            } else {
                LOGGER.log(Level.WARNING, "Response is not image {0}. ", id);
                emptyImg(response, response.getOutputStream(), ctx);
            }

        } else { 
            //LOG to file
            //File file = new File(Options.getInstance().getString("thumbsDir") + File.separator + "missed.txt");
            //FileUtils.writeStringToFile(file, id + System.getProperty("line.separator"), "UTF-8", true);
            //LOGGER.log(Level.WARNING, "{0} not found", id);
            emptyImg(response, response.getOutputStream(), ctx);
        }
    }

    enum Actions {

        THUMB {
            @Override
            void doPerform(HttpServletRequest request, HttpServletResponse response, ServletContext ctx) throws Exception {
                String id = request.getParameter("id");
                if (id != null && !id.equals("")) {
                    try {
                        writeImg(response, id, "thumb", ctx);
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                        emptyImg(response, response.getOutputStream(), ctx);
                    }
                } else {
                    LOGGER.info("no id");
                    emptyImg(response, response.getOutputStream(), ctx);
                }

            }
        },
        MEDIUM {
            @Override
            void doPerform(HttpServletRequest request, HttpServletResponse response, ServletContext ctx) throws Exception {
                String id = request.getParameter("id");
                if (id != null && !id.equals("")) {
                    try {
                        writeImg(response, id, "thumb-large", ctx);
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                        emptyImg(response, response.getOutputStream(), ctx);
                    }
                } else {
                    LOGGER.info("no id");
                    emptyImg(response, response.getOutputStream(), ctx);
                }

            }
        },
        FULL {
            @Override
            void doPerform(HttpServletRequest request, HttpServletResponse response, ServletContext ctx) throws Exception {
                if (!ImageAccess.isAllowed(request, true)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().println("insuficient rights!!");
                    return;
                }
                String id = request.getParameter("id");
                if (id != null && !id.equals("")) {
                    try {
                        
                        JSONObject doc = getDocument(id);
                        if (doc == null) {
                            return;
                        }
        
                        InputStream is = getFromFedora(id, "orig");

                        String mime = doc.getString("mimetype");
                        if (mime != null) {
                            response.setContentType(mime);
                        } else {
                            response.setContentType("image/jpeg");
                        }
                        response.setHeader("Content-Disposition", "filename=" + doc.getString("nazev"));
                        IOUtils.copy(is, response.getOutputStream());

                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                        emptyImg(response, response.getOutputStream(), ctx);
                    }
                } else {
                    LOGGER.info("no id");
                    emptyImg(response, response.getOutputStream(), ctx);
                }

            }
        };
 
        abstract void doPerform(HttpServletRequest req, HttpServletResponse resp, ServletContext ctx) throws Exception;
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
