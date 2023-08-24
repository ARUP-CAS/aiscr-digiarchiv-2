/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.xmlrpc.XmlRpcException;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcTransport;
import org.apache.xmlrpc.client.XmlRpcTransportFactory;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class AmcrAPI {

  public static final Logger LOGGER = Logger.getLogger(AmcrAPI.class.getName());

  JSONObject conf;

  private String URL = "http://kryton.smartgis.cz/~smetak/isamcr-p-5-8-21/xmlrpc/0/?t=600";
  private String USER = "testuser";
  private String PWD = "testpwd";

  private String sid = null;
  private boolean logged;

  public boolean connect() throws Exception {
    if (sid == null) {
      Options opts = Options.getInstance();
      conf = opts.getJSONObject("amcrapi");
      URL = conf.getString("url");
      USER = conf.getString("user");
      PWD = conf.getString("pwd");

      sid = getSid();
      logged = login() == 1;
    }
    return logged;

  }

  private String sha1(String s) {
    try {
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
      byte[] array = md.digest(s.getBytes());
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < array.length; ++i) {
        sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException ex) {
      Logger.getLogger(AmcrAPI.class.getName()).log(Level.SEVERE, null, ex);
      return s;
    }
  }

  private void raw() throws MalformedURLException {
    final XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
    config.setServerURL(new URL(URL));

    final XmlRpcClient client = new XmlRpcClient();

    final XmlRpcTransportFactory transportFactory = new XmlRpcTransportFactory() {
      public XmlRpcTransport getTransport() {
        return new MessageLoggingTransport(client);
      }
    };
    client.setTransportFactory(transportFactory);
    client.setConfig(config);
  }

  private Object sendRequest(String method, Object[] params) throws Exception {
    try {

      XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
      config.setServerURL(new URL(URL));
      final XmlRpcClient client = new XmlRpcClient();
      client.setConfig(config);
      return client.execute(method, params);

    } catch (MalformedURLException | XmlRpcException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      throw new Exception(ex);
    }
  }

  private String getSid() throws Exception {
    return (String) sendRequest("get_sid", new Object[]{});
  }

  private Integer login() throws Exception {
    int resp = (Integer) sendRequest("login", new Object[]{sid, USER, sha1(PWD), "deprecated", false, "{\"api\":1}"});
    if (resp == 1) {
      LOGGER.log(Level.INFO, "login success");
    } else {
      LOGGER.log(Level.SEVERE, "admin login fail for {0}", USER);
    }
    return resp;
  }

  public JSONObject login(String user, String pwd) throws Exception {
    LOGGER.log(Level.INFO, "logining " +user);
    if (connect()) {
      int resp = (Integer) sendRequest("login", new Object[]{sid, user, sha1(pwd), "deprecated", false, "{\"api\":1}"});
      if (resp == 1) {
        LOGGER.log(Level.INFO, "login success");
        return getUserInfo();
      } else {
        LOGGER.log(Level.WARNING, "login fail for {0} with resp {1}", new Object[]{user, resp});
        return new JSONObject().put("error", "login fail");
      }
    } else {
      return new JSONObject().put("error", "login fail."); 
    }
  }
  

  private JSONObject getUserInfo() throws Exception {
    JSONObject ja = new JSONObject();

    if (connect()) {
      Object o = sendRequest("get_current_user", new Object[]{sid});
      if (o instanceof HashMap) {
        Map map = (Map) o;
        if (!map.containsKey("onlyForDebugging") || !map.get("onlyForDebugging").equals(1)) {
          JSONObject jo1 = new JSONObject(map);

          ja.put(map.get("id") + "", jo1);
        } else {
          LOGGER.log(Level.FINE, "{0}", o);
        }

        return ja;
      }
      Object[] os = (Object[]) o;

      for (Object ob : os) {

        if (ob instanceof HashMap) {
          Map map = (Map) ob;
          if (!map.containsKey("onlyForDebugging") || !map.get("onlyForDebugging").equals(1)) {
            JSONObject jo1 = new JSONObject(map);

            ja.put(map.get("id") + "", jo1);
          } else {
            LOGGER.log(Level.FINE, "{0}", ob);
          }

        } else {
          LOGGER.log(Level.FINE, "{0}", ob);
        }

      }

      LOGGER.log(Level.INFO, "Found {0} docs", ja.length());

    }
    return ja;

  }

  public JSONObject getHeslar(String id) throws Exception {
    JSONObject ja = new JSONObject();

    if (connect()) {
      Object o = sendRequest("get_list", new Object[]{sid, id});
      Object[] os = (Object[]) o;

      for (Object ob : os) {

        if (ob instanceof HashMap) {
          Map map = (Map) ob;
          if (!map.containsKey("onlyForDebugging") || !map.get("onlyForDebugging").equals(1)) {
            JSONObject jo1 = new JSONObject(map);

            ja.put(map.get("id") + "", jo1);
          } else {
            LOGGER.log(Level.FINE, "{0}", ob);
          }

        } else {
          LOGGER.log(Level.FINE, "{0}", ob);
        }

      }

      LOGGER.log(Level.INFO, "Found {0} docs", ja.length());

    }
    return ja;

  }
}
