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
public class DokumentCastSearcher implements ComponentSearcher, EntitySearcher {

  public static final Logger LOGGER = Logger.getLogger(DokumentCastSearcher.class.getName());

  final String ENTITY = "dokument_cast";
  private boolean parentSearchable = true;

  @Override
  public void getRelated(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {

    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    String fields = "*,ident_cely,entity,dokument_cast_archeologicky_zaznam,dokument_cast_neident_akce:[json]";
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      
      if (doc.has("dokument_cast_archeologicky_zaznam")) {
        JSONArray cdjs = doc.getJSONArray("dokument_cast_archeologicky_zaznam");
        for (int j = 0; j < cdjs.length(); j++) {
          String cdj = cdjs.getString(j);
          JSONObject sub = SolrSearcher.getById(client, cdj, fields);
          if (sub != null) {
            doc.put(sub.getString("entity"), sub);
          }
          
        }
      }
      
    }
  }

  @Override
  public boolean isRelatedSearchable() {
    return parentSearchable;
  }

    @Override
    public JSONObject search(HttpServletRequest request) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public String export(HttpServletRequest request) {
        return "";
    }

    @Override
    public String[] getSearchFields(String pristupnost) {
        return new String[]{"*,ident_cely,entity,dokument_cast_archeologicky_zaznam,dokument_cast_neident_akce:[json]"};
    }

    @Override
    public void filter(JSONObject jo, String pristupnost, String org) {
        
    }

    @Override
    public void getChilds(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
        
    }

    @Override
    public String[] getChildSearchFields(String pristupnost) {
        return new String[]{"*,ident_cely,entity,dokument_cast_archeologicky_zaznam,dokument_cast_neident_akce:[json]"};
    }

    @Override
    public String[] getRelationsFields() {
        return new String[]{"*,ident_cely,entity,dokument_cast_archeologicky_zaznam,dokument_cast_neident_akce:[json]"};
    }

    @Override
    public void checkRelations(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
       
    }

}
