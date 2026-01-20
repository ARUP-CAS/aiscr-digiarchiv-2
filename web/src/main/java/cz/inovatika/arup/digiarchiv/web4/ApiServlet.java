
package cz.inovatika.arup.digiarchiv.web4;

import jakarta.servlet.RequestDispatcher;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author alber
 */
@WebServlet(name = "ApiServlet", urlPatterns = {"/api"})
public class ApiServlet extends HttpServlet {

    /**
     * Resolves and validates a safe internal path for API forwarding based on the
     * provided path info. Returns {@code null} if the path is invalid or not allowed.
     */
    private String getSafePath(String pathInfo) {
        if (pathInfo == null || pathInfo.isEmpty()) {
            return null;
        }

        // Disallow backslashes and parent directory traversal.
        if (pathInfo.contains("..") || pathInfo.contains("\\")) {
            return null;
        }

        // Ensure the path starts with a single '/' so Paths.get works as expected.
        String normalizedInput = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;

        Path normalized;
        try {
            normalized = Paths.get(normalizedInput).normalize();
        } catch (Exception ex) {
            return null;
        }

        if (normalized.startsWith("..")) {
            return null;
        }

        // Map all API resources under a fixed internal prefix.
        String relative = normalized.toString().replace('\\', '/');
        if (!relative.startsWith("/")) {
            relative = "/" + relative;
        }
        String dispatcherPath = "/WEB-INF/api" + relative;
        return dispatcherPath;
    }

    /**
     * 
     * 
     */
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
        String safePath = getSafePath(request.getPathInfo());
        if (safePath == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        // String path = request.getPathInfo();
        RequestDispatcher rd = request.getRequestDispatcher(safePath);
        rd.forward(request, response);
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
