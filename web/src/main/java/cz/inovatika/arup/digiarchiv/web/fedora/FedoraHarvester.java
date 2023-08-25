/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web.fedora;

import cz.inovatika.arup.digiarchiv.web.FormatUtils;
import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private static final String CONTAINS = "http://www.w3.org/ns/ldp#contains";

    JSONObject ret = new JSONObject();
    JSONArray errors = new JSONArray();
    SolrClient solr;
    int offset = 0;

    List<SolrInputDocument> idocsEntities = new ArrayList();
    List<SolrInputDocument> idocsHeslar = new ArrayList();
    List<SolrInputDocument> idocsOrganizations = new ArrayList();
    List<SolrInputDocument> idocsUzivatel = new ArrayList();
    List<SolrInputDocument> idocsOAI = new ArrayList();

    long requestTime;
    long processTime;

    /**
     * Full fedora harvest and index
     *
     * @return
     * @throws IOException
     */
    public JSONObject harvest() throws IOException {
        try {
            Instant start = Instant.now();
            solr = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build();
            getModels();
            solr.commit("oai");
            solr.commit("entities");
            solr.commit("heslar");
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
        return ret;
    }

    /**
     * Harvest Fedora for updates and index
     *
     * @return
     * @throws IOException
     */
    public JSONObject update() throws IOException {
        try {
            Instant start = Instant.now();
            solr = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build();
            int indexed = 0;

            //http://192.168.8.33:8080/rest/fcr:search?condition=fedora_id%3DAMCR-test%2Frecord%2F*&condition=modified%3E%3D2023-08-01T00%3A00%3A00.000Z&offset=0&max_results=10
            int batchSize = 100;
            int pOffset = 0;
            String lastDate = SolrSearcher.getLastDatestamp().toInstant().toString(); // 2023-08-01T00:00:00.000Z
            //lastDate = "2023-08-17T00:00:00.000Z";
            ret.put("lastDate", lastDate);

            String s = FedoraUtils.search("condition=" + URLEncoder.encode("fedora_id=AMCR-test/record/*", "UTF8")
                    + "&condition=" + URLEncoder.encode("modified>=" + lastDate, "UTF8") + "&offset=" + pOffset + "&max_results=" + batchSize);
            JSONObject json = new JSONObject(s);
            // getModels();

            JSONArray records = json.getJSONArray("items");
            while( records.length() > 0 ) {
                processUpdateItems(records);
                indexed += records.length();
                pOffset += batchSize;
                s = FedoraUtils.search("condition=" + URLEncoder.encode("fedora_id=AMCR-test/record/*", "UTF8")
                    + "&condition=" + URLEncoder.encode("modified>=" + lastDate, "UTF8") + "&offset=" + pOffset + "&max_results=" + batchSize);
                json = new JSONObject(s);
                records = json.getJSONArray("items");
                checkLists(0);
            }

            ret.put("updated", indexed);

            checkLists(0);
            ret.put("items", json);
            solr.commit("oai");
            solr.commit("entities");
            solr.commit("heslar");
            solr.close();
            Instant end = Instant.now();
            String interval = FormatUtils.formatInterval(end.toEpochMilli() - start.toEpochMilli());
            ret.put("ellapsed time", interval);
            ret.put("request time", FormatUtils.formatInterval(requestTime));
            ret.put("process time", FormatUtils.formatInterval(processTime));
            ret.put("errors", errors);
            LOGGER.log(Level.INFO, "Update finished in {0}", interval);
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

    private void processUpdateItems(JSONArray records) throws Exception {
        for (int i = 0; i < records.length(); i++) {
            String id = records.getJSONObject(i).getString("fedora_id");
            id = id.substring(id.lastIndexOf("record/") + 7);
            id = id.substring(0, id.indexOf("/metadata"));
            processRecord(id);
        }
    }

    public void setOffset(int offset) throws IOException {
        this.offset = offset;
    }

    /**
     * Index all records in one model from Fedora
     *
     * @param models List of models to process
     * @return
     * @throws IOException
     */
    public JSONObject indexModels(String[] models) throws IOException {
        try {
            Instant start = Instant.now();
            solr = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build();
            for (String model : models) {
                processModel(model);
            }
            solr.commit("oai");
            solr.commit("entities");
            solr.commit("heslar");
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
        return ret;
    }

    /**
     * Index one record from Fedora
     *
     * @param id
     * @return
     * @throws IOException
     */
    public JSONObject indexId(String id) throws IOException {
        try {
            Instant start = Instant.now();
            solr = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build();
            processRecord(id);
            checkLists(0);
            solr.close();
            Instant end = Instant.now();
            String interval = FormatUtils.formatInterval(end.toEpochMilli() - start.toEpochMilli());
            ret.put("ellapsed time", interval);
            LOGGER.log(Level.INFO, "Index by ID finished in {0}", interval);
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

    private void getModels() throws Exception {

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

            for (int i = offset; i < records.length(); i++) {
                String id = records.getJSONObject(i).getString("@id");
                id = id.substring(id.lastIndexOf("/") + 1);
                processRecord(id, model);
                ret.put(model, indexed++);
                checkLists(batchSize);
                // LOGGER.log(Level.INFO, "Indexed {0}", indexed);
            }

            checkLists(0);
            LOGGER.log(Level.INFO, "Index model {0} finished", model);
        }
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
            LOGGER.log(Level.SEVERE, null, ex);
            errors.put(id + ":  " + ex);
            // throw new Exception(ex);
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
                fm.fillSolrFields(idoc);
                if (FedoraModel.isOAI((String) idoc.getFieldValue("entity"))) {
                    SolrInputDocument oaidoc = createOAIDocument(xml, idoc);
                    idocsOAI.add(oaidoc);
                }
                String core = fm.coreName();
                switch (core) {
                    case "entities":
                        idocsEntities.add(idoc);
                        break;
                    case "heslar":
                        idocsHeslar.add(idoc);
                        break;
                    case "organizations":
                        idocsOrganizations.add(idoc);
                        break;
                    case "uzivatel":
                        idocsUzivatel.add(idoc);
                        break;
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
                throw ex;
            }

        }

    }

    private SolrInputDocument createOAIDocument(String xml, SolrInputDocument edoc) {
        SolrInputDocument idoc = new SolrInputDocument();
        idoc.setField("ident_cely", edoc.getFieldValue("ident_cely"));
        idoc.setField("model", edoc.getFieldValue("entity"));
        idoc.setField("stav", edoc.getFieldValue("stav"));
        idoc.setField("pristupnost", edoc.getFieldValue("pristupnost"));
        idoc.setField("datestamp", edoc.getFieldValue("datestamp"));
        idoc.setField("historie_typ_zmeny", edoc.getFieldValue("historie_typ_zmeny"));
        idoc.setField("historie_uzivatel", edoc.getFieldValue("historie_uzivatel"));
        String xmlData = xml.substring(xml.indexOf("<amcr"));
        idoc.setField("xml", xmlData);
        return idoc;
    }

    private void checkLists(int size) throws SolrServerException, IOException {
        if (idocsEntities.size() > size) {
            solr.add("entities", idocsEntities);
            solr.commit("entities");
            idocsEntities.clear();
            if (!idocsOAI.isEmpty()) {
                solr.add("oai", idocsOAI);
                solr.commit("oai");
                idocsOAI.clear();
            }
            //return;
        }
        if (idocsHeslar.size() > size) {
            solr.add("heslar", idocsHeslar);
            idocsHeslar.clear();
        }
        if (idocsOrganizations.size() > size) {
            solr.add("organizations", idocsOrganizations);
            idocsOrganizations.clear();
        }
        if (idocsUzivatel.size() > size) {
            solr.add("uzivatel", idocsUzivatel);
            idocsUzivatel.clear();
        }
    }
}
