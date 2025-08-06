package cz.inovatika.arup.digiarchiv.web4.index;

import cz.inovatika.arup.digiarchiv.web4.LoginServlet;
import cz.inovatika.arup.digiarchiv.web4.Options;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
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
                doc.remove("pian_chranene_udaje");
            }
        }
    }

    @Override
    public String[] getChildSearchFields(String pristupnost) {
        return this.getSearchFields(pristupnost);
    }

    @Override
    public void checkRelations(JSONObject jo, HttpJdkSolrClient client, HttpServletRequest request) {
        JSONArray docs = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);

            JSONArray az_dj_pian = new JSONArray();
            JSONArray id_akce = new JSONArray();
            JSONArray id_lokalita = new JSONArray();
            SolrQuery query = new SolrQuery("*")
                    .addFilterQuery("az_dj_pian:\"" + doc.getString("ident_cely") + "\"")
                    .setRows(10000)
                    .setFields("ident_cely,entity");
            try {
                JSONArray ja = SolrSearcher.json(client, "entities", query).getJSONObject("response").getJSONArray("docs");
                for (int a = 0; a < ja.length(); a++) {
                    az_dj_pian.put(ja.getJSONObject(a).getString("ident_cely"));
                    if ("akce".equals(ja.getJSONObject(a).getString("entity"))) {
                        id_akce.put(ja.getJSONObject(a).getString("ident_cely"));
                    }

                    if ("lokalita".equals(ja.getJSONObject(a).getString("entity"))) {
                        id_lokalita.put(ja.getJSONObject(a).getString("ident_cely"));
                    }
                }
            } catch (SolrServerException | IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }

            doc.put("az_dj_pian", az_dj_pian);
            doc.put("id_akce", id_akce);
            doc.put("id_lokalita", id_lokalita);
        }
    }

    @Override
    public String[] getRelationsFields() {
        return new String[]{"ident_cely"};
    }

    @Override
    public void getChilds(JSONObject jo, HttpJdkSolrClient client, HttpServletRequest request) {
        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        AkceSearcher as = new AkceSearcher();
        String[] af = as.getChildSearchFields(LoginServlet.pristupnost(request.getSession()));
        LokalitaSearcher ls = new LokalitaSearcher();
        String[] lf = ls.getChildSearchFields(LoginServlet.pristupnost(request.getSession()));
        for (int i = 0; i < ja.length(); i++) {
            try {
                JSONObject doc = ja.getJSONObject(i);
                if (LoginServlet.userId(request) != null) {
                    SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
                }

//                SolrQuery query = new SolrQuery("*")
//                        .setRows(1000)
//                        .addFilterQuery("entity:akce")
//                        .addFilterQuery("az_dj_pian:\"" + doc.getString("ident_cely") + "\"");
//                query.setFields(af); 
//                JSONObject r = SolrSearcher.json(client, "entities", query);
//                as.filter(r, LoginServlet.pristupnost(request.getSession()), LoginServlet.organizace(request.getSession()));
//                JSONArray cdjs = r.getJSONObject("response").getJSONArray("docs");
//                for (int j = 0; j < cdjs.length(); j++) {
//                    JSONObject cdj = cdjs.getJSONObject(j);
//                    doc.append("akce", cdj);
//                }
//                
//                query = new SolrQuery("*")
//                        .setRows(1000)
//                        .addFilterQuery("entity:lokalita")
//                        .addFilterQuery("az_dj_pian:\"" + doc.getString("ident_cely") + "\"");
//                query.setFields(lf);
//                r = SolrSearcher.json(client, "entities", query);
//                ls.filter(r, LoginServlet.pristupnost(request.getSession()), LoginServlet.organizace(request.getSession()));
//                cdjs = r.getJSONObject("response").getJSONArray("docs");
//
//                for (int j = 0; j < cdjs.length(); j++) {
//                    JSONObject cdj = cdjs.getJSONObject(j);
//                    doc.append("lokalita", cdj);
//                }
            } catch (Exception ex) {
                Logger.getLogger(PIANSearcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void processAsChild(HttpServletRequest request, JSONObject jo) {

    }

    @Override
    public JSONObject search(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        try {
            HttpJdkSolrClient client = IndexUtils.getClientNoOp();
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

        return new String[]{"ident_cely,entity,pristupnost,stav,pian_typ,pian_presnost,pian_geom_system,pian_chranene_udaje:[json]",
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

    public void addPians(JSONObject jo, HttpJdkSolrClient client, HttpServletRequest request) {
        String pristupnost = LoginServlet.pristupnost(request.getSession());
        if ("E".equals(pristupnost)) {
            pristupnost = "D";
        }
        PIANSearcher ps = new PIANSearcher();
        String[] fs = ps.getSearchFields(pristupnost);
        String fields = String.join(",", fs);

        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
            if (doc.has("pian_id")) {
                JSONArray cdjs = doc.getJSONArray("pian_id");
                for (int j = 0; j < cdjs.length(); j++) {
                    String cdj = cdjs.getString(j);
                    JSONObject sub = SolrSearcher.getById(client, cdj, fields);
                    if (sub != null) {
                        String docPr = sub.getString("pristupnost");
                        if (docPr.compareToIgnoreCase(pristupnost) > 0) {
                            sub.remove("pian_chranene_udaje");
                        }
                        doc.append("pian", sub);
                    }

                }
            }
        }
    }

    public JSONObject getMapPians(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        // Menime entity
        try {
            HttpJdkSolrClient client = IndexUtils.getClientNoOp();
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
            query.setFields("pian:[json],pian_id,ident_cely,organizace,pristupnost", "loc_rpt:loc_rpt_" + pristupnost, "loc:loc_rpt_" + pristupnost);

            query.setRows(Math.max(Options.getInstance().getClientConf().getJSONObject("mapOptions").optInt("docsForCluster", 5000), Integer.parseInt(request.getParameter("rows"))));

            JSONObject jo = SearchUtils.json(query, client, "entities");
            SolrSearcher.addFavorites(jo, client, request);
            if (request.getParameter("getFullPian") != null) {
                addPians(jo, client, request);
            }
            return jo;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            json.put("error", ex);
        }
        return json;
    }

    @Override
    public String export(HttpServletRequest request) {
        try {
            HttpJdkSolrClient client = IndexUtils.getClientNoOp();
            SolrQuery query = new SolrQuery();
            setQuery(request, query);
            return SearchUtils.csv(query, client, "entities");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return ex.toString();
        }
    }

}
