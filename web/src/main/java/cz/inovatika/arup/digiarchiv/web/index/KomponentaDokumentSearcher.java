package cz.inovatika.arup.digiarchiv.web.index;

import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class KomponentaDokumentSearcher implements ComponentSearcher {

  public static final Logger LOGGER = Logger.getLogger(KomponentaDokumentSearcher.class.getName());

  final String ENTITY = "komponenta_dokument";
  private boolean parentSearchable;

  @Override
  public void getRelated(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {

    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    String fields = "*";
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      /**
       Nechame nejvissi zaznam
       * C-LD-000012218-K01 
       * parent je C-LD-000012218-D01 
       * ale nechame dokument  C-LD-000012218
        * 
       */
      if (doc.has("parent")) {
        String p = doc.getString("parent");
        p = p.substring(0, p.length() - 4);
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
