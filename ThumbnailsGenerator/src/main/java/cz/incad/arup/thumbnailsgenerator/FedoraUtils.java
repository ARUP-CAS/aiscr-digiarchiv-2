/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cz.incad.arup.thumbnailsgenerator;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import org.json.JSONArray;
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
            .uri(new URI(SEARCH_POINT + urlQuery))
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
  
  public static Path requestFile(String url, String filepath) throws URISyntaxException, IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(new URI(API_POINT + url))
            .header("Authorization", auth_header())
            .build();
    HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFileDownload(Paths.get(filepath), StandardOpenOption.WRITE, StandardOpenOption.CREATE));
    return response.body();
  }
  
  public static InputStream requestInputStream(String url) throws URISyntaxException, IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(new URI(API_POINT + url))
            .header("Authorization", auth_header())
            .build();
    HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    return response.body();
  }
  
  public static byte[] requestBytes(String url, String filepath) throws URISyntaxException, IOException, InterruptedException {
      System.out.println(API_POINT + url);
    HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(new URI(API_POINT + url))
            .header("Authorization", auth_header())
            .build();
    HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    return response.body().readAllBytes();
  }
  
  public static JSONObject getJsonById(String id) throws URISyntaxException, IOException, InterruptedException, Exception {
    return new JSONArray(request("record/" + id)).getJSONObject(0);
  }
}
