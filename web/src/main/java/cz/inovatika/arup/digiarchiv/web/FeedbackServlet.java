/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.mail.SimpleEmail;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.mail.DefaultAuthenticator;
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
      
      if (js.has("reCaptchaMsg")) {
          String r = verifyRecaptcha(js.getString("reCaptchaMsg"), js.getString("key"));
          response.getWriter().println(r);
          return;
      }
    
      String systemMail = Options.getInstance().getJSONObject("mail").getString("destMail");
      sendMail(js.getString("name"), systemMail, js.getString("text"), js.getString("ident_cely"), js.getString("mail")).toString();
      response.getWriter().println(sendMail(js.getString("name"), js.getString("mail"), js.getString("text"), js.getString("ident_cely"), systemMail).toString());
  }
  
  private static String verifyRecaptcha(String reCaptchaMsg, String key){
      
//      {
//  "event": {
//    "token": "TOKEN",
//    "expectedAction": "USER_ACTION",
//    "siteKey": "6Ld-VoUdAAAAAHJBY8-h6I-h6Gf2nFwY-gY4ndre",
//  }
//}
      
        try {
            String apikey = Options.getInstance().getString("reCaptchaApiKey");
            HttpClient client = HttpClient.newHttpClient();
            JSONObject body =new JSONObject();
            JSONObject event = new JSONObject().put("token", reCaptchaMsg).put("siteKey",  key).put("expectedAction", "USER_ACTION");
            body.put("event", event);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://recaptchaenterprise.googleapis.com/v1/projects/ais-cr-1684933938335/assessments?key=" + apikey ))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    //.header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String r = response.body();
            return r;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return ex.toString();
        }
    }


  private JSONObject sendMail(String fromName, String fromMail, String text, String ident_cely, String toMail) {
    JSONObject ret = new JSONObject();
     try {
      SimpleEmail email = new SimpleEmail();
      JSONObject mail = Options.getInstance().getJSONObject("mail");
      email.setHostName(mail.getString("smtp.host")); 
      email.setSmtpPort(mail.getInt("smtp.port"));
      email.setCharset("utf-8");
      // email.setAuthentication(mail.getString("smtp.user"), mail.getString("smtp.pwd"));
      // email.setAuthenticator(new DefaultAuthenticator(mail.getString("smtp.user"), mail.getString("smtp.pwd")));
      // email.setSSLOnConnect(true);

      if (mail.has("smtp.starttls.enable")) {
        email.setSSLOnConnect(mail.getBoolean("smtp.starttls.enable"));
      }

      email.addTo(toMail);
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
