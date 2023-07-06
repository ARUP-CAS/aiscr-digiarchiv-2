/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web.fedora;

import cz.inovatika.arup.digiarchiv.web.FormatUtils;
import cz.inovatika.arup.digiarchiv.web.Options;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
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
  SolrClient solr;

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
   * @param model
   * @return
   * @throws IOException
   */
  public JSONObject indexModel(String model) throws IOException {
    try {
      Instant start = Instant.now();
      solr = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build();
      processModel(model);
      solr.commit("oai");
      solr.commit("entities");
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
      solr.commit("oai");
      solr.commit("entities");
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

  private void getModels() throws URISyntaxException, IOException, InterruptedException, SolrServerException, XMLStreamException {

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

  private void processModel(String model) throws URISyntaxException, IOException, InterruptedException, SolrServerException, XMLStreamException {
    LOGGER.log(Level.INFO, "Processing model {0}", model);
    ret.put(model, 0);
    int indexed = 0;
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
      }
    }
  }

  private void processRecord(String id) throws URISyntaxException, IOException, InterruptedException, SolrServerException, XMLStreamException {
    // http://192.168.8.33:8080/rest/AMCR-test/record/C-201449117/metadata
    // returns xml
    LOGGER.log(Level.FINE, "Processing record {0}", id);
    String xml = FedoraUtils.requestXml("record/" + id + "/metadata");
    String model = FedoraModel.getModel(xml);
    indexXml(xml, model);
  }

  private void processRecord(String id, String model) throws URISyntaxException, IOException, InterruptedException, SolrServerException, XMLStreamException {
    // http://192.168.8.33:8080/rest/AMCR-test/record/C-201449117/metadata
    // returns xml
    LOGGER.log(Level.FINE, "Processing record {0}", id);
    String xml = FedoraUtils.requestXml("record/" + id + "/metadata");
    indexXml(xml, model);
  }

  private void indexXml(String xml, String model) throws URISyntaxException, IOException, InterruptedException, SolrServerException, XMLStreamException {

    Class clazz = FedoraModel.getModelClass(model);
    if (clazz != null) {
      FedoraModel fm = FedoraModel.parseXml(xml, clazz);
      if (fm.isOAI()) {
        SolrInputDocument oaidoc = fm.createOAIDocument(xml);
        solr.add("oai", oaidoc);
      }

      DocumentObjectBinder dob = new DocumentObjectBinder();
      SolrInputDocument idoc = dob.toSolrInputDocument(fm);
      fm.fillSolrFields(idoc);
      if (fm.isEntity()) {
        solr.add("entities", idoc);
      }
      if (fm.isHeslo()) {
        solr.add("heslar", idoc);
      }

    }

  }
}
