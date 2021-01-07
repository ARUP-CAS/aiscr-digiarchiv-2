package cz.inovatika.arup.digiarchiv.web.imagging;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;

/**
 * @author Inovatika
 */
public class ImageAccess {
  public static boolean isAllowed(HttpServletRequest request) {
    boolean full = Boolean.parseBoolean(request.getParameter("full"));
    boolean allow;
      String userPr = LoginServlet.pristupnost(request.getSession());
      String size = request.getParameter("size");
      boolean isThumb = !full && ((size == null) || "thumb".equals(size));
      if (isThumb) {
        allow = true;
      } else {
        String id = request.getParameter("id");
        String field = request.getParameter("field");
        if(field == null) {
          field = "dokument";
        } 
        JSONObject dok = SolrSearcher.getDokBySoubor(id);
        if (dok == null) {
          return false;
        }
        String imgPr = dok.getString("pristupnost");
        boolean sameOrg = dok.has("organizace") && LoginServlet.organizace(request.getSession()).toLowerCase().equals(dok.getString("organizace").toLowerCase())  && "C".compareTo(userPr) >= 0;
        if ("A".equals(imgPr)) {
          allow = true;
        } else {
          allow = (userPr != null) && (userPr.compareToIgnoreCase(imgPr) >= 0 || sameOrg);
        }
      }
    
    return allow;
  }
}

