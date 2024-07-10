package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class VyskovyBodSearcher implements ComponentSearcher {

  public static final Logger LOGGER = Logger.getLogger(VyskovyBodSearcher.class.getName());

  final String ENTITY = "vyskovy_bod";
  private boolean parentSearchable;

  @Override
  public void getRelated(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {

    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    String fields = "*";
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      if (doc.has("vyskovy_bod_parent")) {
        String p = doc.getString("vyskovy_bod_parent");
        JSONObject sub = SolrSearcher.getById(client, p, fields);
        if (sub != null) {
          doc.append(sub.getString("entity"), sub);
          parentSearchable = true;
        }
      }
    }
  }

  @Override
  public boolean isRelatedSearchable() {
    return parentSearchable;
  }

}
