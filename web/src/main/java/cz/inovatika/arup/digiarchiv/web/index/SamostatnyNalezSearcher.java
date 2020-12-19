/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class SamostatnyNalezSearcher implements EntitySearcher {

  public static final Logger LOGGER = Logger.getLogger(SamostatnyNalezSearcher.class.getName());
  final String ENTITY = "samostatny_nalez";

  @Override
  public void getChilds(JSONObject jo, HttpSolrClient client, HttpServletRequest request) {
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      if (LoginServlet.userId(request) != null) {
        SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
      }
      String fields = "ident_cely,katastr,okres,vedouci_projektu,typ_projektu,datum_zahajeni,datum_ukonceni,organizace_prihlaseni,dalsi_katastry,podnet,pian_id,pian:[json]";
      SolrSearcher.addChildField(client, doc, "projekt_id", "projekt", fields);
//      if (doc.has("projekt")) {
//        JSONObject obj = doc.getJSONArray("projekt").getJSONObject(0);
//        doc.put("pian", obj.opt("pian"));
//        doc.put("pian_id", obj.opt("pian_id"));
//      }
    }
  }

  @Override
  public JSONObject search(HttpServletRequest request) {
    JSONObject json = new JSONObject();
    try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
      SolrQuery query = new SolrQuery("*");
      setQuery(request, query);
      JSONObject jo = SearchUtils.json(query, client, "entities");

      String pristupnost = LoginServlet.pristupnost(request.getSession());
      filter(jo, pristupnost);
      SolrSearcher.addFavorites(jo, client, request);
      return jo;

    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      json.put("error", ex);
    }
    return json;
  }

  private void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
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
  }

  /**
   * Odstrani akce a lokalit z vysledku podle pristupnosti
   *
   * @param jo
   * @param pristupnost
   */
  private void filter(JSONObject jo, String pristupnost) {
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

  private void removeVal(JSONObject doc, String key, int j) {
    if (doc.optJSONArray(key) != null) {
      doc.getJSONArray(key).remove(j);
    }
  }

}
