package cz.inovatika.arup.digiarchiv.web4.museion;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import cz.inovatika.arup.digiarchiv.web4.Options;
import cz.inovatika.arup.digiarchiv.web4.index.SolrClientFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alber
 */
public class MuseionClient {

  public static final Logger LOGGER = Logger.getLogger(MuseionClient.class.getName());

  // private static final String API_POINT = Options.getInstance().getJSONObject("museion").getString("end_point");
//    private static final String clientId = Options.getInstance().getJSONObject("museion").getString("clientId");
//    private static final String clientSecret = Options.getInstance().getJSONObject("museion").getString("clientSecret");
  private static final HttpClient client = HttpClient.newHttpClient();

  private String request(String body, String url) throws URISyntaxException, IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(url))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }

  XMLInputFactory _xmlFactory = XMLInputFactory.newFactory();

  public <T> Object parseXml(String xml, Class clazz, String name) throws Exception {
    try {
      XMLStreamReader sr = _xmlFactory.createXMLStreamReader(new StringReader(xml));
      JacksonXmlModule module = new JacksonXmlModule();
      module.setDefaultUseWrapper(false);
      XmlMapper xmlMapper = new XmlMapper(module);
      xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      xmlMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
      sr.nextTag();
      while (!sr.getLocalName().equals(name) && sr.hasNext()) {
        sr.nextTag();
      }
      return xmlMapper.readValue(sr, clazz);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Error parsing {0}", xml);
      // Logger.getLogger(FedoraModel.class.getName()).log(Level.SEVERE, null, ex);
      throw new Exception(ex);
    }
  }

  public PredmetyDleAmcr requestPredmetyDleAmcrId(String amcrId, String amcrTyp, String url, String clientId, String clientSecret) throws Exception {
    String body = String.format(
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://iispp.npu.cz/ServiceAuth\" xmlns:nal=\"http://www.museion.cz/NalezyAmcrService\">\n"
            + "    <soapenv:Header>\n"
            + "        <ser:AuthToken>\n"
            + "            <clientId>%s</clientId>\n"
            + "            <clientSecret>%s</clientSecret>\n"
            + "        </ser:AuthToken>\n"
            + "    </soapenv:Header>\n"
            + "    <soapenv:Body>\n"
            + "        <nal:predmetyDleAmcrIdRequest>\n"
            + "            <amcrId>%s</amcrId>\n"
            + "            <amcrTyp>%s</amcrTyp>\n"
            + "        </nal:predmetyDleAmcrIdRequest>\n"
            + "    </soapenv:Body>\n"
            + "</soapenv:Envelope>",
            clientId, clientSecret, amcrId, amcrTyp);

    String xml = request(body, url);
    return (PredmetyDleAmcr) parseXml(xml, PredmetyDleAmcr.class, "predmetyDleAmcrIdResponse");
  }

  public JSONObject predmetyDleAmcrId(String amcrId, String amcrTyp) {
    JSONObject ret = new JSONObject();
    try {

      JSONArray end_points = Options.getInstance().getJSONObject("museion").getJSONArray("end_points");

      for (int i = 0; i < end_points.length(); i++) {
        JSONObject js = end_points.getJSONObject(i);
        String url = js.getString("url");
        PredmetyDleAmcr resp = requestPredmetyDleAmcrId(amcrId, amcrTyp, url, js.getString("clientId"), js.getString("clientSecret"));
        ObjectMapper objectMapper = new ObjectMapper();
        ret.put(resp.organizaceId, new JSONObject(objectMapper.writeValueAsString(resp)));
      }
      return ret;
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Error getting predmetyDleAmcrId: {0}", ex);
      return new JSONObject().put("error", ex);
    }
  }

  public PredmetyStatistika predmetyStatistika(String url, String clientId, String clientSecret) {

    try {
      String body = String.format(
              "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://iispp.npu.cz/ServiceAuth\" xmlns:nal=\"http://www.museion.cz/NalezyAmcrService\">\n"
              + "    <soapenv:Header>\n"
              + "        <ser:AuthToken>\n"
              + "            <clientId>%s</clientId>\n"
              + "            <clientSecret>%s</clientSecret>\n"
              + "        </ser:AuthToken>\n"
              + "    </soapenv:Header>\n"
              + "    <soapenv:Body>\n"
              + "        <nal:predmetyStatistikaRequest/>\n"
              + "    </soapenv:Body>\n"
              + "</soapenv:Envelope>",
              clientId, clientSecret);

      String xml = request(body, url);
      PredmetyStatistika s = (PredmetyStatistika) parseXml(xml, PredmetyStatistika.class, "statistika");
      return s;

    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Error getting predmetyStatistika: {0}", ex);
      return null;
    }
  }

  public JSONObject predmetyStatistikaAsJSON() {
    JSONObject ret = new JSONObject();

    try {
      JSONArray end_points = Options.getInstance().getJSONObject("museion").getJSONArray("end_points");

      for (int i = 0; i < end_points.length(); i++) {
        JSONObject js = end_points.getJSONObject(i);
        String url = js.getString("url");
        PredmetyStatistika stats = predmetyStatistika(url, js.getString("clientId"), js.getString("clientSecret"));
        if (stats != null) {
          ObjectMapper objectMapper = new ObjectMapper();
          ret.put(stats.organizaceId, new JSONObject(objectMapper.writeValueAsString(stats)));
        }
      }
      return ret;

    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Error getting predmetyStatistikaAsJSON: {0}", ex);
      return new JSONObject().put("error", ex);
    }
  }

  public JSONObject indexStatistika() {
    JSONObject ret = new JSONObject();
    try {
      LOGGER.log(Level.INFO, "Indexing Museion statistika...");
      JSONObject entitaMap = Options.getInstance().getJSONObject("museion").getJSONObject("entita");
      JSONArray end_points = Options.getInstance().getJSONObject("museion").getJSONArray("end_points");
      int indexed = 0;

      for (int i = 0; i < end_points.length(); i++) {
        JSONObject js = end_points.getJSONObject(i);
        String url = js.getString("url");
        PredmetyStatistika stats = predmetyStatistika(url, js.getString("clientId"), js.getString("clientSecret"));
        if (stats != null) {
          SolrClient solr = SolrClientFactory.getSolrClient();
          List<SolrInputDocument> idocs = new ArrayList();
          for (AmcrEntita entita : stats.amcrIdSys) {
            SolrInputDocument idoc = new SolrInputDocument();
            idoc.setField("id", stats.organizaceId + "_" + entita.id);
            idoc.setField("end_point", url);
            idoc.setField("organizaceId", stats.organizaceId);
            idoc.setField("type", "amcrIdSys");
            idoc.setField("amcrId", entita.id);
            idoc.setField("entity", entitaMap.optString(entita.typ));
            idocs.add(idoc);
          }
          for (AmcrEntita entita : stats.amcrIdPom) {
            SolrInputDocument idoc = new SolrInputDocument();
            idoc.setField("id", stats.organizaceId + "_" + entita.id);
            idoc.setField("end_point", url);
            idoc.setField("organizaceId", stats.organizaceId);
            idoc.setField("type", "amcrIdSys");
            idoc.setField("amcrId", entita.id);
            idoc.setField("entity", entitaMap.optString(entita.typ));
            idocs.add(idoc);
          }
          if (!idocs.isEmpty()) {
            solr.add("museion", idocs);
            indexed += idocs.size();
          }
          solr.commit("museion");
          ret.put("indexed", indexed);
        }
      }

    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Error getting predmetyStatistikaAsFilter: {0}", ex);
      ret.put("error", ex);
    }
    return ret;
  }
}
