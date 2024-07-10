package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
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
public class ADBSearcher implements EntitySearcher {

    public static final Logger LOGGER = Logger.getLogger(ADBSearcher.class.getName());

    final String ENTITY = "adb";

    @Override
    public String[] getRelationsFields() {
        return new String[]{"dokument", "projekt"};
    }

    @Override
    public void checkRelations(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
    }

    @Override
    public void filter(JSONObject jo, String pristupnost, String org) {
        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
            if (doc.getString("pristupnost").compareToIgnoreCase(pristupnost) > 0) {
                doc.remove("chranene_udaje");
            }

            Object[] keys = doc.keySet().toArray();
            for (Object okey : keys) {
                String key = (String) okey;
                if (key.endsWith("_D") && "D".compareToIgnoreCase(pristupnost) > 0) {
                    doc.remove((String) key);
                }
                if (key.endsWith("_C") && "C".compareToIgnoreCase(pristupnost) > 0) {
                    doc.remove((String) key);
                }
                if (key.endsWith("_B") && "B".compareToIgnoreCase(pristupnost) > 0) {
                    doc.remove((String) key);
                }

            }

        }
    }

    @Override
    public String[] getChildSearchFields(String pristupnost) {
        return this.getSearchFields(pristupnost);
    }

    @Override
    public void getChilds(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        String fields = "ident_cely,entity,katastr,okres,vedouci_akce,specifikace_data,datum_zahajeni,datum_ukonceni,je_nz,pristupnost,organizace,dalsi_katastry,lokalizace"
                + ",nazev,typ_lokality,druh,popis";

        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
            if (LoginServlet.userId(request) != null) {
                SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
            }

            if (doc.has("parent")) {
                String p = doc.getString("parent");
                p = p.substring(0, p.length() - 4);
                JSONObject sub = SolrSearcher.getById(client, p, fields);
                if (sub != null) {
                    doc.append(sub.getString("entity"), sub);
                }

            }

        }
    }

    @Override
    public JSONObject search(HttpServletRequest request) {
        JSONObject json = new JSONObject();
                try {            Http2SolrClient client = IndexUtils.getClientNoOp();
            SolrQuery query = new SolrQuery();
            setQuery(request, query);
            JSONObject jo = SearchUtils.json(query, client, "entities");
            SolrSearcher.addFavorites(jo, client, request);
            return jo;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            json.put("error", ex);
        }
        return json;
    }

    @Override
    public String export(HttpServletRequest request) {
                try {            Http2SolrClient client = IndexUtils.getClientNoOp();
            SolrQuery query = new SolrQuery();
            setQuery(request, query);
            return SearchUtils.csv(query, client, "entities");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return ex.toString();
        }
    }

    @Override
    public String[] getSearchFields(String pristupnost) {
        return new String[]{"*,adb_chranene_udaje_vyskovy_bod:[json],adb_chranene_udaje:[json]"};
    }

    public void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
        SolrSearcher.addCommonParams(request, query, ENTITY);
        String pristupnost = LoginServlet.pristupnost(request.getSession());
        if ("E".equals(pristupnost)) {
            pristupnost = "D";
        }
        query.set("df", "text_all_" + pristupnost);
        if (Boolean.parseBoolean(request.getParameter("mapa"))) {
            SolrSearcher.addLocationParams(request, query);
        }

        SolrSearcher.addFilters(request, query, pristupnost);
    }

}
