package cz.inovatika.arup.digiarchiv.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.fedora.models.Uzivatel;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author alber
 */
public class AuthService {
    public static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());
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
            String r = response.body();
            return r;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
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
            try {
                JSONObject tokenJSON = new JSONObject(token);
                if (tokenJSON.has("token")) {
                    String xml = getUserInfo(new JSONObject(token).getString("token"));
                    Uzivatel uz = (Uzivatel) FedoraModel.parseXml(xml, Uzivatel.class);
                    uz.setPristupnost();
                    uz.setCteniDokumentu();
                    ObjectMapper objectMapper = new ObjectMapper();
                    return new JSONObject(objectMapper.writeValueAsString(uz));
                } else if (tokenJSON.has("non_field_errors")) {
                    return new JSONObject().put("error", "dialog.alert.Špatné přihlašovací údaje");
                } else {
                    return tokenJSON;
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
                return new JSONObject().put("error", "dialog.alert.login_server_error");
            }
        } else {
            return new JSONObject().put("error", "invalid token");
        }
    }
    
    
//    public static JSONObject loginBasic(HttpServletRequest req) throws URISyntaxException, IOException, InterruptedException, Exception {
//        String token = getToken(user, pwd);
//        if (token != null) {
//            String xml = getUserInfo(new JSONObject(token).getString("token"));
//            Uzivatel uz = (Uzivatel) FedoraModel.parseXml(xml, Uzivatel.class);
//            uz.setPristupnost();
//            ObjectMapper objectMapper = new ObjectMapper();
//            return new JSONObject(objectMapper.writeValueAsString(uz));
//        } else {
//            return new JSONObject().put("error", "");
//        }
//    }
}
