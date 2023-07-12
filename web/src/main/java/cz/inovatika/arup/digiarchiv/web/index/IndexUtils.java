
package cz.inovatika.arup.digiarchiv.web.index;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.fedora.models.Vocab;
import static cz.inovatika.arup.digiarchiv.web.index.SolrSearcher.getSufixesByLevel;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author alberto
 */
public class IndexUtils {
  
  private static final Logger LOGGER = Logger.getLogger(IndexUtils.class.getName());
  
  private static SolrClient _solr;
  
  public synchronized static SolrClient getClient() {
    try {
      if (_solr == null) {
        _solr = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build();
      }
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    return _solr;
  }

  public static void addSecuredFieldNonRepeat(SolrInputDocument idoc, String field, Object value, String level) {
    List<String> prSufix = getSufixesByLevel(level);
    for (String sufix : prSufix) {
      String f = field + "_" + sufix;
      if (idoc.getFieldValues(f) == null || !idoc.getFieldValues(f).contains(value)) {
        idoc.addField(f, value);
      }
    }
  }

  public static void addSecuredFieldNonRepeat(SolrInputDocument idoc, String field, Object value, List<String> prSufix) {
    for (String sufix : prSufix) {
      String f = field + "_" + sufix;
      if (idoc.getFieldValues(f) == null || !idoc.getFieldValues(f).contains(value)) {
        idoc.addField(f, value);
      }
    }
  }
  
  public static void addVocabField(SolrInputDocument idoc, String field, Vocab v) {
    if(v != null) {
      idoc.setField(field, v.getValue());
    }
  }
  
  public static void addSecuredVocabField(SolrInputDocument idoc, String field, Vocab v, String pristupnost) {
    if(v != null) {
      addSecuredFieldNonRepeat(idoc, field, v.getValue(), pristupnost);
    }
  }
  
  public static void addSecuredJSONField(SolrInputDocument idoc, Object o) {
    if (o != null) {
      try {
        ObjectMapper objectMapper = new ObjectMapper();
        idoc.addField("chranene_udaje", objectMapper.writeValueAsString(o));
      } catch (JsonProcessingException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
    }
  }
}
