package cz.inovatika.arup.digiarchiv.web4;
import java.io.File;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 *
 * @author alberto
 */
@WebServlet(name = "TextsServlet", urlPatterns = {"/texts/*"})
public class TextsServlet extends HttpServlet {

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
    response.setContentType("text/html;charset=UTF-8");

    String lang = request.getParameter("lang");
    String filename = InitServlet.CONFIG_DIR + File.separator + "texts"
            + File.separator + InitServlet.asSafePath(request.getParameter("id"));
    File f;
    if (lang != null) {
      f = new File(filename + "_" + lang + ".html");
      if (f.exists()) {
        FileUtils.copyFile(f, response.getOutputStream());
      }else {
        f = new File(filename + ".html");
        if (f.exists()) {
          FileUtils.copyFile(f, response.getOutputStream());
        } else {
          response.getWriter().println("Text not found in <h1>" + StringEscapeUtils.escapeHtml4(filename) + ".html</h1>");
        }
      }
    } else {
      f = new File(filename + ".html");
      if (f.exists()) {
        FileUtils.copyFile(f, response.getOutputStream());
      } else {
        response.getWriter().println("Text not found in <h1>" + StringEscapeUtils.escapeHtml4(filename) + ".html</h1>");
      }
    }
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
