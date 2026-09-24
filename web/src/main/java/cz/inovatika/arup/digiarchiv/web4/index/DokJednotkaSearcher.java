package cz.inovatika.arup.digiarchiv.web4.index;

import cz.inovatika.arup.digiarchiv.web4.LoginServlet;
import cz.inovatika.arup.digiarchiv.web4.Options;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.Akce;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrClient;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class DokJednotkaSearcher implements ComponentSearcher, EntitySearcher {

    public static final Logger LOGGER = Logger.getLogger(DokJednotkaSearcher.class.getName());

    final String ENTITY = "dok_jednotka";
    private boolean parentSearchable;

    @Override
    public void getRelatedInHandle(JSONObject jo, SolrClient client, HttpServletRequest request) {
        String pristupnost = LoginServlet.pristupnost(request.getSession());
        if ("E".equals(pristupnost)) {
            pristupnost = "D";
        }
        String org = LoginServlet.organizace(request.getSession());
        PIANSearcher ps = new PIANSearcher();
        String[] fs = ps.getSearchFields(pristupnost);
        String pfields = String.join(",", fs);

        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        AkceSearcher as = new AkceSearcher();
        String[] akceFields = as.getChildSearchFields(pristupnost);
        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);

            String ident_cely = doc.getString("ident_cely");
            SolrQuery query = new SolrQuery("*").addFilterQuery("az_dj:\"" + ident_cely + "\"");
            query.setFields(akceFields);
            try {
                JSONObject sub = SolrSearcher.json(client, "entities", query);
                filter(sub, pristupnost, org);
                JSONArray subs = sub.getJSONObject("response").getJSONArray("docs");

                for (int j = 0; j < subs.length(); j++) {
                    doc.append(subs.getJSONObject(i).getString("entity"), subs.getJSONObject(i));
                    doc.put("datestamp", subs.getJSONObject(i).getString("datestamp"));
                }
                parentSearchable = true;

            } catch (SolrServerException | IOException ex) {
                Logger.getLogger(DokJednotkaSearcher.class.getName()).log(Level.SEVERE, "", ex);
            }

            if (doc.has("dj_pian")) {
                JSONObject sub = SolrSearcher.getById(client, doc.getString("dj_pian"), pfields, true);
                if (sub != null) {
                    ps.filterOne(sub, pristupnost, org);
                    doc.append("pian", sub);
                }
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
    public JSONObject export(HttpServletRequest request) {
        return new JSONObject();
    }

    @Override
    public String[] getSearchFields(String pristupnost) {
      
      List<Object> fields = Options.getInstance().getJSONObject("fields").getJSONArray("common").toList();
        List<Object> azHeaderFields = Options.getInstance().getJSONObject("fields").getJSONObject("dokumentacni_jednotka").getJSONArray("header").toList();
        List<Object> azDetailFields = Options.getInstance().getJSONObject("fields").getJSONObject("dokumentacni_jednotka").getJSONArray("detail").toList();

        fields.addAll(azHeaderFields);
        fields.addAll(azDetailFields);

        fields.add("pian_id:az_dj_pian");
        fields.add("loc_rpt:loc_rpt_" + pristupnost);
        fields.add("loc:loc_rpt_" + pristupnost);
        fields.add("katastr:f_katastr_" + pristupnost);
        fields.add("dj_komponenta:[json]");

        String[] ret = fields.toArray(new String[0]);
        return ret;
        
        //return new String[]{"*,dj_komponenta:[json]"};
    }

    @Override
    public void filter(JSONObject jo, String pristupnost, String org) {
        AkceSearcher a = new AkceSearcher();
        a.filter(jo, pristupnost, org);
        LokalitaSearcher l = new LokalitaSearcher();
        l.filter(jo, pristupnost, org);
        
        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
            String organizace = doc.optString("akce_organizace");
            String docPr = doc.getString("pristupnost");

            boolean sameOrg = org.toLowerCase().equals(organizace.toLowerCase()) && "C".compareTo(pristupnost) >= 0;
            if (docPr.compareToIgnoreCase(pristupnost) > 0 && !sameOrg) {
                doc.remove("akce");
                doc.remove("lokalita");
            } 
        }
        
    }

    @Override
    public void getChilds(JSONObject jo, SolrClient client, HttpServletRequest request) {
        
    }

    @Override
    public String[] getChildSearchFields(String pristupnost) {
        return getSearchFields(pristupnost);
    }

    @Override
    public String[] getRelationsFields() {
        return new String[]{"*,dj_komponenta:[json]"};
    }

    @Override
    public void checkRelations(JSONObject jo, SolrClient client, HttpServletRequest request) {
        
    }

}
