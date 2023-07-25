package cz.inovatika.arup.digiarchiv.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Alberto Hernandez
 */
public class I18n {

  public static final Logger LOGGER = Logger.getLogger(I18n.class.getName());

  private static I18n _sharedInstance = null;
  private Map<String, JSONObject> locales;

  public synchronized static I18n getInstance() throws IOException, JSONException {
    if (_sharedInstance == null) {
      _sharedInstance = new I18n();
    }
    return _sharedInstance;
  }

  public synchronized static void resetInstance() {
    _sharedInstance = null;
    LOGGER.log(Level.FINE, "I18n reseted");
  }

  public I18n() throws IOException, JSONException {
    locales = new HashMap<>();
    LOGGER.log(Level.FINE, locales.toString());
  }

  public void load(String locale) throws IOException, JSONException {

    String filename = locale + ".json";

    File fdef = new File(InitServlet.DEFAULT_I18N_DIR + File.separator + filename);

    String json = FileUtils.readFileToString(fdef, "UTF-8");
    JSONObject def = new JSONObject(json);

    String path = InitServlet.CONFIG_DIR + File.separator + "i18n/" + filename;

    //Merge file defined in custom dir
    File f = new File(path);
    if (f.exists() && f.canRead()) {
      json = FileUtils.readFileToString(f, "UTF-8");
      JSONObject customClientConf = new JSONObject(json);
      Iterator keys = customClientConf.keys();
      while (keys.hasNext()) {
        String key = (String) keys.next();
        LOGGER.log(Level.FINE, "key {0} will be overrided", key);
        def.put(key, customClientConf.get(key));
      }

    }

    Options opts = Options.getInstance();
    
    //Load Solr keys
    JSONObject heslarDoc = new JSONObject();
    String urlHeslar = opts.getString("solrhost", "http://localhost:8983/solr/")
            + "heslar/export?q=*:*&wt=json&sort=ident_cely%20asc&fl=ident_cely," + locale;
    InputStream inputStream = RESTHelper.inputStream(urlHeslar);
    String solrResp = org.apache.commons.io.IOUtils.toString(inputStream, "UTF-8");
    JSONArray docs = new JSONObject(solrResp).getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < docs.length(); i++) {
      heslarDoc.put(docs.getJSONObject(i).getString("ident_cely"), docs.getJSONObject(i).getString(locale));
    }
    
    // Organizace
    String field = "nazev_zkraceny";
    if ("en".equals(locale)) {
      field += "_en";
    }
    String urlOrg = opts.getString("solrhost", "http://localhost:8983/solr/")
            + "organizations/export?q=*:*&wt=json&sort=ident_cely%20asc&fl=ident_cely," + field;
    inputStream = RESTHelper.inputStream(urlOrg);
    solrResp = org.apache.commons.io.IOUtils.toString(inputStream, "UTF-8");
    docs = new JSONObject(solrResp).getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < docs.length(); i++) {
      heslarDoc.put(docs.getJSONObject(i).getString("ident_cely"), docs.getJSONObject(i).getString(field));
    }
    
    //Pristupnost indexujeme zkratky
    String urlPr = opts.getString("solrhost", "http://localhost:8983/solr/")
            + "heslar/select?q=nazev_heslare:pristupnost&wt=json&fl=ident_cely,zkratka," + locale;
    inputStream = RESTHelper.inputStream(urlPr);
    solrResp = org.apache.commons.io.IOUtils.toString(inputStream, "UTF-8");
    docs = new JSONObject(solrResp).getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < docs.length(); i++) {
      heslarDoc.put(docs.getJSONObject(i).getString("zkratka"), docs.getJSONObject(i).getString(locale));
    }

    def.put("heslar", heslarDoc);
    
    String url = opts.getString("solrhost", "http://localhost:8983/solr/")
            + "translations/export?q=*:*&wt=json&sort=ident_cely%20asc&fl=ident_cely," + locale;

    LOGGER.log(Level.FINE, "requesting url {0}", url);
    inputStream = RESTHelper.inputStream(url);
    solrResp = org.apache.commons.io.IOUtils.toString(inputStream, "UTF-8");
    docs = new JSONObject(solrResp).getJSONObject("response").getJSONArray("docs");
    
    for (int i = 0; i < docs.length(); i++) {
      def.put(docs.getJSONObject(i).getString("id"), docs.getJSONObject(i).getString(locale));
      String heslar = docs.getJSONObject(i).getString("heslar");
      LOGGER.log(Level.FINE, heslar);
      String pole = heslar;
      def.put(pole + "_" + docs.getJSONObject(i).getString("heslo"), docs.getJSONObject(i).getString(locale)); 

      if (opts.getClientConf().getJSONObject("heslarToPole").has(heslar)) {
        Object obj = opts.getClientConf().getJSONObject("heslarToPole").get(heslar);
        if (obj instanceof JSONArray) {
          JSONArray poli = (JSONArray) obj;
          for (int p = 0; p < poli.length(); p++) {
            def.put(poli.getString(p) + "_" + docs.getJSONObject(i).getString("heslo"), docs.getJSONObject(i).getString(locale));
          }
        } else {
          pole = (String) obj;
          def.put(pole + "_" + docs.getJSONObject(i).getString("heslo"), docs.getJSONObject(i).getString(locale));
        }

      }
    }

    locales.put(locale, def);

  }

  public JSONObject getLocale(String locale) throws IOException {
    if (!locales.containsKey(locale)) {
      load(locale);
    }
    return locales.get(locale);
  }

}
