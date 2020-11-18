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
public class AkceSearcher implements EntitySearcher{
  
  public static final Logger LOGGER = Logger.getLogger(AkceSearcher.class.getName());
  
  final String ENTITY = "akce";
  
  @Override
  public void getChilds(JSONObject jo, HttpSolrClient client, HttpServletRequest request) {
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      if (LoginServlet.userId(request) != null) {
        SolrSearcher.addIsFavorite(client, doc, LoginServlet.userId(request));
      }
      String fields = "ident_cely,katastr,okres,autor,rok_vzniku,typ_dokumentu,material_originalu,pristupnost,rada,material_originalu,organizace,popis,soubor_filepath";
      SolrSearcher.addChildField(client, doc, "child_dokument", "dokument", fields);
      
      fields = "ident_cely,katastr,okres,vedouci_projektu,typ_projektu,datum_zahajeni,datum_ukonceni,organizace_prihlaseni,dalsi_katastry,podnet";
      SolrSearcher.addChildField(client, doc, "vazba_projekt", "projekt", fields);
    }
  }

  @Override
  public JSONObject search(HttpServletRequest request) {
    JSONObject json = new JSONObject();
    try (HttpSolrClient client = new HttpSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
      String handler = LoginServlet.isLogged(request.getSession()) ? "/search" : "/search";
      SolrQuery query = new SolrQuery()
              .setFacet(true)
              .setRequestHandler(handler);
      setQuery(request, query);
      JSONObject jo = SearchUtils.json(query, client, "entities");
      //  getChilds(jo, client, request);
      return jo;

    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      json.put("error", ex);
    }
    return json;
  }

  public void setQuery(HttpServletRequest request, SolrQuery query) throws IOException {
    SolrSearcher.addCommonParams(request, query);
    query.addFilterQuery("{!tag=entityF}entity:" + ENTITY);
    String pristupnost = LoginServlet.pristupnost(request.getSession());
    if ("E".equals(pristupnost)) {
      pristupnost = "D";
    }
    query.set("df", "text_all_" + pristupnost);
    SolrSearcher.addFilters(request, query, pristupnost);
    
    if (Boolean.parseBoolean(request.getParameter("mapa"))) {
      SolrSearcher.addLocationParams(request, query);
    }

    query.setFields("ident_cely","katastr","okres","vedouci_akce", "loc",
    "specifikace_data", "datum_zahajeni", "datum_ukonceni", "je_nz", "pristupnost" ,
    "organizace","dalsi_katastry","lokalizace","vazba_projekt","child_dokument",
    "hlavni_typ","vedlejsi_typ","vedouci_akce_ostatni","organizace_ostatni","uzivatelske_oznaceni","ulozeni_nalezu","poznamka",
    "dok_jednotka:[json],pian:[json],adb:[json],vazba_projekt_akce:[json],dokument:[json],projekt:[json],ext_zdroj:[json]");
  }
  
}
