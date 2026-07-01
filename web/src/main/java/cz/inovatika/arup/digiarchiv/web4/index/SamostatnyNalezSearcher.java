package cz.inovatika.arup.digiarchiv.web4.index;

import cz.inovatika.arup.digiarchiv.web4.LoginServlet;
import cz.inovatika.arup.digiarchiv.web4.Options;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.common.params.CursorMarkParams;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class SamostatnyNalezSearcher implements EntitySearcher {

    public static final Logger LOGGER = Logger.getLogger(SamostatnyNalezSearcher.class.getName());
    final String ENTITY = "samostatny_nalez";

    @Override
    public String[] getChildSearchFields(String pristupnost) {
        return this.getSearchFields(pristupnost);
    }

    @Override
    public void checkRelations(JSONObject jo, SolrClient client, HttpServletRequest request) {
        JSONArray docs = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            if (doc.has("samostatny_nalez_projekt") && !SolrSearcher.existsById(client, doc.getString("samostatny_nalez_projekt"))) {
                doc.remove("samostatny_nalez_projekt");
            }
        }
    }

    @Override
    public String[] getRelationsFields() {
        return new String[]{"ident_cely", "samostatny_nalez_projekt"};
    }

    @Override
    public void getChilds(JSONObject jo, SolrClient client, HttpServletRequest request) {
        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
            if (LoginServlet.userId(request) != null) {
                SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
            }
            String fields = "ident_cely,katastr,okres,vedouci_projektu,typ_projektu,datum_zahajeni,datum_ukonceni,organizace_prihlaseni,dalsi_katastry,podnet,pian_id,pian:[json]";
            SolrSearcher.addChildField(client, doc, "projekt", "valid_projekt", fields);
        }
    }

    @Override
    public void processAsChild(HttpServletRequest request, JSONObject jo) {

    }

    @Override
    public JSONObject search(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        try (SolrClient client = new HttpJdkSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
            SolrQuery query = new SolrQuery("*");
            setQuery(request, query);
            JSONObject jo = SearchUtils.json(query, client, "entities");
            String pristupnost = LoginServlet.pristupnost(request.getSession());
            filter(jo, pristupnost, LoginServlet.organizace(request.getSession()));
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
        try (SolrClient client = new HttpJdkSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
            SolrQuery query = new SolrQuery();
            setQuery(request, query);
            SolrSearcher.addExportParams(query, ENTITY);
            JSONObject jo = SearchUtils.json(query, client, "entities");
            String pristupnost = LoginServlet.pristupnost(request.getSession());
            filter(jo, pristupnost, LoginServlet.organizace(request.getSession()));
            String format = request.getParameter("format");
            if (format == null) {
              format = "json";
            }
            switch (format) {
              case "csv":
                return SearchUtils.csv(query, client, "entities");
              case "json": 
                return jo.toString();
              default: 
                return SearchUtils.json(query, client, "entities").toString();
            }
            
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return ex.toString();
        }
    }

    @Override
    public String[] getSearchFields(String pristupnost) {
        List<Object> fields = Options.getInstance().getJSONObject("fields").getJSONArray("common").toList();
        List<Object> headerFields = Options.getInstance().getJSONObject("fields").getJSONObject("samostatny_nalez").getJSONArray("header").toList();
        List<Object> detailFields = Options.getInstance().getJSONObject("fields").getJSONObject("samostatny_nalez").getJSONArray("detail").toList();

        fields.addAll(headerFields);
        fields.addAll(detailFields);

        fields.add("samostatny_nalez_lokalizace:samostatny_nalez_lokalizace_" + pristupnost);
        fields.add("loc_rpt:loc_rpt_" + pristupnost);
        fields.add("loc:loc_rpt_" + pristupnost);

        String[] ret = fields.toArray(new String[0]);
        return ret;
    }

    private void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
        SolrSearcher.addCommonParams(request, query, ENTITY);
        String pristupnost = LoginServlet.pristupnost(request.getSession());
        if ("E".equals(pristupnost)) {
            pristupnost = "D";
        }
        query.set("df", "text_all_" + pristupnost);
        query.setFields(getSearchFields(pristupnost));

        SolrSearcher.addFilters(request, query, pristupnost);
        if (Boolean.parseBoolean(request.getParameter("mapa"))) {
            SolrSearcher.addLocationParams(request, query);
        }
    }

    /**
     * Filter katastr podle pristupnosti
     *
     * @param jo
     * @param pristupnost
     */
    @Override
    public void filter(JSONObject jo, String pristupnost, String org) {
        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
            if (doc.getString("pristupnost").compareTo(pristupnost) > 0) {
                doc.remove("samostatny_nalez_chranene_udaje");
                doc.remove("katastr");
                doc.remove("f_katastr");
                doc.remove("samostatny_nalez_katastr_" + pristupnost);
            }
        }
    }
}
