/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web4;

import cz.inovatika.arup.digiarchiv.web4.index.EntitySearcher;
import cz.inovatika.arup.digiarchiv.web4.index.SearchUtils;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author alber
 */
@WebServlet(name = "XLSXServlet", urlPatterns = {"/xlsx"})
public class XLSXServlet extends HttpServlet {

  public static final Logger LOGGER = Logger.getLogger(XLSXServlet.class.getName());

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
    response.setContentType("application/xlsx;charset=UTF-8");
    try {
      response.setHeader("Content-Disposition", "filename=export.xlsx");
      String entity = "" + request.getParameter("entity");
      EntitySearcher searcher = SearchUtils.getSearcher(entity);
      if (searcher == null) {
        return;
      }
      String csv = searcher.export(request);
      csvToXLSX(csv, response, entity);
    } catch (Exception e1) {
      LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.toString());
      response.getWriter().print(e1.toString());
    }
  }

  public static void csvToXLSX(String csv, HttpServletResponse response, String entity) {
    try {
      XSSFWorkbook workBook = new XSSFWorkbook();
      XSSFSheet sheet = workBook.createSheet(entity);
      String currentLine = null;
      int rowNum = 0;
      BufferedReader br = new BufferedReader(new StringReader(csv));
      while ((currentLine = br.readLine()) != null) {
        String str[] = currentLine.split(",");
        XSSFRow currentRow = sheet.createRow(rowNum++);
        for (int i = 0; i < str.length; i++) {
          XSSFCell cell = currentRow.createCell(i);
          cell.setCellType(CellType.STRING);
          cell.setCellValue(str[i]);
        }
      }
      br.close();
      workBook.write(response.getOutputStream());
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
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
