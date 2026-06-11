package cz.inovatika.arup.digiarchiv.web4.museion;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import cz.inovatika.arup.digiarchiv.web4.Options;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import org.json.JSONObject;

/**
 *
 * @author alber
 */
public class MuseionClient {

    public static final Logger LOGGER = Logger.getLogger(MuseionClient.class.getName());

    private static final String API_POINT = Options.getInstance().getJSONObject("museion").getString("end_point");
    private static final String clientId = Options.getInstance().getJSONObject("museion").getString("clientId");
    private static final String clientSecret = Options.getInstance().getJSONObject("museion").getString("clientSecret");
    private static final HttpClient client = HttpClient.newHttpClient();

    private String request(String body) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(API_POINT))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
    
    XMLInputFactory _xmlFactory = XMLInputFactory.newFactory();
    public <T> PredmetyDleAmcr parseXml(String xml) throws Exception {
        try {
            XMLStreamReader sr = _xmlFactory.createXMLStreamReader(new StringReader(xml));
            JacksonXmlModule module = new JacksonXmlModule();
            module.setDefaultUseWrapper(false);
            XmlMapper xmlMapper = new XmlMapper(module);
            xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            xmlMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
            sr.nextTag();
            sr.nextTag();
            sr.nextTag();
            return (PredmetyDleAmcr) xmlMapper.readValue(sr, PredmetyDleAmcr.class);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error parsing {0}", xml);
            // Logger.getLogger(FedoraModel.class.getName()).log(Level.SEVERE, null, ex);
            throw new Exception(ex);
        }
    }

    public JSONObject predmetyDleAmcrId(String amcrId, String amcrTyp) {

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
                    + "        <nal:predmetyDleAmcrIdRequest>\n"
                    + "            <amcrId>%s</amcrId>\n"
                    + "            <amcrTyp>%s</amcrTyp>\n"
                    + "        </nal:predmetyDleAmcrIdRequest>\n"
                    + "    </soapenv:Body>\n"
                    + "</soapenv:Envelope>",
                    clientId, clientSecret, amcrId, amcrTyp);
            
            String xml = request(body);
            PredmetyDleAmcr resp = parseXml(xml);
            
            ObjectMapper objectMapper = new ObjectMapper();
                
            return new JSONObject(objectMapper.writeValueAsString(resp));
            
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error getting predmetyDleAmcrId: {0}", ex);
            return new JSONObject().put("error", ex);
        }
    }
}
