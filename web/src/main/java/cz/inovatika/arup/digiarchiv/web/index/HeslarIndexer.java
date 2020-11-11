/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.AmcrAPI;
import cz.inovatika.arup.digiarchiv.web.FormatUtils;
import cz.inovatika.arup.digiarchiv.web.Options;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class HeslarIndexer {

  public static final Logger LOGGER = Logger.getLogger(HeslarIndexer.class.getName());
  
  public JSONObject clean() {

    JSONObject ret = new JSONObject();
    try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
      

      client.deleteByQuery("heslar", "*:*", 10);
      // client.commit();
      ret.put("msg", "Index cleaned");

    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  public JSONObject indexHeslare() throws IOException {
    Date start = new Date();
    JSONObject jo = new JSONObject();
    try {
      AmcrAPI amcr = new AmcrAPI();
      boolean logged = amcr.connect();
      if (!logged) {
        return new JSONObject().put("error", "Cant connect");
      }
      int total = 0;
//      String[] hs = Options.getInstance().getStrings("heslare");
//      for (String h : hs) {
//        jo.put(h, indexHeslar(amcr, h));
//      }
      jo.put("obdobi_prvni", indexHeslar(amcr, "obdobi_prvni"));
      Date end = new Date();

      String msg = String.format("Index heslar finished. Docs found :%1$d", total);
      LOGGER.log(Level.INFO, msg);

      jo.put("ellapsed time", FormatUtils.formatInterval(end.getTime() - start.getTime()));
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex.toString());
    }

    return jo;

  }

  private JSONObject indexHeslar(AmcrAPI amcr, String heslar) {
    JSONObject ret = new JSONObject();
    JSONObject jh = getHeslar(amcr, heslar);
    JSONArray docs = new JSONArray();
    Iterator keys = jh.keys();
    int k =0;
    while (keys.hasNext()) {
      String key = (String) keys.next();
      int id = jh.getJSONObject(key).getInt("id");
      docs.put(jh.getJSONObject(key).put("uniqueid", heslar + "_" + id).put("heslar_name", heslar));
      k++;
    }
    postSolr(docs.toString());
    return ret.put("num keys", k);
  }

  private JSONObject getHeslar(AmcrAPI amcr, String id) {
    try {
      LOGGER.log(Level.INFO, "Vyplnim heslar {0}...", id);
      if (id.equals("katastr")) {
        //to je zvlastni pripad
        JSONObject k1 = amcr.getHeslar("katastry1");
        JSONObject k2 = amcr.getHeslar("katastry2");

        Iterator<?> keys = k2.keys();

        while (keys.hasNext()) {
          String key = (String) keys.next();
          k1.put(key, k2.getJSONObject(key));
        }

        return k1;
      } else {
        return amcr.getHeslar(id);

      }

    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);

      return new JSONObject();
    }

  }

  private void postSolr(String jo) {
    try {
      String url = Options.getInstance().getString("solrhost", "http://localhost:8983/solr/")
              + "heslar/update/json?wt=json&commitWithin=1000";

      HttpClient httpClient = HttpClientBuilder.create().build();
      HttpPost post = new HttpPost(url);

      StringEntity entity = new StringEntity(jo, "UTF-8");
      entity.setContentType("application/json");
      post.setEntity(entity);
      HttpResponse response = httpClient.execute(post);
      HttpEntity httpEntity = response.getEntity();
      InputStream in = httpEntity.getContent();

      String encoding = httpEntity.getContentEncoding() == null ? "UTF-8" : httpEntity.getContentEncoding().getName();
      encoding = encoding == null ? "UTF-8" : encoding;
      String responseText = IOUtils.toString(in, encoding);
      LOGGER.log(Level.FINE, responseText);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, null, e);
    }
  }

}
