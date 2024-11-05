
package cz.inovatika.arup.digiarchiv.web;

import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;

/**
 *
 * @author alber
 */
public class LogAnalytics {
    
    public static void log(HttpServletRequest request, String ident_cely, String type) {
        JSONObject user = LoginServlet.user(request);
        String ip = request.getRemoteAddr();
    }
}
