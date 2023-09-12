
package cz.inovatika.arup.digiarchiv.web.index;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.fedora.models.Historie;
import cz.inovatika.arup.digiarchiv.web.fedora.models.Lang;
import cz.inovatika.arup.digiarchiv.web.fedora.models.Vocab;
import static cz.inovatika.arup.digiarchiv.web.index.SolrSearcher.getSufixesByLevel;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author alberto
 */
public class IndexUtils {
  
  private static final Logger LOGGER = Logger.getLogger(IndexUtils.class.getName());
  
  private static Http2SolrClient _solr;
  
  public synchronized static Http2SolrClient getClient() {
    try {
      if (_solr == null) {
        _solr = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build();
      }
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    return _solr;
  }
  
  public static void closeClient() {
    if (_solr != null) {
      _solr.close();
      _solr = null;
    }
  }
  
  public static void addFieldNonRepeat(SolrInputDocument idoc, String field, Object value) {
    if (idoc.getFieldValues(field) == null || !idoc.getFieldValues(field).contains(value)) {
      idoc.addField(field, value);
    }
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

  public static void setSecuredField(SolrInputDocument idoc, String field, Object value, String level) {
    List<String> prSufix = getSufixesByLevel(level);
    for (String sufix : prSufix) {
      String f = field + "_" + sufix;
      idoc.setField(f, value);
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
      idoc.addField(field, v.getId());
    }
  }
  
  public static void addRefField(SolrInputDocument idoc, String field, Vocab v) {
    if(v != null) {
      idoc.addField(field, v.getValue());
    }
  }
  
  public static void addLangField(SolrInputDocument idoc, String field, Lang v) {
    if(v != null) {
      idoc.addField(field, v.getValue());
    }
  }
  
  public static void addSecuredVocabField(SolrInputDocument idoc, String field, Vocab v, String pristupnost) {
    if(v != null) {
      addSecuredFieldNonRepeat(idoc, field, v.getId(), pristupnost);
    }
  }
  
  public static void addJSONField(SolrInputDocument idoc, String field, Object o) {
    if (o != null) {
      try {
        ObjectMapper objectMapper = new ObjectMapper();
        idoc.addField(field, objectMapper.writeValueAsString(o));
      } catch (JsonProcessingException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
    }
  }
  
  public static void setSecuredJSONField(SolrInputDocument idoc, Object o) {
    if (o != null) {
      try {
        ObjectMapper objectMapper = new ObjectMapper();
        idoc.setField("chranene_udaje", objectMapper.writeValueAsString(o));
      } catch (JsonProcessingException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
    }
  }
  
  public static void setSecuredJSONFieldPrefix(SolrInputDocument idoc, String prefix, Object o) {
    if (o != null) { 
      try {
        ObjectMapper objectMapper = new ObjectMapper();
        idoc.setField(prefix +"_chranene_udaje", objectMapper.writeValueAsString(o));
      } catch (JsonProcessingException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
    }
  }
  
  public static void setDateStamp(SolrInputDocument idoc, List<Historie> historie) {
    if (!historie.isEmpty()) {
      historie.sort(new Comparator<Historie>() {
        @Override
        public int compare(Historie h1, Historie h2) {
          // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
          return h2.datum_zmeny.compareTo(h1.datum_zmeny);
        }
      });
      idoc.setField("datestamp", historie.get(0).datum_zmeny);
      idoc.setField("historie_typ_zmeny", historie.get(0).typ_zmeny);
      idoc.setField("historie_uzivatel", historie.get(0).uzivatel.getId());
    } else {
        idoc.setField("datestamp", ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT));
    }
  }
  
//  public static void addSecuredJSONField(SolrInputDocument idoc, Object o) {
//    if (o != null) {
//      try {
//        ObjectMapper objectMapper = new ObjectMapper();
//        idoc.addField("chranene_udaje", objectMapper.writeValueAsString(o));
//      } catch (JsonProcessingException ex) {
//        LOGGER.log(Level.SEVERE, null, ex);
//      }
//    }
//  }
}