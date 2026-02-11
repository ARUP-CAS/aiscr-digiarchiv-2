package cz.inovatika.arup.digiarchiv.web4.fedora;

import cz.inovatika.arup.digiarchiv.web4.FormatUtils;
import cz.inovatika.arup.digiarchiv.web4.InitServlet;
import cz.inovatika.arup.digiarchiv.web4.Options;
import cz.inovatika.arup.digiarchiv.web4.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web4.index.SolrClientFactory;
import cz.inovatika.arup.digiarchiv.web4.index.SolrSearcher;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
//import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
//import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class FedoraHarvester {

    public static final Logger LOGGER = Logger.getLogger(FedoraHarvester.class.getName());

    private static final String STATUS_RUNNING = "runing";
    private static final String STATUS_FINISHED = "finished";
    private static final String STATUS_STOPPED = "stoped";
    private static final String CONTAINS = "http://www.w3.org/ns/ldp#contains";

    JSONObject ret = new JSONObject();
    JSONArray errors = new JSONArray();
    // SolrClient solr;
    int offset = 0;

    List<SolrInputDocument> idocsEntities = new ArrayList();
    List<SolrInputDocument> idocsDeleted = new ArrayList();
    List<SolrInputDocument> idocsOAI = new ArrayList();
    Map<String, List<SolrInputDocument>> idocs = new HashMap<>();

    long requestTime;
    long processTime;

    DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("YYYY-MM-dd")
            .withZone(ZoneId.systemDefault());

    private void writeToErrorsFile(String id) throws IOException {
        Instant date = Instant.now();
        File f = new File(InitServlet.CONFIG_DIR + File.separator + "logs" + File.separator + "index_errors." + formatter.format(date) + ".json");
        FileUtils.writeStringToFile(f, id + System.getProperty("line.separator"), "UTF-8", true);
    }

    private void writeRetToFile(String type, Instant date) throws IOException {

        File f = new File(InitServlet.CONFIG_DIR + File.separator + "logs" + File.separator + type + "." + formatter.format(date) + ".json");
        FileUtils.writeStringToFile(f, ret.toString(2), "UTF-8");
    }

    private String readStatusFile(String type) throws IOException {
        if (type.contains("/")) {
            return "none";
        }
        File f = new File(InitServlet.CONFIG_DIR + File.separator + type + "_" + "status.txt");
        if (f.exists() && f.canRead()) {
            return FileUtils.readFileToString(f, "UTF-8");
        } else {
            return "none";
        }
    }

    private void writeStatusFile(String type, String status) throws IOException {
        File f = new File(InitServlet.CONFIG_DIR + File.separator + type + "_" + "status.txt");
        FileUtils.writeStringToFile(f, status, "UTF-8");
    }

    private void writeUpdateFile(Instant date) throws IOException {
        File f = new File(InitServlet.CONFIG_DIR + File.separator + "update_time.txt");
        FileUtils.writeStringToFile(f, date.toString(), "UTF-8");
    }

    private String readUpdateFile() throws IOException {
        File f = new File(InitServlet.CONFIG_DIR + File.separator + "update_time.txt");
        if (f.exists() && f.canRead()) {
            return FileUtils.readFileToString(f, "UTF-8");
        } else {
            return null;
        }
    }

    /**
     * Stop fedora index process
     *
     * @return
     * @throws IOException
     */
    public JSONObject stopIndex() throws Exception {
        writeStatusFile("index", STATUS_STOPPED);
        return new JSONObject().put("msg", "Index process stopped");
    }

    /**
     * Stop fedora update process
     *
     * @return
     * @throws IOException
     */
    public JSONObject stopUpdate() throws Exception {
        writeStatusFile("update", STATUS_STOPPED);
        return new JSONObject().put("msg", "Update process stopped");
    }

    /**
     * Full fedora harvest and index
     *
     * @return
     * @throws IOException
     */
    public JSONObject harvest() throws IOException {
        Instant start = Instant.now();
        try {
            SolrClient solr = SolrClientFactory.getSolrClient();
            ret = new JSONObject();
            getModels(solr);
            solr.commit("oai");
            solr.commit("entities");
            for (String key : idocs.keySet()) {
                solr.commit(key);
            }
            solr.commit();
            
            // clear indexed before start
            String q = "indextime:[* TO " + start.toString() + "]";
            solr.deleteByQuery("entities", q);
            solr.deleteByQuery("oai", q);
            for (String key : idocs.keySet()) {
                solr.deleteByQuery(key, q);
            }
            solr.commit();

            Instant end = Instant.now();
            String interval = FormatUtils.formatInterval(end.toEpochMilli() - start.toEpochMilli());
            ret.put("ellapsed time", interval);
            ret.put("request time", FormatUtils.formatInterval(requestTime));
            ret.put("process time", FormatUtils.formatInterval(processTime));
            ret.put("errors", errors);
            LOGGER.log(Level.INFO, "Harvest finished in {0}", interval);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            ret.put("error", ex);
        }
        writeRetToFile("index", start);
        return ret;
    }

    /**
     * Harvest Fedora for updates and index
     *
     * @param from Date to update from
     * @param until Date to update until
     * @return
     * @throws IOException
     */
    public JSONObject update(String from, String until) throws IOException {
        SolrClient solr = SolrClientFactory.getSolrClient();
        ret = new JSONObject();
        return update(from, until, solr);
    }

    private JSONObject update(String from, String until, SolrClient solr) throws IOException {
        try {
            String status = readStatusFile("update");
            if (STATUS_RUNNING.equals(status)) {
                LOGGER.log(Level.INFO, "Update is still running. Start is {0}", from);
                ret.put("msg", "Update is still running");
                return ret;
            }
            writeStatusFile("update", STATUS_RUNNING);
            Instant start = Instant.now();
            String search_fedora_id_prefix = Options.getInstance().getJSONObject("fedora").getString("search_fedora_id_prefix");
            String lastDate = from;
            if (lastDate == null) {
                lastDate = readUpdateFile(); // 2023-08-01T00:00:00.000Z
            }
            if (lastDate == null) {
                lastDate = SolrSearcher.getLastDatestamp().toInstant().toString(); // 2023-08-01T00:00:00.000Z
            }
            // lastDate = "2024-07-30T15:37:05.633Z";
            ret.put("lastDate", lastDate);

            int total = 0;

            // http://192.168.8.33:8080/rest/fcr:search?condition=fedora_id%3DAMCR-test%2Frecord%2F*&condition=modified%3E%3D2023-08-01T00%3A00%3A00.000Z&offset=0&max_results=10
            String baseQuery = "condition=" + URLEncoder.encode("fedora_id=" + search_fedora_id_prefix + "record/*/metadata", "UTF8")
                    + "&condition=" + URLEncoder.encode("modified>" + lastDate, "UTF8");
            if (until != null) {
                baseQuery += "&condition=" + URLEncoder.encode("modified<" + until, "UTF8");
            }
            JSONObject searchJSON = searchFedora(baseQuery, false, "update", true, solr);
            total += searchJSON.optInt("total", 0);

            
            
            // http://192.168.8.33:8080/rest/fcr:search?condition=fedora_id%3DAMCR-test%2Fmodel%2Fdeleted%2F*&condition=modified%3E%3D2023-08-01T00%3A00%3A00.000Z&offset=0&max_results=100
            baseQuery = "condition=" + URLEncoder.encode("fedora_id=" + search_fedora_id_prefix + "model/deleted/*", "UTF8")
                    + "&condition=" + URLEncoder.encode("modified>" + lastDate, "UTF8");
            if (until != null) {
                baseQuery += "&condition=" + URLEncoder.encode("modified<" + until, "UTF8");
            }
            searchJSON = searchFedora(baseQuery, true, "update", false, solr);
            total += searchJSON.optInt("total", 0);
            Instant end = Instant.now();
            String interval = FormatUtils.formatInterval(end.toEpochMilli() - start.toEpochMilli());
            ret.put("ellapsed time", interval);
            ret.put("request time", FormatUtils.formatInterval(requestTime));
            ret.put("process time", FormatUtils.formatInterval(processTime));
            ret.put("errors", errors);

            writeRetToFile("update", start);
            writeStatusFile("update", STATUS_FINISHED);
            if (until != null) {
                writeUpdateFile(start);
            }
            if (total > 0) {
                LOGGER.log(Level.INFO, "Running update for changes after start");
                update(start.toString(), until, solr);
            }
            LOGGER.log(Level.INFO, "Update finished in {0}", interval);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            writeStatusFile("update", STATUS_FINISHED);
        }
        return ret;
    }

    private JSONObject searchFedora(String baseQuery, boolean isDeleted, String indexType, boolean withRelated, SolrClient solr) throws IOException {
        try {
            int indexed = 0;
            int batchSize = 1000;
            int max_results = 10000000;
            int pOffset = 0;
            String s = FedoraUtils.search(baseQuery + "&include_total_result_count=true&offset=" + pOffset + "&max_results=" + max_results);
            JSONObject json = new JSONObject(s);
            JSONArray records = json.getJSONArray("items");
            int totalResults = json.getJSONObject("pagination").getInt("totalResults");
            //while (records.length() > 0) {
                processSearchItems(records, indexType, totalResults, isDeleted, withRelated, solr);
                indexed += records.length();
//                pOffset += batchSize;
//                s = FedoraUtils.search(baseQuery + "&include_total_result_count=true&offset=" + pOffset + "&max_results=" + max_results);
//                json = new JSONObject(s);
//                records = json.getJSONArray("items");
                checkLists(0, indexed, indexType, totalResults, solr);
//                String status = readStatusFile(indexType);
//                if (STATUS_STOPPED.equals(status)) {
//                    LOGGER.log(Level.INFO, "Index stopped at {0}", formatter.format(Instant.now()));
//                    ret.put("msg", "Index stopped at " + formatter.format(Instant.now()));
//                    return ret;
//                }
            //}

            if (isDeleted) {
                ret.put("deleted", indexed);
            } else {
                ret.put(indexType, indexed);
            }

            checkLists(0, indexed, "update", totalResults, solr);
            // ret.put("items", json);
            ret.put("total", indexed);
            solr.commit("oai");
            solr.commit("entities");

            for (String key : idocs.keySet()) {
                solr.commit(key);
            }

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            ret.put("error", ex);
        }
        return ret;
    }

    private void processSearchItems(JSONArray records, String model, int totalInModel, boolean isDeleted, boolean withRelated, SolrClient solr) throws Exception {
        
        int batchSize = 1000;
        for (int i = 0; i < records.length(); i++) {
            String id = records.getJSONObject(i).getString("fedora_id");
            if (isDeleted) {
                id = id.substring(id.lastIndexOf("member/") + 7);
                processDeleted(id, records.getJSONObject(i).getString("modified"));
            } else {
                if (id.contains("record")) {
                    id = id.substring(id.lastIndexOf("record/") + 7);
                } else {
                    id = id.substring(id.lastIndexOf("member/") + 7);
                }

                if (id.contains("/metadata")) {
                    id = id.substring(0, id.indexOf("/metadata"));
                }
                id = id.split("/")[0];
                // LOGGER.log(Level.INFO, "Updating item  {0} ", id);
                try {
                    processRecord(id, withRelated, solr);
                    checkLists(batchSize, i+1, model, totalInModel, solr);
                } catch (Exception ex) {
                    writeToErrorsFile(id);
                    LOGGER.log(Level.SEVERE, "Error processing " + id, ex);
                }
            }
        }
    }

    public void setOffset(int offset) throws IOException {
        this.offset = offset;
    }

    /**
     * Index all deleted records to "deleted" core
     *
     * @return
     * @throws IOException
     */
    public JSONObject indexDeleted() throws IOException {
        Instant start = Instant.now();
        try {
            SolrClient solr = SolrClientFactory.getSolrClient();
            processModel("deleted", solr);

            solr.commit("oai");
            solr.commit("entities");
            for (String key : idocs.keySet()) {
                solr.commit(key);
            }
            solr.commit();
            Instant end = Instant.now();
            String interval = FormatUtils.formatInterval(end.toEpochMilli() - start.toEpochMilli());
            ret.put("ellapsed time", interval);
            ret.put("request time", FormatUtils.formatInterval(requestTime));
            ret.put("process time", FormatUtils.formatInterval(processTime));
            ret.put("errors", errors);
            LOGGER.log(Level.INFO, "Index models finished in {0}", interval);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            errors.put(ex);
        }
        writeRetToFile("models", start);
        return ret;
    }

    /**
     * Index all records in one model from Fedora
     *
     * @param models List of models to process
     * @return
     * @throws IOException
     */
    public JSONObject indexModels(String[] models) throws Exception {
        try {
            SolrClient solr = SolrClientFactory.getSolrClient();
            indexModels(models, solr);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            writeStatusFile("index", STATUS_FINISHED);
            errors.put(ex);
        }
        return ret;
    }

    private JSONObject indexModels(String[] models, SolrClient solr) throws Exception {
        writeStatusFile("index", STATUS_RUNNING);
        Instant start = Instant.now();
        for (String model : models) {
            if (Options.getInstance().getJSONObject("fedora").optBoolean("useSearch", false)) {
                searchModel(model, solr);
            } else {
                processModel(model, solr);
            }
        }
        solr.commit("oai");
        solr.commit("entities");
        for (String key : idocs.keySet()) {
            solr.commit(key);
        }
        Instant end = Instant.now();
        String interval = FormatUtils.formatInterval(end.toEpochMilli() - start.toEpochMilli());
        ret.put("ellapsed time", interval);
        ret.put("request time", FormatUtils.formatInterval(requestTime));
        ret.put("process time", FormatUtils.formatInterval(processTime));
        ret.put("errors", errors);
        LOGGER.log(Level.INFO, "Index models finished in {0}", interval);

        writeRetToFile("models", start);
        return ret;
    }

    public JSONObject reindexByFilter(String fq) throws Exception {
        JSONObject ret = new JSONObject();
        LOGGER.log(Level.INFO, "Reindex by filter {0} started", fq);
        int batchSize = 500;
        int indexed = 0;
        String url = Options.getInstance().getString("solrhost", "http://localhost:8983/solr/")
                + "entities/export?q=*:*&wt=json&sort=ident_cely%20asc&fl=ident_cely&fq=" + URLEncoder.encode(fq, "UTF8");
//        InputStream inputStream = RESTHelper.inputStream(url);
//        String solrResp = org.apache.commons.io.IOUtils.toString(inputStream, "UTF-8");
        String solrResp = IndexUtils.requestSolr(url);
        JSONArray docs = new JSONObject(solrResp).getJSONObject("response").getJSONArray("docs");

        SolrClient solr = SolrClientFactory.getSolrClient();
        for (int i = 0; i < docs.length(); i++) {
            String id = docs.getJSONObject(i).getString("ident_cely");
            try {
                ret.put(id, indexId(id, false, solr));
                checkLists(batchSize, indexed++, fq, docs.length(), solr);
                LOGGER.log(Level.FINE, "Index by ID {0} finished. {1} of {2}", new Object[]{id, i + 1, docs.length()});
            } catch (Exception e) {
                // LOGGER.log(Level.WARNING, "Error indexing {0}, -> {1}", new Object[]{id, e.toString()});
                ret.put(id, e.toString());
            }

            checkLists(0, docs.length(), fq, docs.length(), solr);
        }
        return ret;
    }

    /**
     * Index one record from Fedora
     *
     * @param id
     * @return
     * @throws IOException
     */
    public JSONObject indexId(String id) throws Exception {
        SolrClient solr = SolrClientFactory.getSolrClient();
        JSONObject j = indexId(id, true, solr);
        return j;
    }

    private JSONObject indexId(String id, boolean commit, SolrClient solr) throws Exception {
        Instant start = Instant.now();
        processRecord(id, false, solr);
        if (commit) {
            checkLists(0, 1, id, 1, solr);
        }
        Instant end = Instant.now();
        String interval = FormatUtils.formatInterval(end.toEpochMilli() - start.toEpochMilli());
        ret.put("ellapsed time", interval);
        LOGGER.log(Level.FINE, "Index by ID {0} finished in {1}", new Object[]{id, interval});

        return ret;
    }

    public FedoraModel getIdParsed(String id) throws IOException {
        try {

            LOGGER.log(Level.INFO, "Processing record {0}", id);
            String xml = FedoraUtils.requestXml("record/" + id + "/metadata");
            String model = FedoraModel.getModel(xml);
            Class clazz = FedoraModel.getModelClass(model);
            if (clazz != null) {
                return FedoraModel.parseXml(xml, clazz);
            }
            return null;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public String getId(String id) throws IOException {
        try {

            LOGGER.log(Level.INFO, "Processing record {0}", id);
            String xml = FedoraUtils.requestXml("record/" + id + "/metadata");
            String model = FedoraModel.getModel(xml);
            Class clazz = FedoraModel.getModelClass(model);
            if (clazz != null) {
                FedoraModel fm = FedoraModel.parseXml(xml, clazz);

            }
            return xml;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return ex.toString();
        }
    }

    public JSONObject getIdMetadata(String id) throws IOException {
        try {
            LOGGER.log(Level.INFO, "Processing record {0}", id);
            return FedoraUtils.getJsonMetadataById(id);

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return new JSONObject().put("error", ex);
        }
    }

    public JSONObject getIdJSON(String id) throws IOException {
        try {
            LOGGER.log(Level.INFO, "Processing record {0}", id);
            return FedoraUtils.getJsonById(id);

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return new JSONObject().put("error", ex);
        }
    }

    private void getModels(SolrClient solr) throws Exception {

        JSONArray models = Options.getInstance().getJSONObject("fedora").getJSONArray("models");
        for (int i = 0; i < models.length(); i++) {
            // processModel(models.getString(i));
            if (Options.getInstance().getJSONObject("fedora").optBoolean("useSearch", true)) {
                searchModel(models.getString(i), solr);
            } else {
                processModel(models.getString(i), solr);
            }
        }
    }

//    private void getModelsFromFedora() throws Exception {
//
//        JSONObject json = new JSONArray(FedoraUtils.request("model")).getJSONObject(0);
//        // returns list of models (entities) in CONTAINS 
//
//        if (json.has(CONTAINS)) {
//            JSONArray models = json.getJSONArray(CONTAINS);
//            for (int i = 0; i < models.length(); i++) {
//                String id = models.getJSONObject(i).getString("@id");
//                id = id.substring(id.lastIndexOf("/") + 1);
//                // processModel(id);
//                searchModel(id, solr);
//            }
//        }
//    }
    /**
     * Harvest Fedora for model and index
     *
     * @param model model to index
     * @return
     * @throws IOException
     */
    public JSONObject searchModel(String model, SolrClient solr) throws IOException {
        try {
            LOGGER.log(Level.INFO, "Searching model {0}", model);
            ret = new JSONObject();
//        String status = readStatusFile("update");
//        if (STATUS_RUNNING.equals(status)) {
//            LOGGER.log(Level.INFO, "Update is still running");
//            ret.put("msg", "Update is still running");
//            return ret;
//        }
            writeStatusFile("models", STATUS_RUNNING);
            Instant start = Instant.now();
            String search_fedora_id_prefix = Options.getInstance().getJSONObject("fedora").getString("search_fedora_id_prefix");

// rest/fcr:search?condition=fedora_id%3DAMCR%2Fmodel%2Fdokument%2Fmember%2F*&offset=0&max_results=100
            String baseQuery = "condition=";
            if ("deleted".equals(model)) {
                baseQuery += URLEncoder.encode("fedora_id=" + search_fedora_id_prefix + "model/deleted/*", "UTF8");
            } else {
                baseQuery += URLEncoder.encode("fedora_id=" + search_fedora_id_prefix + "model/" + model + "/member/*", "UTF8");
            }

            searchFedora(baseQuery, "deleted".equals(model), model, false, solr);
            // update(start.toString(), solr);
            Instant end = Instant.now();
            String interval = FormatUtils.formatInterval(end.toEpochMilli() - start.toEpochMilli());
            ret.put("ellapsed time", interval);
            ret.put("request time", FormatUtils.formatInterval(requestTime));
            ret.put("process time", FormatUtils.formatInterval(processTime));
            ret.put("errors", errors);
            LOGGER.log(Level.INFO, "searchModel FINISHED in {0}", interval);

            writeStatusFile("models", STATUS_FINISHED);
            writeRetToFile("models", start);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "searchModel FAILED", ex);
            writeStatusFile("update", STATUS_FINISHED);
        }
        return ret;
    }

    private void processModel(String model, SolrClient solr) throws Exception {

        String status = readStatusFile("index");
        if (STATUS_STOPPED.equals(status)) {
            LOGGER.log(Level.INFO, "Index stopped at {0}", formatter.format(Instant.now()));
            ret.put("msg", "Index stopped at " + formatter.format(Instant.now()));
            return;
        }
        LOGGER.log(Level.INFO, "Processing model {0}", model);
        ret.put(model, 0);
        int indexed = offset;
        int batchSize = 500;
        //http://192.168.8.33:8080/rest/AMCR-test/model/projekt/member
        // returns list of records in CONTAINS
//    [{
//      "@id": "http://192.168.8.33:8080/rest/AMCR-test/model/projekt/member/C-201449117"
//    }],

        long start = Instant.now().toEpochMilli();
        JSONObject json = new JSONArray(FedoraUtils.request("model/" + model + "/member")).getJSONObject(0);
        requestTime += Instant.now().toEpochMilli() - start;

        if (json.has(CONTAINS)) {
            JSONArray records = json.getJSONArray(CONTAINS);
            int totalInModel = records.length();

            for (int i = offset; i < records.length(); i++) {
                String id = records.getJSONObject(i).getString("@id");
                id = id.substring(id.lastIndexOf("/") + 1);
                if ("deleted".equals(model)) {
                    processDeleted(id, null);
                } else {
                    processRecord(id, model);
                }

                ret.put(model, indexed++);
                checkLists(batchSize, indexed, model, totalInModel, solr);

                if (STATUS_STOPPED.equals(readStatusFile("index"))) {
                    checkLists(0, indexed, model, totalInModel, solr);
                    LOGGER.log(Level.INFO, "Index stopped at {0}", formatter.format(Instant.now()));
                    ret.put("msg", "Index stopped at " + formatter.format(Instant.now()));
                    return;
                }
            }
            checkLists(0, indexed, model, totalInModel, solr);
            solr.commit("oai");
            solr.commit("entities");
            LOGGER.log(Level.INFO, "Index model {0} finished", model);
        }
    }

    private void processDeleted(String id, String datestamp) throws Exception {
        SolrInputDocument idoc = new SolrInputDocument();
        idoc.setField("ident_cely", id);
        if (datestamp == null) {
            try {
//                JSONObject json = FedoraUtils.getJsonById(id);
//                String d = json.getJSONArray("http://fedora.info/definitions/v4/repository#lastModified")
//                        .getJSONObject(0).getString("@value");
//                idoc.setField("datestamp", d);
                IndexUtils.setDateStampFromDeleted(idoc, id);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Deleted id {0} error", id);
                idoc.setField("datestamp", ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT));
            }
        } else {
            idoc.setField("datestamp", datestamp);
        }
        idocsDeleted.add(idoc);

        SolrInputDocument sdoc = new SolrInputDocument();
        sdoc.addField("ident_cely", id);
        sdoc.addField("datestamp", idoc.getFieldValue("datestamp"));
        Map<String, Object> fieldModifier = new HashMap<>(1);
        fieldModifier.put("set", true);
        sdoc.addField("is_deleted", fieldModifier);  // add the map as the field value
        idocsEntities.add(sdoc);
        idocsOAI.add(sdoc);

    }

    private void processRecord(String id, boolean processRelated, SolrClient solr) throws Exception {
        try {
            // http://192.168.8.33:8080/rest/AMCR-test/record/C-201449117/metadata
            // returns xml
            LOGGER.log(Level.FINE, "Processing record {0}", id);
            long start = Instant.now().toEpochMilli();
            String xml = FedoraUtils.requestXml("record/" + id + "/metadata");
            requestTime += Instant.now().toEpochMilli() - start;
            String model = FedoraModel.getModel(xml);
            if (model == null) {
                LOGGER.log(Level.SEVERE, "Error parsing xml {0} for record {1}", new Object[]{xml, id});
                writeToErrorsFile(id);
                errors.put(id + ": Error parsing xml");
                return;
            }
            start = Instant.now().toEpochMilli();
            indexXml(xml, model);
            if (processRelated) {
                checkLists(0, 1, id, 1, solr);
                processRelated(id, model, solr);
            }
            processTime += Instant.now().toEpochMilli() - start;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error processing record " + id, ex);
            writeToErrorsFile(id);
            errors.put(id + ":  " + ex);
            throw ex;
        }
    }

    private void processRelated(String id, String model, SolrClient solr) {
        //https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/350#issuecomment-2340668709
        SolrQuery query = new SolrQuery("*");
        List<String> fields = new ArrayList<>();
        boolean hasRelated = true;
        switch (model) {
            case "dokument":
                query.addFilterQuery("ident_cely:\"" + id + "\"")
                        .setFields("dokument_cast_projekt");
                fields.add("dokument_cast_projekt");
                break;
            case "projekt":
                query.addFilterQuery("ident_cely:\"" + id + "\"")
                        .setFields("projekt_dokument");
                fields.add("projekt_dokument");
                break;
            case "archeologicky_zaznam":
                fields.add("az_dokument");
                fields.add("akce_projekt");
                query.addFilterQuery("ident_cely:\"" + id + "\"")
                        .setFields((String[]) fields.toArray(String[]::new));
                break;
            case "pian":
                fields.add("ident_cely");
                fields.add("az_dokument");
                query.addFilterQuery("az_dj_pian:\"" + id + "\"")
                        .setFields((String[]) fields.toArray(String[]::new));
                break;
            case "samostatny_nalez":
                fields.add("samostatny_nalez_projekt");
                query.addFilterQuery("ident_cely:\"" + id + "\"")
                        .setFields((String[]) fields.toArray(String[]::new));
                break;
            default:
                hasRelated = false;
        }
        if (hasRelated) {
            LOGGER.log(Level.INFO, "Updating related for {0} of model {1}", new Object[]{id, model});
            try {
                SolrDocumentList docs = solr.query("entities", query).getResults();
                for (SolrDocument doc : docs) {
                    for (String f : fields) {
                        Collection<Object> ids = doc.getFieldValues(f);
                        if (ids != null) {
                            for (Object o : ids) {
                                LOGGER.log(Level.INFO, "Process related  {0} ", new Object[]{o});
                                processRecord((String) o, false, solr);
                            }
                        }
                    }

                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    private void processRecord(String id, String model) throws Exception {
        try {
            // http://192.168.8.33:8080/rest/AMCR-test/record/C-201449117/metadata
            // returns xml
            LOGGER.log(Level.FINE, "Processing record {0}", id);
            long start = Instant.now().toEpochMilli();
            String xml = FedoraUtils.requestXml("record/" + id + "/metadata");
            requestTime += Instant.now().toEpochMilli() - start;

            start = Instant.now().toEpochMilli();
            indexXml(xml, model);
            processTime += Instant.now().toEpochMilli() - start;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error processing record {0}", id);
            LOGGER.log(Level.SEVERE, null, ex);
            writeToErrorsFile(id);
            errors.put(model + " " + id + ":  " + ex.toString());
            // throw new Exception(ex);
        }
    }

    private void indexXml(String xml, String model) throws Exception {

        Class clazz = FedoraModel.getModelClass(model);
        if (clazz != null) {
            try {
                FedoraModel fm = FedoraModel.parseXml(xml, clazz);

                DocumentObjectBinder dob = new DocumentObjectBinder();
                SolrInputDocument idoc = dob.toSolrInputDocument(fm);

                /**
                 * 
                 * Odstraneno na zaklade https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/642
                if (idoc.containsKey("stav") && Integer.parseInt(idoc.getFieldValue("stav").toString()) == -1) {
                    LOGGER.log(Level.FINE, "Skiping record {0}. Stav = -1", idoc.getFieldValue("ident_cely"));
                    return;
                }
                 */
//                if (!fm.isSearchable()) {
//                    LOGGER.log(Level.FINE, "Skiping record {0}. Not searchable", idoc.getFieldValue("ident_cely"));
//                    return;
//                }
                fm.fillSolrFields(idoc);
                String entity = (String) idoc.getFieldValue("entity");
                if (xml.contains("<amcr:geom_gml>")) {
                    // tady bychom meli nechat vsechny 
                }
                if (FedoraModel.isOAI(entity)) {
                    SolrInputDocument oaidoc = createOAIDocument(xml, idoc);
                    idocsOAI.add(oaidoc);
                }
                String core = fm.coreName();
                switch (core) {
                    case "entities":
                        idocsEntities.add(idoc);
                        break;
                    default:
                        if (!idocs.containsKey(core)) {
                            idocs.put(core, new ArrayList());
                        }
                        idocs.get(core).add(idoc);
                }
            } catch (Exception ex) {
                // LOGGER.log(Level.SEVERE, null, ex);
                throw ex;
            }
        }
    }

    private SolrInputDocument createOAIDocument(String xml, SolrInputDocument edoc) {
        SolrInputDocument idoc = new SolrInputDocument();
        idoc.setField("ident_cely", edoc.getFieldValue("ident_cely"));
        String model = (String) edoc.getFieldValue("entity");
        if (model.equals("akce") || model.equals("lokalita")) {
            model = "archeologicky_zaznam:" + model;
        } else if (model.equals("knihovna_3d")) {
            model = "dokument:3d";
        } else if (model.equals("heslo")) {
            model = "heslo:" + edoc.getFieldValue("nazev_heslare");
        }
        idoc.setField("model", model);
        if (edoc.containsKey("projekt_organizace")) {
            idoc.setField("organizace", edoc.getFieldValue("projekt_organizace"));
        }
        if (edoc.containsKey("samostatny_nalez_predano_organizace")) {
            idoc.setField("organizace", edoc.getFieldValue("samostatny_nalez_predano_organizace"));
        }
        // idoc.setField("organizace", edoc.getFieldValue("organizace"));
        idoc.setField("stav", edoc.getFieldValue("stav"));
        idoc.setField("pristupnost", edoc.getFieldValue("pristupnost"));
        idoc.setField("datestamp", edoc.getFieldValue("datestamp"));
        idoc.setField("historie_typ_zmeny", edoc.getFieldValue("historie_typ_zmeny"));
        idoc.setField("historie_uzivatel", edoc.getFieldValue("historie_uzivatel"));
        String xmlData = xml.substring(xml.indexOf("<amcr"));
        idoc.setField("xml", xmlData);
        return idoc;
    }

    private void checkLists(int size, int indexed, String model, int totalInModel, SolrClient solr) throws SolrServerException, IOException {
        try {
            if (idocsEntities.size() > size) {
                // LOGGER.log(Level.INFO, "Entities {0}", idocsEntities.size());
                solr.add("entities", idocsEntities);
                solr.commit("entities");
                idocsEntities.clear();
                LOGGER.log(Level.INFO, "Indexed {0} of {1} -> {2}", new Object[]{indexed, totalInModel, model});
            }
            if (idocsDeleted.size() > size) {
                LOGGER.log(Level.INFO, "deleted {0}", idocsDeleted.size());
                solr.add("deleted", idocsDeleted);
                solr.commit("deleted");
                idocsDeleted.clear();
            }

            for (String key : idocs.keySet()) {
                List l = idocs.get(key);
                if (l.size() > size) {
                    // LOGGER.log(Level.INFO, "Sending {0}...", key);
                    solr.add(key, l);
                    solr.commit(key);
                    l.clear();
                    LOGGER.log(Level.INFO, "Indexed {0} - {1} of {2}", new Object[]{indexed, key, totalInModel});
                }
            }
            if (idocsOAI.size() > size) {
                // LOGGER.log(Level.INFO, "OAI {0}", idocsOAI.size());
                solr.add("oai", idocsOAI);
                solr.commit("oai");
                idocsOAI.clear();
            }
        } catch (Exception ex) {
            //LOGGER.log(Level.SEVERE, "Can't commit changes", ex);
            LOGGER.log(Level.SEVERE, "Can't commit changes");
            SolrClientFactory.resetSolrClient();
            throw ex;// new Exception(ex)
        }
    }

    /**
     * Index all records in one model from Fedora
     *
     * @param models List of models to process
     * @return
     * @throws IOException
     */
    public JSONObject checkDatestamp(String[] entities, boolean reindex) throws Exception {
        JSONObject ret = new JSONObject();
        try {
            SolrClient solr = SolrClientFactory.getSolrClient();
            for (String entity : entities) {
                int rows = 500;
                SolrQuery query = new SolrQuery("*")
                        .addFilterQuery("entity:" + entity)
                        .setFields("ident_cely,datestamp")
                        .setRows(rows)
                        .setSort("ident_cely", SolrQuery.ORDER.asc);
                boolean done = false;
                QueryResponse rsp = null;
                SolrDocumentList docs;
                int totalDocs = 0;
                int totalBad = 0;
                String cursorMark = CursorMarkParams.CURSOR_MARK_START;
                while (!done) {
                    query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
                    try {
                        rsp = solr.query("entities", query);
                        docs = rsp.getResults();
                        for (SolrDocument doc : docs) {
                            String ident_cely = (String) doc.getFirstValue("ident_cely");
                            Date solrdate = (Date) doc.getFirstValue("datestamp");

                            String fedoraDate = FedoraUtils.getDateStamp(ident_cely);

                            if (fedoraDate == null) {
                                ret.append(entity, new JSONObject()
                                        .put("ident_cely", ident_cely)
                                        .put("solr_date", solrdate)
                                        .put("fedoraDate", "unknown"));
                                totalBad++;
                            } else {
                                if (Instant.parse(fedoraDate).truncatedTo(ChronoUnit.SECONDS).isAfter(solrdate.toInstant())) {

                                    ret.append(entity, new JSONObject()
                                            .put("ident_cely", ident_cely)
                                            .put("solr_date", solrdate.toInstant().toString())
                                            .put("fedoraDate", fedoraDate));
                                    if (reindex) {
                                        indexId(entity, true, solr);
                                    }
                                    totalBad++;
                                }
                            }

                            totalDocs++;
                        }
                        LOGGER.log(Level.INFO, "Currently {0} files processed", totalDocs);

                        String nextCursorMark = rsp.getNextCursorMark();

                        if (cursorMark.equals(nextCursorMark) || rsp.getResults().size() < rows) {
                            done = true;
                        } else {
                            cursorMark = nextCursorMark;
                            // done = true;
                        }
                        ret.put("total", totalDocs);
                        ret.put("totalBad", totalBad);
                        LOGGER.log(Level.INFO, "Finished. total: {0}. Bad: {1}", new Object[]{totalDocs, totalBad});
                    } catch (SolrServerException e) {
                        LOGGER.log(Level.SEVERE, null, e);
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            writeStatusFile("index", STATUS_FINISHED);
            errors.put(ex);
        }
        return ret;
    }
}
