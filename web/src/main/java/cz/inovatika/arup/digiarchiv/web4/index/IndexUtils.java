package cz.inovatika.arup.digiarchiv.web4.index;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import cz.inovatika.arup.digiarchiv.web4.Options;
import cz.inovatika.arup.digiarchiv.web4.fedora.FedoraUtils;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.Historie;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.Lang;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.Vocab;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrClient;
//import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class IndexUtils {

    private static final Logger LOGGER = Logger.getLogger(IndexUtils.class.getName());

    static Map<String, String> nalezTypy;
    
    public static String requestSolr(String url) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Authorization", Options.getInstance().getJSONObject("hiko").getString("bearer"))
                .GET()
                .build();

        try (HttpClient httpclient = HttpClient
                .newBuilder()
                .build()) {
            HttpResponse<String> response = httpclient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        }
    }

    public static String getTypNalezu(String typ) {
        if (nalezTypy == null) {
            nalezTypy = new HashMap();
            nalezTypy.put("objekt", Options.getInstance().getString("nalez_objekt_id"));
            nalezTypy.put("predmet", Options.getInstance().getString("nalez_predmet_id"));
        }
        return nalezTypy.get(typ);
    }

    
    public static void addAndCommit(String collection, List<SolrInputDocument> idocs) {
        try {
            SolrClient solr = SolrClientFactory.getSolrClient();
            solr.add(collection, idocs, 10);
            // client.commit(collection);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public static void addAndCommit(String collection, SolrInputDocument idoc) {
        try {
            SolrClient solr = SolrClientFactory.getSolrClient();
            solr.add(collection, idoc, 10);
            // client.commit(collection);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public static void addFieldNonRepeatByJSONVal(SolrInputDocument idoc, String field, Object val) {
        if (val != null) {
            if (val instanceof JSONArray) {
                JSONArray ja = (JSONArray) val;
                for (int j = 0; j < ja.length(); j++) {
                    SolrSearcher.addFieldNonRepeat(idoc, field, ja.get(j));
                }
            } else {
                SolrSearcher.addFieldNonRepeat(idoc, field, val);
            }
        }
    }

    public static void addFieldNonRepeat(SolrInputDocument idoc, String field, Object value) {
        if (idoc.getFieldValues(field) == null || !idoc.getFieldValues(field).contains(value)) {
            idoc.addField(field, value);
        }
    }

    public static void addSecuredFieldNonRepeat(SolrInputDocument idoc, String field, Object value, String level) {
        List<String> prSufix = SolrSearcher.getSufixesByLevel(level);
        for (String sufix : prSufix) {
            String f = field + "_" + sufix;
            if (idoc.getFieldValues(f) == null || !idoc.getFieldValues(f).contains(value)) {
                idoc.addField(f, value);
            }
        }
    }

    public static void setSecuredField(SolrInputDocument idoc, String field, Object value, String level) {
        List<String> prSufix = SolrSearcher.getSufixesByLevel(level);
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
            LOGGER.log(Level.SEVERE, "Can't get datestamp from fedora for {0}", id);
            idoc.setField("datestamp", ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT));
        }
    }

    public static void setDateStampFromDeleted(SolrInputDocument idoc, String id) {
        try {
            String s = FedoraUtils.request("model/deleted/member/" + id);
            JSONArray ja = new JSONArray(s);
            JSONObject j2 = ja.getJSONObject(0);
            String d = j2.getJSONArray("http://fedora.info/definitions/v4/repository#lastModified")
                    .getJSONObject(0).getString("@value");
            idoc.setField("datestamp", d);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.toString());
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

    public static void addByPath(SolrInputDocument idoc, String path, String field, List<String> prSufix, boolean isSecured) {
        boolean secured = path.contains("chranene_udaje") || isSecured;
        String[] parts = path.split("\\.", 2);
        Collection<Object> vals = idoc.getFieldValues(parts[0]);
        if (vals == null) {
            return;
        }
        if (parts.length > 1) {
            for (Object o : vals) {
                try {
                    Object jpReturns = JsonPath.read((String) o, "$." + parts[1]);
                    if (jpReturns instanceof List) {
                        List<String> svals = (List<String>) jpReturns;
                        for (String val : svals) {
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
                } catch (Exception pnfex) {
                    LOGGER.log(Level.FINE, "No value for ", o);
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
