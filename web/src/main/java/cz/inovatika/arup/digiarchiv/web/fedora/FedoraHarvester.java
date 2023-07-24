/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web.fedora;

import cz.inovatika.arup.digiarchiv.web.FormatUtils;
import cz.inovatika.arup.digiarchiv.web.Options;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrClient;
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
  SolrClient solr;

  List<SolrInputDocument> idocsEntities = new ArrayList();
  List<SolrInputDocument> idocsHeslar = new ArrayList();
  List<SolrInputDocument> idocsOrganizations = new ArrayList();
  List<SolrInputDocument> idocsOAI = new ArrayList();

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
      LOGGER.log(Level.INFO, "Index models finished in {0}", interval);
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
      if (!idocsEntities.isEmpty()) {
        solr.add("entities", idocsEntities);
        solr.commit("entities");

        solr.add("oai", idocsOAI);
        solr.commit("oai");
        idocsEntities.clear();
        idocsOAI.clear();
      }
      if (!idocsHeslar.isEmpty()) {
        solr.add("heslar", idocsHeslar);
        idocsHeslar.clear();
      }
      if (!idocsOrganizations.isEmpty()) {
        solr.add("organizations", idocsOrganizations);
        idocsOrganizations.clear();
      }
      solr.close();
      Instant end = Instant.now();
      String interval = FormatUtils.formatInterval(end.toEpochMilli() - start.toEpochMilli());
      ret.put("ellapsed time", interval);
      LOGGER.log(Level.INFO, "Index by ID finished in {0}", interval);
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
    JSONObject json = new JSONArray(FedoraUtils.request("model/" + model + "/member")).getJSONObject(0);
    if (json.has(CONTAINS)) {
      JSONArray records = json.getJSONArray(CONTAINS);
      
      for (int i = 0; i < records.length(); i++) {
        String id = records.getJSONObject(i).getString("@id");
        id = id.substring(id.lastIndexOf("/") + 1);
        processRecord(id, model);
        ret.put(model, indexed++);

        if (idocsEntities.size() > batchSize) {
          solr.add("entities", idocsEntities);
          solr.commit("entities");

//          solr.add("oai", idocsOAI);
//          solr.commit("oai");
          idocsEntities.clear();
          idocsOAI.clear();
          LOGGER.log(Level.INFO, "Indexed {0}", indexed);
          return; 
        }
        if (idocsHeslar.size() > batchSize) {
          solr.add("heslar", idocsHeslar);
          idocsHeslar.clear();
          LOGGER.log(Level.INFO, "Indexed {0}", indexed);
        }
        if (!idocsOrganizations.isEmpty()) {
          solr.add("organizations", idocsOrganizations);
          idocsOrganizations.clear();
          LOGGER.log(Level.INFO, "Indexed {0}", indexed);
        }
      }

      if (!idocsEntities.isEmpty()) {
        solr.add("entities", idocsEntities);
        solr.commit("entities");

      }
      if (!idocsOAI.isEmpty()) {
        solr.add("oai", idocsOAI);
        solr.commit("oai");
        idocsEntities.clear();
        idocsOAI.clear();
      }
      if (!idocsHeslar.isEmpty()) {
        solr.add("heslar", idocsHeslar);
        idocsHeslar.clear();
      }
      if (!idocsOrganizations.isEmpty()) {
        solr.add("organizations", idocsOrganizations);
        idocsOrganizations.clear();
      }
      LOGGER.log(Level.INFO, "Index model {0} finished", model);
    }
  }

  private void processRecord(String id) throws Exception {
    // http://192.168.8.33:8080/rest/AMCR-test/record/C-201449117/metadata
    // returns xml
    LOGGER.log(Level.FINE, "Processing record {0}", id);
    String xml = FedoraUtils.requestXml("record/" + id + "/metadata");
    String model = FedoraModel.getModel(xml);
    indexXml(xml, model);
  }

  private void processRecord(String id, String model) throws Exception {
    try {
      // http://192.168.8.33:8080/rest/AMCR-test/record/C-201449117/metadata
      // returns xml
      LOGGER.log(Level.FINE, "Processing record {0}", id);
      String xml = FedoraUtils.requestXml("record/" + id + "/metadata");
      indexXml(xml, model);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Error processing record {0}", id);
      LOGGER.log(Level.SEVERE, null, ex);
      // throw new Exception(ex);
    }
  }

  private void indexXml(String xml, String model) throws Exception {

    Class clazz = FedoraModel.getModelClass(model);
    if (clazz != null) {
      FedoraModel fm = FedoraModel.parseXml(xml, clazz);
      if (fm.isOAI()) {
        SolrInputDocument oaidoc = fm.createOAIDocument(xml);
        idocsOAI.add(oaidoc);
      }

      DocumentObjectBinder dob = new DocumentObjectBinder();
      SolrInputDocument idoc = dob.toSolrInputDocument(fm);
      fm.fillSolrFields(idoc);
      String core = fm.coreName();
      switch(core) {
        case "entities":
              idocsEntities.add(idoc);
              break;
        case "heslar":
              idocsHeslar.add(idoc);
              break;
        case "organizations":
              idocsOrganizations.add(idoc);
              break;
      }

    }

  }
}
