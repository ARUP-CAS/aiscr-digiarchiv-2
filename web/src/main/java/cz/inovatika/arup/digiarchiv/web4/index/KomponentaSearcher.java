package cz.inovatika.arup.digiarchiv.web4.index;

import cz.inovatika.arup.digiarchiv.web4.LoginServlet;
import cz.inovatika.arup.digiarchiv.web4.Options;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

public class KomponentaSearcher implements ComponentSearcher, EntitySearcher {

  public static final Logger LOGGER = Logger.getLogger(KomponentaSearcher.class.getName());

  final String ENTITY = "komponenta";
  private boolean parentSearchable;

  @Override
  public void getRelatedInHandle(JSONObject jo, SolrClient client, HttpServletRequest request) {
    getRelated(jo, client, request, true);
  }
  
  public void getRelated(JSONObject jo, SolrClient client, HttpServletRequest request, boolean inHandle) {

    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
      try {
        JSONObject doc = ja.getJSONObject(i);

        DokumentSearcher ds = new DokumentSearcher("dokument");
        String dfs = String.join(",", ds.getChildSearchFields("D"));
        SolrQuery query = new SolrQuery("*")
                .addFilterQuery("entity:dokument")
                .addFilterQuery("komponenta_dokument_ident_cely:\"" + doc.getString("ident_cely") + "\"");
        query.setFields(dfs);

        JSONObject r = inHandle ? SolrSearcher.jsonSelect(client, "entities", query) : SolrSearcher.json(client, "entities", query);
        ds.filter(r, LoginServlet.pristupnost(request.getSession()), LoginServlet.organizace(request.getSession()));
        JSONArray reldocs = r.getJSONObject("response").getJSONArray("docs");
        for (int j = 0; j < reldocs.length(); j++) {
          JSONObject cdj = reldocs.getJSONObject(j);
          doc.append("dokument", cdj);
          doc.put("datestamp", cdj.getString("datestamp"));
        }

        String ident_cely = doc.getString("ident_cely");
        query = new SolrQuery("*").addFilterQuery("komponenta_ident_cely:\"" + ident_cely + "\"");
        AkceSearcher as = new AkceSearcher();
        query.setFields(as.getChildSearchFields("A"));
        try {
          JSONObject sub = inHandle ? SolrSearcher.jsonSelect(client, "entities", query) : SolrSearcher.json(client, "entities", query);
          JSONArray subs = sub.getJSONObject("response").getJSONArray("docs");

          for (int j = 0; j < subs.length(); j++) {
            doc.append(subs.getJSONObject(i).getString("entity"), subs.getJSONObject(i));
            doc.put("datestamp", subs.getJSONObject(i).getString("datestamp"));
          }
          parentSearchable = true;

        } catch (SolrServerException | IOException ex) {
          Logger.getLogger(DokJednotkaSearcher.class.getName()).log(Level.SEVERE, null, ex);
        }
      } catch (SolrServerException ex) {
        Logger.getLogger(KomponentaSearcher.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
        Logger.getLogger(KomponentaSearcher.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  @Override
  public boolean isRelatedSearchable() {
    return parentSearchable;
  }

  @Override
  public void processAsChild(HttpServletRequest request, JSONObject jo) {

  }

  @Override
  public JSONObject search(HttpServletRequest request) {
    JSONObject json = new JSONObject();
    try (SolrClient client = new HttpJdkSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
      SolrQuery query = new SolrQuery()
              .setFacet(true);
      setQuery(request, query);
      JSONObject jo = SearchUtils.json(query, client, "entities");
      SolrSearcher.addFavorites(jo, client, request);
      addPians(jo, client, request);
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
    query.setFields(getSearchFields(pristupnost));
    if (Boolean.parseBoolean(request.getParameter("mapa"))) {
      SolrSearcher.addLocationParams(request, query);
    }

    SolrSearcher.addFilters(request, query, pristupnost);
  }

  @Override
  public String export(HttpServletRequest request) {
    try (SolrClient client = new HttpJdkSolrClient.Builder(Options.getInstance().getString("solrhost")).build()) {
            SolrQuery query = new SolrQuery();
            setQuery(request, query);
            SolrSearcher.addExportParams(query, ENTITY);
            JSONObject jo = SearchUtils.json(query, client, "entities");
            String pristupnost = LoginServlet.pristupnost(request.getSession());
            filter(jo, pristupnost, LoginServlet.organizace(request.getSession()));
            SolrSearcher.processExportDocs(jo.getJSONObject("response").getJSONArray("docs"), ENTITY);
            String format = request.getParameter("format");
            if (format == null) {
              format = "json";
            }
            switch (format) {
              case "csv":
              case "xlsx":  
                List<String> labels = SolrSearcher.getExportField(ENTITY, "label");
                JSONArray ls = new JSONArray(labels);
                String ret = org.json.CDL.rowToString(new JSONArray(labels));
                try {
                  ret += org.json.CDL.toString(ls, jo.getJSONObject("response").getJSONArray("docs"));
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error", ex);
                    return ex.toString();
                }
                return ret;
              case "xml": 
                return "<?xml version=\"1.0\" encoding=\"utf-8\"?><docs>" + XML.toString(jo.getJSONObject("response").getJSONArray("docs"), "doc") + "</docs>";
              case "json": 
                return jo.toString();
              default: 
                return SearchUtils.json(query, client, "entities").toString();
            }
            
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return ex.toString();
        }
  }

  @Override
  public String[] getSearchFields(String pristupnost) {
    //return new String[]{"*,komponenta_aktivita:[json],komponenta_areal:[json],komponenta_obdobi:[json],komponenta_nalez_objekt:[json],komponenta_nalez_predmet:[json]"};

    List<Object> fields = Options.getInstance().getJSONObject("fields").getJSONArray("common").toList();
    List<Object> headerFields = Options.getInstance().getJSONObject("fields").getJSONObject("komponenta").getJSONArray("header").toList();
    List<Object> detailFields = Options.getInstance().getJSONObject("fields").getJSONObject("komponenta").getJSONArray("detail").toList();

    fields.addAll(headerFields);
    fields.addAll(detailFields);

    fields.add("pian_id:az_dj_pian");
    fields.add("loc_rpt:loc_rpt_" + pristupnost);
    fields.add("loc:loc_rpt_" + pristupnost);
    fields.add("katastr:f_katastr_" + pristupnost);
    fields.add("okres:f_okres");

    String[] ret = fields.toArray(new String[0]);
    return ret;

  }

  @Override
  public void filter(JSONObject jo, String pristupnost, String org) {
    JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
    for (int i = 0; i < ja.length(); i++) {
      JSONObject doc = ja.getJSONObject(i);
      String organizace = doc.optString("akce_organizace");
      String docPr = doc.getString("pristupnost");

      boolean sameOrg = org.toLowerCase().equals(organizace.toLowerCase()) && "C".compareTo(pristupnost) >= 0;
      if (docPr.compareToIgnoreCase(pristupnost) > 0 && !sameOrg) {
        doc.remove("chranene_udaje");
        doc.remove("az_chranene_udaje");
        doc.remove("akce_chranene_udaje");
      }
    }
  }

  @Override
  public void getChilds(JSONObject jo, SolrClient client, HttpServletRequest request) {
    getRelated(jo, client, request, false);
    addPians(jo, client, request);
  }

  @Override
  public String[] getChildSearchFields(String pristupnost) {
    return getSearchFields(pristupnost);
  }

  @Override
  public String[] getRelationsFields() {
    return new String[]{"*,komponenta_aktivita:[json],komponenta_obdobi:[json]"};
  }

  @Override
  public void checkRelations(JSONObject jo, SolrClient client, HttpServletRequest request) {
    getRelated(jo, client, request, false);
  }
  
  public void addPians(JSONObject jo, SolrClient client, HttpServletRequest request) {
        String pristupnost = LoginServlet.pristupnost(request.getSession());
        if ("E".equals(pristupnost)) {
            pristupnost = "D";
        }
        PIANSearcher ps = new PIANSearcher();
        String[] fs = ps.getSearchFields(pristupnost);
        String fields = String.join(",", fs);

        JSONArray ja = jo.getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject doc = ja.getJSONObject(i);
            if (doc.has("pian_ident_cely")) {
                JSONArray cdjs = doc.getJSONArray("pian_ident_cely");
                
                String[] pians = (String[]) cdjs.toList().toArray(String[]::new);
        
                SolrQuery query = new SolrQuery("ident_cely:(\"" + String.join("\" OR \"", pians ) + "\")")
                    .addFilterQuery("entity:pian")
                    .setSort("ident_cely", SolrQuery.ORDER.asc)
                        .setFields(fields)
                    .setParam("stats", false)
                    .setFacet(false);
                JSONObject joPians = SearchUtils.json(query, client, "entities");
                doc.put("pian", joPians.getJSONObject("response").getJSONArray("docs"));
            }
        }
    }

}
