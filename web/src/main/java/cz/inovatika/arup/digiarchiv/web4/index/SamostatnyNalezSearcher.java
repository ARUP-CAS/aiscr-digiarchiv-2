package cz.inovatika.arup.digiarchiv.web4.index;

import cz.inovatika.arup.digiarchiv.web4.LoginServlet;
import cz.inovatika.arup.digiarchiv.web4.Options;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.SamostatnyNalez;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.jetty.HttpJettySolrClient;
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
    public void checkRelations(JSONObject jo, SolrClient client, HttpServletRequest request) {
        JSONArray docs = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            if (doc.has("samostatny_nalez_projekt") && !SolrSearcher.existsById(client, doc.getString("samostatny_nalez_projekt"))) {
                doc.remove("samostatny_nalez_projekt");
            }
        }
    }

    @Override
    public String[] getRelationsFields() {
        return new String[]{"ident_cely", "samostatny_nalez_projekt"};
    }

    @Override
    public void getChilds(JSONObject jo, SolrClient client, HttpServletRequest request) {
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
    public void processAsChild(HttpServletRequest request, JSONObject jo) {

    }

    @Override
    public JSONObject search(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        try (SolrClient client = new HttpJettySolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
            SolrQuery query = new SolrQuery("*");
            setQuery(request, query);
            JSONObject jo = SearchUtils.json(query, client, "entities");
            String pristupnost = LoginServlet.pristupnost(request.getSession());
            filter(jo, request);
            SolrSearcher.addFavorites(jo, client, request);
            return jo;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            json.put("error", ex);
        }
        return json;
    }

    @Override
    public JSONObject export(HttpServletRequest request) {
        try (SolrClient client = new HttpJettySolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
            SolrQuery query = new SolrQuery();
            setQuery(request, query);
            SolrSearcher.addExportParams(query, ENTITY, request.getParameter("rows"), request.getParameter("page"));
            JSONObject jo = SearchUtils.json(query, client, "entities");
            String pristupnost = LoginServlet.pristupnost(request.getSession());
            filter(jo, request);
            SolrSearcher.processExportDocs(jo.getJSONObject("response").getJSONArray("docs"), ENTITY);
            
            return jo;
            
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            return new JSONObject().put("error",ex.toString());
        }
    }

    @Override
    public String[] getSearchFields(String pristupnost) {
        List<Object> fields = Options.getInstance().getJSONObject("fields").getJSONArray("common").toList();
        List<Object> headerFields = Options.getInstance().getJSONObject("fields").getJSONObject("samostatny_nalez").getJSONArray("header").toList();
        List<Object> detailFields = Options.getInstance().getJSONObject("fields").getJSONObject("samostatny_nalez").getJSONArray("detail").toList();

        fields.addAll(headerFields);
        fields.addAll(detailFields);

        fields.add("samostatny_nalez_lokalizace:samostatny_nalez_lokalizace_" + pristupnost);
        fields.add("loc_rpt:loc_rpt_" + pristupnost);
        fields.add("loc:loc_rpt_" + pristupnost);
        fields.add("historie:[json]");

        String[] ret = fields.toArray(new String[0]);
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
     * Filter podle pristupnosti
     *
     * @param jo
     * @param request
     */
    @Override
    public void filter(JSONObject jo, HttpServletRequest request) {
      String pristupnost = LoginServlet.pristupnost(request.getSession());
      String org = LoginServlet.organizace(request.getSession());
        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        SamostatnyNalez sn = new SamostatnyNalez();
        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
            
            if (!isAllowed(LoginServlet.user(request), doc)) {
            //if (doc.getString("pristupnost").compareTo(pristupnost) > 0) {
                doc.remove("samostatny_nalez_chranene_udaje");
                doc.remove("katastr");
                doc.remove("f_katastr");
                doc.remove("samostatny_nalez_katastr_" + pristupnost);
            }
        }
    }
    
    public boolean isAllowed(JSONObject user, JSONObject doc) {
//-- A: stav = 4
//-- B: stav = 4 OR historie[typ_zmeny='SN01']/uzivatel = {user}.ident_cely
//-- C: stav = 4 OR historie[typ_zmeny='SN01']/uzivatel = {user}.ident_cely OR (projekt/organizace = {user}.organizace) OR (predano_organizace = {user}.organizace)
//-- D-E: bez omezení
        int stav = doc.optInt("stav");
        String userPr = user.optString("pristupnost", "A");
        
        // pristupnost uzivatele vetsi nez zaznamu
        if (doc.getString("pristupnost").compareTo(userPr) <= 0) {
          return true;
        }
        
        // uzivatel D/E
        if (userPr.compareToIgnoreCase("C") > 0) {
            return true;
        }
        
        String userId = user.optString("ident_cely", "A");
        String userOrg = "none";
        if (user.has("organizace")) {
            userOrg = user.getJSONObject("organizace").optString("id", "");
        }

        String projektId = doc.optString("samostatny_nalez_projekt");
        String sn_predano_organizace = doc.optString("samostatny_nalez_predano_organizace");
        
        JSONArray h = doc.getJSONArray("historie");
          String uzivatelSN01 = "KKK";

          for (int i = 0; i < h.length(); i++) {
            JSONObject hi = h.getJSONObject(i);
            if ("SN01".equals(hi.optString("typ_zmeny"))) {
              uzivatelSN01 = hi.getJSONObject("uzivatel").getString("id");
            }
          }
          
        
        for (int i = 0; i < h.length(); i++) {
          JSONObject hi = new JSONObject(h.get(i).toString());

          if ("SN01".equals(hi.optString("typ_zmeny"))) {
            uzivatelSN01 = hi.getJSONObject("uzivatel").getString("id");
          }
        }
        

        if (projektId == null) {
          SolrQuery query = new SolrQuery("ident_cely:\"" + doc.getString("ident_cely") + "\"")
                  .setFields("samostatny_nalez_projekt");
          JSONObject jsonS = SearchUtils.searchById(query, "entities", doc.getString("ident_cely"), false);
          if (jsonS.getJSONObject("response").getInt("numFound") > 0) {
              projektId = jsonS.getJSONObject("response").getJSONArray("docs").getJSONObject(0).optString("samostatny_nalez_projekt");
          }
        }

        String projektOrg = null;
        SolrQuery query = new SolrQuery("ident_cely:\"" + projektId + "\"")
                .setFields("projekt_organizace");
        JSONObject json = SearchUtils.searchById(query, "entities", projektId, false);
        if (json.getJSONObject("response").getInt("numFound") > 0) {
            projektOrg = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0).optString("projekt_organizace", "nonexist");
        }
        
        if (userPr.equalsIgnoreCase("C")) {
//-- C: stav = 4 OR historie[typ_zmeny='SN01']/uzivatel = {user}.ident_cely OR (projekt/organizace = {user}.organizace) OR (predano_organizace = {user}.organizace)
            return (userId.equals(uzivatelSN01) || (userOrg.equals(projektOrg)) || (userOrg.equals(sn_predano_organizace)));
        } else if (userPr.equalsIgnoreCase("B")) {
//-- B: stav = 4 OR historie[typ_zmeny='SN01']/uzivatel = {user}.ident_cely
            return userId.equals(uzivatelSN01);
        } else {
            return false;
        }
        
        
    }
}
