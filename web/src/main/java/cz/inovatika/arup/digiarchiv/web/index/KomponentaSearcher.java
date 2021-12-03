package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class KomponentaSearcher implements ComponentSearcher {

  public static final Logger LOGGER = Logger.getLogger(KomponentaSearcher.class.getName());

  final String ENTITY = "komponenta";

  @Override
  public void getRelated(JSONObject jo, HttpSolrClient client, HttpServletRequest request) {

    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    String fields = "*";
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      if (doc.has("parent")) {
        JSONObject ch = SolrSearcher.getById(client, doc.getString("parent"), fields);
        if (ch != null) {
          if (ch.has("parent_akce")) {
            JSONObject sub = SolrSearcher.getById(client, ch.getString("parent_akce"), fields);
            if (sub != null) {
              doc.append(sub.getString("entity"), sub);
            }
          }
          if (ch.has("parent_lokalita")) {
            JSONObject sub = SolrSearcher.getById(client, ch.getString("parent_lokalita"), fields);
            if (sub != null) {
              doc.append(sub.getString("entity"), sub);
            }
          }

        }
      }
    }
  }

}
