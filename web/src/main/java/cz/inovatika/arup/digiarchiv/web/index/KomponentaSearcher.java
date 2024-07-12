package cz.inovatika.arup.digiarchiv.web.index;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class KomponentaSearcher implements ComponentSearcher, EntitySearcher {

  public static final Logger LOGGER = Logger.getLogger(KomponentaSearcher.class.getName());

  final String ENTITY = "komponenta";
  private boolean parentSearchable;

  @Override
  public void getRelated(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {

    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);

            String ident_cely = doc.getString("ident_cely");
            SolrQuery query = new SolrQuery("*").addFilterQuery("komponenta_ident_cely:\"" + ident_cely + "\"");
            AkceSearcher as = new AkceSearcher();
            query.setFields(as.getChildSearchFields("A"));
            try {
                JSONObject sub = SolrSearcher.json(client, "entities", query);
                JSONArray subs = sub.getJSONObject("response").getJSONArray("docs");

                for (int j = 0; j < subs.length(); j++) {
                    doc.append(subs.getJSONObject(i).getString("entity"), subs.getJSONObject(i));
                }
                parentSearchable = true;

            } catch (SolrServerException | IOException ex) {
                Logger.getLogger(DokJednotkaSearcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
  }

  @Override
  public boolean isRelatedSearchable() {
    return parentSearchable;
  }

    @Override
    public JSONObject search(HttpServletRequest request) {
        return new JSONObject();
    }

    @Override
    public String export(HttpServletRequest request) {
        return "";
    }

    @Override
    public String[] getSearchFields(String pristupnost) {
        return new String[]{"*,komponenta_aktivita:[json],komponenta_obdobi:[json]"};
    }

    @Override
    public void filter(JSONObject jo, String pristupnost, String org) {
        
    }

    @Override
    public void getChilds(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
        
    }

    @Override
    public String[] getChildSearchFields(String pristupnost) {
        return getSearchFields(pristupnost);
    }

    @Override
    public String[] getRelationsFields() {
        return new String[]{"*,komponenta_aktivita:[json],komponenta_obdobi:[json]"};
    }

    @Override
    public void checkRelations(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
        
    }

}
