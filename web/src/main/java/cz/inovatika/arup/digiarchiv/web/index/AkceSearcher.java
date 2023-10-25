package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;
import java.io.IOException;
import java.util.Arrays;
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
            if (doc.getString("pristupnost").compareTo(pristupnost) > 0) {
                doc.remove("chranene_udaje");
                doc.remove("akce_chranene_udaje");
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

        DokumentSearcher ds = new DokumentSearcher();
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
            SolrSearcher.addChildField(client, doc, "dokument", "valid_dokument", dfs);
            SolrSearcher.addChildField(client, doc, "projekt", "valid_projekt", fields);
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
        if (doc.has("dokument")) {
            SolrQuery query = new SolrQuery("*")
                    .addFilterQuery("{!join fromIndex=entities to=ident_cely from=dokument}ident_cely:\"" + doc.getString("ident_cely") + "\"")
                    .setRows(10000)
                    .setFields("ident_cely");
            try {
                JSONArray ja = SolrSearcher.json(client, "entities", query).getJSONObject("response").getJSONArray("docs");
                for (int a = 0; a < ja.length(); a++) {
                    valid_dokuments.put(ja.getJSONObject(a).getString("ident_cely"));
                }
            } catch (SolrServerException | IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        doc.put("dokument", valid_dokuments);

        if (doc.has("projekt") && !SolrSearcher.existsById(client, doc.getString("projekt"))) {
            doc.remove("projekt");
        }
        }
    }

    @Override
    public JSONObject search(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
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
        try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
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
        return new String[]{"ident_cely", "okres", "f_okres:okres", "hlavni_vedouci", "loc", "entity", "datestamp",
            "specifikace_data", "datum_zahajeni", "datum_ukonceni", "je_nz", "pristupnost",
            "organizace", "f_organizace:organizace", "projekt", "dokument",
            "hlavni_typ", "f_hlavni_typ:hlavni_typ", "vedlejsi_typ", "f_vedlejsi_typ:vedlejsi_typ",
            "organizace_ostatni", "uzivatelske_oznaceni:uzivatelske_oznaceni_" + pristupnost, "ulozeni_nalezu", "poznamka",
            "pian:[json],adb:[json],projekt:[json],dokument:[json],projekt:[json]",
            "komponenta:[json],komponenta_dokument:[json],neident_akce:[json],aktivita:[json]",
            "vedouci_akce_ostatni:[json]",
            "dokumentacni_jednotka_pian",
            "dokumentacni_jednotka:[json]",
            "akce_chranene_udaje:[json]",
            "chranene_udaje:[json]",
            "ext_odkaz:[json]",
            "katastr:f_katastr_" + pristupnost, 
            "lat:lat_" + pristupnost,
            "lng:lng_" + pristupnost,
            "loc_rpt:loc_rpt_" + pristupnost,
            "loc:loc_rpt_" + pristupnost,
            "lokalizace:lokalizace_okolnosti_" + pristupnost,
            "dalsi_katastry:f_dalsi_katastry_" + pristupnost};
    }

    @Override
    public String[] getChildSearchFields(String pristupnost) {
        //  return getSearchFields(pristupnost);
        return new String[]{"ident_cely,entity,pristupnost,okres,hlavni_vedouci,specifikace_data,datum_zahajeni,datum_ukonceni,je_nz,pristupnost,organizace,dokument",
            "lokalizace:lokalizace_okolnosti_" + pristupnost,
            "katastr:f_katastr_" + pristupnost,
            "dalsi_katastry:f_dalsi_katastry_" + pristupnost};
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

        if (Boolean.parseBoolean(request.getParameter("mapa")) && request.getParameter("format") == null) {
            query.setFields("ident_cely,entity,hlavni_vedouci,organizace,pristupnost,pian:[json],katastr,okres,child_dokument,vazba_projekt",
                    "dokumentacni_jednotka_pian",
                    "dokumentacni_jednotka:[json]",
                    "chranene_udaje:[json]",
                    "akce_chranene_udaje:[json]",
                    "lat:lat_" + pristupnost,
                    "lng:lng_" + pristupnost,
                    "loc_rpt:loc_rpt_" + pristupnost,
                    "loc:loc_" + pristupnost,
                    "lokalizace:lokalizace_okolnosti_" + pristupnost,
                    "dalsi_katastry:f_dalsi_katastry_" + pristupnost);

        } else {
            query.setFields(getSearchFields(pristupnost));
        }
    }

}
