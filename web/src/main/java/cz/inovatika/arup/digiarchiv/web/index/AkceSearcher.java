package cz.inovatika.arup.digiarchiv.web.index;

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
public class AkceSearcher implements EntitySearcher {

  public static final Logger LOGGER = Logger.getLogger(AkceSearcher.class.getName());

  final String ENTITY = "akce";

  private final List<String> allowedFields = Arrays.asList(new String[]{"ident_cely", "entity", "pristupnost", "child_dokument",
    "specifikace_data", "datum_zahajeni", "datum_ukonceni", "je_nz", "organizace", "hlavni_vedouci", "okres", "datestamp"});

  @Override
  public void filter(JSONObject jo, String pristupnost, String org) {
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      if (doc.getString("pristupnost").compareTo(pristupnost) > 0) {
        doc.remove("chranene_udaje");
//        Object[] keys =  doc.keySet().toArray();
//        for (Object key : keys) {
//          if (!allowedFields.contains((String)key)) {
//            doc.remove((String)key);
//          }
//          
//        }
      }
    }
  }

  @Override
  public void getChilds(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
    PIANSearcher ps = new PIANSearcher();
    String pristupnost = LoginServlet.pristupnost(request.getSession());
    if ("E".equals(pristupnost)) {
      pristupnost = "D";
    }
    String[] fs = ps.getSearchFields(pristupnost);
    String fields = String.join(",", fs);
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      if (doc.has("pian_id")) {
        JSONArray cdjs = doc.getJSONArray("pian_id");
        for (int j = 0; j < cdjs.length(); j++) {
          String cdj = cdjs.getString(j);
          JSONObject sub = SolrSearcher.getById(client, cdj, fields);
          if (sub != null) {
            doc.append("pian", sub);
          }
          
        }
      }
      
//      if (LoginServlet.userId(request) != null) {
//        SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
//      }
//      String fields = "ident_cely,entity,pristupnost,katastr,okres,autor,rok_vzniku,typ_dokumentu,material_originalu,pristupnost,rada,material_originalu,organizace,popis,soubor_filepath";
//      SolrSearcher.addChildField(client, doc, "child_dokument", "dokument", fields);
//
//      fields = "ident_cely,pristupnost,katastr,okres,vedouci_projektu,typ_projektu,datum_zahajeni,datum_ukonceni,organizace_prihlaseni,dalsi_katastry,podnet";
//
//      SolrSearcher.addChildField(client, doc, "vazba_projekt", "projekt", fields);
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
    return new String[]{"ident_cely", "katastr", "f_katastr:katastr", "okres", "f_okres:okres", "hlavni_vedouci", "loc", "entity", "datestamp",
      "specifikace_data", "datum_zahajeni", "datum_ukonceni", "je_nz", "pristupnost",
      "organizace", "f_organizace:organizace", "vazba_projekt", "child_dokument",
      "hlavni_typ", "f_hlavni_typ:hlavni_typ", "vedlejsi_typ", "f_vedlejsi_typ:vedlejsi_typ",
      "vedouci_akce_ostatni", "organizace_ostatni", "uzivatelske_oznaceni:uzivatelske_oznaceni_"+pristupnost, "ulozeni_nalezu", "poznamka",
      "pian:[json],adb:[json],projekt:[json],dokument:[json],projekt:[json],ext_zdroj:[json]",
      "komponenta:[json],komponenta_dokument:[json],neident_akce:[json],aktivita:[json]",
      "dokumentacni_jednotka_pian",
      "dokumentacni_jednotka:[json]",
      "chranene_udaje:[json]",
      "lat:lat_" + pristupnost,
      "lng:lng_" + pristupnost,
      "loc_rpt:loc_rpt_" + pristupnost,
      "loc:loc_rpt_" + pristupnost,
      "lokalizace:lokalizace_okolnosti_" + pristupnost,
      "dalsi_katastry:f_dalsi_katastry_" + pristupnost};
  }

  @Override
  public String[] getChildSearchFields(String pristupnost) {
    //  return getSearchFields(pristupnost);
    return new String[]{"ident_cely,entity,pristupnost,okres,hlavni_vedouci,specifikace_data,datum_zahajeni,datum_ukonceni,je_nz,pristupnost,organizace",
      "lokalizace:lokalizace_okolnosti_" + pristupnost,
      "katastr:f_katastr_" + pristupnost,
      "dalsi_katastry:f_dalsi_katastry_" + pristupnost};
  }

  public void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
    SolrSearcher.addCommonParams(request, query, ENTITY);
    String pristupnost = LoginServlet.pristupnost(request.getSession());
    if ("E".equals(pristupnost)) {
      pristupnost = "D";
    }
    query.set("df", "text_all_" + pristupnost);
    SolrSearcher.addFilters(request, query, pristupnost);

    if (Boolean.parseBoolean(request.getParameter("mapa"))) {
      SolrSearcher.addLocationParams(request, query);
    }

    if (Boolean.parseBoolean(request.getParameter("mapa")) && request.getParameter("format") == null) {
      query.setFields("ident_cely,entity,hlavni_vedouci,organizace,pristupnost,pian:[json],katastr,okres,child_dokument,vazba_projekt",
      "dokumentacni_jednotka_pian",
      "dokumentacni_jednotka:[json]",
      "chranene_udaje:[json]",
      "lat:lat_" + pristupnost,
      "lng:lng_" + pristupnost,
      "loc_rpt:loc_rpt_" + pristupnost,
      "loc:loc_" + pristupnost,
      "lokalizace:lokalizace_okolnosti_" + pristupnost,
      "dalsi_katastry:f_dalsi_katastry_" + pristupnost);

    } else {
      query.setFields(getSearchFields(pristupnost));
    }
  }

}
