/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@WebServlet(name = "HeatImageServlet", urlPatterns = {"/heatimg"})
public class HeatImageServlet extends HttpServlet {

  public static final Logger LOGGER = Logger.getLogger(HeatImageServlet.class.getName());

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

    try (OutputStream out = response.getOutputStream()) {
      response.setContentType("image/png");

      try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("akceCore")).build()) {
        SolrQuery query = new SolrQuery("*:*")
                .setFacet(true)
                .setParam("facet.heatmap", "loc_rpt")
                .setParam("facet.heatmap.format", "png")
                //.setParam("facet.heatmap.maxCells", "400000")
                //.setParam("facet.heatmap.maxLevel", "7")
                .setParam("facet.heatmap.geom", "[\"12.3651123046875 48.72720881940671\" TO \"18.6273193359375 50.85450904781293\"]")
                .setParam("rows", "0");

        QueryRequest req = new QueryRequest(query);

        NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
        rawJsonResponseParser.setWriterType("json");
        req.setResponseParser(rawJsonResponseParser);

        NamedList<Object> resp = client.request(req);
        JSONObject r = new JSONObject((String)resp.get("response"));
        
        //System.out.println(r.toString(2));
                
        System.out.println(r.getJSONObject("facet_counts"));
        String png = r.getJSONObject("facet_counts")
                .getJSONObject("facet_heatmaps")
                .getJSONObject("loc_rpt")
                .getString("counts_png");
        
        BufferedImage image = null;
        byte[] imageByte;
        try {
            Base64 decoder = new Base64();
            imageByte = decoder.decode(png);
            ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
            image = ImageIO.read(bis);
            bis.close();
            ImageIO.write(image, "png", out);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        

      } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }

    }
  }

  private BufferedImage logoImg(HttpServletResponse response, OutputStream out) throws IOException {
    String empty = getServletContext().getRealPath(File.separator) + "/assets/img/logo-watermark-white.png";
    response.setContentType("image/gif");
    return ImageIO.read(new File(empty));

  }

  private void emptyImg(HttpServletResponse response, OutputStream out) throws IOException {
    String empty = getServletContext().getRealPath(File.separator) + "/assets/img/empty.gif";
    response.setContentType("image/gif");
    BufferedImage bi = ImageIO.read(new File(empty));
    ImageIO.write(bi, "gif", out);

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
