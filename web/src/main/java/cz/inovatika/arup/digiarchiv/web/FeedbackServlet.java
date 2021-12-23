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
import org.apache.commons.mail.SimpleEmail;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.mail.EmailException;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@WebServlet(name = "FeedbackServlet", urlPatterns = {"/feedback"})
public class FeedbackServlet extends HttpServlet {

  public static final Logger LOGGER = Logger.getLogger(FeedbackServlet.class.getName());

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
      JSONObject js = new JSONObject(IOUtils.toString(request.getInputStream(), "UTF-8"));
      // sendMail(js.getString("name"), js.getString("mail"), js.getString("text"));
      response.getWriter().println(sendMail(js.getString("name"), js.getString("mail"), js.getString("text"), js.getString("ident_cely")).toString());
  }

  private JSONObject sendMail(String fromName, String fromMail, String text, String ident_cely) {
    JSONObject ret = new JSONObject();
    try {
      SimpleEmail email = new SimpleEmail();
      JSONObject mail = Options.getInstance().getJSONObject("mail");
      email.setHostName(mail.getString("smtp.host"));
      email.setSmtpPort(mail.getInt("smtp.port"));
      email.setCharset("utf-8");

      if (mail.has("smtp.starttls.enable")) {
        email.setSSLOnConnect(mail.getBoolean("smtp.starttls.enable"));
      }

      email.addTo(mail.getString("destMail"));
      email.setFrom(fromMail, fromName);
      email.setSubject(mail.getString("subject") + ident_cely);
      email.setMsg("Komentář k záznamu https://digiarchiv.aiscr.cz/id/"+ident_cely+":\n\n" + text);
      email.send();
      ret.put("hasError", false);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("hasError", true).put("error", ex);
    }
    return ret;
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
