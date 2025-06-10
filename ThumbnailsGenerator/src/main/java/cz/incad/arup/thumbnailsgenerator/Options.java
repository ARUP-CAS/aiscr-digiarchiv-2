package cz.incad.arup.thumbnailsgenerator;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Alberto Hernandez
 */
public class Options {

    public static final Logger LOGGER = Logger.getLogger(Options.class.getName());

    //Directory where cant override configuration  
    public static String CONFIG_DIR = ".amcr";

    //Default configuration file 
    public static String DEFAULT_CONFIG_FILE = "config.json";

    private static Options _sharedInstance = null;
    private JSONObject server_conf;

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

    public Options() throws IOException, JSONException {
        server_conf = new JSONObject();
        String json;

        if (System.getProperty("amcr_app_dir") != null) {
            CONFIG_DIR = System.getProperty("amcr_app_dir");
        }

        String path = CONFIG_DIR + File.separator + "config.json";
        LOGGER.log(Level.INFO, "Config dir is {0}", CONFIG_DIR);

        //Get server options
        File fserver = new File(getClass().getResource("server_config.json").getPath());
        if ( fserver.exists()) {
            String sjson = FileUtils.readFileToString(fserver, "UTF-8");
            server_conf = new JSONObject(sjson);
        } else {
            server_conf = new JSONObject(IOUtils.toString(
                    this.getClass().getResourceAsStream("server_config.json"),
                    "UTF-8"));

        }

        //Merge options defined in custom dir
        File f = new File(path);

        if (f.exists() && f.canRead()) {
            json = FileUtils.readFileToString(f, "UTF-8");
            JSONObject customServerConf = new JSONObject(json);
            Iterator keys2 = customServerConf.keys();
            while (keys2.hasNext()) {
                String key = (String) keys2.next();
                LOGGER.log(Level.FINE, "key {0} will be overrided", key);
                server_conf.put(key, customServerConf.get(key));
            }
        }
        LOGGER.log(Level.FINE, "Configuration is : {0} ", server_conf.toString());

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
}
