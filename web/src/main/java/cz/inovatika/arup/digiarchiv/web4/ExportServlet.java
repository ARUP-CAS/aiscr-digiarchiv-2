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
import org.json.JSONException;
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
      //response.setHeader("Content-Disposition", "filename=export_" + entity + "_" + now.toEpochMilli() + "." + format);
      switch (format) {
        case "csv":
          List<String> labels = SolrSearcher.getExportField(entity, "label");
          JSONArray ls = new JSONArray(labels);
          String csv = org.json.CDL.rowToString(new JSONArray(labels));
          csv += toString(entity, jo.getJSONObject("response").getJSONArray("docs"), ',', request.getParameter("lang"));
          response.setContentType("text/csv;charset=UTF-8");
          response.getWriter().print(csv);
          break;
        case "xlsx":
          response.setContentType("application/xlsx;charset=UTF-8");
          csvToXLSX(response, jo.getJSONObject("response").getJSONArray("docs"), entity, ',', request.getParameter("lang"));
          break;
        case "xml":
          response.setContentType("text/xml;charset=UTF-8");
          String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><docs>" + XML.toString(jo.getJSONObject("response").getJSONArray("docs"), "doc") + "</docs>";
          response.getWriter().print(xml);
          break;
        case "json":
          response.setContentType("application/json;charset=UTF-8");
          JSONArray exFields = Options.getInstance().getClientConf().getJSONObject("exportFields").getJSONArray(entity);
          JSONArray ret = new JSONArray();
          JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
          for (int i = 0; i < ja.length(); i += 1) {
            JSONObject doc = ja.getJSONObject(i);
            ret.put(translateJSON(doc, exFields, request.getParameter("lang")));
          }

          response.getWriter().print(ret.toString());
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

  private static JSONObject translateJSON(JSONObject jo, JSONArray exFields, String locale) throws JSONException {
    JSONObject ret = new JSONObject();
    if (exFields == null || exFields.isEmpty()) {
      return null;
    }
    for (int i = 0; i < exFields.length(); i += 1) {
      JSONObject f = exFields.getJSONObject(i);
      String name = f.optString("name");
      String field = f.optString("label", name);
      Object val = jo.opt(field);
      if (val == null) {
        val = jo.opt(name);
      }
      if (val != null && f.has("heslar")) {
        val = I18n.translate((String) val, locale);
      }
      ret.put(field, val);
    }
    return ret;
  }

  private static JSONArray toJSONArray(JSONObject jo, JSONArray exFields, String locale) throws JSONException {
    if (exFields == null || exFields.isEmpty()) {
      return null;
    }
    JSONArray ja = new JSONArray();
    for (int i = 0; i < exFields.length(); i += 1) {
      JSONObject f = exFields.getJSONObject(i);
      String name = f.optString("name");
      Object val = jo.opt(f.optString("label", name));
      if (val != null && f.has("heslar")) {
        val = I18n.translate((String) val, locale);
      }
      ja.put(val);
    }
    return ja;
  }

  /**
   * Produce a comma delimited text from a JSONArray of JSONObjects using a
   * provided list of names. The list of names is not included in the output.
   */
  private static String toString(String entity, JSONArray ja, char delimiter, String locale) throws JSONException {
    JSONArray exFields = Options.getInstance().getClientConf().getJSONObject("exportFields").getJSONArray(entity);
    if (exFields == null || exFields.length() == 0) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ja.length(); i += 1) {
      JSONObject jo = ja.optJSONObject(i);
      if (jo != null) {
        sb.append(rowToString(toJSONArray(jo, exFields, locale), delimiter));
      }
    }
    return sb.toString();
  }

  /**
   * Produce a comma delimited text row from a JSONArray. Values containing the
   * comma character will be quoted. Troublesome characters may be removed.
   *
   * @param ja A JSONArray of strings.
   * @param delimiter custom delimiter char
   * @return A string ending in NEWLINE.
   */
  public static String rowToString(JSONArray ja, char delimiter) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ja.length(); i += 1) {
      if (i > 0) {
        sb.append(delimiter);
      }
      Object object = ja.opt(i);
      if (object != null) {
        String string = object.toString();
        if (!string.isEmpty() && (string.indexOf(delimiter) >= 0
                || string.indexOf('\n') >= 0 || string.indexOf('\r') >= 0
                || string.indexOf(0) >= 0 || string.charAt(0) == '"')) {
          sb.append('"');
          int length = string.length();
          for (int j = 0; j < length; j += 1) {
            char c = string.charAt(j);
            if (c >= ' ' && c != '"') {
              sb.append(c);
            }
          }
          sb.append('"');
        } else {
          sb.append(string);
        }
      }
    }
    sb.append('\n');
    return sb.toString();
  }

  /**
   * Fills a xlsx row from a JSONArray. Values containing the comma character
   * will be quoted. Troublesome characters may be removed.
   *
   * @param ja A JSONArray of strings.
   * @param delimiter custom delimiter char
   * @param currentRow xlsx row
   */
  public static void rowToXLSX(JSONArray ja, char delimiter, XSSFRow currentRow) {
    for (int i = 0; i < ja.length(); i += 1) {
      XSSFCell cell = currentRow.createCell(i);
      cell.setCellType(CellType.STRING);
      StringBuilder sb = new StringBuilder();
      Object object = ja.opt(i);
      if (object != null) {
          cell.setCellValue(object.toString());
      }
    }
  }

  /**
   * Fills a xlsx a sheet using a provided list of names. The list of names is
   * not included in the output.
   */
  private static void toXLSX(String entity, JSONArray ja, char delimiter, String locale, XSSFSheet sheet) throws JSONException {
    JSONArray exFields = Options.getInstance().getClientConf().getJSONObject("exportFields").getJSONArray(entity);
    if (exFields == null || exFields.length() == 0) {
      return;
    }

    int rowNum = 0;
    List<String> labels = SolrSearcher.getExportField(entity, "label");
    JSONArray ls = new JSONArray(labels);
    XSSFRow currentRow = sheet.createRow(rowNum++);
    rowToXLSX(ls, delimiter, currentRow);
    for (int i = 0; i < ja.length(); i += 1) {
      JSONObject jo = ja.optJSONObject(i);
      if (jo != null) {
        currentRow = sheet.createRow(rowNum++);
        rowToXLSX(toJSONArray(jo, exFields, locale), delimiter, currentRow);
      }
    }
  }

  public static void csvToXLSX(HttpServletResponse response, JSONArray docs, String entity, char delimiter, String locale) {
    try {
      XSSFWorkbook workBook = new XSSFWorkbook();
      XSSFSheet sheet = workBook.createSheet(entity);
      toXLSX(entity, docs, delimiter, locale, sheet);
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
