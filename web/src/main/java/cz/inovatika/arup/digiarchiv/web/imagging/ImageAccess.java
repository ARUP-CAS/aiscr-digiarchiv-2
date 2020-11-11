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
//    if (full) {
//      // same as FileViewerComponent (in frontend)
//      allow = (userPr != null) && (userPr.compareToIgnoreCase("D") >= 0);
//    } else {
      String size = request.getParameter("size");
      if ((size == null) || "thumb".equals(size)) {
        allow = true;
      } else {
        String id = request.getParameter("id");
        String field = request.getParameter("field");
        if(field == null) {
          field = "dokument";
        }
        JSONObject dok = SolrSearcher.getDokBySoubor(id);
        String imgPr = dok.getString("pristupnost");
        boolean sameOrg = LoginServlet.organizace(request.getSession()).toLowerCase().equals(dok.getString("organizace").toLowerCase())  && "C".compareTo(userPr) >= 0;
        if ("A".equals(imgPr)) {
          allow = true;
        } else {
          allow = (userPr != null) && (userPr.compareToIgnoreCase(imgPr) >= 0 || sameOrg);
        }
      }
    // }
    
    return allow;
  }
}

