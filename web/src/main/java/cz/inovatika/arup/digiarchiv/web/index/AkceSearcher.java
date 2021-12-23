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
  public String[] getSearchFields(String pristupnost) {
    return new String[]{"ident_cely","katastr","f_katastr:katastr","okres","f_okres:okres","vedouci_akce", "loc","entity","datestamp",
      "specifikace_data", "datum_zahajeni", "datum_ukonceni", "je_nz", "pristupnost" ,
      "organizace","f_organizace:organizace","dalsi_katastry","lokalizace","vazba_projekt","child_dokument",
      "hlavni_typ","f_hlavni_typ:hlavni_typ","vedlejsi_typ","f_vedlejsi_typ:vedlejsi_typ", 
      "vedouci_akce_ostatni","organizace_ostatni","uzivatelske_oznaceni","ulozeni_nalezu","poznamka",
      "dok_jednotka:[json],pian:[json],adb:[json],vazba_projekt_akce:[json],dokument:[json],projekt:[json],ext_zdroj:[json]",
      "komponenta:[json],komponenta_dokument:[json],neident_akce:[json],aktivita:[json]"};
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
      query.setFields("ident_cely,entity,vedouci_akce,organizace,pristupnost,loc_rpt,pian:[json],katastr,okres");
    } else{
      query.setFields("ident_cely","katastr","f_katastr:katastr","okres","f_okres:okres","vedouci_akce", "loc","entity","datestamp",
      "specifikace_data", "datum_zahajeni", "datum_ukonceni", "je_nz", "pristupnost" ,
      "organizace","f_organizace:organizace","dalsi_katastry","lokalizace","vazba_projekt","child_dokument",
      "hlavni_typ","f_hlavni_typ:hlavni_typ","vedlejsi_typ","f_vedlejsi_typ:vedlejsi_typ", "vedouci_akce_ostatni","organizace_ostatni","uzivatelske_oznaceni","ulozeni_nalezu","poznamka",
      "dok_jednotka:[json],pian:[json],adb:[json],vazba_projekt_akce:[json],dokument:[json],projekt:[json],ext_zdroj:[json]");
     }
  }
  
}
