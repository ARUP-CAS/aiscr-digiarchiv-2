package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraHarvester;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class SearchUtils {

    static final Logger LOGGER = Logger.getLogger(SearchUtils.class.getName());

    static Map<String, String> obdobi_poradi;

    static Map<String, String> pristupnostMap;

    public static String getObdobiPoradi(String obdobi) {
        if (obdobi_poradi == null) {
            initObdobiPoradi();
        }
        return obdobi_poradi.get(obdobi.toLowerCase());
    }

    private static void initObdobiPoradi() {
        try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
            obdobi_poradi = new HashMap<>();

            SolrQuery query = new SolrQuery()
                    .setQuery("heslar_name:obdobi_druha")
                    .setRows(1000)
                    .setFields("poradi,nazev,zkratka");
            QueryResponse resp = client.query("heslar", query);
            for (SolrDocument doc : resp.getResults()) {
                obdobi_poradi.put(((String) doc.getFieldValue("zkratka")).toLowerCase(), "" + doc.getFieldValue("poradi"));
                obdobi_poradi.put(((String) doc.getFieldValue("nazev")).toLowerCase(), "" + doc.getFieldValue("poradi"));
            }
            // LOGGER.log(Level.INFO, "obdobi: {0}", obdobi_poradi.size());
        } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public static Map<String, String> getPristupnostMap() {
        if (pristupnostMap == null) {
            initPristupnostMap();
        }
        return pristupnostMap;
    }

    private static void initPristupnostMap() {
        try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
            pristupnostMap = new HashMap<>();

            SolrQuery query = new SolrQuery()
                    .setQuery("nazev_heslare:pristupnost")
                    .setRows(1000)
                    .setFields("ident_cely,zkratka");
            QueryResponse resp = client.query("heslar", query);
            for (SolrDocument doc : resp.getResults()) {
                pristupnostMap.put(((String) doc.getFieldValue("ident_cely")), "" + doc.getFieldValue("zkratka"));
            }
            // LOGGER.log(Level.INFO, "obdobi: {0}", obdobi_poradi.size());
        } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            pristupnostMap = null;
        }
    }

//    public static JSONObject json(SolrQuery query, String coreUrl) {
//        query.set("wt", "json");
//        query.setRequestHandler("/search");
//        String jsonResponse;
//        try (Http2SolrClient client = new Http2SolrClient.Builder(coreUrl).build()) {
//            QueryRequest qreq = new QueryRequest(query);
//            // qreq.setPath();
//            NoOpResponseParser dontMessWithSolr = new NoOpResponseParser();
//            dontMessWithSolr.setWriterType("json");
//            client.setParser(dontMessWithSolr);
//            NamedList<Object> qresp = client.request(qreq);
//            jsonResponse = (String) qresp.get("response");
//            client.close();
//            return new JSONObject(jsonResponse);
//        } catch (SolrServerException | IOException ex) {
//            LOGGER.log(Level.SEVERE, null, ex);
//            return new JSONObject().put("error", ex);
//        }
//    }

    public static SolrDocumentList docs(SolrQuery query, String coreUrl) {
        query.set("wt", "json");
        try (Http2SolrClient client = new Http2SolrClient.Builder(coreUrl).build()) {
            return client.query(query).getResults();
        } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static JSONObject searchOrIndex(SolrQuery query, String core, String id) throws Exception {
        try {
            Http2SolrClient client = IndexUtils.getClientNoOp();
            JSONObject json = json(query, client, core, false);
            if (json.getJSONObject("response").getInt("numFound") > 0) {
                return json;
            } else {
                FedoraHarvester fh = new FedoraHarvester();
                fh.indexId(id);
                return json(query, client, core, false);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error retrieving {0}", id);
            LOGGER.log(Level.SEVERE, null, ex);
            throw ex;
        }
    }

    public static JSONObject searchById(SolrQuery query, String core, String id) {
        return searchById(query, core, id, true);
    }

    public static JSONObject searchById(SolrQuery query, String core, String id, boolean onlySearchable) {
        try {
            Http2SolrClient client = IndexUtils.getClientNoOp();
            JSONObject json = json(query, client, core, onlySearchable);
            return json;
        } catch (Exception ex) {
            return new JSONObject();
        }
    }

    public static JSONObject json(SolrQuery query, Http2SolrClient client, String core) { 
        return json(query, client, core, true);
    }

    public static JSONObject json(SolrQuery query, Http2SolrClient client, String core, boolean onlySearchable) { 
        query.set("wt", "json");
        query.addFilterQuery("-is_deleted:true");
        if (onlySearchable) {
            query.setRequestHandler("/search");
        } else {
            query.setRequestHandler("/select");
        }
        //    System.out.println(query);
        String qt = query.get("qt");
        String jsonResponse;
        try {
            QueryRequest qreq = new QueryRequest(query);
            if (qt != null) {
                qreq.setPath(qt);
            }
            NoOpResponseParser dontMessWithSolr = new NoOpResponseParser();
            dontMessWithSolr.setWriterType("json");
            NamedList<Object> qresp = client.request(qreq, core);
            jsonResponse = (String) qresp.get("response");
            return new JSONObject(jsonResponse);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return new JSONObject().put("error", ex);
        }
    }

    public static String csv(SolrQuery query, Http2SolrClient client, String core) {
        query.set("wt", "csv");
        String qt = query.get("qt");
        try {
            QueryRequest qreq = new QueryRequest(query);
            if (qt != null) {
                qreq.setPath(qt);
            }
            NoOpResponseParser dontMessWithSolr = new NoOpResponseParser();
            dontMessWithSolr.setWriterType("csv");
            // client.setParser(dontMessWithSolr);
            NamedList<Object> qresp = client.request(qreq, core);
            return (String) qresp.get("response");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return ex.toString();
        }
    }

    public static JSONObject clean(Instant date, String entity) {
        JSONObject ret = new JSONObject();

        String solrhost = Options.getInstance().getString("solrhost");
        String collection = entity;
        boolean searchable = isSearchable(entity);
        if (searchable) {
            collection = "entities";
        }

        try (SolrClient solr = new Http2SolrClient.Builder(solrhost).build()) {
            String query = "indextime:[* TO " + date.toString() + "]";
            if (searchable) {
                query += " AND entity:" + entity;
            }
            solr.deleteByQuery(collection, query, 1);
            ret.put("resp", "success");
        } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            ret.put("error", ex.toString());
        }
        return ret;
    }

    public static boolean isSearchable(String entity) {
        List<Object> entities = Options.getInstance().getClientConf().getJSONArray("entities").toList();
        return entities.indexOf(entity) > -1;
    }

    public static EntitySearcher getSearcher(String entity) {
        EntitySearcher searcher;
        switch ((String) entity) {
            case "akce":
                searcher = new AkceSearcher();
                break;
            case "lokalita":
                searcher = new LokalitaSearcher();
                break;
            case "projekt":
                searcher = new ProjektSearcher();
                break;
            case "samostatny_nalez":
                searcher = new SamostatnyNalezSearcher();
                break;
            case "knihovna_3d":
                // searcher = new Library3DSearcher();
                searcher = new DokumentSearcher("knihovna_3d");
                break;
            case "let":
                searcher = new LetSearcher();
                break;
            case "ext_zdroj":
                searcher = new ExtZdrojSearcher();
                break;
            case "pian":
                searcher = new PIANSearcher();
                break;
            case "adb":
                searcher = new ADBSearcher();
                break;
            case "dokumentacni_jednotka":
                searcher = new DokJednotkaSearcher();
                break;
            case "dokument_cast":
                searcher = new DokumentCastSearcher();
                break;
            case "dokument": 
                searcher = new DokumentSearcher("dokument");
                break;
            case "komponenta":
                searcher = new KomponentaSearcher();
                break;
            case "vyskovy_bod":
                searcher = new VyskovyBodSearcher();
                break;
            default:
                searcher = null;
        }
        return searcher;
    }

    public static ComponentSearcher getComponentSearcher(String component) {
        ComponentSearcher searcher;
        switch ((String) component) {
            case "dokumentacni_jednotka":
                searcher = new DokJednotkaSearcher();
                break;
            case "jednotka_dokumentu":
                searcher = new JednotkaDokumentuSearcher();
                break;
            case "dokument_cast":
                searcher = new DokumentCastSearcher();
                break;
            case "komponenta":
                searcher = new KomponentaSearcher();
                break;
            case "komponenta_dokument":
                searcher = new KomponentaDokumentSearcher();
                break;
            case "vyskovy_bod":
                searcher = new VyskovyBodSearcher();
                break;
            case "adb":
                searcher = new ADBSearcher();
                break;
            default:
                searcher = null;
        }
        return searcher;
    }

    public static void addJSONFields(JSONObject doc, SolrInputDocument idoc) {
        for (String s : doc.keySet()) {
            switch (s) {
                case "_version_":
                case "_root_":
                case "indextime":
                    break;
                default:
                    addFieldNonRepeat(idoc, s, doc.get(s));
                // idoc.addField(s, doc.get(s));
            }
        }
    }

    public static void addJSONFieldsSufixed(JSONObject doc, SolrInputDocument idoc, String[] sufixes) {
        for (String s : doc.keySet()) {
            switch (s) {
                case "_version_":
                case "_root_":
                case "indextime":
                    break;
                default:
                    idoc.addField(s, doc.get(s));
                    for (String sufix : sufixes) {
                        idoc.addField(s + "_" + sufix, doc.get(s));
                    }
            }
        }
    }

    public static void addJSONFields(JSONObject doc, String prefix, SolrInputDocument idoc) {
        for (String s : doc.keySet()) {
            switch (s) {
                case "_version_":
                case "_root_":
                case "indextime":
                    break;
                default:
                    // idoc.addField(prefix + "_" + s, doc.optString(s));
                    addFieldNonRepeat(idoc, prefix + "_" + s, doc.optString(s));
            }
        }
    }

    public static void addFieldNonRepeat(SolrInputDocument idoc, String field, Object value) {

        if (idoc.getFieldValues(field) == null || !idoc.getFieldValues(field).contains(value)) {
            idoc.addField(field, value);
        }
    }
}
