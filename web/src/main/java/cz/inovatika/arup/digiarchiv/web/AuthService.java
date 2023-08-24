package cz.inovatika.arup.digiarchiv.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import static cz.inovatika.arup.digiarchiv.web.fedora.FedoraUtils.auth_header;
import cz.inovatika.arup.digiarchiv.web.fedora.models.Uzivatel;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author alber
 */
public class AuthService {

    private static final String API_POINT = Options.getInstance().getString("auth");
    private static final HttpClient client = HttpClient.newHttpClient();
    
    public static String getToken(String user, String pwd){
        try {
            JSONObject body = new JSONObject().put("username", user).put("password", pwd);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(API_POINT + "token-auth/"))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            return response.body();
        } catch (Exception ex) {
            Logger.getLogger(AuthService.class.getName()).log(Level.SEVERE, null, ex);
            return ex.toString();
        }
    }

    public static String getUserInfo(String token) throws URISyntaxException, IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(new URI(API_POINT + "uzivatel-info/"))
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }
    
    

    public static JSONObject login(String user, String pwd) throws URISyntaxException, IOException, InterruptedException, Exception {
        String token = getToken(user, pwd);
        if (token != null) {
            String xml = getUserInfo(new JSONObject(token).getString("token"));
            Uzivatel uz = (Uzivatel) FedoraModel.parseXml(xml, Uzivatel.class);
            uz.setPristupnost();
            ObjectMapper objectMapper = new ObjectMapper();
            return new JSONObject(objectMapper.writeValueAsString(uz));
        } else {
            return new JSONObject().put("error", "");
        }
    }
}
