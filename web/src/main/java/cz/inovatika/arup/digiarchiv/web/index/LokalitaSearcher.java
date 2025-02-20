
package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;
import java.io.IOException;
import java.util.ArrayList;
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
public class LokalitaSearcher implements EntitySearcher {

    public static final Logger LOGGER = Logger.getLogger(LokalitaSearcher.class.getName());

    final String ENTITY = "lokalita";

    private final List<String> allowedFields = Arrays.asList(new String[]{"ident_cely", "entity", "pristupnost", "okres", "typ_lokality",
        "druh", "datum_zahajeni", "datum_ukonceni", "child_dokument", "organizace", "datestamp"});

    @Override
    public void filter(JSONObject jo, String pristupnost, String org) {
        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
//            doc.put("pian", doc.opt("pian_" + pristupnost));
//            doc.remove("pian_" + pristupnost);
            if (doc.optString("pristupnost").compareTo(pristupnost) > 0) {
                if (doc.getString("pristupnost").compareTo(pristupnost) > 0) {
                    doc.remove("az_chranene_udaje");
                    doc.remove("lokalita_chranene_udaje");
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
        
    }
    
    public void addOkresy(JSONObject jo) { 

//        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
//        for (int i = 0; i < ja.length(); i++) {
//            JSONObject doc = ja.getJSONObject(i);
//            if (doc.has("az_chranene_udaje")) {
//                JSONArray cdjs = doc.getJSONObject("az_chranene_udaje").optJSONArray("dalsi_katastr", new JSONArray());
//                List<String> okresy = new ArrayList<>();
//                for (int j = 0; j < cdjs.length(); j++) {
//                    JSONObject dk = cdjs.getJSONObject(j);
//                    String ruian = dk.optString("id");
//                    String okres = SolrSearcher.getOkresByKatastr(ruian);
//                    if (!okresy.contains(okres)) {
//                        okresy.add(okres);
//                    }
//                }
//                doc.getJSONObject("az_chranene_udaje").put("okresy", okresy);
//            }
//        }
    }

    @Override
    public String[] getChildSearchFields(String pristupnost) {
        return getSearchFields(pristupnost);
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
                //System.out.println(query);
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

            if (doc.has("az_projekt") && !SolrSearcher.existsById(client, doc.getString("az_projekt"))) {
                doc.remove("az_projekt");
            }
        }
        addOkresy(jo);
    }

    @Override
    public void getChilds(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
        if (jo.has("response")) {
            JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
            for (int i = 0; i < ja.length(); i++) {
                JSONObject doc = ja.getJSONObject(i);
                if (LoginServlet.userId(request) != null) {
                    SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
                }
                DokumentSearcher as = new DokumentSearcher("dokument");
                String fields = String.join(",", as.getChildSearchFields("D"));

                //String fields = "ident_cely,entity,katastr,okres,autor,rok_vzniku,typ_dokumentu,material_originalu,pristupnost,rada,material_originalu,organizace,popis,soubor_filepath";
                SolrSearcher.addChildField(client, doc, "dokument", "full_dokument", fields);
            }
        } else {
            JSONObject doc = jo.getJSONObject("doc");
            if (LoginServlet.userId(request) != null) {
                SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
            }
            String fields = "ident_cely,katastr,okres,autor,rok_vzniku,typ_dokumentu,material_originalu,pristupnost,rada,material_originalu,organizace,popis,soubor_filepath";
            SolrSearcher.addChildField(client, doc, "dokument", "full_dokument", fields);
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
            if (doc.has("az_dj_pian")) { 
                JSONArray cdjs = doc.getJSONArray("az_dj_pian");
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

    @Override
    public void processAsChild(HttpServletRequest request, JSONObject jo) {

        if (!Boolean.parseBoolean(request.getParameter("mapa"))) {
            addOkresy(jo);
        }
    }

    @Override
    public JSONObject search(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        try { 
            Http2SolrClient client = IndexUtils.getClientNoOp();
            SolrQuery query = new SolrQuery();
            setQuery(request, query);
            JSONObject jo = SearchUtils.json(query, client, "entities");
            if (jo.has("error")) {
                return jo;
            }
            String pristupnost = LoginServlet.pristupnost(request.getSession());

//            if (Boolean.parseBoolean(request.getParameter("mapa"))
//                    && jo.getJSONObject("response").getInt("numFound") <= Options.getInstance().getClientConf().getJSONObject("mapOptions").getInt("docsForMarker")) {
//                addPians(jo, client, request);
//            } 
            if (Boolean.parseBoolean(request.getParameter("isExport"))) {
                addPians(jo, client, request);
            }
            if (!Boolean.parseBoolean(request.getParameter("mapa"))) {
                checkRelations(jo, client, request);
            }
            filter(jo, pristupnost, LoginServlet.organizace(request.getSession()));
            if (!Boolean.parseBoolean(request.getParameter("mapa"))){
                addOkresy(jo);
            }
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
        List<Object> azHeaderFields = Options.getInstance().getJSONObject("fields").getJSONObject("archeologicky_zaznam").getJSONArray("header").toList();
        List<Object> azDetailFields = Options.getInstance().getJSONObject("fields").getJSONObject("archeologicky_zaznam").getJSONArray("detail").toList();
        List<Object> headerFields = Options.getInstance().getJSONObject("fields").getJSONObject("lokalita").getJSONArray("header").toList();
        List<Object> detailFields = Options.getInstance().getJSONObject("fields").getJSONObject("lokalita").getJSONArray("detail").toList();

        fields.addAll(azHeaderFields);
        fields.addAll(azDetailFields);
        fields.addAll(headerFields);
        fields.addAll(detailFields);

        fields.add("pian_id:az_dj_pian");
        fields.add("loc_rpt:loc_rpt_" + pristupnost);
        fields.add("loc:loc_rpt_" + pristupnost);
        fields.add("katastr:f_katastr_" + pristupnost);

        String[] ret = fields.toArray(new String[0]);
        return ret;
//        
//        return new String[]{"ident_cely,entity,pristupnost,okres,lokalita_typ_lokality,lokalita_druh,lokalita_zachovalost,lokalita_jistota,datestamp,dokument,projekt",
//            "dokumentacni_jednotka:[json]",
//            "lokalita_chranene_udaje:[json]",
//            "chranene_udaje:[json]",
//            "pian_" + pristupnost + ":[json]",
//            "loc_rpt:loc_rpt_" + pristupnost,
//            "nazev:lokalita_chranene_udaje_nazev_" + pristupnost,
//            "popis:lokalita_chranene_udaje_popis_" + pristupnost,
//            "katastr:f_katastr_" + pristupnost,
//            "dalsi_katastry:f_dalsi_katastry_" + pristupnost};
    }

    public void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
        SolrSearcher.addCommonParams(request, query, ENTITY);
        // query.addFilterQuery("{!tag=entityF}stav:3");
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
//            query.setFields("ident_cely,entity,nazev,organizace,pristupnost,pian_id,pian:[json],okres,child_dokument",
//                    "katastr:f_katastr_" + pristupnost,
//                    "loc_rpt:loc_rpt_" + pristupnost);
//        } else {
//            query.setFields(getSearchFields(pristupnost));
//        }

    }

}
