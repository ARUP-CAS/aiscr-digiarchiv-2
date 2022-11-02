
package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class DokumentSearcher implements EntitySearcher {

  public static final Logger LOGGER = Logger.getLogger(DokumentSearcher.class.getName());
  
  final String ENTITY = "dokument";

  @Override
  public JSONObject search(HttpServletRequest request) {
    JSONObject json = new JSONObject();
    try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
      SolrQuery query = new SolrQuery();
      setQuery(request, query);
      JSONObject jo = SearchUtils.json(query, client, "entities");
      SolrSearcher.addFavorites(jo, client, request); 
      // getChilds(jo, client, request);
      String pristupnost = LoginServlet.pristupnost(request.getSession());
      filter(jo, pristupnost, LoginServlet.organizace(request.getSession()));
      return jo;
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      json.put("error", ex);
    }
    return json;
  }

  @Override
  public String export(HttpServletRequest request) {
    try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
      SolrQuery query = new SolrQuery();
      setQuery(request, query);
      return SearchUtils.csv(query, client, "entities");
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return ex.toString();
    }
  }
  
  @Override
  public String[] getChildSearchFields(String pristupnost) {
    String[] f = new String[]{"ident_cely,pristupnost,katastr,okres,autor,rok_vzniku,typ_dokumentu,material_originalu,pristupnost,rada,material_originalu,organizace,popis,soubor_filepath,location_info:[json]"};
    //if (pristupnost)
    return f;
  } 
  
  @Override
  public void getChilds(JSONObject jo, HttpSolrClient client, HttpServletRequest request) {
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    String fieldsAkce = "ident_cely,katastr,okres,vedouci_akce,specifikace_data,datum_zahajeni,datum_ukonceni,je_nz,pristupnost,organizace,dalsi_katastry,lokalizace";
    String fieldsLok = "ident_cely,katastr,okres,nazev,typ_lokality,druh,pristupnost,dalsi_katastry,popis";  
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
//      if (LoginServlet.userId(request) != null) {
//        SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
//      } 
      SolrSearcher.addChildField(client, doc, "jednotka_dokumentu_vazba_akce", "akce", fieldsAkce);
      SolrSearcher.addChildField(client, doc, "jednotka_dokumentu_vazba_druha_akce", "akce", fieldsAkce);
      
      SolrSearcher.addChildField(client, doc, "jednotka_dokumentu_vazba_lokalita", "lokalita", fieldsLok);
      SolrSearcher.addChildField(client, doc, "jednotka_dokumentu_vazba_druha_lokalita", "lokalita", fieldsLok);
    }
    
    
  }
  
  @Override
  public String[] getSearchFields(String pristupnost) {
//    return new String[]{"*,neident_akce:[json],dok_jednotka:[json],pian:[json],adb:[json],soubor:[json],jednotka_dokumentu:[json],let:[json],nalez_dokumentu:[json],komponenta_dokument:[json],tvar:[json],location_info:[json]",
//              "okres","f_okres"};
    
    String[] f = new String[]{"*,neident_akce:[json],dok_jednotka:[json],pian:[json],adb:[json],soubor:[json],jednotka_dokumentu:[json],let:[json],nalez_dokumentu:[json],komponenta_dokument:[json],tvar:[json],location_info:[json]",
              "okres","f_okres"};
    // if (pristupnost)
    return f;
  }

  public void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
    SolrSearcher.addCommonParams(request, query, ENTITY);
    String pristupnost = LoginServlet.pristupnost(request.getSession());
    if ("E".equals(pristupnost)) {
      pristupnost = "D";
    }
    SolrSearcher.addFilters(request, query, pristupnost);
    query.set("df", "text_all_A");
    
    if (Boolean.parseBoolean(request.getParameter("mapa"))) {
      SolrSearcher.addLocationParams(request, query);
    } 
    if (Boolean.parseBoolean(request.getParameter("mapa")) && request.getParameter("format") == null) {
      query.setFields("ident_cely,entity,autor,rok_vzniku,organizace,pristupnost,loc_rpt,pian:[json],f_katastr,f_okres,location_info:[json]");
    } else {
      query.setFields("*,neident_akce:[json],dok_jednotka:[json],pian:[json],adb:[json],soubor:[json],jednotka_dokumentu:[json],let:[json],nalez_dokumentu:[json],komponenta_dokument:[json],tvar:[json]",
              "okres","f_okres","location_info:[json]");
    }
    
  }
  
  /**
   * Odstrani akce a lokalit z vysledku podle pristupnosti
   *
   * @param jo
   * @param pristupnost
   */
  @Override
  public void filter(JSONObject jo, String pristupnost, String org) {
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      String organizace = doc.getString("organizace");
      boolean sameOrg = org.toLowerCase().equals(organizace.toLowerCase()) && "C".compareTo(pristupnost) >= 0;
      if (doc.has("neident_akce")) {
        doc.put("f_katastr", doc.getJSONArray("neident_akce_katastr").toList());
      }
      if (doc.has("lokalita")) {
        JSONArray lp = doc.getJSONArray("lokalita");
        for (int j = lp.length() - 1; j > -1; j--) {
          if (lp.getJSONObject(j).getString("pristupnost").compareTo(pristupnost) > 0 && !sameOrg) {
            // removeVal(doc, "lokalita", j);
            lp.getJSONObject(j).remove("katastr");
          } else {
            doc.append("f_katastr", lp.getJSONObject(j).getString("katastr"));
          }
        }
      }

      if (doc.has("akce")) {
        JSONArray lp = doc.getJSONArray("akce");
        for (int j = lp.length() - 1; j > -1; j--) {
          if (lp.getJSONObject(j).getString("pristupnost").compareTo(pristupnost) > 0 && !sameOrg) {
            // removeVal(doc, "akce", j);
            lp.getJSONObject(j).remove("katastr");
          } else {
            doc.append("f_katastr", lp.getJSONObject(j).getString("katastr"));
          }
        }
      }
      if (doc.has("location_info")) {
        JSONArray lp = doc.getJSONArray("location_info");
        for (int j = lp.length() - 1; j > -1; j--) {
          if (lp.getJSONObject(j).getString("pristupnost").compareTo(pristupnost) > 0 && !sameOrg) {
            lp.remove(j);// .getJSONObject(j).remove("location_info");
          } 
        }
      }
      
 
    }
  }

  public void removeVal(JSONObject doc, String key, int j) {
    if (doc.optJSONArray(key) != null) {
      doc.getJSONArray(key).remove(j);
    }
  }

  public String getPristupnostBySoubor(String id, String field) {
    try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {

      SolrQuery query = new SolrQuery("*").addFilterQuery("filepath:\"" + id + "\"").setRows(1).setFields("dokument", "samostatny_nalez");
      QueryResponse rsp = client.query("relations", query);
      if (rsp.getResults().isEmpty()) {
        return null;
      } else {
        String dok = (String) rsp.getResults().get(0).getFirstValue("dokument");
        if (dok == null || "".equals(dok)) {
          dok = (String) rsp.getResults().get(0).getFirstValue("samostatny_nalez");
        }

        SolrQuery queryDok = new SolrQuery("*").addFilterQuery("ident_cely:\"" + dok + "\"").setRows(1).setFields("pristupnost");
        QueryResponse rsp2 = client.query("entities", queryDok);
        if (rsp2.getResults().isEmpty()) {
          return null;
        } else {
          return (String) rsp2.getResults().get(0).getFirstValue("pristupnost");
        }
      }
    } catch (IOException | SolrServerException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return null;
    }
  }
}
