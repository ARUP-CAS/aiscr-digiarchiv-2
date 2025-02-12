package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
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
            try {
                JSONObject doc = ja.getJSONObject(i);
                
                DokumentSearcher ds = new DokumentSearcher("dokument");
                String dfs = String.join(",", ds.getChildSearchFields("D"));
                SolrQuery query = new SolrQuery("*")
                        .addFilterQuery("entity:dokument")
                        .addFilterQuery("komponenta_dokument_ident_cely:\"" + doc.getString("ident_cely") + "\"");
                query.setFields(dfs);
                
                JSONObject r = SolrSearcher.json(client, "entities", query);
                ds.filter(r, LoginServlet.pristupnost(request.getSession()), LoginServlet.organizace(request.getSession()));
                JSONArray reldocs = r.getJSONObject("response").getJSONArray("docs");
                for (int j = 0; j < reldocs.length(); j++) {
                    JSONObject cdj = reldocs.getJSONObject(j);
                    doc.append("dokument", cdj);
                }
                
                String ident_cely = doc.getString("ident_cely");
                query = new SolrQuery("*").addFilterQuery("komponenta_ident_cely:\"" + ident_cely + "\"");
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
            } catch (SolrServerException ex) {
                Logger.getLogger(KomponentaSearcher.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(KomponentaSearcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public boolean isRelatedSearchable() {
        return parentSearchable;
    }

    @Override
    public void processAsChild(HttpServletRequest request, JSONObject jo) {

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
        return new String[]{"*,komponenta_aktivita:[json],komponenta_areal:[json],komponenta_obdobi:[json],komponenta_nalez_objekt:[json],komponenta_nalez_predmet:[json]"};
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
