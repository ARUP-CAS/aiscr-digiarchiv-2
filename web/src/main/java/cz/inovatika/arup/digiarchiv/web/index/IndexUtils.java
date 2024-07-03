package cz.inovatika.arup.digiarchiv.web.index;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraUtils;
import cz.inovatika.arup.digiarchiv.web.fedora.models.Historie;
import cz.inovatika.arup.digiarchiv.web.fedora.models.Lang;
import cz.inovatika.arup.digiarchiv.web.fedora.models.Vocab;
import static cz.inovatika.arup.digiarchiv.web.index.SolrSearcher.getSufixesByLevel;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class IndexUtils {

    private static final Logger LOGGER = Logger.getLogger(IndexUtils.class.getName());

    private static Http2SolrClient _solrBin;
    private static Http2SolrClient _solrNoOp;

    public synchronized static Http2SolrClient getClientNoOp() {
        try {
            if (_solrNoOp == null) {
                NoOpResponseParser dontMessWithSolr = new NoOpResponseParser();
                dontMessWithSolr.setWriterType("json");
                _solrNoOp = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost"))
                        .withResponseParser(dontMessWithSolr)
                        .build();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return _solrNoOp;
    }

    public synchronized static Http2SolrClient getClientBin() {
        try {
            if (_solrBin == null) {
                _solrBin = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return _solrBin;
    }

    public static void closeClient() {
        if (_solrBin != null) {
            _solrBin.close();
            _solrBin = null;
        }
        if (_solrNoOp != null) {
            _solrNoOp.close();
            _solrNoOp = null;
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
        if (v != null) {
            idoc.addField(field, v.getId());
        }
    }

    public static void addRefField(SolrInputDocument idoc, String field, Vocab v) {
        if (v != null) {
            idoc.addField(field, v.getValue());
        }
    }

    public static void addLangField(SolrInputDocument idoc, String field, Lang v) {
        if (v != null) {
            idoc.addField(field, v.getValue());
        }
    }

    public static void addSecuredVocabField(SolrInputDocument idoc, String field, Vocab v, String pristupnost) {
        if (v != null) {
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

    public static void setSecuredJSONField(SolrInputDocument idoc, String field, Object o) {
        if (o != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                idoc.setField(field, objectMapper.writeValueAsString(o));
            } catch (JsonProcessingException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void setSecuredJSONFieldPrefix(SolrInputDocument idoc, String prefix, Object o) {
        if (o != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                idoc.setField(prefix + "_chranene_udaje", objectMapper.writeValueAsString(o));
            } catch (JsonProcessingException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void setDateStamp(SolrInputDocument idoc, String id) {
        try {
            JSONObject json = FedoraUtils.getJsonMetadataById(id);
            String d = json.getJSONArray("http://fedora.info/definitions/v4/repository#lastModified")
                    .getJSONObject(0).getString("@value");
            idoc.setField("datestamp", d);
        } catch (Exception ex) {
            idoc.setField("datestamp", ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT));
        }
    }

    public static void setDateStampFromHistory(SolrInputDocument idoc, List<Historie> historie) {
        if (!historie.isEmpty()) {
            historie.sort(new Comparator<Historie>() {
                @Override
                public int compare(Historie h1, Historie h2) {
                    // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    return h2.datum_zmeny.compareTo(h1.datum_zmeny);
                }
            });
            // idoc.setField("datestamp", historie.get(0).datum_zmeny);
            idoc.setField("historie_typ_zmeny", historie.get(0).typ_zmeny);
            idoc.setField("historie_uzivatel", historie.get(0).uzivatel.getId());
        } else {
            // idoc.setField("datestamp", ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT));
        }
    }
    
    public static void addByPath(SolrInputDocument idoc, String path, String field, List<String> prSufix) {
        boolean secured = path.contains("chranene_udaje");
        String[] parts = path.split("\\.", 2);
        Collection<Object> vals = idoc.getFieldValues(parts[0]);
            
        if (vals == null) {
            return;
        }
        if (parts.length > 1) {
            
            for (Object o: vals) {
                Object jpReturns = JsonPath.read((String)o, "$." + parts[1]);
                if (jpReturns instanceof List) {
                    List<String> svals = (List<String>)jpReturns;
                    for (String val: svals) {
                        if (secured) {
                            for (String sufix : prSufix) {
                                IndexUtils.addFieldNonRepeat(idoc, field + "_" + sufix, val);
                            }
                        } else {
                            addFieldNonRepeat(idoc, field, val);
                        }

                    }
                } else {
                    if (secured) {
                        for (String sufix : prSufix) {
                            IndexUtils.addFieldNonRepeat(idoc, field + "_" + sufix, jpReturns);
                        }
                    } else {
                        addFieldNonRepeat(idoc, field, jpReturns);
                    }
                }
                
            }
            
        } else {
            if (secured) {
                for (String sufix : prSufix) {
                    IndexUtils.addFieldNonRepeat(idoc, field + "_" + sufix, vals);
                }
            } else {
                addFieldNonRepeat(idoc, field, vals);
            }
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
