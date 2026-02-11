package cz.inovatika.arup.digiarchiv.web4.index;

import java.util.logging.Logger;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrClient;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class JednotkaDokumentuSearcher implements ComponentSearcher {

  public static final Logger LOGGER = Logger.getLogger(JednotkaDokumentuSearcher.class.getName());

  final String ENTITY = "jednotka_dokumentu";
  private boolean parentSearchable;

  @Override
  public void getRelated(JSONObject jo, SolrClient client, HttpServletRequest request) {

    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    String fields = "ident_cely,katastr,okres,autor,rok_vzniku,typ_dokumentu,material_originalu,pristupnost,rada,material_originalu,organizace,popis,soubor_filepath";
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      
      if (doc.has("dokument")) {
        JSONArray cdjs = doc.getJSONArray("dokument");
        for (int j = 0; j < cdjs.length(); j++) {
          String cdj = cdjs.getString(j);
          JSONObject sub = SolrSearcher.getById(client, cdj, fields);
          if (sub != null) {
            doc.append("dokumenty", sub);
          parentSearchable = true;
          }
          
        }
      }
      
    }
  }

  @Override
  public boolean isRelatedSearchable() {
    return parentSearchable;
  }

}
