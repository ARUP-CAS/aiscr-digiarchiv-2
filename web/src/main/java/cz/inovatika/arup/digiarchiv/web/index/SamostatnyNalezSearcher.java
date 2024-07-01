package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;

import java.io.IOException;
import java.util.List;
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
public class SamostatnyNalezSearcher implements EntitySearcher {

    public static final Logger LOGGER = Logger.getLogger(SamostatnyNalezSearcher.class.getName());
    final String ENTITY = "samostatny_nalez";

    @Override
    public String[] getChildSearchFields(String pristupnost) {
        return this.getSearchFields(pristupnost);
    }

    @Override
    public void checkRelations(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
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
    public void getChilds(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
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
    public JSONObject search(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        try {
            Http2SolrClient client = IndexUtils.getClientNoOp();
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
        try {
            Http2SolrClient client = IndexUtils.getClientNoOp();
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
        List<Object> fields = Options.getInstance().getJSONObject("fields").getJSONArray("common").toList();
        List<Object> headerFields = Options.getInstance().getJSONObject("fields").getJSONObject("samostatny_nalez").getJSONArray("header").toList();
        List<Object> detailFields = Options.getInstance().getJSONObject("fields").getJSONObject("samostatny_nalez").getJSONArray("detail").toList();

        fields.addAll(headerFields);
        fields.addAll(detailFields);

        fields.add("samostatny_nalez_lokalizace:samostatny_nalez_lokalizace_" + pristupnost);
        fields.add("loc_rpt:loc_rpt_" + pristupnost);
        fields.add("loc:loc_rpt_" + pristupnost);

        String[] ret = fields.toArray(new String[0]);

//        String[] ret = new String[]{"ident_cely, datestamp, entity, stav, pristupnost",
//            "samostatny_nalez_evidencni_cislo, samostatny_nalez_projekt, samostatny_nalez_okres, samostatny_nalez_hloubka, samostatny_nalez_poznamka, samostatny_nalez_nalezove_okolnosti",
//            "obdobi, presna_datace, druh_nalezu, specifikace, pocet, nalezce, datum_nalezu, predano, predano_organizace", "predmet_kategorie",
//            "datum_vlozeni, odpovedny_pracovnik_archivace, datum_archivace, child_soubor, soubor_filepath",
//            "soubor:[json]", "katastr:f_katastr_" + pristupnost,
//            "chranene_udaje:[json]",
//            "lokalizace:f_lokalizace_" + pristupnost,
//            "f_katastr:f_katastr_" + pristupnost,
//            "loc_rpt:loc_rpt_" + pristupnost, "loc:loc_rpt_" + pristupnost,
//            "lat:lat_" + pristupnost, "lng:lng_" + pristupnost};
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
