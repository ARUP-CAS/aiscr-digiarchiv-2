package cz.inovatika.arup.digiarchiv.web.imagging;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;

/**
 * @author Inovatika
 */
public class ImageAccess {
  public static boolean isAllowed(HttpServletRequest request, boolean isFull) {
    boolean allow;
      String userPr = LoginServlet.pristupnost(request.getSession());
      if (!isFull) {
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
        if ("samostatny_nalez".equals(dok.getString("entity"))) {
          // Pro samostatne nalezy neni omezeni
          // https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/85
          return true;
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

