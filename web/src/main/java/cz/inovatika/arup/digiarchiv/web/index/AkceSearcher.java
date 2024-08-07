package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;
import java.io.IOException;
import java.util.List;
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
public class AkceSearcher implements EntitySearcher {

    public static final Logger LOGGER = Logger.getLogger(AkceSearcher.class.getName());

    final String ENTITY = "akce";

    @Override
    public void filter(JSONObject jo, String pristupnost, String org) {
        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
            String organizace = doc.optString("akce_organizace");
            String docPr = doc.getString("pristupnost");

            boolean sameOrg = org.toLowerCase().equals(organizace.toLowerCase()) && "C".compareTo(pristupnost) >= 0;
            if (docPr.compareToIgnoreCase(pristupnost) > 0 && !sameOrg) {
                doc.remove("chranene_udaje");
                doc.remove("az_chranene_udaje");
                doc.remove("akce_chranene_udaje");
//                doc.remove("loc");
//                doc.remove("loc_rpt");
            }
        }
    }

    @Override
    public void getChilds(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
        String pristupnost = LoginServlet.pristupnost(request.getSession());
        if ("E".equals(pristupnost)) {
            pristupnost = "D";
        }
        PIANSearcher ps = new PIANSearcher();
        String[] fs = ps.getSearchFields(pristupnost);
        String fields = String.join(",", fs);

        DokumentSearcher ds = new DokumentSearcher("dokument");
        String dfs = String.join(",", ds.getChildSearchFields(pristupnost));

        ProjektSearcher prs = new ProjektSearcher();
        String pfs = String.join(",", prs.getChildSearchFields(pristupnost));

        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
            if (doc.has("pian_id")) {
                JSONArray cdjs = doc.getJSONArray("pian_id");
                for (int j = 0; j < cdjs.length(); j++) {
                    String cdj = cdjs.getString(j);
                    JSONObject sub = SolrSearcher.getById(client, cdj, fields);
                    if (sub != null) {
                        doc.append("pian", sub);
                    }

                }
            }

//      if (LoginServlet.userId(request) != null) {
//        SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
//      }
            SolrSearcher.addChildField(client, doc, "az_dokument", "valid_dokument", dfs);
            SolrSearcher.addChildField(client, doc, "akce_projekt", "valid_projekt", fields);
        }
    }

    public void addPians(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
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
                        doc.append("pian", sub);
                    }

                }
            }
        }
    }

    @Override
    public String[] getRelationsFields() {
        return new String[]{"ident_cely", "dokument", "projekt"};
    }

    @Override
    public void checkRelations(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {

        JSONArray docs = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            JSONArray valid_dokuments = new JSONArray();
            if (doc.has("az_dokument")) {
                SolrQuery query = new SolrQuery("*")
                        .addFilterQuery(doc.getJSONArray("az_dokument").join(" "))
                        .setRows(10000)
                        .setFields("ident_cely")
                        .setParam("df", "ident_cely");
                try {
                    JSONArray ja = SolrSearcher.json(client, "entities", query).getJSONObject("response").getJSONArray("docs");
                    for (int a = 0; a < ja.length(); a++) {
                        valid_dokuments.put(ja.getJSONObject(a).getString("ident_cely"));
                    }
                } catch (SolrServerException | IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
            doc.put("az_dokument", valid_dokuments);

            if (doc.has("akce_projekt") && !SolrSearcher.existsById(client, doc.getString("akce_projekt"))) {
                doc.remove("akce_projekt");
            }
        }
    }

    @Override
    public JSONObject search(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        try {
            Http2SolrClient client = IndexUtils.getClientNoOp();
            SolrQuery query = new SolrQuery();
            setQuery(request, query);
            //LOGGER.log(Level.INFO, "send request");
            JSONObject jo = SearchUtils.json(query, client, "entities");

            //LOGGER.log(Level.INFO, "checkRelations");
            checkRelations(jo, client, request);
            String pristupnost = LoginServlet.pristupnost(request.getSession());
            //LOGGER.log(Level.INFO, "filter");
            filter(jo, pristupnost, LoginServlet.organizace(request.getSession()));

            if (Boolean.parseBoolean(request.getParameter("mapa"))) {
                //LOGGER.log(Level.INFO, "addPians");
                addPians(jo, client, request);
            }
            //LOGGER.log(Level.INFO, "addFavorites");
            SolrSearcher.addFavorites(jo, client, request);
            //LOGGER.log(Level.INFO, "hotovo");
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
        List<Object> azHeaderFields = Options.getInstance().getJSONObject("fields").getJSONObject("archeologicky_zaznam").getJSONArray("header").toList();
        List<Object> azDetailFields = Options.getInstance().getJSONObject("fields").getJSONObject("archeologicky_zaznam").getJSONArray("detail").toList();
        List<Object> headerFields = Options.getInstance().getJSONObject("fields").getJSONObject("akce").getJSONArray("header").toList();
        List<Object> detailFields = Options.getInstance().getJSONObject("fields").getJSONObject("akce").getJSONArray("detail").toList();

        fields.addAll(azHeaderFields);
        fields.addAll(azDetailFields);
        fields.addAll(headerFields);
        fields.addAll(detailFields);

        fields.add("loc_rpt:loc_rpt_" + pristupnost);
        fields.add("loc:loc_rpt_" + pristupnost);
        fields.add("katastr:f_katastr_" + pristupnost);

        String[] ret = fields.toArray(new String[0]);
        return ret;

//        return new String[]{"ident_cely", "okres", "f_okres:okres", "hlavni_vedouci", "loc", "entity", "datestamp",
//            "specifikace_data", "datum_zahajeni", "datum_ukonceni", "je_nz", "odlozena_nz", "pristupnost",
//            "organizace", "f_organizace:organizace", "projekt", "dokument",
//            "hlavni_typ", "f_hlavni_typ:hlavni_typ", "vedlejsi_typ", "f_vedlejsi_typ:vedlejsi_typ",
//            "organizace_ostatni", "uzivatelske_oznaceni:uzivatelske_oznaceni_" + pristupnost, "ulozeni_nalezu", "poznamka",
//            "pian:[json],adb:[json],projekt:[json],dokument:[json],projekt:[json]",
//            "komponenta:[json],komponenta_dokument:[json],neident_akce:[json],aktivita:[json]",
//            "vedouci_akce_ostatni:[json]",
//            "dokumentacni_jednotka_pian",
//            "dokumentacni_jednotka:[json]",
//            "akce_chranene_udaje:[json]",
//            "chranene_udaje:[json]",
//            "ext_odkaz:[json]",
//            "pian_id",
//            "katastr:f_katastr_" + pristupnost, 
//            "lat:lat_" + pristupnost,
//            "lng:lng_" + pristupnost,
//            "loc_rpt:loc_rpt_" + pristupnost,
//            "loc:loc_rpt_" + pristupnost,
//            "lokalizace:lokalizace_okolnosti_" + pristupnost,
//            "dalsi_katastry:f_dalsi_katastry_" + pristupnost};
    }

    @Override
    public String[] getChildSearchFields(String pristupnost) {
        List<Object> fields = Options.getInstance().getJSONObject("fields").getJSONArray("common").toList();
        List<Object> azHeaderFields = Options.getInstance().getJSONObject("fields").getJSONObject("archeologicky_zaznam").getJSONArray("header").toList();
        //List<Object> azDetailFields = Options.getInstance().getJSONObject("fields").getJSONObject("archeologicky_zaznam").getJSONArray("detail").toList();
        List<Object> headerFields = Options.getInstance().getJSONObject("fields").getJSONObject("akce").getJSONArray("header").toList();
        //List<Object> detailFields = Options.getInstance().getJSONObject("fields").getJSONObject("akce").getJSONArray("detail").toList();

        fields.addAll(azHeaderFields);
        //fields.addAll(azDetailFields);
        fields.addAll(headerFields);
        //fields.addAll(detailFields);

        fields.add("loc_rpt:loc_rpt_" + pristupnost);
        fields.add("loc:loc_rpt_" + pristupnost);
        fields.add("katastr:f_katastr_" + pristupnost);

        String[] ret = fields.toArray(new String[0]);
        return ret;
    }

    public void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
        SolrSearcher.addCommonParams(request, query, ENTITY);
        String pristupnost = LoginServlet.pristupnost(request.getSession());
        if ("E".equals(pristupnost)) {
            pristupnost = "D";
        }
        query.set("df", "text_all_" + pristupnost);
        SolrSearcher.addFilters(request, query, pristupnost);

        if (Boolean.parseBoolean(request.getParameter("mapa"))) {
            SolrSearcher.addLocationParams(request, query);
        }
        query.setFields(getSearchFields(pristupnost));
//        if (Boolean.parseBoolean(request.getParameter("mapa")) && request.getParameter("format") == null) {
//            query.setFields("ident_cely,entity,hlavni_vedouci,organizace,pristupnost,pian:[json],katastr,okres,child_dokument,vazba_projekt,pian_id",
//                    "dokumentacni_jednotka_pian",
//                    "dokumentacni_jednotka:[json]",
//                    "chranene_udaje:[json]",
//                    "akce_chranene_udaje:[json]",
//                    "lat:lat_" + pristupnost,
//                    "lng:lng_" + pristupnost,
//                    "loc_rpt:loc_rpt_" + pristupnost,
//                    "loc:loc_" + pristupnost,
//                    "lokalizace:lokalizace_okolnosti_" + pristupnost,
//                    "dalsi_katastry:f_dalsi_katastry_" + pristupnost);
//            query.setFields(getSearchFields(pristupnost));
//        } else {
//            query.setFields(getSearchFields(pristupnost));
//        }
    }

}
