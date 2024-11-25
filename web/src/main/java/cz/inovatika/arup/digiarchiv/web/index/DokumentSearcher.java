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
import org.apache.solr.client.solrj.response.QueryResponse;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class DokumentSearcher implements EntitySearcher {

    public static final Logger LOGGER = Logger.getLogger(DokumentSearcher.class.getName());

    String ENTITY = "dokument";

    public DokumentSearcher(String entity) {
        this.ENTITY = entity;
    }

    @Override
    public void processAsChild(HttpServletRequest request, JSONObject jo) {

    }

    @Override
    public JSONObject search(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        try {
            Http2SolrClient client = IndexUtils.getClientNoOp();
            SolrQuery query = new SolrQuery();
            setQuery(request, query);
            JSONObject jo = SearchUtils.json(query, client, "entities");
            // checkRelations(jo, client, request);

//            if (Boolean.parseBoolean(request.getParameter("mapa"))
//                    && jo.getJSONObject("response").getInt("numFound") <= Options.getInstance().getClientConf().getJSONObject("mapOptions").getInt("docsForMarker")) {
//                addPians(jo, client, request);
//            }
            if (Boolean.parseBoolean(request.getParameter("isExport"))) {
                addPians(jo, client, request);
            }
            SolrSearcher.addFavorites(jo, client, request);
            // getChilds(jo, client, request);
            String pristupnost = LoginServlet.pristupnost(request.getSession());

            addProjekt(jo, client, request);
            filter(jo, pristupnost, LoginServlet.organizace(request.getSession()));
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
    public void checkRelations(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
        JSONArray docs = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            JSONArray dokument_cast_archeologicky_zaznam = new JSONArray();
            if (doc.has("dokument_cast_archeologicky_zaznam")) {
                // String fq = "{!join to=ident_cely from=dokument_cast_archeologicky_zaznam}ident_cely:\"" + doc.getString("ident_cely") + "\"";
                String fq = "az_dokument:\"" + doc.getString("ident_cely") + "\"";
                SolrQuery query = new SolrQuery("*")
                        .addFilterQuery(fq)
                        .setRows(10000)
                        .setFields("ident_cely");
                try {
                    JSONArray ja = SolrSearcher.json(client, "entities", query).getJSONObject("response").getJSONArray("docs");
                    for (int a = 0; a < ja.length(); a++) {
                        dokument_cast_archeologicky_zaznam.put(ja.getJSONObject(a).getString("ident_cely"));
                    }
                } catch (SolrServerException | IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
            doc.put("dokument_cast_archeologicky_zaznam", dokument_cast_archeologicky_zaznam);

            JSONArray dokument_cast_projekt = new JSONArray();
            if (doc.has("dokument_cast_projekt")) {
                String fq = "ident_cely:\"" + doc.getJSONArray("dokument_cast_projekt").getString(0) + "\"";
                SolrQuery query = new SolrQuery("*")
                        .addFilterQuery(fq)
                        .setRows(10000)
                        .setFields("ident_cely");
                try {
                    JSONArray ja = SolrSearcher.json(client, "entities", query).getJSONObject("response").getJSONArray("docs");
                    for (int a = 0; a < ja.length(); a++) {
                        dokument_cast_projekt.put(ja.getJSONObject(a).getString("ident_cely"));
                    }
                } catch (SolrServerException | IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
            doc.put("dokument_cast_projekt", dokument_cast_projekt);

            // dokument_cast_projekt
        }
    }

    @Override
    public void getChilds(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
        PIANSearcher ps = new PIANSearcher();
        String pristupnost = LoginServlet.pristupnost(request.getSession());
        if ("E".equals(pristupnost)) {
            pristupnost = "D";
        }
        String[] fs = ps.getSearchFields(pristupnost);
        String fields = String.join(",", fs);
        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        String[] f = new String[]{
            "katastr:f_katastr_" + pristupnost,
            "ident_cely,entity,okres,vedouci_akce,specifikace_data,datum_zahajeni,datum_ukonceni,je_nz,pristupnost,organizace,dalsi_katastry,lokalizace"
        };

//        for (int i = 0; i < ja.length(); i++) {
//            JSONObject doc = ja.getJSONObject(i);
//            SolrSearcher.addChildFieldByEntity(client, doc, "dokument_cast_archeologicky_zaznam", String.join(",", f));
//        }
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

    public void addProjekt(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
        String pristupnost = LoginServlet.pristupnost(request.getSession());
        if ("E".equals(pristupnost)) {
            pristupnost = "D";
        }
        ProjektSearcher ps = new ProjektSearcher();
        String[] fs = ps.getSearchFields(pristupnost);
        String fields = String.join(",", fs);

        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
            if (doc.has("dokument_cast_projekt")) {
                JSONArray cdjs = doc.getJSONArray("dokument_cast_projekt");
                for (int j = 0; j < cdjs.length(); j++) {
                    String cdj = cdjs.getString(j);
                    JSONObject sub = SolrSearcher.getById(client, cdj, fields);
                    if (sub != null) {
                        String docPr = sub.getString("pristupnost");
                        if (docPr.compareToIgnoreCase(pristupnost) > 0) {
                            sub.remove("projekt_chranene_udaje");
                        }
                        doc.append("projekt", sub);
                    }

                }
            }
        }
    }

    @Override
    public String[] getSearchFields(String pristupnost) {

        List<Object> fields = Options.getInstance().getJSONObject("fields").getJSONArray("common").toList();
        List<Object> headerFields = Options.getInstance().getJSONObject("fields").getJSONObject("dokument").getJSONArray("header").toList();
        List<Object> detailFields = Options.getInstance().getJSONObject("fields").getJSONObject("dokument").getJSONArray("detail").toList();

        fields.addAll(headerFields);
        fields.addAll(detailFields);

        fields.add("loc_rpt:loc_rpt_" + pristupnost);
        fields.add("loc:loc_rpt_" + pristupnost);
        fields.add("dokument_az_chranene_udaje_" + pristupnost + ":[json]");
        fields.add("f_areal");
        fields.add("f_obdobi"); 
        fields.add("dokument_extra_data_odkaz:extra_data_odkaz");

        String[] ret = fields.toArray(new String[0]);

        return ret;
    }

    @Override
    public String[] getChildSearchFields(String pristupnost) {

        List<Object> fields = Options.getInstance().getJSONObject("fields").getJSONArray("common").toList();
        List<Object> headerFields = Options.getInstance().getJSONObject("fields").getJSONObject("dokument").getJSONArray("header").toList();

        fields.addAll(headerFields);

        String[] ret = fields.toArray(new String[0]);

        return ret;

        // return getSearchFields(pristupnost);
    }

    @Override
    public String[] getRelationsFields() {
        return new String[]{"ident_cely", "dokument_cast_archeologicky_zaznam"};
    }

    public void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
        SolrSearcher.addCommonParams(request, query, ENTITY);
        String pristupnost = LoginServlet.pristupnost(request.getSession());
        if ("E".equals(pristupnost)) {
            pristupnost = "D";
        }
        SolrSearcher.addFilters(request, query, pristupnost);
        query.set("df", "text_all_A");

        if (Boolean.parseBoolean(request.getParameter("mapa"))) {
            SolrSearcher.addLocationParams(request, query);
        }
//        if (Boolean.parseBoolean(request.getParameter("mapa")) && request.getParameter("format") == null) {
//            query.setFields("ident_cely,entity,autor,rok_vzniku,organizace,pristupnost,loc_rpt,pian:[json],f_katastr,f_okres,location_info:[json],jednotka_dokumentu_vazba_akce,jednotka_dokumentu_vazba_druha_akce,jednotka_dokumentu_vazba_lokalita,jednotka_dokumentu_vazba_druha_lokalita");
//        } else {
//            query.setFields(getSearchFields(pristupnost));
////      query.setFields("*,neident_akce:[json],dok_jednotka:[json],pian:[json],adb:[json],soubor:[json],jednotka_dokumentu:[json],let:[json],nalez_dokumentu:[json],komponenta_dokument:[json],tvar:[json]",
////              "okres","f_okres","location_info:[json]");
//        }
        query.setFields(getSearchFields(pristupnost));

    }

    /**
     * Odstrani zabezpecene pole, akce a lokalit z vysledku podle pristupnosti a
     * organizace
     *
     * @param jo
     * @param pristupnost
     * @param org
     */
    @Override
    public void filter(JSONObject jo, String pristupnost, String org) {
        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
            String organizace = doc.optString("dokument_organizace");
            String docPr = doc.getString("pristupnost");

            doc.put("dokument_az_chranene_udaje", doc.opt("dokument_az_chranene_udaje_" + pristupnost));
            doc.remove("dokument_az_chranene_udaje_" + pristupnost);

            boolean sameOrg = org.toLowerCase().equals(organizace.toLowerCase()) && "C".compareTo(pristupnost) >= 0;
            if (docPr.compareTo(pristupnost) > 0 && !sameOrg) {
                doc.remove("katastr");
                doc.remove("dalsi_katastry");
                doc.remove("loc");
                doc.remove("lat");
                doc.remove("lng");
                doc.remove("parent_akce_katastr");
                doc.remove("dok_jednotka");

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
//            if (doc.has("dokument_cast_neident_akce_katastr")) {
//                doc.put("f_katastr", doc.getJSONArray("dokument_cast_neident_akce_katastr").toList());
//            }
            if (doc.has("lokalita")) {
                JSONArray lp = doc.getJSONArray("lokalita");
                for (int j = lp.length() - 1; j > -1; j--) {
                    if (lp.getJSONObject(j).getString("pristupnost").compareTo(pristupnost) > 0 && !sameOrg) {
                        // removeVal(doc, "lokalita", j);
                        lp.getJSONObject(j).remove("katastr");
                        lp.getJSONObject(j).remove("dalsi_katastry");
                    } else {
                        doc.append("f_katastr", lp.getJSONObject(j).get("katastr"));
                    }
                }
            }

//            if (doc.has("akce")) {
//                JSONArray lp = doc.getJSONArray("akce");
//                for (int j = lp.length() - 1; j > -1; j--) {
//                    if (lp.getJSONObject(j).getString("pristupnost").compareTo(pristupnost) > 0 && !sameOrg) {
//                        // removeVal(doc, "akce", j);
//                        lp.getJSONObject(j).remove("katastr");
//                        lp.getJSONObject(j).remove("dalsi_katastry");
//                    } else {
//                        doc.append("f_katastr", lp.getJSONObject(j).getString("katastr"));
//                    }
//                }
//            }
            if (doc.has("location_info")) {
                JSONArray lp = doc.getJSONArray("location_info");
                for (int j = lp.length() - 1; j > -1; j--) {
                    if (lp.getJSONObject(j).has("pristupnost") && lp.getJSONObject(j).getString("pristupnost").compareToIgnoreCase(pristupnost) > 0 && !sameOrg) {
                        //lp.remove(j);
                        lp.getJSONObject(j).remove("katastr");
                    }
                }
            }

        }
    }

    public void removeVal(JSONObject doc, String key, int j) {
        if (doc.optJSONArray(key) != null) {
            doc.getJSONArray(key).remove(j);
        }
    }

    public String getPristupnostBySoubor(String id, String field) {
        try {
            Http2SolrClient client = IndexUtils.getClientNoOp();

            SolrQuery query = new SolrQuery("*").addFilterQuery("filepath:\"" + id + "\"").setRows(1).setFields("dokument", "samostatny_nalez");
            QueryResponse rsp = client.query("relations", query);
            if (rsp.getResults().isEmpty()) {
                return null;
            } else {
                String dok = (String) rsp.getResults().get(0).getFirstValue("dokument");
                if (dok == null || "".equals(dok)) {
                    dok = (String) rsp.getResults().get(0).getFirstValue("samostatny_nalez");
                }

                SolrQuery queryDok = new SolrQuery("*").addFilterQuery("ident_cely:\"" + dok + "\"").setRows(1).setFields("pristupnost");
                QueryResponse rsp2 = client.query("entities", queryDok);
                if (rsp2.getResults().isEmpty()) {
                    return null;
                } else {
                    return (String) rsp2.getResults().get(0).getFirstValue("pristupnost");
                }
            }
        } catch (IOException | SolrServerException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
