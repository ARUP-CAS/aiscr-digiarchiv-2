package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;
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
public class DokJednotkaSearcher implements ComponentSearcher {

  public static final Logger LOGGER = Logger.getLogger(DokJednotkaSearcher.class.getName());

  final String ENTITY = "dok_jednotka";
  private boolean parentSearchable;

  @Override
  public void getRelated(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    String fields = "ident_cely,entity,katastr,okres,vedouci_akce,specifikace_data,datum_zahajeni,datum_ukonceni,je_nz,pristupnost,organizace,dalsi_katastry,lokalizace"
            + ",nazev,typ_lokality,druh,popis";
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
//      if (doc.has("parent_akce")) {
//        JSONObject sub = SolrSearcher.getById(client, doc.getString("parent_akce"), fields);
//        if (sub != null) {
//          doc.append(sub.getString("entity"), sub);
//          parentSearchable = true;
//        }
//      }
//      if (doc.has("parent_lokalita")) {
//        JSONObject sub = SolrSearcher.getById(client, doc.getString("parent_lokalita"), fields);
//        if (sub != null) {
//          doc.append(sub.getString("entity"), sub);
//          parentSearchable = true;
//        }
//      }
      
      String ident_cely = doc.getString("ident_cely");
      SolrQuery query = new SolrQuery("*").addFilterQuery("dokumentacni_jednotka_ident_cely:\""+ident_cely+"\"");
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

}
