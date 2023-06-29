/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web.fedora;

import cz.inovatika.arup.digiarchiv.web.Options;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
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

  public JSONObject harvest() {
    JSONObject ret = new JSONObject();
    try {
      JSONObject fedoraOpts = Options.getInstance().getJSONObject("fedora");
      String username = fedoraOpts.getString("user");
      String password = fedoraOpts.getString("pwd");
      String valueToEncode = username + ":" + password;
      String header = "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
              .GET()
              .uri(new URI(fedoraOpts.getString("api.point")))
              .header("Authorization", header)
              .header("Accept", "application/ld+json")
              .build();
 
      HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

      LOGGER.log(Level.INFO, "Status {0}", response.statusCode());
      LOGGER.log(Level.INFO, "Status {0}", response.body());
      
      ret.put("response", new JSONArray(response.body()));
      
    } catch (URISyntaxException | IOException | InterruptedException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  } 
  
  private void getModels() {
    
  }
  
  private void processModel() {
    
  }

}
