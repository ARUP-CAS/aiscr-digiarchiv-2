/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.FormatUtils;
import cz.inovatika.arup.digiarchiv.web.I18n;
import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.index.models.Akce;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class IndexerTranslations {
  
  static final Logger LOGGER = Logger.getLogger(IndexerTranslations.class.getName());

  public static JSONObject fromCSV() {
    JSONObject ret = new JSONObject();
    int success = 0;
    int errors = 0;
    try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
      client.deleteByQuery("translations", "*:*");
      client.commit("translations");
      Date start = new Date();
      JSONArray ja = new JSONArray();
      ret.put("errors msgs", ja);
      String thesauriDir = Options.getInstance().getString("thesauriDir");
      File dir = new File(thesauriDir);
      LOGGER.log(Level.INFO, "indexing from {0}", thesauriDir);
      for (File file : dir.listFiles()) {
        LOGGER.log(Level.INFO, "indexing from {0}", file.getName());
        // Reader in = new FileReader(file);
        Reader in = new FileReader(file, Charset.forName("UTF-8"));
        //readOne( , uniqueid, "", translationsClient, ret, hasRelations);

        Date tstart = new Date();
        int tsuccess = 0;
        int terrors = 0;
        JSONObject typeJson = new JSONObject();

        //CSVFormat f = CSVFormat.newFormat('#').withEscape('\\').withQuote('\"').withFirstRecordAsHeader();
        CSVFormat f = CSVFormat.newFormat('#').withEscape('\\').withFirstRecordAsHeader();
        CSVParser parser = new CSVParser(in, f);
        Map<String, Integer> header = parser.getHeaderMap();
        try {

          for (final CSVRecord record : parser) {
            try {
              SolrInputDocument doc = new SolrInputDocument();

              doc.addField("id", record.get(0) + "_" + record.get(2));
              for (Map.Entry<String, Integer> entry : header.entrySet()) {
                doc.addField(entry.getKey().toLowerCase().trim(), record.get(entry.getKey()));
              }

              client.add("translations", doc);
              tsuccess++;
              success++;
              if (success % 500 == 0) {
                client.commit("translations");
                LOGGER.log(Level.INFO, "Indexed {0} docs", success);
              }
            } catch (Exception ex) {
              terrors++;
              errors++;
              ret.getJSONArray("errors msgs").put(record);
              LOGGER.log(Level.SEVERE, "Error indexing doc {0}", record);
              LOGGER.log(Level.SEVERE, null, ex);
            }
          }

          
          client.commit("translations");

          typeJson.put("docs indexed", tsuccess).put("errors", terrors);
          Date tend = new Date();

          typeJson.put("ellapsed time", FormatUtils.formatInterval(tend.getTime() - tstart.getTime()));
          ret.put(file.getName(), typeJson).put("docs indexed", success);
        } finally {
          parser.close();
        }

      }
      I18n.resetInstance();
      LOGGER.log(Level.INFO, "Indexed Finished. {0} success, {1} errors", new Object[]{success, errors});

      ret.put("errors", errors);

      Date end = new Date();
      ret.put("ellapsed time", FormatUtils.formatInterval(end.getTime() - start.getTime()));
    } catch (IOException | SolrServerException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    return new JSONObject().put("translations", ret);
  }
  

  public void fromCSV(Map<String, Integer> header, CSVRecord record) {

    Class aClass = Akce.class;
    java.lang.reflect.Field[] fields = aClass.getDeclaredFields();

    for (java.lang.reflect.Field field : fields) {
      if (header.containsKey(field.getName())) {
        try {
          field.set(this, record.get(header.get(field.getName())));
        } catch (IllegalArgumentException | IllegalAccessException ex) {
          LOGGER.log(Level.SEVERE, null, ex);
        }
      }
    }

  }
}
