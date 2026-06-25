package cz.inovatika.arup.digiarchiv.web4.fedora.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web4.Options;
import cz.inovatika.arup.digiarchiv.web4.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web4.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web4.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web4.index.SolrSearcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@JsonInclude(Include.NON_NULL)
public class Komponenta implements FedoraModel {

  @Field
  public String entity = "komponenta";

  @Field
  public boolean searchable;

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
  @JacksonXmlProperty(localName = "ident_cely")
  @Field
  public String ident_cely;

//<xs:element name="obdobi" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{obdobi.ident_cely}" | "{obdobi.heslo}" -->
  @JacksonXmlProperty(localName = "obdobi")
  public Vocab komponenta_obdobi;

//<xs:element name="jistota" minOccurs="0" maxOccurs="1" type="xs:boolean"/> <!-- "{jistota}" -->
  @JacksonXmlProperty(localName = "jistota")
  public String jistota;

  @Field
  public Boolean komponenta_jistota;

//<xs:element name="presna_datace" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{presna_datace}" -->
  @JacksonXmlProperty(localName = "presna_datace")
  @Field
  public String komponenta_presna_datace;

//<xs:element name="areal" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{areal.ident_cely}" | "{areal.heslo}" -->
  @JacksonXmlProperty(localName = "areal")
  public Vocab komponenta_areal;

//<xs:element name="aktivita" minOccurs="0" maxOccurs="unbounded" type="amcr:vocabType"/> <!-- "{aktivity.ident_cely}" | "{aktivity.heslo}" -->
  @JacksonXmlProperty(localName = "aktivita")
  public List<Vocab> komponenta_aktivita = new ArrayList();

//<xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
  @JacksonXmlProperty(localName = "poznamka")
  @Field
  public String komponenta_poznamka;

//<xs:element name="nalez_objekt" minOccurs="0" maxOccurs="unbounded" type="amcr:nalez_objektType"/> <!-- "{objekty}" -->
  @JacksonXmlProperty(localName = "nalez_objekt")
  public List<NalezObjekt> komponenta_nalez_objekt = new ArrayList();

//<xs:element name="nalez_predmet" minOccurs="0" maxOccurs="unbounded" type="amcr:nalez_predmetType"/><!-- "{predmety}" -->
  @JacksonXmlProperty(localName = "nalez_predmet")
  public List<NalezPredmet> komponenta_nalez_predmet = new ArrayList();

  @Field
  public String komponenta_typ_nalezu;

  @Field
  public String komponenta_kategoria_nalezu;

  @Override
  public String coreName() {
    return "entities";
  }

  @Override
  public boolean isSearchable() {
    return searchable;
  }

  @Override
  public void fillSolrFields(SolrInputDocument idoc) throws Exception {

  }

  @Override
  public boolean filterOAI(JSONObject user, SolrDocument doc) {
    return true;
  }

  public void fromSamostatnyNalez(SolrInputDocument idoc) {
    SolrInputDocument kdoc = idoc.deepCopy();
    kdoc.setField("ident_cely", idoc.getFieldValue("ident_cely") + "-K001");
    kdoc.setField("entity", "komponenta");
    kdoc.setField("komponenta_zdroj", "samostatny_nalez");
    kdoc.setField("komponenta_zdroj_ident_cely", idoc.getFieldValue("ident_cely"));
    Vocab v = new Vocab();
    v.setKey((String) idoc.getFieldValue("samostatny_nalez_obdobi"));
    IndexUtils.addJSONField(kdoc, "komponenta_obdobi", v);
    kdoc.setField("entity", "komponenta");
    try {
      IndexUtils.addAndCommit("entities", kdoc);
    } catch (Exception ex) {
      Logger.getLogger(Komponenta.class.getName()).log(Level.SEVERE, "Error indexing komponenta {0}", ident_cely);
    } 
  }

  public void fillSolrFields(SolrInputDocument rootDoc, SolrInputDocument parentDoc, String prefix) {
    DocumentObjectBinder dob = new DocumentObjectBinder();
    SolrInputDocument kdoc = dob.toSolrInputDocument(this);
    IndexUtils.addJSONField(kdoc, "komponenta_obdobi", komponenta_obdobi);
    IndexUtils.addJSONField(kdoc, "komponenta_areal", komponenta_areal);
    kdoc.setField("searchable", rootDoc.getFieldValue("searchable"));
    kdoc.setField("is_deleted", rootDoc.getFieldValue("is_deleted"));
    kdoc.setField("datestamp", rootDoc.getFieldValue("datestamp"));

    String pristupnost = (String) rootDoc.getFieldValue("pristupnost");
    if (rootDoc.getFieldValue("entity").equals("dokument")) {
      pristupnost = "A";
    }
    kdoc.setField("pristupnost", pristupnost);
    kdoc.setField("komponenta_zdroj", rootDoc.getFieldValue("entity"));
    kdoc.setField("komponenta_zdroj_ident_cely", rootDoc.getFieldValue("ident_cely"));

    for (Vocab a : komponenta_aktivita) {
      // IndexUtils.addVocabField(kdoc, "aktivita", a);
      IndexUtils.addJSONField(kdoc, "komponenta_aktivita", a);
    }
    if (!komponenta_nalez_objekt.isEmpty()) {
      // komponenta_typ_nalezu = "objekt";
      komponenta_typ_nalezu = IndexUtils.getTypNalezu("objekt");
    }
    for (NalezObjekt no : komponenta_nalez_objekt) {
      no.setNalezKategorie();
      IndexUtils.addJSONField(kdoc, "komponenta_nalez_objekt", no);
      rootDoc.addField("nalez_dokumentu_pocet", no.pocet);
      rootDoc.addField("nalez_dokumentu_poznamka", no.poznamka);

    }
    if (!komponenta_nalez_predmet.isEmpty()) {
      // komponenta_typ_nalezu = "predmet";
      komponenta_typ_nalezu = "HES-001126";
    }
    for (NalezPredmet np : komponenta_nalez_predmet) {
      np.setNalezKategorie();
      IndexUtils.addJSONField(kdoc, "komponenta_nalez_predmet", np);
      rootDoc.addField("nalez_dokumentu_pocet", np.pocet);
      rootDoc.addField("nalez_dokumentu_poznamka", np.poznamka);
    }
    kdoc.setField("komponenta_typ_nalezu", komponenta_typ_nalezu);

    rootDoc.addField("komponenta_dokument_ident_cely", ident_cely);
    if (jistota != null) {
      komponenta_jistota = Boolean.parseBoolean(jistota);
      kdoc.addField("komponenta_jistota", komponenta_jistota);
      rootDoc.addField("komponenta_dokument_jistota", komponenta_jistota);
      rootDoc.addField("dokument_cast_komponenta_dokument_jistota", komponenta_jistota);
    } else {
      komponenta_jistota = null;
      kdoc.removeField("komponenta_jistota");
    }
    rootDoc.addField("komponenta_dokument_presna_datace", komponenta_presna_datace);
    rootDoc.addField("komponenta_dokument_poznamka", komponenta_poznamka);

    //Add fields from parent and root documents
    //IndexUtils.addSecuredFieldNonRepeat(kdoc, "f_katastr", hlavni_katastr.getValue(), pristupnost);
    if (parentDoc.getFieldValue("dj_pian") != null) {
      try {
        addPian(kdoc, (String) parentDoc.getFieldValue("dj_pian"), pristupnost);
      } catch (Exception ex) {
        Logger.getLogger(Komponenta.class.getName()).log(Level.SEVERE, "Error adding PIAN {0}", ex);
      }
    }
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
    setFieldsFromSelf(kdoc, prSufix);
    setFieldsFromRoot(kdoc, rootDoc, prSufix);
    setFieldsFromParent(kdoc, parentDoc, prSufix);
    setFullText(kdoc);

    for (String sufix : SolrSearcher.prSufixAll) {
      kdoc.addField("text_all_" + sufix, ident_cely);
      IndexUtils.addFieldNonRepeat(kdoc, "text_all_" + sufix, rootDoc.getFieldValue("ident_cely"));
    }

    try {
      IndexUtils.addAndCommit("entities", kdoc);
    } catch (Exception ex) {
      Logger.getLogger(Komponenta.class.getName()).log(Level.SEVERE, "Error indexing komponenta {0}", ident_cely);
    }
  }

  private void setFieldsFromSelf(SolrInputDocument idoc, List<String> prSufix) {
    List<Object> indexFields = Options.getInstance().getJSONObject("fields").getJSONObject("komponenta").getJSONObject("facets").getJSONArray("self").toList();
    for (Object f : indexFields) {
      String s = (String) f;
      if (s.contains(":")) {
        String dest = s.split(":")[0];
        String orig = s.split(":")[1];
        IndexUtils.addByPath(idoc, orig, dest, prSufix, false);
      }

    }
  }

  /**
   *
   * Přístupnost Organizace Kraj Okres Katastr Komponenta - * Nález - *
   * Akce/lokalita - typ dokumentační jednotky Akce/projekt - vedoucí Akce - typ
   * Lokalita - * Dokument - kategorie Dokument - řada Dokument - typ Dokument -
   * dokumentované tvary (příznaky) ADB - * PIAN - * Sam. nález - nálezce Sam.
   * nález - okolnosti
   */
  private void setFieldsFromRoot(SolrInputDocument idoc, SolrInputDocument rootDoc, List<String> prSufix) {
    List<Object> indexFields = Options.getInstance().getJSONObject("fields").getJSONObject("komponenta").getJSONObject("facets").getJSONArray("root").toList();
    setFieldsFromSuper(idoc, rootDoc, prSufix, indexFields);
  }

  private void setFieldsFromParent(SolrInputDocument idoc, SolrInputDocument parentDoc, List<String> prSufix) {
    List<Object> indexFields = Options.getInstance().getJSONObject("fields").getJSONObject("komponenta").getJSONObject("facets").getJSONArray("parent").toList();
    setFieldsFromSuper(idoc, parentDoc, prSufix, indexFields);
  }

  private void setFieldsFromSuper(SolrInputDocument idoc, SolrInputDocument origDoc, List<String> prSufix, List<Object> indexFields) {

    for (Object f : indexFields) {
      String s = (String) f;
      String dest = s;
      String orig = s;
      if (s.contains(":")) {
        dest = s.split(":")[0];
        orig = s.split(":")[1];
        if (s.contains(".")) {
          IndexUtils.addByPath(idoc, origDoc, orig, dest, prSufix, false);
        }
      }
      for (String sufix : prSufix) {
        if (origDoc.containsKey(orig) && origDoc.getFieldValues(orig) != null) {
          for (Object v : origDoc.getFieldValues(orig)) {
            IndexUtils.addFieldNonRepeat(idoc, dest, v);
          }
        }
        if (origDoc.containsKey(orig + "_" + sufix)) {
          IndexUtils.addFieldNonRepeat(idoc, dest + "_" + sufix, origDoc.getFieldValues(orig + "_" + sufix));
        }
      }

    }
  }

  private void setFullText(SolrInputDocument idoc) {
    List<Object> indexFields = Options.getInstance().getJSONObject("fields").getJSONObject("komponenta").getJSONArray("full_text").toList();
    for (Object f : indexFields) {
      String s = (String) f;
      if (s.contains(".")) {
        IndexUtils.addByPath(idoc, s, "text_all", Arrays.asList(SolrSearcher.prSufixAll), true);
      } else {
        for (String sufix : SolrSearcher.prSufixAll) {
          if (idoc.containsKey(s)) {
            IndexUtils.addFieldNonRepeat(idoc, "text_all_" + sufix, idoc.getFieldValues(s));
          }
          if (idoc.containsKey(s + "_" + sufix)) {
            IndexUtils.addFieldNonRepeat(idoc, "text_all_" + sufix, idoc.getFieldValues(s + "_" + sufix));
          }
        }
      }
    }
  }

  private void addPian(SolrInputDocument idoc, String pian, String pristupnost) throws Exception {
    idoc.addField("pian_id", pian);
    idoc.addField("pian_ident_cely", pian);
    SolrQuery query = new SolrQuery("ident_cely:\"" + pian + "\"")
            .setFields("*,pian_chranene_udaje:[json]");
    JSONObject json = SearchUtils.searchOrIndex(query, "entities", pian);

    if (json.getJSONObject("response").getInt("numFound") > 0) {
      for (int d = 0; d < json.getJSONObject("response").getJSONArray("docs").length(); d++) {
        JSONObject pianDoc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(d);

        // IndexUtils.setSecuredJSONField(idoc, "pian", pianDoc, pristupnost);
        IndexUtils.addFieldNonRepeat(idoc, "f_pian_typ", pianDoc.getString("pian_typ"));
        IndexUtils.addFieldNonRepeat(idoc, "f_pian_presnost", pianDoc.getString("pian_presnost"));
        IndexUtils.addSecuredFieldNonRepeat(idoc, "f_pian_zm10", pianDoc.getJSONObject("pian_chranene_udaje").getString("zm10"), pristupnost);

        for (String key : pianDoc.keySet()) {
          switch (key) {
            case "entity":
            case "searchable":
            case "_version_":
            case "stav":
            case "chranene_udaje":
              break;
            default:
              // idoc.setField("dj_pian_" + key, pianDoc.opt(key));
              if (key.startsWith("loc")) {
                //SolrSearcher.addFieldNonRepeat(idoc, key, pianDoc.opt(key));

                JSONArray val = pianDoc.optJSONArray(key);
                for (int i = 0; i < val.length(); i++) {
                  SolrSearcher.addFieldNonRepeat(idoc, key, val.opt(i));
                }

              } else if (key.startsWith("lat") || key.startsWith("lng")) {
                // SolrSearcher.addFieldNonRepeat(idoc, "lng" + key.substring(3), pianDoc.opt(key));
                JSONArray val = pianDoc.optJSONArray(key);
                for (int i = 0; i < val.length(); i++) {
                  SolrSearcher.addFieldNonRepeat(idoc, key, val.getBigDecimal(i).toString());
                }

              } else {
                // SolrSearcher.addFieldNonRepeat(idoc, "dj_pian_" + key, pianDoc.opt(key));
              }
          }
        }
      }
    }
  }

}
