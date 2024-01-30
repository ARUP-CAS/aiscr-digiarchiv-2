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
import org.apache.solr.client.solrj.response.QueryResponse;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class DokumentSearcher implements EntitySearcher {

    public static final Logger LOGGER = Logger.getLogger(DokumentSearcher.class.getName());

    final String ENTITY = "dokument";

    @Override
    public JSONObject search(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
            SolrQuery query = new SolrQuery();
            setQuery(request, query);
            JSONObject jo = SearchUtils.json(query, client, "entities");
            if (Boolean.parseBoolean(request.getParameter("mapa"))) {
                getChilds(jo, client, request);
            }
            SolrSearcher.addFavorites(jo, client, request);
            // getChilds(jo, client, request);
            String pristupnost = LoginServlet.pristupnost(request.getSession());
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
    public void checkRelations(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
        JSONArray docs = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            JSONArray dokument_cast_archeologicky_zaznam = new JSONArray();
            if (doc.has("dokument_cast_archeologicky_zaznam")) {
                SolrQuery query = new SolrQuery("*")
                        .addFilterQuery("{!join fromIndex=entities to=ident_cely from=dokument_cast_archeologicky_zaznam}ident_cely:\"" + doc.getString("ident_cely") + "\"")
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
//        String fieldsAkce = "ident_cely,katastr,okres,vedouci_akce,specifikace_data,datum_zahajeni,datum_ukonceni,je_nz,pristupnost,organizace,dalsi_katastry,lokalizace";
//        String fieldsLok = "ident_cely,katastr,okres,nazev,typ_lokality,druh,pristupnost,dalsi_katastry,popis";


        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
            if (LoginServlet.userId(request) != null) {
                SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
            }
            
            SolrSearcher.addChildFieldByEntity(client, doc, "dokument_cast_archeologicky_zaznam", String.join(",", f));

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
    public String[] getSearchFields(String pristupnost) {
        String[] f = new String[]{
            Options.getInstance().getJSONObject("fields").getJSONArray("common").join(",").replaceAll("\"", ""),
            Options.getInstance().getJSONObject("fields").getJSONObject("dokument").getJSONArray("header").join(",").replaceAll("\"", ""),
            Options.getInstance().getJSONObject("fields").getJSONObject("dokument").getJSONArray("detail").join(",").replaceAll("\"", ""),
            "dokument_cast_neident_akce:[json],dok_jednotka:[json],pian:[json],adb:[json],soubor:[json],nalez_dokumentu:[json],location_info:[json]",
            // "dokument_cast:[json]",
            "komponenta:[json]",
            "okres", "f_okres", "pian_id",
//            "f_pian_presnost:f_pian_presnost_" + pristupnost,
//            "f_pian_typ:f_pian_typ_" + pristupnost,
//            "f_pian_zm10:f_pian_zm10_" + pristupnost,
            "loc_rpt:loc_" + pristupnost,
            "katastr:f_katastr_" + pristupnost,
            "dalsi_katastry:f_dalsi_katastry_" + pristupnost,
            "f_typ_vyzkumu:f_typ_vyzkumu_" + pristupnost,
            "lokalizace:f_lokalizace_" + pristupnost};
        return f;
    }

    @Override
    public String[] getChildSearchFields(String pristupnost) {
        String[] f = new String[]{
            "ident_cely,entity,pristupnost,katastr,okres,autor,rok_vzniku,typ_dokumentu,material_originalu,pristupnost,rada,material_originalu,organizace,popis,soubor_filepath,location_info:[json]"};
        //if (pristupnost)
        return f;
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
            String organizace = doc.optString("organizace");
            String docPr = doc.getString("pristupnost");

            boolean sameOrg = org.toLowerCase().equals(organizace.toLowerCase()) && "C".compareTo(pristupnost) >= 0;
            if (docPr.compareTo(pristupnost) > 0 && !sameOrg) {
                doc.remove("katastr");
                doc.remove("dalsi_katastry");
                doc.remove("loc");
                doc.remove("lat");
                doc.remove("lng");
                doc.remove("pian");
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
            if (doc.has("dokument_cast_neident_akce_katastr")) {
                doc.put("f_katastr", doc.getJSONArray("dokument_cast_neident_akce_katastr").toList());
            }
            if (doc.has("lokalita")) {
                JSONArray lp = doc.getJSONArray("lokalita");
                for (int j = lp.length() - 1; j > -1; j--) {
                    if (lp.getJSONObject(j).getString("pristupnost").compareTo(pristupnost) > 0 && !sameOrg) {
                        // removeVal(doc, "lokalita", j);
                        lp.getJSONObject(j).remove("katastr");
                        lp.getJSONObject(j).remove("dalsi_katastry");
                    } else {
                        doc.append("f_katastr", lp.getJSONObject(j).getString("katastr"));
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
        try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {

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
