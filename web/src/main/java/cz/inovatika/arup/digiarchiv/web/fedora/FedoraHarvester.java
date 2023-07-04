/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web.fedora;

import cz.inovatika.arup.digiarchiv.web.FormatUtils;
import cz.inovatika.arup.digiarchiv.web.Options;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class FedoraHarvester {

  public static final Logger LOGGER = Logger.getLogger(FedoraHarvester.class.getName());

  private static final String CONTAINS = "http://www.w3.org/ns/ldp#contains";

  private String api_point;
  private String auth_header;
  JSONObject ret = new JSONObject();

  public JSONObject harvest() {
    try {
      Instant start = Instant.now();
      JSONObject fedoraOpts = Options.getInstance().getJSONObject("fedora");
      String username = fedoraOpts.getString("user");
      String password = fedoraOpts.getString("pwd");
      String valueToEncode = username + ":" + password;
      auth_header = "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
      api_point = fedoraOpts.getString("api.point");
      getModels();
      Instant end = Instant.now();
      String interval = FormatUtils.formatInterval(end.toEpochMilli() - start.toEpochMilli());
      ret.put("ellapsed time", interval);
      LOGGER.log(Level.INFO, "Harvest finished in {0}", interval);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  private String request(String url) throws URISyntaxException, IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(new URI(api_point + url))
            .header("Authorization", auth_header)
            .header("Accept", "application/ld+json")
            .build();
    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

//      LOGGER.log(Level.INFO, "Status {0}", response.statusCode());
//      LOGGER.log(Level.INFO, "Status {0}", response.body());
    return response.body();
  }

  private String requestXml(String url) throws URISyntaxException, IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(new URI(api_point + url))
            .header("Authorization", auth_header)
            .build();
    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

//      LOGGER.log(Level.INFO, "Status {0}", response.statusCode());
//      LOGGER.log(Level.INFO, "Status {0}", response.body());
    return response.body();
  }

  private void getModels() throws URISyntaxException, IOException, InterruptedException {

    JSONObject json = new JSONArray(request("model")).getJSONObject(0);
    // returns list of models (entities) in CONTAINS 

    if (json.has(CONTAINS)) {
      JSONArray models = json.getJSONArray(CONTAINS);
      for (int i = 0; i < models.length(); i++) {
        String id = models.getJSONObject(i).getString("@id");
        id = id.substring(id.lastIndexOf("/") + 1);
        processModel(id);
      }
    }
  }

  private void processModel(String model) throws URISyntaxException, IOException, InterruptedException {
    LOGGER.log(Level.INFO, "Processing model {0}", model);
    ret.append("models", model);
    //http://192.168.8.33:8080/rest/AMCR-test/model/projekt/member
    // returns list of records in CONTAINS
//    [{
//      "@id": "http://192.168.8.33:8080/rest/AMCR-test/model/projekt/member/C-201449117"
//    }],
    JSONObject json = new JSONArray(request("model/" + model + "/member")).getJSONObject(0);
    if (json.has(CONTAINS)) {
      JSONArray records = json.getJSONArray(CONTAINS);
      for (int i = 0; i < records.length(); i++) {
        String id = records.getJSONObject(i).getString("@id");
        id = id.substring(id.lastIndexOf("/") + 1);
        processRecord(id);
      }
    }

  }

  private String processRecord(String id) throws URISyntaxException, IOException, InterruptedException {
    // http://192.168.8.33:8080/rest/AMCR-test/record/C-201449117/metadata
    // returns xml
    LOGGER.log(Level.INFO, "Processing record {0}", id);
    String xml = requestXml("record/" + id + "/metadata");
    return xml;
  }

}
