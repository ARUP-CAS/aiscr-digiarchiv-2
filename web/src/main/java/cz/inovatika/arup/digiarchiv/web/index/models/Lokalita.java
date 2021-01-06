package cz.inovatika.arup.digiarchiv.web.index.models;

import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class Lokalita implements Entity {

  static final Logger LOGGER = Logger.getLogger(Lokalita.class.getName());

  @Field
  public String ident_cely;

  @Field
  public int stav;

  @Field
  public String okres;

  @Field
  public String katastr;

  @Field
  public String dalsi_katastry;

  @Field
  public String typ_lokality;

  @Field
  public String druh;

  @Field
  public String nazev;

  @Field
  public String popis;

  @Field
  public String poznamka;

  @Field
  public String uzivatelske_oznaceni;

  @Field
  public String pristupnost;

  @Field
  public Date datum_zapisu;

  @Field
  public String odpovedny_pracovnik_zapisu;

  @Field
  public Date datum_archivace;

  @Field
  public String odpovedny_pracovnik_archivace;

  @Field
  public List<String> dok_jednotka;

  @Field
  public List<String> ext_zdroj;

  @Field
  public List<String> child_dok_jednotka;

  @Field
  public List<String> child_dokument;

  @Field
  public List<String> child_ext_zdroj;
  
  String[] sufixes = new String[]{"A","B","C","D"};
  String[] dokFields = new String[]{"f_areal", "f_obdobi", "f_aktivita", "f_kategorie", "f_druh_nalezu", "f_specifikace"};
  

  @Override
  public void fillFields(SolrInputDocument idoc) {

    boolean searchable = stav == 3;
    idoc.setField("searchable", searchable);

    if (dalsi_katastry != null) {
      String[] kats = dalsi_katastry.split(";");
      for (String kat : kats) {
        String[] parts = kat.split("\\(");
        String kt = parts[0];
        String ok = parts[1].substring(0, parts[1].length() - 1);
        SolrSearcher.addFieldNonRepeat(idoc, "dalsi_katastr", kt.trim());
        SolrSearcher.addFieldNonRepeat(idoc, "dalsi_okres",ok.trim());
//        idoc.addField("dalsi_katastr", kt.trim());
//        idoc.addField("dalsi_okres", ok.trim());
      }
      idoc.setField("dalsi_katastry", kats);
    }

    if (nazev != null) {
      idoc.addField("nazev_sort", nazev);
    }
    
    if (druh != null) {
      idoc.addField("f_druh_lokality", druh);
    }

    if (okres != null) {
      String okres_sort = okres;
      if (katastr != null) {
        okres_sort += " " + katastr;
        SolrSearcher.addFieldNonRepeat(idoc, "katastr_sort", katastr);
      }
      idoc.addField("okres_sort", okres_sort);
    }

  }

  @Override
  public void addRelations(HttpSolrClient client, SolrInputDocument idoc) {
    if (this.child_dok_jednotka != null) {
      addDokJednotka(client, idoc);
    }
    if (this.child_ext_zdroj != null) {
      addExterniZdroj(client, idoc);
    }
    
    if (child_dokument != null) {
      // Tady dohledame pres dokument udaje pro sdilene facety
      /*
      potřebujeme z relevantní jednotka_dokument (tj. ta, 
      přes kterou jde propojení dokument-lokalita) přenést údaje z 
      komponenta_dokument a nalez_dokument 
      (tj. areál, období, aktivita, kategorie/druh/specifikace nálezu)
      */
      processDokument(client, idoc);
    }
  }
  
  @Override
  public void secondRound(HttpSolrClient client, SolrInputDocument idoc) {
    
    if (child_dokument != null) {
      // Tady dohledame pres dokument udaje pro sdilene facety
      /*
      potřebujeme z relevantní jednotka_dokument (tj. ta, 
      přes kterou jde propojení dokument-lokalita) přenést údaje z 
      komponenta_dokument a nalez_dokument 
      (tj. areál, období, aktivita, kategorie/druh/specifikace nálezu)
      */
      processDokument(client, idoc);
    }
  }

  
  private void processDokument(HttpSolrClient client, SolrInputDocument idoc) {
           
    for (String dok : this.child_dokument) {
      SolrQuery query = new SolrQuery("ident_cely:\"" + dok + "\"")
              // .setFields("f_areal", "f_obdobi", "f_aktivita", "f_kategorie", "f_druh_nalezu", "f_specifikace", "nalez_kategorie");
              .setFields("jednotka_dokumentu:[json]");
      
      JSONObject json = SearchUtils.json(query, client, "entities");
      if (json.getJSONObject("response").getInt("numFound") > 0) {
        JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
        JSONArray ja = doc.getJSONArray("jednotka_dokumentu");
        for (int i = 0; i < ja.length(); i++) {
          JSONObject jd = ja.getJSONObject(i);
          if (jd.has("vazba_lokalita") && ident_cely.equals(jd.getString("vazba_lokalita"))) {
            SearchUtils.addJSONFields(jd, "jednotka_dokumentu", idoc);
          }
        }
      }
    }
  }

  private void addExterniZdroj(HttpSolrClient client, SolrInputDocument idoc) {
    for (String dok : this.child_ext_zdroj) {

      SolrQuery query = new SolrQuery("ident_cely:\"" + dok + "\"")
              .setFields("*,ext_odkaz:[json]");
      JSONObject json = SearchUtils.json(query, client, "entities");
      if (json.getJSONObject("response").getInt("numFound") > 0) {
        JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
        SolrSearcher.addFieldNonRepeat(idoc, "ext_zdroj", doc.toString());
        if (doc.has("rok_vydani_vzniku")) {
          String r = doc.getString("rok_vydani_vzniku");
          if (StringUtils.isNumeric(r)) {
            SolrSearcher.addFieldNonRepeat(idoc, "rok_vydani", doc.optString("rok_vydani_vzniku"));
          }
        }
      }
    }
  }

  private void addNalez(SolrInputDocument idoc, JSONObject json) {
    Object nalez = json.get("nalez");
    if (nalez instanceof JSONArray) {
      JSONArray ja = (JSONArray) nalez;
      for (int i = 0; i < ja.length(); i++) {
        SolrSearcher.addJSONFields(ja.getJSONObject(i), "nalez", idoc);
//        SolrSearcher.addFieldNonRepeat(idoc, "nalez_druh_nalezu", ja.getJSONObject(i).optString("druh_nalezu"));
//        SolrSearcher.addFieldNonRepeat(idoc, "nalez_typ_nalezu", ja.getJSONObject(i).optString("typ_nalezu"));
//        SolrSearcher.addFieldNonRepeat(idoc, "nalez_kategorie", ja.getJSONObject(i).optString("kategorie"));
//        SolrSearcher.addFieldNonRepeat(idoc, "nalez_specifikace", ja.getJSONObject(i).optString("specifikace"));
      }
    } else {
      SolrSearcher.addJSONFields((JSONObject) nalez, "nalez", idoc);
//      SolrSearcher.addFieldNonRepeat(idoc, "nalez_druh_nalezu", ((JSONObject) nalez).optString("druh_nalezu"));
//      SolrSearcher.addFieldNonRepeat(idoc, "nalez_typ_nalezu", ((JSONObject) nalez).optString("typ_nalezu"));
//      SolrSearcher.addFieldNonRepeat(idoc, "nalez_kategorie", ((JSONObject) nalez).optString("kategorie"));
//      SolrSearcher.addFieldNonRepeat(idoc, "nalez_specifikace", ((JSONObject) nalez).optString("specifikace"));
    }
  } 

  private void addDokJednotka(HttpSolrClient client, SolrInputDocument idoc) {
    this.dok_jednotka = new ArrayList<>();
    for (String dok : this.child_dok_jednotka) {

      SolrQuery query = new SolrQuery("ident_cely:\"" + dok + "\"")
              .setFields("*,komponenta:[json],pian:[json],adb:[json]");
      JSONObject json = SearchUtils.json(query, client, "entities");
      if (json.getJSONObject("response").getInt("numFound") > 0) {
        JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
        SolrSearcher.addFieldNonRepeat(idoc, "dok_jednotka", doc.toString());
        for (String s : doc.keySet()) {
          if ("komponenta".equals(s)) {
            JSONArray ja = doc.getJSONArray("komponenta");
            for (int i = 0; i < ja.length(); i++) {
              JSONObject k = ja.getJSONObject(i);
              for (String fk : k.keySet()) {
                SolrSearcher.addFieldNonRepeat(idoc, "komponenta_" + fk, k.optString(fk));
                if (fk.contains("aktivita") && k.optString(fk).equals("1")) {
                  SolrSearcher.addFieldNonRepeat(idoc, "f_aktivita", fk.substring("aktivita_".length()));
                } else if ("nalez".equals(fk)) {
                  addNalez(idoc, k);
                }
              }

              if (k.has("obdobi")) {
                SolrSearcher.addFieldNonRepeat(idoc, "obdobi_poradi", SearchUtils.getObdobiPoradi(k.getString("obdobi")));
              }
            }
          } else {
            SolrSearcher.addFieldNonRepeat(idoc, "dok_jednotka_" + s, doc.optString(s));
          }
        }
        if (doc.has("pian")) {
          JSONObject pianDoc;
          Object p = doc.get("pian");
          if (p instanceof JSONArray) {
            // System.out.println(field + "; " + id + "; " + doc.getJSONArray("pian"));
            JSONArray pians = (JSONArray) p;
            pianDoc = pians.getJSONObject(0);
            for (int pian = 0; pian < pians.length(); pian++) {
              SolrSearcher.addFieldNonRepeat(idoc, "pian_id", pians.getJSONObject(pian).optString("ident_cely"));
              SolrSearcher.addFieldNonRepeat(idoc, "pian_typ", pians.getJSONObject(pian).optString("typ"));
              SolrSearcher.addFieldNonRepeat(idoc, "pian_presnost", pians.getJSONObject(pian).optString("presnost"));
              SolrSearcher.addFieldNonRepeat(idoc, "pian_zm10", pians.getJSONObject(pian).optString("zm10"));
            }

          } else {
            pianDoc = (JSONObject) p;
            SolrSearcher.addFieldNonRepeat(idoc, "pian_id", pianDoc.optString("ident_cely"));
            SolrSearcher.addFieldNonRepeat(idoc, "pian_typ", pianDoc.optString("typ"));
            SolrSearcher.addFieldNonRepeat(idoc, "pian_presnost", pianDoc.optString("presnost"));
            SolrSearcher.addFieldNonRepeat(idoc, "pian_zm10", pianDoc.optString("zm10"));
          }
          if (pianDoc.has("centroid_n")) {
            String loc = pianDoc.optString("centroid_n") + "," + pianDoc.optString("centroid_e");
              SolrSearcher.addFieldNonRepeat(idoc, "lat", pianDoc.optString("centroid_n"));
              SolrSearcher.addFieldNonRepeat(idoc, "lng", pianDoc.optString("centroid_e"));
            SolrSearcher.addFieldNonRepeat(idoc, "loc", loc);
            SolrSearcher.addFieldNonRepeat(idoc, "loc_rpt", loc);
          }
          SolrSearcher.addFieldNonRepeat(idoc, "pian", pianDoc.toString());

        }
      }
    }
  }

  @Override
  public void setFullText(SolrInputDocument idoc) {
    List<Object> indexFields = Options.getInstance().getJSONObject("indexFieldsByType").getJSONArray("lokalita").toList();
    String pristupnost = (String) idoc.getFieldValue("pristupnost");
    List<String> prSufix = new ArrayList<>();

    if ("A".compareTo(pristupnost) >= 0) {
      prSufix.add("A");
    }
    if ("B".compareTo(pristupnost) >= 0) {
      prSufix.add("B");
    }
    if ("C".compareTo(pristupnost) >= 0) {
      prSufix.add("C");
    }
    if ("D".compareTo(pristupnost) >= 0) {
      prSufix.add("D");
    }

    Object[] fields = idoc.getFieldNames().toArray();
    for (Object f : fields) {
      String s = (String) f;
      SolrSearcher.addSecuredFieldFacets(s, idoc, prSufix);
      
      if (indexFields.contains(s)) {
        for (String sufix : prSufix) {
          SolrSearcher.addFieldNonRepeat(idoc, "text_all_" + sufix, idoc.getFieldValues(s));
        }
      } 
      
//      if (indexFields.contains(s)) {
//        for (String sufix : prSufix) {
//          idoc.addField("text_all_" + sufix, idoc.getFieldValues(s));
//        }
//      } 
    }

    // Fields allways searchable
    String[] defFields = new String[]{"ident_cely", "okres",
      "typ_lokality", "druh"};
    for (String field : defFields) {
      Object[] vals = idoc.getFieldValues(field).toArray();
      idoc.addField("text_all_A", vals);
      idoc.addField("text_all_B", vals);
      idoc.addField("text_all_C", vals);
    }
  } 

  @Override
  public boolean isEntity() {
    return true;
  }
}
