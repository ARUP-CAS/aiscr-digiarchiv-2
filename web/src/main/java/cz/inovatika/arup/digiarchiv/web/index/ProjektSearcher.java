package cz.inovatika.arup.digiarchiv.web.index;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class ProjektSearcher implements EntitySearcher {

  public static final Logger LOGGER = Logger.getLogger(ProjektSearcher.class.getName());
  final String ENTITY = "projekt";
  
  private final List<String> allowedFields = Arrays.asList(new String[]{"ident_cely", "entity", "pristupnost", "vedouci_projektu", "okres", "organizace_prihlaseni", "datestamp",
    "typ_projektu", "datum_zahajeni", "datum_ukonceni", "podnet", "child_akce", "child_samostatny_nalez"});
  
  @Override
  public void filter(JSONObject jo, String pristupnost, String org) {
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      String docPr = doc.getString("pristupnost");
      
      if (doc.getString("pristupnost").compareTo(pristupnost) > 0) {
        Object[] keys =  doc.keySet().toArray();
        for (Object key : keys) {
          if (!allowedFields.contains((String)key)) {
            doc.remove((String)key);
          }
          
        }
      }

      if (docPr.compareTo(pristupnost) > 0) {
        doc.remove("chranene_udaje");
        
//        doc.remove("katastr");
//        doc.remove("dalsi_katastry");
//        doc.remove("loc");
//        doc.remove("lat");
//        doc.remove("lng");
//        doc.remove("pian");
//        doc.remove("parent_akce_katastr");
//        doc.remove("dok_jednotka");
//        
//        Object[] keys = doc.keySet().toArray();
//        for (Object okey : keys) {
//          String key = (String) okey;
//          if (key.endsWith("_D") && "D".compareTo(pristupnost) > 0) {
//            doc.remove((String) key);
//          }
//          if (key.endsWith("_C") && "C".compareTo(pristupnost) > 0) {
//            doc.remove((String) key);
//          }
//          if (key.endsWith("_B") && "B".compareTo(pristupnost) > 0) {
//            doc.remove((String) key);
//          }
//
//        }

      }
      
      if (doc.has("location_info")) {
        JSONArray lp = doc.getJSONArray("location_info");
        for (int j = lp.length() - 1; j > -1; j--) {
          if (lp.getJSONObject(j).has("pristupnost") && lp.getJSONObject(j).getString("pristupnost").compareTo(pristupnost) > 0) {
            lp.remove(j);// .getJSONObject(j).remove("location_info");
          }
        }
      }

    }
    
  }
  
  @Override
  public String[] getChildSearchFields(String pristupnost) {
    return new String[]{"ident_cely,pristupnost,okres,vedouci_projektu,typ_projektu,datum_zahajeni,datum_ukonceni,organizace_prihlaseni,podnet", 
            "katastr:f_katastr_" + pristupnost,  
            "dalsi_katastry:f_dalsi_katastry_" + pristupnost};
  }

    @Override
    public void checkRelations(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
    }

    @Override
    public String[] getRelationsFields() {
        return new String[]{"dokument", "projekt"};
    }
  
  @Override
  public void getChilds(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
      
      PIANSearcher ps = new PIANSearcher();
        String pristupnost = LoginServlet.pristupnost(request.getSession());
        if ("E".equals(pristupnost)) {
            pristupnost = "D";
        }
        String[] fs = ps.getSearchFields(pristupnost);
        String pfields = String.join(",", fs);
        
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      if (LoginServlet.userId(request) != null) {
        SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
      }
      String fields = "ident_cely,pristupnost,entity,"
              + "katastr,"
              + "okres,vedouci_akce,specifikace_data,datum_zahajeni,datum_ukonceni,je_nz,pristupnost,"
              + "organizace.dalsi_katastry,lokalizace,pian:[json]";
      SolrSearcher.addChildFieldByEntity(client, doc, "archeologicky_zaznam", fields);

      fields = "ident_cely,pristupnost,katastr,okres,nalezce,datum_nalezu,typ_dokumentu,material_originalu,rada,pristupnost,obdobi,presna_datace,druh,specifikace,soubor_filepath";
      SolrSearcher.addChildField(client, doc, "samostatny_nalez", "valid_samostatny_nalez", fields, "searchable:true");
      if (doc.has("pian_id")) {
        JSONArray cdjs = doc.getJSONArray("pian_id");
        for (int j = 0; j < cdjs.length(); j++) {
          String cdj = cdjs.getString(j);
          JSONObject sub = SolrSearcher.getById(client, cdj, pfields);
          if (sub != null) {
            doc.append("pian", sub);
          }
          
        }
      }
    }
  }

  @Override
  public JSONObject search(HttpServletRequest request) {
    JSONObject json = new JSONObject();
    try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
      SolrQuery query = new SolrQuery();
      setQuery(request, query);
      JSONObject jo = SearchUtils.json(query, client, "entities");
      SolrSearcher.addFavorites(jo, client, request);
      return jo;

    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      json.put("error", ex);
    }
    return json;
  }

  @Override
  public String export(HttpServletRequest request) {
    try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
      SolrQuery query = new SolrQuery();
      setQuery(request, query);
      return SearchUtils.csv(query, client, "entities");
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return ex.toString();
    }
  }
  
  @Override
  public String[] getSearchFields(String pristupnost) {
    
    return new String[]{"okres",
      "dalsi_katastry:f_dalsi_katastry_" + pristupnost,
      //"hlavni_katastr:f_katastr_" + pristupnost,
      "katastr:f_katastr_" + pristupnost,
      "entity",
      "ident_cely",
      "stav",
      "typ_projektu",
      "podnet",
      "planovane_zahajeni",
      "vedouci_projektu",
      "organizace",
      "uzivatelske_oznaceni",
      "oznaceni_stavby",
      "kulturni_pamatka",
      "datum_zahajeni",
      "datum_ukonceni",
      "termin_odevzdani_nz",
      "pristupnost",
      "chranene_udaje:[json]",
      "datestamp",
      "oznamovatel",
      "soubor",
      "archeologicky_zaznam",
      "pian_id",
      "samostatny_nalez",
      "dokument",
      "lat:lat_" + pristupnost,
      "lng:lng_" + pristupnost,
      "loc_rpt:loc_rpt_" + pristupnost,
      "loc:loc_" + pristupnost};
  }

  public void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
    SolrSearcher.addCommonParams(request, query, ENTITY);
    String pristupnost = LoginServlet.pristupnost(request.getSession());
    if ("E".equals(pristupnost)) {
      pristupnost = "D";
    }
    query.set("df", "text_all_" + pristupnost);
    query.setFields(getSearchFields(pristupnost));
    if (Boolean.parseBoolean(request.getParameter("mapa"))) {
      SolrSearcher.addLocationParams(request, query);
    }
    SolrSearcher.addFilters(request, query, pristupnost);
  }

}
