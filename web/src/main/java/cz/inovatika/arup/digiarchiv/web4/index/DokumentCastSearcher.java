package cz.inovatika.arup.digiarchiv.web4.index;

import cz.inovatika.arup.digiarchiv.web4.LoginServlet;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrClient;
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
    public void getRelated(JSONObject jo, SolrClient client, HttpServletRequest request) {

        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        String fields = "*,ident_cely,entity,dokument_cast_archeologicky_zaznam,dokument_cast_neident_akce:[json]";
        for (int i = 0; i < ja.length(); i++) {

            try {
                JSONObject doc = ja.getJSONObject(i);

                DokumentSearcher ds = new DokumentSearcher("dokument");
                String dfs = String.join(",", ds.getChildSearchFields("D"));
                SolrQuery query = new SolrQuery("*")
                        .addFilterQuery("entity:dokument")
                        .addFilterQuery("dokument_cast_ident_cely:\"" + doc.getString("ident_cely") + "\"");
                query.setFields(dfs);

                JSONObject r = SolrSearcher.json(client, "entities", query);
                ds.filter(r, LoginServlet.pristupnost(request.getSession()), LoginServlet.organizace(request.getSession()));
                JSONArray reldocs = r.getJSONObject("response").getJSONArray("docs");
                for (int j = 0; j < reldocs.length(); j++) {
                    JSONObject cdj = reldocs.getJSONObject(j);
                    doc.append("dokument", cdj);
                    doc.put("datestamp", cdj.getString("datestamp"));
                }

                if (doc.has("dokument_cast_archeologicky_zaznam")) {
                    JSONArray cdjs = doc.getJSONArray("dokument_cast_archeologicky_zaznam");
                    for (int j = 0; j < cdjs.length(); j++) {
                        String cdj = cdjs.getString(j);
                        JSONObject sub = SolrSearcher.getById(client, cdj, fields);
                        if (sub != null) {
                            doc.put(sub.getString("entity"), sub);
                            doc.put("datestamp", sub.getString("datestamp"));
                        }

                    }
                }

                // SolrSearcher.addChildField(client, doc, "dokument_cast_ident_cely", "dokument", dfs);
            } catch (SolrServerException | IOException ex) {
                Logger.getLogger(DokumentCastSearcher.class.getName()).log(Level.SEVERE, null, ex);
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
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public String export(HttpServletRequest request) {
        return "";
    }

    @Override
    public String[] getSearchFields(String pristupnost) {
        return new String[]{"*,ident_cely,entity,dokument_cast_archeologicky_zaznam,dokument_cast_neident_akce:[json],dokument_cast_komponenta:[json]"};
    }

    @Override
    public void filter(JSONObject jo, String pristupnost, String org) {

    }

    @Override
    public void getChilds(JSONObject jo, SolrClient client, HttpServletRequest request) {

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
    public void checkRelations(JSONObject jo, SolrClient client, HttpServletRequest request) {

    }

}
