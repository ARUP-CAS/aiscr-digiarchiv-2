package cz.inovatika.arup.digiarchiv.web.fedora;

import cz.inovatika.arup.digiarchiv.web.FormatUtils;
import cz.inovatika.arup.digiarchiv.web.InitServlet;
import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.RESTHelper;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.common.SolrInputDocument;
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
    SolrClient solr;
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

    private void writeRetToFile(String type, Instant date) throws IOException {

        File f = new File(InitServlet.CONFIG_DIR + File.separator + "logs" + File.separator + type + "." + formatter.format(date) + ".json");
        FileUtils.writeStringToFile(f, ret.toString(2), "UTF-8");
    }

    private String readStatusFile(String type) throws IOException {
        File f = new File(InitServlet.CONFIG_DIR + File.separator + type + "_"  + "status.txt");
        if (f.exists() && f.canRead()) {
            return FileUtils.readFileToString(f, "UTF-8");
        } else {
            return "none";
        }
        
    }

    private void writeStatusFile(String type, String status) throws IOException {
        File f = new File(InitServlet.CONFIG_DIR + File.separator + type + "_"  + "status.txt");
        FileUtils.writeStringToFile(f, status, "UTF-8");
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
            ret = new JSONObject();
            solr = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build();
            getModels();
            solr.commit("oai");
            solr.commit("entities");
            for (String key : idocs.keySet()) {
                solr.commit(key);
            }
            solr.close();
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
            if (solr != null) {
                solr.close();
            }
        } finally {

            if (solr != null) {
                solr.close();
            }
        }
        writeRetToFile("index", start);
        return ret;
    }

    /**
     * Harvest Fedora for updates and index
     *
     * @return
     * @throws IOException
     */
    public JSONObject update() throws Exception {
        ret = new JSONObject();
        String status = readStatusFile("update");
        if (STATUS_RUNNING.equals(status)) {
            LOGGER.log(Level.INFO, "Update is still running");
            ret.put("msg", "Update is still running");
            return ret;
        }
        writeStatusFile("update", STATUS_RUNNING);
        Instant start = Instant.now();
        String search_fedora_id_prefix = Options.getInstance().getJSONObject("fedora").getString("search_fedora_id_prefix");
        String lastDate = SolrSearcher.getLastDatestamp().toInstant().toString(); // 2023-08-01T00:00:00.000Z
        ret.put("lastDate", lastDate);

        // http://192.168.8.33:8080/rest/fcr:search?condition=fedora_id%3DAMCR-test%2Frecord%2F*&condition=modified%3E%3D2023-08-01T00%3A00%3A00.000Z&offset=0&max_results=10
        String baseQuery = "condition=" + URLEncoder.encode("fedora_id=" + search_fedora_id_prefix + "record/*", "UTF8")
                + "&condition=" + URLEncoder.encode("modified>" + lastDate, "UTF8");
        update(baseQuery, false);

        // http://192.168.8.33:8080/rest/fcr:search?condition=fedora_id%3DAMCR-test%2Fmodel%2Fdeleted%2F*&condition=modified%3E%3D2023-08-01T00%3A00%3A00.000Z&offset=0&max_results=100
        baseQuery = "condition=" + URLEncoder.encode("fedora_id=" + search_fedora_id_prefix + "model/deleted/", "UTF8")
                + "&condition=" + URLEncoder.encode("modified>" + lastDate, "UTF8");
        update(baseQuery, true);

        Instant end = Instant.now();
        String interval = FormatUtils.formatInterval(end.toEpochMilli() - start.toEpochMilli());
        ret.put("ellapsed time", interval);
        ret.put("request time", FormatUtils.formatInterval(requestTime));
        ret.put("process time", FormatUtils.formatInterval(processTime));
        ret.put("errors", errors);
        LOGGER.log(Level.INFO, "Update finished in {0}", interval);

        writeRetToFile("update", start);
        writeStatusFile("update", STATUS_FINISHED);
        return ret;
    }

    private JSONObject update(String baseQuery, boolean isDeleted) throws IOException {
        try {
            solr = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build();
            int indexed = 0;

            int batchSize = 100;
            int pOffset = 0;

            String s = FedoraUtils.search(baseQuery + "&offset=" + pOffset + "&max_results=" + batchSize);
            JSONObject json = new JSONObject(s);
            // getModels(); 

            JSONArray records = json.getJSONArray("items");
            while (records.length() > 0) {
                processUpdateItems(records, isDeleted);
                indexed += records.length();
                pOffset += batchSize;
                s = FedoraUtils.search(baseQuery + "&offset=" + pOffset + "&max_results=" + batchSize);
                json = new JSONObject(s);
                records = json.getJSONArray("items");
                checkLists(0, indexed, "update", records.length());
                String status = readStatusFile("update");
                if (STATUS_STOPPED.equals(status)) {
                    LOGGER.log(Level.INFO, "Update stopped at {0}", formatter.format(Instant.now()));
                    ret.put("msg", "Update stopped at " + formatter.format(Instant.now()));
                    solr.close();
                    return ret;
                }
            }

            ret.put("updated", indexed);

            checkLists(0, indexed, "update", indexed);
            ret.put("items", json);
            solr.commit("oai");
            solr.commit("entities");

            for (String key : idocs.keySet()) {
                solr.commit(key);
            }

            solr.close();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            ret.put("error", ex);
            if (solr != null) {
                solr.close();
            }
        } finally {

            if (solr != null) {
                solr.close();
            }
        }
        return ret;
    }

    private void processUpdateItems(JSONArray records, boolean isDeleted) throws Exception {
        for (int i = 0; i < records.length(); i++) {
            String id = records.getJSONObject(i).getString("fedora_id");
            if (isDeleted) {
                id = id.substring(id.lastIndexOf("member/") + 7);
                processDeleted(id, records.getJSONObject(i).getString("modified"));
            } else {
                id = id.substring(id.lastIndexOf("record/") + 7);
                if (id.contains("/metadata")) {
                    id = id.substring(0, id.indexOf("/metadata"));
                }
                id = id.split("/")[0];
                processRecord(id);
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
            solr = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build();

            processModel("deleted");

            solr.commit("oai");
            solr.commit("entities");
            for (String key : idocs.keySet()) {
                solr.commit(key);
            }
            solr.close();
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
            if (solr != null) {
                solr.close();
            }
        } finally {

            if (solr != null) {
                solr.close();
            }
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
    public JSONObject indexModels(String[] models) throws IOException {
        Instant start = Instant.now();
        try {
            solr = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build();
            for (String model : models) {
                processModel(model);
            }
            solr.commit("oai");
            solr.commit("entities");
            for (String key : idocs.keySet()) {
                solr.commit(key);
            }
            solr.close();
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
            if (solr != null) {
                solr.close();
            }
        } finally {

            if (solr != null) {
                solr.close();
            }
        }
        writeRetToFile("models", start);
        return ret;
    }

    public JSONObject reindexByFilter(String fq) throws Exception {
        JSONObject ret = new JSONObject();
        String url = Options.getInstance().getString("solrhost", "http://localhost:8983/solr/")
                + "entities/export?q=*:*&wt=json&sort=ident_cely%20asc&fl=ident_cely&fq=" + fq;
        InputStream inputStream = RESTHelper.inputStream(url);
        String solrResp = org.apache.commons.io.IOUtils.toString(inputStream, "UTF-8");
        JSONArray docs = new JSONObject(solrResp).getJSONObject("response").getJSONArray("docs");
        solr = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build();
        for (int i = 0; i < docs.length(); i++) {
            String id = docs.getJSONObject(i).getString("ident_cely");
            ret.put(id, indexId(id, false));
            LOGGER.log(Level.INFO, "Index by ID {0} finished. {1} of {2}", new Object[]{id, i + 1, docs.length()});
        }
        checkLists(0, docs.length(), fq, docs.length());
        solr.close();
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
        solr = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build();
        JSONObject j = indexId(id, true);
        solr.close();
        return j;
    }

    public JSONObject indexId(String id, boolean commit) throws Exception {
        Instant start = Instant.now();
        try {
            processRecord(id);
            if (commit) {
                checkLists(0, 1, id, 1);
            }
            Instant end = Instant.now();
            String interval = FormatUtils.formatInterval(end.toEpochMilli() - start.toEpochMilli());
            ret.put("ellapsed time", interval);
            LOGGER.log(Level.FINE, "Index by ID {0} finished in {1}", new Object[]{id, interval});
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            errors.put(ex);
            if (solr != null) {
                solr.close();
            }
            throw ex;
        } finally {

            if (solr != null) {
                solr.close();
            }
        }
        return ret;
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

    private void getModels() throws Exception {

        JSONArray models = Options.getInstance().getJSONObject("fedora").getJSONArray("models");
        for (int i = 0; i < models.length(); i++) {
            processModel(models.getString(i));
        }
    }

    private void getModelsFromFedora() throws Exception {

        JSONObject json = new JSONArray(FedoraUtils.request("model")).getJSONObject(0);
        // returns list of models (entities) in CONTAINS 

        if (json.has(CONTAINS)) {
            JSONArray models = json.getJSONArray(CONTAINS);
            for (int i = 0; i < models.length(); i++) {
                String id = models.getJSONObject(i).getString("@id");
                id = id.substring(id.lastIndexOf("/") + 1);
                processModel(id);
            }
        }
    }

    private void processModel(String model) throws Exception {
        
        String status = readStatusFile("index");
        if (STATUS_STOPPED.equals(status)) {
            LOGGER.log(Level.INFO, "Index stopped at {0}", formatter.format(Instant.now()));
            ret.put("msg", "Index stopped at " + formatter.format(Instant.now()));
            return;
        }
        LOGGER.log(Level.INFO, "Processing model {0}", model);
        ret.put(model, 0);
        int indexed = 0;
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
                checkLists(batchSize, indexed, model, totalInModel);
                
                if (STATUS_STOPPED.equals(readStatusFile("index"))) {
                    checkLists(0, indexed, model, totalInModel);
                    LOGGER.log(Level.INFO, "Index stopped at {0}", formatter.format(Instant.now()));
                    ret.put("msg", "Index stopped at " + formatter.format(Instant.now()));
                    return;
                }
            }

            checkLists(0, indexed, model, totalInModel);
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

    private void processRecord(String id) throws Exception {
        try {
            // http://192.168.8.33:8080/rest/AMCR-test/record/C-201449117/metadata
            // returns xml
            LOGGER.log(Level.FINE, "Processing record {0}", id);
            long start = Instant.now().toEpochMilli();
            String xml = FedoraUtils.requestXml("record/" + id + "/metadata");
            requestTime += Instant.now().toEpochMilli() - start;
            String model = FedoraModel.getModel(xml);
            start = Instant.now().toEpochMilli();
            indexXml(xml, model);
            processTime += Instant.now().toEpochMilli() - start;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error processing record {0}", id);
            // LOGGER.log(Level.SEVERE, null, ex);
            errors.put(id + ":  " + ex);
            throw ex;
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
            // LOGGER.log(Level.SEVERE, null, ex);
            errors.put(model + " " + id + ":  " + ex);
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
                if (!fm.isSearchable()) {
                    LOGGER.log(Level.FINE, "Skiping record {0}. Not searchable", idoc.getFieldValue("ident_cely"));
                    return;
                }
                fm.fillSolrFields(idoc);
                String entity = (String) idoc.getFieldValue("entity");
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

    private void checkLists(int size, int indexed, String model, int totalInModel) throws SolrServerException, IOException {
        if (idocsEntities.size() > size) {
            solr.add("entities", idocsEntities);
            solr.commit("entities");
            idocsEntities.clear();
            LOGGER.log(Level.INFO, "Indexed {0} of {1} -> {2}", new Object[]{indexed, totalInModel, model});
        }
        if (idocsOAI.size() > size) {
            solr.add("oai", idocsOAI);
            solr.commit("oai");
            idocsOAI.clear();
        }
        if (idocsDeleted.size() > size) {
            solr.add("deleted", idocsDeleted);
            solr.commit("deleted");
            idocsDeleted.clear();
        }

        for (String key : idocs.keySet()) {
            List l = idocs.get(key);
            if (l.size() > size) {
                solr.add(key, l);
                l.clear();
                LOGGER.log(Level.INFO, "Indexed {0} - {1}", new Object[]{indexed, key});
            }
        }
    }
}
