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
public class ProjektSearcher implements EntitySearcher {

    public static final Logger LOGGER = Logger.getLogger(ProjektSearcher.class.getName());
    final String ENTITY = "projekt";

    private final List<String> allowedFields = Arrays.asList(new String[]{"ident_cely", "entity", "pristupnost", "vedouci_projektu", "okres", "organizace_prihlaseni", "datestamp",
        "typ_projektu", "datum_zahajeni", "datum_ukonceni", "podnet", "child_akce", "child_samostatny_nalez"});

    @Override
    public void filter(JSONObject jo, String pristupnost, String org) {

        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
            String docPr = doc.getString("pristupnost");

//            if (doc.getString("pristupnost").compareTo(pristupnost) > 0) {
//                Object[] keys = doc.keySet().toArray();
//                for (Object key : keys) {
//                    if (!allowedFields.contains((String) key)) {
//                        doc.remove((String) key);
//                    }
//
//                }
//            }
            if (docPr.compareToIgnoreCase(pristupnost) > 0) {
                doc.remove("projekt_chranene_udaje");
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

            if (doc.has("location_info")) {
                JSONArray lp = doc.getJSONArray("location_info");
                for (int j = lp.length() - 1; j > -1; j--) {
                    if (lp.getJSONObject(j).has("pristupnost") && lp.getJSONObject(j).getString("pristupnost").compareToIgnoreCase(pristupnost) > 0) {
                        lp.remove(j);// .getJSONObject(j).remove("location_info");
                    }
                }
            }

        }

    }

    @Override
    public void checkRelations(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
        JSONArray docs = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            JSONArray samostatny_nalez = new JSONArray();

            if (doc.has("projekt_samostatny_nalez")) {
//                SolrQuery query = new SolrQuery("*")
//                        .addFilterQuery("{!join fromIndex=entities to=ident_cely from=projekt_samostatny_nalez}ident_cely:\"" + doc.getString("ident_cely") + "\"")
//                        .setRows(10000)
//                        .setFields("ident_cely");

                SolrQuery query = new SolrQuery("*")
                        .setRows(10000)
                        .addFilterQuery("entity:samostatny_nalez")
                        .addFilterQuery("samostatny_nalez_projekt:\"" + doc.getString("ident_cely") + "\"");

                try {
                    JSONArray ja = SolrSearcher.json(client, "entities", query).getJSONObject("response").getJSONArray("docs");
                    for (int a = 0; a < ja.length(); a++) {
                        samostatny_nalez.put(ja.getJSONObject(a).getString("ident_cely"));
                    }
                } catch (SolrServerException | IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
            doc.put("projekt_samostatny_nalez", samostatny_nalez);

            JSONArray dokument_cast_archeologicky_zaznam = new JSONArray();
            JSONArray id_akce = new JSONArray();
            JSONArray id_lokalita = new JSONArray();
            if (doc.has("projekt_archeologicky_zaznam")) {
                SolrQuery query = new SolrQuery("*")
                        .addFilterQuery("{!join fromIndex=entities to=ident_cely from=projekt_archeologicky_zaznam}ident_cely:\"" + doc.getString("ident_cely") + "\"")
                        .setRows(10000)
                        .setFields("ident_cely,entity");
                try {
                    JSONArray ja = SolrSearcher.json(client, "entities", query).getJSONObject("response").getJSONArray("docs");
                    for (int a = 0; a < ja.length(); a++) {
                        dokument_cast_archeologicky_zaznam.put(ja.getJSONObject(a).getString("ident_cely"));
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
            }
            doc.put("projekt_archeologicky_zaznam", dokument_cast_archeologicky_zaznam);
            doc.put("id_akce", id_akce);
            doc.put("id_lokalita", id_lokalita);
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
        String pfields = String.join(",", fs);

        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
            if (LoginServlet.userId(request) != null) {
                SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
            }

            AkceSearcher as = new AkceSearcher();
            String fields = String.join(",", as.getSearchFields(pristupnost));
//            String fields = "ident_cely,pristupnost,entity,"
//                    + "katastr,"
//                    + "okres,vedouci_akce,specifikace_data,datum_zahajeni,datum_ukonceni,je_nz,pristupnost,"
//                    + "organizace.dalsi_katastry,lokalizace,pian:[json]";
            SolrSearcher.addChildFieldByEntity(client, doc, "projekt_archeologicky_zaznam", fields);

            fields = "ident_cely,pristupnost,katastr,okres,nalezce,datum_nalezu,typ_dokumentu,material_originalu,rada,pristupnost,obdobi,presna_datace,druh,specifikace,soubor_filepath";
            SolrSearcher.addChildField(client, doc, "samostatny_nalez", "valid_samostatny_nalez", fields, "searchable:true");
            if (doc.has("pian_id")) {
                JSONArray cdjs = doc.getJSONArray("pian_id");
                for (int j = 0; j < cdjs.length(); j++) {
                    String cdj = cdjs.getString(j);
                    JSONObject sub = SolrSearcher.getById(client, cdj, pfields);
                    if (sub != null) {
                        doc.append("pian", sub);
                    }

                }
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
            JSONObject jo = SearchUtils.json(query, client, "entities");
            removeInvalid(client, jo);
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

    // remove invalid child_samostatny_nalez
    private void removeInvalid(Http2SolrClient client, JSONObject jo) throws SolrServerException, IOException {
        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
            SolrQuery query = new SolrQuery("*")
                    .setRows(10000)
                    .addFilterQuery("entity:samostatny_nalez")
                    .addFilterQuery("samostatny_nalez_projekt:\"" + doc.getString("ident_cely") + "\"");
            JSONObject sn = SolrSearcher.json(client, "entities", query);
            JSONArray ja2 = sn.getJSONObject("response").getJSONArray("docs");
            JSONArray jaSn = new JSONArray();
            for (int j = 0; j < ja2.length(); j++) {
                jaSn.put(ja2.getJSONObject(j).getString("ident_cely"));
            }
            doc.put("projekt_samostatny_nalez", jaSn);

            query = new SolrQuery("*")
                    .setRows(10000)
                    .addFilterQuery("akce_projekt:\"" + doc.getString("ident_cely") + "\"");
            JSONObject snaz = SolrSearcher.json(client, "entities", query);
            JSONArray jaaz = snaz.getJSONObject("response").getJSONArray("docs");
            JSONArray jaAz = new JSONArray();
            for (int j = 0; j < jaaz.length(); j++) {
                jaAz.put(jaaz.getJSONObject(j).getString("ident_cely"));
            }
            doc.put("projekt_archeologicky_zaznam", jaAz);

        }
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
        List<Object> headerFields = Options.getInstance().getJSONObject("fields").getJSONObject("projekt").getJSONArray("header").toList();
        List<Object> detailFields = Options.getInstance().getJSONObject("fields").getJSONObject("projekt").getJSONArray("detail").toList();

        fields.addAll(headerFields);
        fields.addAll(detailFields);

        fields.add("projekt_hlavni_katastr:projekt_hlavni_katastr_" + pristupnost);
        fields.add("loc_rpt:loc_rpt_" + pristupnost);
        fields.add("loc:loc_rpt_" + pristupnost);

        String[] ret = fields.toArray(new String[0]);

        return ret;
        // return new String[]{"*,pian:[json]", "chranene_udaje:[json]", "okres", "hlavni_katastr:hlavni_katastr_" + pristupnost, "katastr:f_katastr_" + pristupnost, "f_okres:okres"};
    }

    public void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
        SolrSearcher.addCommonParams(request, query, ENTITY);
        String pristupnost = LoginServlet.pristupnost(request.getSession());
        if ("E".equals(pristupnost)) {
            pristupnost = "D";
        }
        query.set("df", "text_all_" + pristupnost);
        query.setFields(getSearchFields(pristupnost));
        if (Boolean.parseBoolean(request.getParameter("mapa"))) {
            SolrSearcher.addLocationParams(request, query);
        }
        SolrSearcher.addFilters(request, query, pristupnost);
    }

    @Override
    public String[] getRelationsFields() {
        return new String[]{"ident_cely", "projekt_samostatny_nalez"};
    }

    @Override
    public String[] getChildSearchFields(String pristupnost) {
        List<Object> fields = Options.getInstance().getJSONObject("fields").getJSONArray("common").toList();
        List<Object> headerFields = Options.getInstance().getJSONObject("fields").getJSONObject("projekt").getJSONArray("header").toList();

        fields.addAll(headerFields);
        fields.add("projekt_hlavni_katastr:projekt_hlavni_katastr_" + pristupnost);

        String[] ret = fields.toArray(new String[0]);

        return ret;
    }
}
