/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web.fedora;

import cz.inovatika.arup.digiarchiv.web.Options;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class FedoraUtils {
  
  private static final String API_POINT = Options.getInstance().getJSONObject("fedora").getString("api.point");
  private static final String SEARCH_POINT = Options.getInstance().getJSONObject("fedora").getString("search.point");
  private static final HttpClient client = HttpClient.newHttpClient();
  
  
  public static final String auth_header() {
    
      JSONObject fedoraOpts = Options.getInstance().getJSONObject("fedora");
      String username = fedoraOpts.getString("user");
      String password = fedoraOpts.getString("pwd");
      String valueToEncode = username + ":" + password;
      return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
  }

  public static String request(String url) throws URISyntaxException, IOException, InterruptedException {
    
    HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(new URI(API_POINT + url))
            .header("Authorization", auth_header())
            .header("Accept", "application/ld+json")
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

//      LOGGER.log(Level.INFO, "Status {0}", response.statusCode());
    return response.body();
  }

  public static String search(String urlQuery) throws URISyntaxException, IOException, InterruptedException {
    
    HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(new URI(SEARCH_POINT + URLEncoder.encode(urlQuery, "UTF8")))
            .header("Authorization", auth_header())
            //.header("Accept", "application/ld+json")
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

//      LOGGER.log(Level.INFO, "Status {0}", response.statusCode());
    return response.body();
  }
  
  public static String requestXml(String url) throws URISyntaxException, IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(new URI(API_POINT + url))
            .header("Authorization", auth_header())
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }
  
  public static JSONObject getById(String id) throws URISyntaxException, IOException, InterruptedException, Exception {
//    String xml = requestXml("record/" + id + "/metadata");
//    
//    Projekt p = Projekt.parse(xml);
//    System.out.println(p.stav);
//    return org.json.XML.toJSONObject(xml);
    return null;
  }
}
