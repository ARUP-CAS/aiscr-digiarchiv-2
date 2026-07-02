/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web4;

import cz.inovatika.arup.digiarchiv.web4.index.EntitySearcher;
import cz.inovatika.arup.digiarchiv.web4.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web4.index.SolrSearcher;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.StringReader;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

/**
 *
 * @author alber
 */
@WebServlet(name = "ExportServlet", urlPatterns = {"/exp"})
public class ExportServlet extends HttpServlet {

  public static final Logger LOGGER = Logger.getLogger(ExportServlet.class.getName());

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
      String entity = "" + request.getParameter("entity");
      EntitySearcher searcher = SearchUtils.getSearcher(entity);
      if (searcher == null) {
        return;
      }
      JSONObject jo = searcher.export(request);
      String format = request.getParameter("format");
      Instant now = Instant.now();
      response.setHeader("Content-Disposition", "filename=export_" + entity + "_" + now.toEpochMilli() + "." + format);
      switch (format) {
        case "csv":
        case "xlsx":
          List<String> labels = SolrSearcher.getExportField(entity, "label");
          JSONArray ls = new JSONArray(labels);
          String csv = org.json.CDL.rowToString(new JSONArray(labels));
          csv += org.json.CDL.toString(ls, jo.getJSONObject("response").getJSONArray("docs"));
          if ("xlsx".equals(format)) {
            response.setContentType("application/xlsx;charset=UTF-8");
            csvToXLSX(csv, response, entity);
          } else {
            response.setContentType("text/csv;charset=UTF-8");
            response.getWriter().print(csv);
          }
          break;
        case "xml":
          response.setContentType("text/xml;charset=UTF-8");
          String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><docs>" + XML.toString(jo.getJSONObject("response").getJSONArray("docs"), "doc") + "</docs>";
          response.getWriter().print(xml);
          break;
        case "json":
          response.setContentType("application/json;charset=UTF-8");
          response.getWriter().print(jo.getJSONObject("response").getJSONArray("docs").toString());
          break;
        default:
          response.setContentType("application/json;charset=UTF-8");
          response.getWriter().print(jo.toString());
      }

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
