
package cz.inovatika.arup.digiarchiv.web4.fedora;

import cz.inovatika.arup.digiarchiv.web4.Options;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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
    //HttpClient client = HttpClient. newHttpClient();
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
    //HttpClient client = HttpClient. newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(new URI(SEARCH_POINT + urlQuery))
            .header("Authorization", auth_header())
            //.header("Accept", "application/ld+json")
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body(); 
  }
  public static String requestXml(String url) throws URISyntaxException, IOException, InterruptedException {
    // HttpClient client = HttpClient. newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(new URI(API_POINT + url))
            .header("Authorization", auth_header())
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    String ret = response.body();
    return ret;
  }
  
  public static JSONObject getJsonById(String id) throws URISyntaxException, IOException, InterruptedException, Exception {
    return new JSONArray(request("record/" + id)).getJSONObject(0);
  }
  
  public static JSONObject getJsonMetadataById(String id) throws URISyntaxException, IOException, InterruptedException, Exception {
    return new JSONArray(request("record/" + id + "/metadata/fcr:metadata")).getJSONObject(0);
  }
  
  public static Path requestFile(String url, String filepath, String mime) throws URISyntaxException, IOException, InterruptedException {
    //HttpClient client = HttpClient. newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(new URI(API_POINT + url))
            .header("Authorization", auth_header())
            .header("Content-Disposition", "attachment; filename=test.pdf")
            .header("Content-Type", mime)
            .build();
    HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(Paths.get(filepath), StandardOpenOption.WRITE, StandardOpenOption.CREATE));
    // HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFileDownload(Paths.get(filepath), StandardOpenOption.WRITE, StandardOpenOption.CREATE));
    
    return response.body();
  }
  
  public static InputStream requestInputStream(String url) throws URISyntaxException, IOException, InterruptedException {
    //HttpClient client = HttpClient. newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(new URI(API_POINT + url))
            .header("Authorization", auth_header())
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    return response.body();
  }
  
}
 