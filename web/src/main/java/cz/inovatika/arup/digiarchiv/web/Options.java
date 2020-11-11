package cz.inovatika.arup.digiarchiv.web;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
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
public class Options {

  public static final Logger LOGGER = Logger.getLogger(Options.class.getName());

  private static Options _sharedInstance = null;

  private static boolean _indexingFlag = false; // survives option reset
  
  private final JSONObject client_conf;
  private final JSONObject server_conf;

  public synchronized static Options getInstance() {
    try {
    if (_sharedInstance == null) {
      _sharedInstance = new Options();
    }
    } catch (IOException | JSONException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    return _sharedInstance;
  }
  
  

  public synchronized static void resetInstance() {
    _sharedInstance = null;
    LOGGER.log(Level.INFO, "Options reseted");
  }

  public synchronized static boolean setIndexingFlag(boolean indexing) {
    boolean old = _indexingFlag;
    _indexingFlag = indexing;

    if ((old != indexing) && (_sharedInstance != null)) {
      _sharedInstance.ensureIndexingFlag();
    }

    return old;
  }
  
  public Options() throws IOException, JSONException {

    File fdef = new File(InitServlet.DEFAULT_CONFIG_FILE);

    String json = FileUtils.readFileToString(fdef, "UTF-8");
    client_conf = new JSONObject(json);

    
    String path = InitServlet.CONFIG_DIR + File.separator + "config.json";
    
    //Get server options
    File fserver = FileUtils.toFile(Options.class.getResource("server_config.json"));
    String sjson = FileUtils.readFileToString(fserver, "UTF-8");
    server_conf = new JSONObject(sjson);
    

    //Merge options defined in custom dir
    File f = new File(path);
    
    if (f.exists() && f.canRead()) {
      json = FileUtils.readFileToString(f, "UTF-8");
      JSONObject customClientConf = new JSONObject(json).getJSONObject("client");
      if (customClientConf != null) {
        deepMerge(customClientConf, client_conf);
      }
      JSONObject customServerConf = new JSONObject(json).getJSONObject("server");
      if (customServerConf != null) {
        deepMerge(customServerConf, server_conf);
      }
    }

  }

  /**
   * Merge "source" into "target". If fields have equal name, merge them
   * recursively.
   *
   * @param source The json source object
   * @param target The json target
   * @return the merged object (target).
   */
  private JSONObject deepMerge(JSONObject source, JSONObject target) throws JSONException {
    if (source == null || JSONObject.getNames(source) == null) {
      return source;
    }
    for (String key : JSONObject.getNames(source)) {
      Object value = source.get(key);
      if (!target.has(key)) {
        // new value for "key":
        target.put(key, value);
      } else // existing value for "key" - recursively deep merge:
      if (value instanceof JSONObject) {
        JSONObject valueJson = (JSONObject) value;

        deepMerge(valueJson, target.getJSONObject(key));
      } else {
        target.put(key, value);
      }
    }
    return target;
  }

  public JSONObject getClientConf() {
    ensureIndexingFlag();
    return client_conf;
  }

  public String getString(String key, String defVal) {
    return server_conf.optString(key, defVal);
  }

  public String getString(String key) {
    return server_conf.optString(key);
  }

  public int getInt(String key, int defVal) {
    return server_conf.optInt(key, defVal);
  }

  public double getDouble(String key, double defVal) {
    return server_conf.optDouble(key, defVal);
  }

  public boolean getBoolean(String key, boolean defVal) {
    return server_conf.optBoolean(key, defVal);
  }

  public String[] getStrings(String key) {
    JSONArray arr = server_conf.optJSONArray(key);
    String[] ret = new String[arr.length()];
    for (int i = 0; i < arr.length(); i++) {
      ret[i] = arr.getString(i);
    }
    return ret;
  }

  public JSONArray getJSONArray(String key) {
    return server_conf.optJSONArray(key);
  }

  public JSONObject getJSONObject(String key) {
    return server_conf.optJSONObject(key);
  }

  private void ensureIndexingFlag() {
    client_conf.put("indexing", _indexingFlag);
  }
}
