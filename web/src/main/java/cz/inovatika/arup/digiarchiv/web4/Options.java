package cz.inovatika.arup.digiarchiv.web4;

import java.io.File;
import java.io.IOException;
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

  public String getOAIIdentify(String version) {
    try {
      String path = InitServlet.CONFIG_DIR + File.separator + "oai_identify_"+version+".xml";
      File f = new File(path);
      if (f.exists() && f.canRead()) {
        return FileUtils.readFileToString(f, "UTF-8");
      } else {
        File fdef = FileUtils.toFile(Options.class.getResource("oai_identify_"+version+".xml"));
        return FileUtils.readFileToString(fdef, "UTF-8");
      }
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return ex.toString();
    }
  }

  public String getOAIListMetadataFormats(String version) {
      String path = InitServlet.CONFIG_DIR + File.separator + "oai_ListMetadataFormats_"+version+".xml";
      LOGGER.log(Level.INFO, "Path {0}", path); 
    try {
      
      File f = new File(path);
      if (f.exists() && f.canRead()) {
        return FileUtils.readFileToString(f, "UTF-8");
      } else {
        File fdef = FileUtils.toFile(Options.class.getResource("oai_ListMetadataFormats_"+version+".xml"));
        return FileUtils.readFileToString(fdef, "UTF-8");
      }
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return ex.toString();
    }
  }

  public String getOAIListSets() {
    try {
      String path = InitServlet.CONFIG_DIR + File.separator + "oai_ListSets.xml";
      File f = new File(path);
      if (f.exists() && f.canRead()) {
        return FileUtils.readFileToString(f, "UTF-8");
      } else {
        File fdef = FileUtils.toFile(Options.class.getResource("oai_ListSets.xml"));
        return FileUtils.readFileToString(fdef, "UTF-8");
      }
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return ex.toString();
    }
  }
  
  public File getDCXslt() {
      String path = InitServlet.CONFIG_DIR + File.separator + "metadata_DC.xslt";
      File f = new File(path);
      
      if (f.exists() && f.canRead()) {
          return f;
      } else {
          File fdef = FileUtils.toFile(Options.class.getResource("metadata_DC.xslt"));
          return fdef;
      }
  }
  
  public File getForbiddenXslt() {
      String path = InitServlet.CONFIG_DIR + File.separator + "forbidden.xslt";
      File f = new File(path);
      if (f.exists() && f.canRead()) {
          return f;
      } else {
          File fdef = FileUtils.toFile(Options.class.getResource("forbidden.xslt"));
          return fdef;
      } 
  }
  
  public File getVersionXslt(String version) {
      String file = version + ".xslt";
//      switch (version) {
//          case "/v2.0":
//          case "2.1_2.0":
//              file = "amcr_2.1_2.0.xslt";
//              break;
//          case "/v2.1":
//          case "2.0_2.1":
//              file = "amcr_2.0_2.1.xslt";
//              break;
//          case "/v2.2":
//          case "2.1_2.2":
//              file = "amcr_2.1_2.2.xslt";
//              break;
//          case "2.2_2.1":
//              file = "amcr_2.2_2.1.xslt";
//              break;
//          default:
//              break;
//      }
      String path = InitServlet.CONFIG_DIR + File.separator + file;
      File f = new File(path);
      if (f.exists() && f.canRead()) {
          return f;
      } else {
          File fdef = FileUtils.toFile(Options.class.getResource(file));
          return fdef;
      }
  }
  
  public File getEmptyXslt() {
      String path = InitServlet.CONFIG_DIR + File.separator + "cleanEmpty.xslt";
      File f = new File(path);
      
      if (f.exists() && f.canRead()) {
          return f;
      } else {
          File fdef = FileUtils.toFile(Options.class.getResource("metadata_DC.xslt"));
          return fdef;
      }
  }
}
