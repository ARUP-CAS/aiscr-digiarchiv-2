/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.FormatUtils;
import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.RESTHelper;
import cz.inovatika.arup.digiarchiv.web.index.models.Akce;
import cz.inovatika.arup.digiarchiv.web.index.models.Dokument;
import cz.inovatika.arup.digiarchiv.web.index.models.Entity;
import cz.inovatika.arup.digiarchiv.web.index.models.Lokalita;
import cz.inovatika.arup.digiarchiv.web.index.models.Projekt;
import cz.inovatika.arup.digiarchiv.web.index.models.SamostatniNalez;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.json.XMLParserConfiguration;

/**
 *
 * @author alberto
 */
public class OAIUtils {

  static final Logger LOGGER = Logger.getLogger(OAIUtils.class.getName());

  public static JSONObject getId(String id) {
    try {
      String url = Options.getInstance().getJSONObject("OAI").getString("url")
              + "?verb=GetRecord&metadataPrefix=oai_amcr&identifier=https://api.aiscr.cz/id/" + id;
      // LOGGER.log(Level.INFO, "Retreiving {0}", url);
      // String xml = IOUtils.toString(new URL(url), Charset.forName("UTF-8"));
      String user = Options.getInstance().getJSONObject("amcrapi").getString("user");
      String pwd = Options.getInstance().getJSONObject("amcrapi").getString("pwd");
      String xml = RESTHelper.toString(url, user, pwd);
      return XML.toJSONObject(xml, XMLParserConfiguration.ORIGINAL);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return null;
    }
  }

  public static JSONObject getMetadataId(String id) {
    try {
      String url = Options.getInstance().getJSONObject("OAI").getString("url")
              + "?verb=GetRecord&metadataPrefix=oai_amcr&identifier=https://api.aiscr.cz/id/" + id;
      LOGGER.log(Level.INFO, "Retreiving {0}", url);
      // String xml = IOUtils.toString(new URL(url), Charset.forName("UTF-8"));
      String user = Options.getInstance().getJSONObject("amcrapi").getString("user");
      String pwd = Options.getInstance().getJSONObject("amcrapi").getString("pwd");
      String xml = RESTHelper.toString(url, user, pwd);
      JSONObject json = XML.toJSONObject(xml, XMLParserConfiguration.ORIGINAL);
      return json.getJSONObject("OAI-PMH")
              .getJSONObject("GetRecord")
              .getJSONObject("record")
              .getJSONObject("metadata")
              .getJSONObject("oai_amcr:amcr");
      //.getJSONObject(entity);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "error getting {0}", id);
      LOGGER.log(Level.SEVERE, null, ex);
      return null;
    }
  }

  public static <T extends Entity> JSONObject indexEntity(String entity, Class<T> clazz) {
    return indexEntity(entity, clazz, false);
  }

  public static <T extends Entity> JSONObject indexEntity(String entity, Class<T> clazz, boolean addRelations) {
    return indexEntity(entity, clazz, addRelations, false);
  }
  public static <T extends Entity> JSONObject indexEntity(String entity, Class<T> clazz, boolean addRelations, boolean second) {
    JSONObject ret = new JSONObject();
    Date start = new Date();
    int indexed = 0;
    String rels = Options.getInstance().getString("solrRels");
    String url = "";
    try (HttpSolrClient solrRels = new HttpSolrClient.Builder(rels).build()) {
      String user = Options.getInstance().getJSONObject("amcrapi").getString("user");
      String pwd = Options.getInstance().getJSONObject("amcrapi").getString("pwd");
      String login = user + ":" + pwd;

      url = Options.getInstance().getJSONObject("OAI").getString("url")
              + "?verb=ListRecords&metadataPrefix=oai_amcr&set=" + entity;
      LOGGER.log(Level.INFO, "Retreiving {0}", url);
      String xml = RESTHelper.toString(url, user, pwd);
      // String xml = IOUtils.toString(new URL(url), Charset.forName("UTF-8"));
      JSONObject json = XML.toJSONObject(xml, XMLParserConfiguration.ORIGINAL);
      indexed += processResponse(json, ret, entity, clazz, solrRels, addRelations, second);
      boolean hasResumption = json.getJSONObject("OAI-PMH").getJSONObject("ListRecords").optJSONObject("resumptionToken") != null;
      while (hasResumption) {
        url = Options.getInstance().getJSONObject("OAI").getString("url")
                + "?verb=ListRecords&resumptionToken="
                + json.getJSONObject("OAI-PMH").getJSONObject("ListRecords").getJSONObject("resumptionToken").getString("content");

        LOGGER.log(Level.FINE, "Retreiving {0}", url);
        xml = RESTHelper.toString(url, user, pwd);
        json = XML.toJSONObject(xml, XMLParserConfiguration.ORIGINAL);
        indexed += processResponse(json, ret, entity, clazz, solrRels, addRelations, second);
        ret.put(entity, indexed);
        LOGGER.log(Level.INFO, "Indexing {0}. Current {1}", new Object[]{entity, indexed});
        hasResumption = json.getJSONObject("OAI-PMH").getJSONObject("ListRecords").optJSONObject("resumptionToken") != null;
      }
      ret.put(entity, indexed);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "error indexing {0}: {1}", new Object[]{url, ex});
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put(entity, "error: " + ex);
    }
    Date end = new Date();
    String ellapsed = FormatUtils.formatInterval(end.getTime() - start.getTime());
    ret.put("ellapsed time", ellapsed);
    LOGGER.log(Level.INFO, "Indexing {0} FINISHED. indexed {1}. Time: {2}", new Object[]{entity, indexed, ellapsed});
    return ret;
  }

  public static JSONObject indexRecord(JSONObject record, String entity) {
    String solrhost = Options.getInstance().getString("solrhost");
    String rels = Options.getInstance().getString("solrRels");
    String collection = "entities";

    try (HttpSolrClient solrRels = new HttpSolrClient.Builder(rels).build()) {
      try (HttpSolrClient solr = new HttpSolrClient.Builder(solrhost).build()) {
        SolrInputDocument idoc = processRecord(solr, record.toString(), entity, getEntityClass(entity), solrRels);
        solr.add(collection, idoc, 1);
        return new JSONObject().put(entity, record);
      } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, null, ex);
        return new JSONObject().put("error", ex);
      }
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }

  public static <T extends Entity> SolrInputDocument processRecord(HttpSolrClient solr, String record, String entity, Class<T> clazz, HttpSolrClient solrRels)
          throws Exception {
    try {

      Entity bean = Entity.parseJson(record, clazz);
      DocumentObjectBinder dob = new DocumentObjectBinder();
      SolrInputDocument idoc = dob.toSolrInputDocument(bean);
      bean.fillFields(idoc);
      bean.addRelations(solrRels, idoc);
      if (bean.isEntity()) {
        if ("dokument".equals(entity) && "3D".equals(idoc.getFieldValue("rada"))) {
          idoc.addField("entity", "knihovna_3d");
        } else {
          idoc.addField("entity", entity);
        }
      }
      bean.setFullText(idoc);
      // docs.add((T) bean);
      return idoc;
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      throw new Exception(ex);
    }
  }

  public static <T extends Entity> int processResponse(JSONObject json, JSONObject ret, String entity, Class<T> clazz, HttpSolrClient solrRels, boolean addRelations, boolean second)
          throws Exception {

    JSONArray records = json.getJSONObject("OAI-PMH").getJSONObject("ListRecords").getJSONArray("record");
    // List<T> docs = new ArrayList<>();
    List<SolrInputDocument> docs = new ArrayList<>();

    String solrhost = Options.getInstance().getString("solrhost");
    String collection = entity;
    int indexed = 0;
    try (HttpSolrClient solr = new HttpSolrClient.Builder(solrhost).build()) {
      for (int i = 0; i < records.length(); i++) {
        String record = records.getJSONObject(i).getJSONObject("metadata").getJSONObject("oai_amcr:amcr").getJSONObject(entity).toString();
        Entity bean = Entity.parseJson(record, clazz);

        DocumentObjectBinder dob = new DocumentObjectBinder();
        SolrInputDocument idoc = dob.toSolrInputDocument(bean);
        bean.fillFields(idoc);
        if (addRelations) {
          bean.addRelations(solrRels, idoc);
        }
        if (second) {
          bean.secondRound(solrRels, idoc);
        }
        if (bean.isEntity()) {
          if ("dokument".equals(entity) && idoc.getFieldValue("rada").equals("3D")) {
            idoc.addField("entity", "knihovna_3d");
          } else {
            idoc.addField("entity", entity);
          }
          collection = "entities";
        }
        bean.setFullText(idoc);
        // docs.add((T) bean);
        docs.add(idoc);
      }

      if (!docs.isEmpty()) {
        // solr.addBeans(docs);
        solr.add(collection, docs);

        indexed += docs.size();
        docs.clear();
      }
      solr.commit(collection);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      throw new Exception(ex);
    }
    return indexed;
  }
  
   public static Class getEntityClass(String entity) {
    switch ((String) entity) {
      case "akce":
        return Akce.class;
      case "lokalita":
        return Lokalita.class;
      case "projekt":
        return Projekt.class;
      case "samostatny_nalez":
        return SamostatniNalez.class;
      case "knihovna_3d":
        return Dokument.class;
      default:
        return Dokument.class;
    }
  }
}
