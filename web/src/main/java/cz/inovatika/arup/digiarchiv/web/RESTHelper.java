package cz.inovatika.arup.digiarchiv.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

public class RESTHelper {

    public static InputStream inputStream(String urlString) throws IOException {
        URLConnection uc = openConnection(urlString);
        HttpURLConnection hcon = (HttpURLConnection) uc;
        hcon.setInstanceFollowRedirects(true);
        return uc.getInputStream();
    }

    public static URLConnection openConnection(String urlString) throws MalformedURLException, IOException {
        URL url = new URL(urlString);
        URLConnection uc = url.openConnection();
        HttpURLConnection hcon = (HttpURLConnection) uc;
        hcon.setInstanceFollowRedirects(true);
        uc.setReadTimeout(Integer.parseInt("100000"));
        uc.setConnectTimeout(Integer.parseInt("10000"));
        return uc;
    }

    public static InputStream inputStream(String urlString, String user, String pass) throws IOException {
        URLConnection uc = openConnection(urlString, user, pass);
        return uc.getInputStream();
    }

    public static String toString(String urlString, String user, String pass) throws IOException {
        URLConnection uc = openConnection(urlString, user, pass);
        return IOUtils.toString(uc.getInputStream(), Charset.forName("UTF-8"));
    }

    public static URLConnection openConnection(String urlString, String user,
            String pass) throws MalformedURLException, IOException {
        URL url = new URL(urlString);
        URLConnection uc = url.openConnection();
        uc.setReadTimeout(Integer.parseInt("100000"));
        uc.setConnectTimeout(Integer.parseInt("10000"));
        String userPassword = user + ":" + pass;
        String encoded = Base64.encodeBase64String(userPassword.getBytes());
        uc.setRequestProperty("Authorization", "Basic " + encoded);
        return uc;
    }

}
