package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.FormatUtils;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class OAIIndexer {

  static final Logger LOGGER = Logger.getLogger(OAIIndexer.class.getName());

  public static JSONObject full(String entity, Class clazz, boolean clean, boolean second) {
    JSONObject ret = new JSONObject();
    Instant start = Instant.now();
    try {
      ret = OAIUtils.indexEntity(entity, clazz, true, second);
      if (clean && !ret.has("error")) {
        ret.put(entity + " clean", SearchUtils.clean(start, entity));
      }
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    Instant end = Instant.now();
    ret.put("ellapsed time", FormatUtils.formatInterval(end.toEpochMilli() - start.toEpochMilli()));

    return ret;
  }

  public static JSONObject full(String entity, Class clazz, boolean clean) {
    return full(entity, clazz, clean, false);
  }
}
