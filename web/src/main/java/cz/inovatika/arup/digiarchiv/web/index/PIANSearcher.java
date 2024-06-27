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
public class PIANSearcher implements EntitySearcher {

    public static final Logger LOGGER = Logger.getLogger(PIANSearcher.class.getName());

    final String ENTITY = "pian";

    @Override
    public void filter(JSONObject jo, String pristupnost, String org) {
        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
            if (doc.getString("pristupnost").compareToIgnoreCase(pristupnost) > 0) {
                doc.remove("chranene_udaje");
            }
        }
    }

    @Override
    public String[] getChildSearchFields(String pristupnost) {
        return this.getSearchFields(pristupnost);
    }

    @Override
    public void checkRelations(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
    }

    @Override
    public String[] getRelationsFields() {
        return new String[]{"dokument", "projekt"};
    }

    @Override
    public void getChilds(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < ja.length(); i++) {
            try {
                JSONObject doc = ja.getJSONObject(i);
                if (LoginServlet.userId(request) != null) {
                    SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
                }

                SolrQuery query = new SolrQuery("*")
                        .addFilterQuery("entity:dokumentacni_jednotka")
                        .addFilterQuery("pian:\"" + doc.getString("ident_cely") + "\"");
                JSONObject r = SolrSearcher.json(client, "entities", query);
                JSONArray cdjs = r.getJSONObject("response").getJSONArray("docs");

                for (int j = 0; j < cdjs.length(); j++) {
                    JSONObject cdj = cdjs.getJSONObject(j); 
                    doc.append("dokumentacni_jednotka", cdj);
                }
            } catch (SolrServerException | IOException ex) {
                Logger.getLogger(PIANSearcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public JSONObject search(HttpServletRequest request) {
        JSONObject json = new JSONObject();
                try {            Http2SolrClient client = IndexUtils.getClientNoOp();
            SolrQuery query = new SolrQuery()
                    .setFacet(true);
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
    public String[] getSearchFields(String pristupnost) {

        return new String[]{"ident_cely,entity,stav,typ,presnost,geom_system,geom_updated_at,geom_sjtsk_updated_at,pristupnost,chranene_udaje:[json]",
            "centroid_n:centroid_n_" + pristupnost, "centroid_e:centroid_e_" + pristupnost,
            "lat:lat_" + pristupnost,
            "lng:lng_" + pristupnost,
            "loc_rpt:loc_rpt_" + pristupnost,
            "loc:loc_" + pristupnost};
    }
    
    public String[] getMapaSearchFields(String pristupnost) {

        return new String[]{"ident_cely,entity,stav,typ,presnost,geom_system,geom_updated_at,geom_sjtsk_updated_at,pristupnost",
            "centroid_n:centroid_n_" + pristupnost, "centroid_e:centroid_e_" + pristupnost,
            "lat:lat_" + pristupnost,
            "lng:lng_" + pristupnost,
            "loc_rpt:loc_rpt_" + pristupnost,
            "loc:loc_" + pristupnost};
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

    public JSONObject getMapPians(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        // Menime entity
                try {            Http2SolrClient client = IndexUtils.getClientNoOp();
            SolrQuery query = new SolrQuery();
            String entity = "" + request.getParameter("entity");
            if (entity == null || "null".equals(entity)) {
                entity = "dokument";
            }
            SolrSearcher.addCommonParams(request, query, entity);
            String pristupnost = LoginServlet.pristupnost(request.getSession());
            if ("E".equals(pristupnost)) {
                pristupnost = "D";
            }
            query.set("df", "text_all_" + pristupnost);
            if (Boolean.parseBoolean(request.getParameter("mapa"))) {
                SolrSearcher.addLocationParams(request, query);
            }
            SolrSearcher.addFilters(request, query, pristupnost);
            query.setFacet(false).setRequestHandler("/select");
            query.set("defType", "edismax");
            query.setFields("pian:[json],ident_cely,organizace,pristupnost", "loc_rpt:loc_rpt_" + pristupnost, "loc:loc_rpt_" + pristupnost);

            query.setRows(Math.min(Options.getInstance().getClientConf().getJSONObject("mapOptions").optInt("docsForCluster", 5000), Integer.parseInt(request.getParameter("rows"))));

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

}
