package cz.inovatika.arup.digiarchiv.web.index;

import cz.inovatika.arup.digiarchiv.web.GPSconvertor;
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
public class PIANSearcher implements EntitySearcher{
  
  public static final Logger LOGGER = Logger.getLogger(PIANSearcher.class.getName());
  
  final String ENTITY = "pian";
  
  @Override
  public void getChilds(JSONObject jo, HttpSolrClient client, HttpServletRequest request) {
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
          String fields = "ident_cely,entity,katastr,okres,vedouci_akce,specifikace_data,datum_zahajeni,datum_ukonceni,je_nz,pristupnost,organizace,dalsi_katastry,lokalizace"
          +",nazev,typ_lokality,druh,popis";
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      if (LoginServlet.userId(request) != null) {
        SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
      }
      
      /**
       * Reálně budou zobrazeny akce/lokalitity, 
       * DJ tuto vazbu jen zprostředkovává. 
       * Lze řešit buď děděním, nebo ořezem, 
       * protože ident_cely DJ je odvozen z ident_cely akce/lokality 
       * (nutno oříznout poslední 4 znaky).
       * 
        "child_dok_jednotka":["X-M-9700709A-D01"]},
      {
        "child_dok_jednotka":["M-201800815A-D01"]},
      {
        "child_dok_jednotka":["C-9000059A-D01"]},
        * 
       */
      if (doc.has("child_dok_jednotka")) {
        JSONArray cdjs = doc.getJSONArray("child_dok_jednotka");
        for (int j = 0; j < cdjs.length(); j++) {
          String cdj = cdjs.getString(j);
          cdj = cdj.substring(0, cdj.length() - 4);
          
          JSONObject sub = SolrSearcher.getById(client, cdj, fields);
          if (sub != null) {
            doc.append(sub.getString("entity"), sub);
          }
          
        }
      }
      
    }
  }

  @Override
  public JSONObject search(HttpServletRequest request) {
    JSONObject json = new JSONObject();
    try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
      SolrQuery query = new SolrQuery()
              .setFacet(true);
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

  public void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
    SolrSearcher.addCommonParams(request, query, ENTITY);
    String pristupnost = LoginServlet.pristupnost(request.getSession());
    if ("E".equals(pristupnost)) {
      pristupnost = "D";
    }
    query.set("df", "text_all_" + pristupnost);
    if (Boolean.parseBoolean(request.getParameter("mapa"))) {
      SolrSearcher.addLocationParams(request, query);
    }
    
    SolrSearcher.addFilters(request, query, pristupnost);
  }
  
  
  public JSONObject getMapPians(HttpServletRequest request) {
    JSONObject json = new JSONObject();
    // Menime entity
    try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
      SolrQuery query = new SolrQuery(); 
      String entity = "" + request.getParameter("entity");
      SolrSearcher.addCommonParams(request, query, entity);
      String pristupnost = LoginServlet.pristupnost(request.getSession());
      if ("E".equals(pristupnost)) {
        pristupnost = "D";
      }
      query.set("df", "text_all_" + pristupnost);
      if (Boolean.parseBoolean(request.getParameter("mapa"))) {
        SolrSearcher.addLocationParams(request, query);
      }
      SolrSearcher.addFilters(request, query, pristupnost);
      query.setFacet(false).setRequestHandler("/select");
      query.set("defType", "edismax");
      query.setFields("pian:[json],ident_cely,organizace,pristupnost,loc_rpt");
      query.setRows(Math.min(Options.getInstance().getInt("maxDocsForCluster", 5000), Integer.parseInt(request.getParameter("rows"))));
      
      JSONObject jo = SearchUtils.json(query, client, "entities");
      SolrSearcher.addFavorites(jo, client, request);
      return jo;

    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      json.put("error", ex);
    }
    return json;
  }
  
}
