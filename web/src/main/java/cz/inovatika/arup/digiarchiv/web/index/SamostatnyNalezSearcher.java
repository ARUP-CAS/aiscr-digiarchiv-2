
package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;

import java.io.IOException;
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
public class SamostatnyNalezSearcher implements EntitySearcher {

  public static final Logger LOGGER = Logger.getLogger(SamostatnyNalezSearcher.class.getName());
  final String ENTITY = "samostatny_nalez";
  
  @Override
  public String[] getChildSearchFields(String pristupnost) {
    return this.getSearchFields(pristupnost);
  }

  @Override
  public void getChilds(JSONObject jo, Http2SolrClient client, HttpServletRequest request) {
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      if (LoginServlet.userId(request) != null) {
        SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
      }
      String fields = "ident_cely,katastr,okres,vedouci_projektu,typ_projektu,datum_zahajeni,datum_ukonceni,organizace_prihlaseni,dalsi_katastry,podnet,pian_id,pian:[json]";
      SolrSearcher.addChildField(client, doc, "projekt", "valid_projekt", fields);
    }
  }

  @Override
  public JSONObject search(HttpServletRequest request) {
    JSONObject json = new JSONObject();
    try (Http2SolrClient client = new Http2SolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
      SolrQuery query = new SolrQuery("*");
      setQuery(request, query);
      JSONObject jo = SearchUtils.json(query, client, "entities");

      String pristupnost = LoginServlet.pristupnost(request.getSession());
      filter(jo, pristupnost, LoginServlet.organizace(request.getSession()));
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
    String[] ret = new String[]{"ident_cely, datestamp, entity, stav, typ, inv_cislo, projekt, okres, hloubka, poznamka, nalezove_okolnosti, pristupnost",
            "obdobi, presna_datace, druh, specifikace, pocet, nalezce, datum_nalezu, predano, predano_organizace", "predmet_kategorie", 
            "datum_vlozeni, odpovedny_pracovnik_archivace, datum_archivace, child_soubor, soubor_filepath",
            "soubor:[json]", "katastr:f_katastr_" + pristupnost,  
            "lokalizace:f_lokalizace_" + pristupnost, 
            "f_katastr:f_katastr_" + pristupnost, 
            "loc_rpt:loc_rpt_" + pristupnost, "loc:loc_rpt_" + pristupnost, 
            "lat:lat_" + pristupnost, "lng:lng_" + pristupnost};
    return ret;
  } 

  private void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
    SolrSearcher.addCommonParams(request, query, ENTITY);
    String pristupnost = LoginServlet.pristupnost(request.getSession());
    if ("E".equals(pristupnost)) {
      pristupnost = "D";
    }
    query.set("df", "text_all_" + pristupnost);
    query.setFields(getSearchFields(pristupnost));

    SolrSearcher.addFilters(request, query, pristupnost);
    if (Boolean.parseBoolean(request.getParameter("mapa"))) {
      SolrSearcher.addLocationParams(request, query);
    }
  }

  /**
   * Filter katastr podle pristupnosti
   *
   * @param jo
   * @param pristupnost
   */
  public void filter(JSONObject jo, String pristupnost, String org) {
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      if (doc.getString("pristupnost").compareTo(pristupnost) > 0) {
        doc.remove("katastr");
        doc.remove("f_katastr");
        doc.remove("f_katastr_" + pristupnost);
      }
    }
  }
}
