/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class SolrSearcher {

  public static final Logger LOGGER = Logger.getLogger(SolrSearcher.class.getName());

  public static void addLocationParams(HttpServletRequest request, SolrQuery query) {
    // query.setRows(200);
    String pristupnost = LoginServlet.pristupnost(request.getSession());
    if ("E".equals(pristupnost)) {
      pristupnost = "D";
    }
    String locField = "loc_rpt_" + pristupnost;
    if (request.getParameter("loc_rpt") != null) {
      // loc_rpt=48.93993884224734,12.204711914062502,50.64177902497231,18.7877197265625
      String[] coords = request.getParameter("loc_rpt").split(",");
      String geom = "[" + coords[1] + " " + coords[0] + " TO " + coords[3] + " " + coords[2] + "]";
      String fq = locField + ":[\"" + coords[1] + " " + coords[0] + "\" TO \"" + coords[3] + " " + coords[2] + "\"]";

      double dist = Math.max((Float.parseFloat(coords[3]) - Float.parseFloat(coords[1])) * .005, .02);
      query.setParam("facet.heatmap.geom", geom)
              .setParam("facet.heatmap.distErr", dist + "")
              .addFilterQuery(fq);

    } else {
      query.setParam("facet.heatmap.geom", "[\"12.30 48.50\" TO \"18.80 51.0\"]")
              .addFilterQuery(locField + ":[\"12.30 48.50\" TO \"18.80 51.0\"]")
              .setParam("facet.heatmap.distErr", "0.04");
    }
    query.setParam("facet.heatmap", "{!key=loc_rpt}" + locField)
            .setParam("facet.heatmap.maxCells", "400000");
  }

  public static void addCommonParams(HttpServletRequest request, SolrQuery query, String entity) throws IOException {
    
    query.addFilterQuery("{!tag=entityF}entity:" + entity);
    String q = "*:*";
    if (request.getParameter("q") != null) {
      q = request.getParameter("q");
    }
    query.setRequestHandler("/search");

    String pristupnost = LoginServlet.pristupnost(request.getSession());
    if ("E".equals(pristupnost)) {
      pristupnost = "D";
    }
    if ("C".compareTo(pristupnost) <= 0) {
      String organizace = LoginServlet.organizace(request.getSession());
      // Zohlednit organizace. Hledame full_text pro "D" + organizace
      q += " OR (text_all_D:(" + q + ") AND organizace:\"" + organizace + "\")";
    }
    query.setQuery(q);

    int rows = Options.getInstance().getClientConf().getInt("defaultRows");
    if (Boolean.parseBoolean(request.getParameter("mapa"))) {
      rows = 200;
    } else if (request.getParameter("rows") != null) {
      rows = Integer.parseInt(request.getParameter("rows"));
    }
    query.setRows(rows);
    int start = 0;
    if (request.getParameter("page") != null) {
      start = (Integer.parseInt(request.getParameter("page"))) * rows;
      query.setStart(start);
    }

    List<Object> facetFields = Options.getInstance().getJSONArray("securedFacets").toList();
    for (Object f : facetFields) {
      if (!((String) f).contains("okres") && !((String) f).equals("pristupnost")) {
        //query.addFacetField("{!ex=" + f + "F key=" + f + "}" + f + "_" + pristupnost);
        query.addFacetField("{!key=" + f + "}" + f + "_" + pristupnost);
      }
    }
    
    if (request.getParameter("sort") != null) {
      query.setParam("sort", request.getParameter("sort"));
    } else {
      JSONArray sorts = Options.getInstance().getClientConf().getJSONArray("sorts");
      for (Object s: sorts) {
        JSONObject sort = (JSONObject) s;
        if (!sort.has("entity")) {
          query.setParam("sort", sort.getString("field") + " " + sort.getString("dir"));
          break;
        }
        if (sort.has("entity") && sort.getJSONArray("entity").join(",").contains(entity)) {
          query.setParam("sort", sort.getString("field") + " " + sort.getString("dir"));
          break;
        }
        
      }
      
    }

    if (request.getParameter("inFavorites") != null) {
      String username = LoginServlet.userId(request);
      SolrQuery favQ = new SolrQuery("username:" + username).setRows(100);
      SolrDocumentList docs = SearchUtils.docs(favQ, Options.getInstance().getString("solrhost") + "favorites");
      List<String> ids = new ArrayList<>();
      for (SolrDocument doc : docs) {
        ids.add((String) doc.getFirstValue("docid"));
      }
      if (!ids.isEmpty()) {
        String fq = String.join(" OR ", ids);
        fq = "ident_cely:(" + fq + ")";
        query.addFilterQuery(fq);
      } else {
        query.addFilterQuery("ident_cely:emtpyrecord");
      }
    }
    query.set("facet", "true");
    //LOGGER.log(Level.INFO, "query: {0}", query );

  }

  private static void addFilterNoQuotes(SolrQuery query, String field, String[] values) {
    String fq = field + ":(";
    for (int i = 0; i < values.length; i++) {
      String[] parts = values[i].split(":");
      fq += (parts.length == 2 ? ops.get(parts[1]) : "") + parts[0] + " ";
    }
    fq = fq.trim() + ")";
    query.addFilterQuery(fq);
    // query.addFilterQuery("{!tag=" + field + "F}" + field + ":(" + String.join(" OR ", values) + ")");
  }

  static Map<String, String> ops = Map.of("or", "", "and", "+", "not", "-");

  private static void addFilter(SolrQuery query, String field, String[] values) {
    String fq = "{!tag=" + field + "F}" + field + ":(";
    for (int i = 0; i < values.length; i++) {
      // values[i] = "\"" + values[i] + "\"";
      String[] parts = values[i].split(":");
      fq += (parts.length == 2 ? ops.get(parts[1]) : "") + "\"" + parts[0] + "\" ";
    }
    fq += ")";
    // query.addFilterQuery("{!tag=" + field + "F}" + field + ":(" + String.join(" OR ", values) + ")");
    query.addFilterQuery(fq);

  }

  private static String getSearchField(String field) throws IOException {
    List<Object> advFields = Options.getInstance().getClientConf().getJSONArray("advancedFields").toList();
    for (Object s : advFields) {
      HashMap adv = (HashMap) s;
      if (adv.get("field").equals(field)) {
        return (String) (adv.containsKey("searchField") ? adv.get("searchField") : field);
      }
    }
    return null;
  }

  public static void addFilters(HttpServletRequest request, SolrQuery query, String pristupnost) throws IOException {
    List<Object> fields = Options.getInstance().getClientConf().getJSONArray("urlFields").toList();
    JSONArray filterFieldsObj = Options.getInstance().getClientConf().getJSONArray("filterFields");
    List<Object> dateFacets = Options.getInstance().getClientConf().getJSONArray("dateFacets").toList();
    List<Object> numberFacets = Options.getInstance().getClientConf().getJSONArray("numberFacets").toList();
    List<String> filterFields = new ArrayList<>();
    for (int i = 0; i < filterFieldsObj.length(); i++) {
      String type = filterFieldsObj.getJSONObject(i).getString("type");
      String field = filterFieldsObj.getJSONObject(i).getString("field");
      filterFields.add(field);
      if ("number".equals(type) || "rok".equals(type)) {
        numberFacets.add(field);
      } else if ("date".equals(type)) {
        dateFacets.add(field);
      }
    }
    
    for (String field : request.getParameterMap().keySet()) {
      if ((field.startsWith("f_")
              || filterFields.contains(field)
              || fields.contains(field)
              || numberFacets.contains(field)
              || dateFacets.contains(field)) && request.getParameter(field) != null) {

        if (field.equals("obdobi_poradi")) {
          String[] parts = request.getParameter(field).split(",");
          String fq = "obdobi_poradi:[" + parts[0] + " TO " + parts[1] + "]";
          query.addFilterQuery(fq);
        } else if (field.equals("rok_vzniku")) {
          String[] parts = request.getParameter(field).split(":")[0].split(",");
          String end = "*";
          if ("".equals(parts[0])) {
            parts[0] = "*";
          }
          if (parts.length > 1) {
            end = parts[1];
          }
          String fq = "rok_vzniku:[" + parts[0] + " TO " + end + "]";
          query.addFilterQuery(fq);
        } else if (dateFacets.contains(field)) {
          String[] parts = request.getParameter(field).split(":")[0].split(",");
          String fq = field + ":[" + parts[0] + "T00:00:00Z TO " + parts[1] + "T23:59:59Z]";
          query.addFilterQuery(fq);
        } else if (numberFacets.contains(field)) {
          String[] parts = request.getParameter(field).split(":")[0].split(",");
          String end = "*";
          if ("".equals(parts[0])) {
            parts[0] = "*";
          }
          if (parts.length > 1) {
            end = parts[1];
          }
          String fq = field + ":[" + parts[0] + " TO " + end + "]";
          query.addFilterQuery(fq);
        } else if (field.startsWith("f_typ_nalezu")) {
          addFilter(query, field + "_" + pristupnost, request.getParameterValues(field));
        } else if (field.startsWith("f_katastr")) {
          addFilter(query, field + "_" + pristupnost, request.getParameterValues(field));
        } else if (field.startsWith("f_areal")) {
          addFilter(query, field + "_" + pristupnost, request.getParameterValues(field));
        } else if (field.startsWith("f_aktivita")) {
          addFilter(query, field + "_" + pristupnost, request.getParameterValues(field));
        } else if (field.startsWith("f_kategorie")) {
          addFilter(query, field + "_" + pristupnost, request.getParameterValues(field));
        } else if (field.startsWith("f_druh_nalezu")) {
          addFilter(query, field + "_" + pristupnost, request.getParameterValues(field));
        } else if (field.startsWith("f_specifikace")) {
          addFilter(query, field + "_" + pristupnost, request.getParameterValues(field));
        } else if (field.startsWith("f_typ_vyzkumu")) {
          addFilter(query, field + "_" + pristupnost, request.getParameterValues(field));
        } else if (field.startsWith("f_dok_jednotka_typ")) {
          addFilter(query, field + "_" + pristupnost, request.getParameterValues(field));
        } else if (field.startsWith("f_adb_typ_sondy")) {
          addFilter(query, field + "_" + pristupnost, request.getParameterValues(field));
        } else if (field.startsWith("f_adb_podnet")) {
          addFilter(query, field + "_" + pristupnost, request.getParameterValues(field));
        } else if (field.startsWith("f_pian_")) {
          addFilter(query, field + "_" + pristupnost, request.getParameterValues(field));
        } else if (field.equals("f_obdobi")) {
          addFilter(query, field + "_" + pristupnost, request.getParameterValues(field));
        } else if (filterFields.contains(field)) {
          addFilterNoQuotes(query, field, request.getParameterValues(field));
        } else {
          addFilter(query, field, request.getParameterValues(field));
        }

      }
    }

    if (request.getParameter("loc_rpt") != null) {
      String locField = "loc_rpt_" + pristupnost;
      String[] coords = request.getParameter("loc_rpt").split(",");
      // String geom = "[" + coords[1] + " " + coords[0] + " TO " + coords[3] + " " + coords[2] + "]";
      String fq = locField + ":[\"" + coords[1] + " " + coords[0] + "\" TO \"" + coords[3] + " " + coords[2] + "\"]";
      double dist = Math.max((Float.parseFloat(coords[1]) - Float.parseFloat(coords[3])) * .005, .02);
      query.addFilterQuery(fq) //        .setParam("facet.heatmap.distErr", dist + "")
              //        .setParam("facet.heatmap.geom", geom)
              ;
    }
  }

  /**
   * Odstrani akce a lokalit z vysledku podle pristupnosti
   *
   * @param jo
   * @param pristupnost
   */
  public static void filter(JSONObject jo, String pristupnost) {
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      if (doc.has("lokalita_pristupnost")) {
        JSONArray lp = doc.getJSONArray("lokalita_pristupnost");
        for (int j = lp.length() - 1; j > -1; j--) {
          if (lp.getString(j).compareTo(pristupnost) > 0) {
            removeVal(doc, "lokalita_ident_cely", j);

            removeVal(doc, "lokalita_poznamka", j);
            removeVal(doc, "lokalita_katastr", j);
            removeVal(doc, "lokalita_typ_lokality", j);
            removeVal(doc, "lokalita_nazev", j);
            removeVal(doc, "lokalita_dalsi_katastry", j);
            removeVal(doc, "lokalita_stav", j);
            removeVal(doc, "lokalita_okres", j);
            removeVal(doc, "lokalita_druh", j);
            removeVal(doc, "lokalita_popis", j);

          }
        }
      }

      if (doc.has("akce_pristupnost")) {
        JSONArray lp = doc.getJSONArray("akce_pristupnost");
        for (int j = lp.length() - 1; j > -1; j--) {
          if (lp.getString(j).compareTo(pristupnost) > 0) {
            removeVal(doc, "akce_ident_cely", j);
            removeVal(doc, "akce_okres", j);
            removeVal(doc, "akce_katastr", j);
            removeVal(doc, "akce_dalsi_katastry", j);
            removeVal(doc, "akce_vedouci_akce", j);
            removeVal(doc, "akce_organizace", j);
            removeVal(doc, "akce_hlavni_typ", j);
            removeVal(doc, "akce_typ", j);
            removeVal(doc, "akce_vedlejsi_typ", j);
            removeVal(doc, "akce_datum_zahajeni_v", j);
            removeVal(doc, "akce_datum_ukonceni_v", j);
            removeVal(doc, "akce_lokalizace", j);
            removeVal(doc, "akce_poznamka", j);
            removeVal(doc, "akce_ulozeni_nalezu", j);
            removeVal(doc, "akce_vedouci_akce_ostatni", j);
            removeVal(doc, "akce_organizace_ostatni", j);
            removeVal(doc, "akce_stav", j);

          }
        }
      }

    }
  }

  public static void removeVal(JSONObject doc, String key, int j) {
    if (doc.optJSONArray(key) != null) {
      doc.getJSONArray(key).remove(j);
    }
  }

  public static String getPristupnostBySoubor(String id, String field) {
    try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {

      SolrQuery query = new SolrQuery("*").addFilterQuery("filepath:\"" + id + "\"").setRows(1).setFields("dokument", "samostatny_nalez");
      QueryResponse rsp = client.query("soubor", query);
      if (rsp.getResults().isEmpty()) {
        return null;
      } else {
        String dok = (String) rsp.getResults().get(0).getFirstValue("dokument");
        if (dok == null || "".equals(dok)) {
          dok = (String) rsp.getResults().get(0).getFirstValue("samostatny_nalez");
        }

        SolrQuery queryDok = new SolrQuery("*").addFilterQuery("ident_cely:\"" + dok + "\"").setRows(1).setFields("pristupnost");
        QueryResponse rsp2 = client.query("dokument", queryDok);
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

  public static JSONObject getDokBySoubor(String id) {
    try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {

      SolrQuery query = new SolrQuery("*").addFilterQuery("filepath:\"" + id + "\"").setRows(1).setFields("dokument", "samostatny_nalez");
      QueryResponse rsp = client.query("soubor", query);
      if (rsp.getResults().isEmpty()) {
        return null;
      } else {
        String dok = (String) rsp.getResults().get(0).getFirstValue("dokument");
        if (dok == null || "".equals(dok)) {
          dok = (String) rsp.getResults().get(0).getFirstValue("samostatny_nalez");
        }

        SolrQuery queryDok = new SolrQuery("*").addFilterQuery("ident_cely:\"" + dok + "\"").setRows(1).setFields("pristupnost,organizace");
        JSONObject jo = json(client, "entities", queryDok);
        if (jo.getJSONObject("response").optInt("numFound", 0) > 0) {
          return jo.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
        } else {
          return null;
        }
      }
    } catch (IOException | SolrServerException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return null;
    }
  }

  public static JSONObject json(SolrClient client, String core, SolrQuery query) throws SolrServerException, IOException {
    QueryRequest req = new QueryRequest(query);

    NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
    rawJsonResponseParser.setWriterType("json");
    req.setResponseParser(rawJsonResponseParser);

    NamedList<Object> resp = client.request(req, core);
    return new JSONObject((String) resp.get("response"));
  }

  public static JSONObject getById(HttpSolrClient client, String id, String fields) {
    try {
      SolrQuery query = new SolrQuery("ident_cely:\"" + id + "\"");
      query.setFields(fields).setRequestHandler("/search");
      JSONObject jo = SearchUtils.json(query, client, "entities");
      if (jo.getJSONObject("response").optInt("numFound", 0) > 0) {
        return jo.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
      }

    } catch (Exception ex) {
      LOGGER.log(Level.WARNING, "Error {0}", ex);
    }
    return null;
  }

  public static void addChildField(HttpSolrClient client, JSONObject doc, String idField, String newField, String queryFields) {
    if (doc.has(idField)) {
      Object obj = doc.get(idField);
      if (obj instanceof JSONArray) {
        JSONArray ids = doc.getJSONArray(idField);
        for (int a = 0; a < ids.length(); a++) {
          String id = ids.getString(a);
          JSONObject sub = SolrSearcher.getById(client, id, queryFields);
          if (sub != null) {
            doc.append(newField, sub);
          }
        }
      } else {
        JSONObject sub = SolrSearcher.getById(client, (String) obj, queryFields);
        if (sub != null) {
          doc.append(newField, sub);
        }
      }
    }
  }

  public static void addFavorites(JSONObject jo, HttpSolrClient client, HttpServletRequest request) {
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      if (LoginServlet.userId(request) != null) {
        addIsFavorite(client, doc, LoginServlet.userId(request));
      }
    }
  }

  public static void addIsFavorite(HttpSolrClient client, JSONObject doc, String username) {
    String ident_cely = doc.getString("ident_cely");
    SolrQuery query = new SolrQuery("uniqueid:" + username + "_" + ident_cely).setRows(1);
    JSONObject jo = SearchUtils.json(query, client, "favorites");
    if (jo.getJSONObject("response").optInt("numFound", 0) > 0) {
      doc.put("isFav", true);
    }
  }

  public static void addSecuredFieldFacets(String field, SolrInputDocument idoc, List<String> prSufix) {
    if (field.equals("loc_rpt")) {
      for (String sufix : prSufix) {
        idoc.addField("loc_rpt_" + sufix, idoc.getFieldValues(field));
      }
    }
    if (field.contains("areal")) {
      for (String sufix : prSufix) {
        idoc.addField("f_areal_" + sufix, idoc.getFieldValues(field));
      }
    }
    if (field.contains("_obdobi")) {
      for (String sufix : prSufix) {
        idoc.addField("f_obdobi_" + sufix, idoc.getFieldValues(field));
      }
    }
    if (field.equals("obdobi")) {
      for (String sufix : prSufix) {
        idoc.addField("f_obdobi_" + sufix, idoc.getFieldValues(field));
      }
    }
    if (field.equals("f_aktivita")) {
      for (String sufix : prSufix) {
        idoc.addField("f_aktivita_" + sufix, idoc.getFieldValues(field));
      }
    }
    if (field.equals("nalez_kategorie")) {
      for (String sufix : prSufix) {
        idoc.addField("f_kategorie_" + sufix, idoc.getFieldValues(field));
      }
    }
    if (field.contains("druh_nalezu")) {
      for (String sufix : prSufix) {
        addFieldNonRepeat(idoc, "f_druh_nalezu_" + sufix, idoc.getFieldValues(field));
      }
    }
    if (field.contains("specifikace") && !field.contains("specifikace_data")) {
      for (String sufix : prSufix) {
        idoc.addField("f_specifikace_" + sufix, idoc.getFieldValues(field));
      }
    }

    if (field.equals("okres")) {
      for (String sufix : prSufix) {
        idoc.addField("f_okres_" + sufix, idoc.getFieldValues(field));
      }
    }
    if (field.contains("katastr") && !field.equals("dalsi_katastry") && !field.contains("sort")) {
      for (String sufix : prSufix) {
        idoc.addField("f_katastr_" + sufix, idoc.getFieldValues(field));
      }
    }
    if (field.equals("f_typ_vyzkumu")) {
      for (String sufix : prSufix) {
        idoc.addField("f_typ_vyzkumu_" + sufix, idoc.getFieldValues(field));
      }
    }
    if (field.contains("typ_nalezu")) {
      for (String sufix : prSufix) {
        idoc.addField("f_typ_nalezu_" + sufix, idoc.getFieldValues(field));
      }
    }
    if (field.contains("adb_typ_sondy")) {
      for (String sufix : prSufix) {
        idoc.addField("f_adb_typ_sondy_" + sufix, idoc.getFieldValues(field));
      }
    }
    if (field.contains("adb_podnet")) {
      for (String sufix : prSufix) {
        idoc.addField("f_adb_podnet_" + sufix, idoc.getFieldValues(field));
      }
    }
    if (field.contains("pian_")) {
      for (String sufix : prSufix) {
        idoc.addField("f_" + field + "_" + sufix, idoc.getFieldValues(field));
      }
    }

    if (field.equals("dok_jednotka_typ")) {
      for (String sufix : prSufix) {
        idoc.addField("f_dok_jednotka_typ_" + sufix, idoc.getFieldValues(field));
      }
    }

  }
  
  public static void addJSONFields(JSONObject doc, String prefix, SolrInputDocument idoc) {
    for (String s : doc.keySet()) {
      switch (s) {
        case "_version_":
        case "_root_":
        case "indextime":
          break;
        default:
          SolrSearcher.addFieldNonRepeat(idoc, prefix + "_" + s, doc.optString(s));
      }
    }
  }

  public static void addFieldNonRepeat(SolrInputDocument idoc, String field, Object value) {
    if (idoc.getFieldValues(field) == null || !idoc.getFieldValues(field).contains(value)) {
      idoc.addField(field, value);
    }
  }

  public static void addSecuredFieldNonRepeat(SolrInputDocument idoc, String field, Object value, List<String> prSufix) {
    for (String sufix : prSufix) {
      String f = field + "_" + sufix;
      if (idoc.getFieldValues(f) == null || !idoc.getFieldValues(f).contains(value)) {
        idoc.addField(f, value);
      }
    }
  }

//  public static void cleanRepeated(SolrInputDocument idoc) {
//    for (String field : idoc.getFieldNames()) {
//      Collection<Object> vals = idoc.getFieldValues(field);
//      
//    }
//  }
}
