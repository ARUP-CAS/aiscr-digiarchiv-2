package cz.inovatika.arup.digiarchiv.web;

import java.io.IOException;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alber
 */
public class LogAnalytics {

    public static final Logger LOGGER = Logger.getLogger(LogAnalytics.class.getName());

    public static void log(HttpServletRequest request, String ident_cely, String type) {
        try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
            JSONObject user = LoginServlet.user(request);
            String ip = request.getRemoteAddr();
            SolrInputDocument idoc = new SolrInputDocument();
            idoc.addField("id", ident_cely + "_" + Instant.now().toEpochMilli());
            idoc.addField("ident_cely", ident_cely);
            idoc.addField("user", user.optString("ident_cely", "anonym"));
            idoc.addField("ip", ip);
            idoc.addField("type", type);
            LOGGER.log(Level.INFO, "Logged user:{0}; ip:{1}; {2} {3}",
                    new String[]{user.optString("ident_cely", "anonym"),
                        ip, ident_cely, type});
            client.add("logs", idoc, 1000); 
        } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
}
