package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class ArcheologickyZaznam implements FedoraModel {

  @Field
  public String entity = "akce nebo lokalita";

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
  @JacksonXmlProperty(localName = "ident_cely")
  @Field
  public String ident_cely;

//  <xs:element name="stav" minOccurs="1" maxOccurs="1" type="xs:integer"/> <!-- "{stav}" -->
  @JacksonXmlProperty(localName = "stav")
  @Field
  public long stav;

//<xs:element name="okres" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "ruian-{hlavni_katastr.okres.kod}" | "{hlavni_katastr.okres.nazev}" -->
  @JacksonXmlProperty(localName = "okres")
  public Vocab okres;

//<xs:element name="pristupnost" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{pristupnost.ident_cely}" | "{pristupnost.heslo}" -->
  @JacksonXmlProperty(localName = "pristupnost")
  public Vocab pristupnost;

//<xs:element name="chranene_udaje" minOccurs="0" maxOccurs="1" type="amcr:az-chranene_udajeType"/> <!-- self -->
  @JacksonXmlProperty(localName = "chranene_udaje")
  private AZChraneneUdaje chranene_udaje;

//<xs:choice minOccurs="1" maxOccurs="1">
//  <xs:element name="akce" type="amcr:akceType"/> <!-- "{akce}" -->
  @JacksonXmlProperty(localName = "akce")
  public Akce akce;
//  <xs:element name="lokalita" type="amcr:lokalitaType"/> <!-- "{lokalita}" -->
  @JacksonXmlProperty(localName = "lokalita")
  public Lokalita lokalita;
//</xs:choice>

//<xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/> <!-- "{historie.historie_set}" -->
//<xs:element name="dokumentacni_jednotka" minOccurs="0" maxOccurs="unbounded" type="amcr:dokumentacni_jednotkaType"/> <!-- "{dokumentacni_jednotky_akce}" -->
  @JacksonXmlProperty(localName = "dokumentacni_jednotka")
  public List<DokumentacniJednotka> dokumentacni_jednotka = new ArrayList();

//<xs:element name="ext_odkaz" minOccurs="0" maxOccurs="unbounded" type="amcr:az-ext_odkazType"/> <!-- "{externi_odkazy}" -->
//<xs:element name="dokument" minOccurs="0" maxOccurs="unbounded" type="amcr:refType"/> <!-- "{casti_dokumentu.dokument.ident_cely}" | "{casti_dokumentu.dokument.ident_cely}" -->
  @JacksonXmlProperty(localName = "dokument")
  public List<Vocab> dokument = new ArrayList();

  @Override
  public boolean isOAI() {
    return true;
  }

  @Override
  public boolean isEntity() {
    return true;
  }

  @Override
  public boolean isHeslo() {
    return false;
  }

  @Override
  public SolrInputDocument createOAIDocument(String xml) {

    SolrInputDocument idoc = new SolrInputDocument();
    idoc.setField("ident_cely", ident_cely);
    idoc.setField("model", "archeologicky_zaznam");
    idoc.setField("pristupnost", SearchUtils.getPristupnostMap().get(pristupnost.getId()));
    idoc.setField("xml", xml);
    return idoc;
  }

  @Override
  public void fillSolrFields(SolrInputDocument idoc) {

    String calcEntity = akce != null ? "akce" : "lokalita";
    idoc.setField("entity", calcEntity);

    boolean searchable = stav == 3;
    idoc.setField("searchable", searchable);
    idoc.setField("pristupnost", SearchUtils.getPristupnostMap().get(pristupnost.getId()));
    IndexUtils.addVocabField(idoc, "okres", okres);

    for (Vocab v : dokument) {
      IndexUtils.addVocabField(idoc, "dokument", v);
    }

//    for (DokumentacniJednotka dj : dokumentacni_jednotka) {
//      // Should index as entity
//      dj.fillSolrFields(idoc);
//    }
    List<SolrInputDocument> idocs = new ArrayList<>();
    try {
      for (DokumentacniJednotka dj : dokumentacni_jednotka) {
        SolrInputDocument djdoc = dj.createSolrDoc();
        idocs.add(djdoc);

        IndexUtils.addJSONField(idoc, "dokumentacni_jednotka", dokumentacni_jednotka);
        // choose dokumentacni_jednotka fields to put in idoc for akce/lokalita
        idoc.addField("dokumentacni_jednotka_ident_cely", dj.ident_cely);
        idoc.addField("dokumentacni_jednotka_pian", djdoc.getFieldValue("pian"));
        idoc.addField("dokumentacni_jednotka_adb", djdoc.getFieldValue("adb"));
        idoc.addField("dokumentacni_jednotka_komponenta_obdobi", djdoc.getFieldValue("komponenta_obdobi"));
        idoc.addField("dokumentacni_jednotka_komponenta_areal", djdoc.getFieldValue("komponenta_areal"));
        idoc.addField("dokumentacni_jednotka_komponenta_aktivita", djdoc.getFieldValue("komponenta_aktivita"));

        idoc.addField("dokumentacni_jednotka_komponenta_typ_nalezu", djdoc.getFieldValue("komponenta_typ_nalezu"));
        idoc.addField("dokumentacni_jednotka_komponenta_nalez_objekt_druh", djdoc.getFieldValue("komponenta_nalez_objekt_druh"));
        idoc.addField("dokumentacni_jednotka_komponenta_nalez_objekt_specifikace", djdoc.getFieldValue("komponenta_nalez_objekt_specifikace"));
        idoc.addField("dokumentacni_jednotka_komponenta_nalez_predmet_druh", djdoc.getFieldValue("komponenta_nalez_predmet_druh"));
        idoc.addField("dokumentacni_jednotka_komponenta_nalez_predmet_specifikace", djdoc.getFieldValue("komponenta_nalez_predmet_specifikace"));
        
        IndexUtils.addFieldNonRepeat(idoc, "dokumentacni_jednotka_typ", djdoc.getFieldValue("typ"));

        // add loc field by pian
        addPian(idoc, (String) djdoc.getFieldValue("pian"));
        
        //add adb fields
        addAdbFields(idoc, (String) djdoc.getFieldValue("adb"));
      }
      if (!idocs.isEmpty()) {
        IndexUtils.getClient().add("entities", idocs, 10);
      }
    } catch (SolrServerException | IOException ex) {
      Logger.getLogger(ArcheologickyZaznam.class.getName()).log(Level.SEVERE, null, ex);
    }

    if (akce != null) {
      akce.fillSolrFields(idoc);
    }

    if (lokalita != null) {
      lokalita.fillSolrFields(idoc);
    }

    if (chranene_udaje != null) {
      chranene_udaje.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
    }
  }

  private void addAdbFields(SolrInputDocument idoc, String ident_cely) {
    SolrQuery query = new SolrQuery("ident_cely:\"" + ident_cely + "\"")
            .setFields("ident_cely,podnet,typ_sondy,autor_popisu,autor_revize");
    JSONObject json = SearchUtils.json(query, IndexUtils.getClient(), "entities");

    if (json.getJSONObject("response").getInt("numFound") > 0) {
      for (int d = 0; d < json.getJSONObject("response").getJSONArray("docs").length(); d++) {
        JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(d);

        for (String key : doc.keySet()) {
          SolrSearcher.addFieldNonRepeat(idoc, "dokumentacni_jednotka_adb_" + key, doc.opt(key));
        }
      }
    }
  }

  private void addPian(SolrInputDocument idoc, String pian) {
    SolrQuery query = new SolrQuery("ident_cely:\"" + pian + "\"");
    JSONObject json = SearchUtils.json(query, IndexUtils.getClient(), "entities");

    if (json.getJSONObject("response").getInt("numFound") > 0) {
      for (int d = 0; d < json.getJSONObject("response").getJSONArray("docs").length(); d++) {
        JSONObject pianDoc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(d);
        idoc.addField("pian", pianDoc.toString());
        for (String key : pianDoc.keySet()) {
          switch (key) {
            case "entity":
            case "searchable":
            case "_version_":
            case "stav":
            case "chranene_udaje":
              break;
            default:
              // idoc.setField("dokumentacni_jednotka_pian_" + key, pianDoc.opt(key));
              if (key.startsWith("loc")) {
                SolrSearcher.addFieldNonRepeat(idoc, key, pianDoc.opt(key));
              } else if (key.startsWith("centroid_n")) {
                SolrSearcher.addFieldNonRepeat(idoc, "lat" + key.substring(10), pianDoc.opt(key));
                SolrSearcher.addFieldNonRepeat(idoc, key, pianDoc.opt(key));
              } else if (key.startsWith("centroid_e")) {
                SolrSearcher.addFieldNonRepeat(idoc, "lng" + key.substring(10), pianDoc.opt(key));
                SolrSearcher.addFieldNonRepeat(idoc, key, pianDoc.opt(key));
              } else {
                SolrSearcher.addFieldNonRepeat(idoc, "dokumentacni_jednotka_pian_" + key, pianDoc.opt(key));
              }
          }
        }
      }
    }
  }

}

class AZChraneneUdaje {

  //<xs:element name="hlavni_katastr" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "ruian-{hlavni_katastr.kod}" | "{hlavni_katastr.nazev}" -->
  @JacksonXmlProperty(localName = "hlavni_katastr")
  public Vocab hlavni_katastr;

//<xs:element name="dalsi_katastr" minOccurs="0" maxOccurs="unbounded" type="amcr:vocabType"/> <!-- "ruian-{katastry.kod}" | "{katastry.nazev}" -->
  @JacksonXmlProperty(localName = "dalsi_katastr")
  public List<Vocab> dalsi_katastr = new ArrayList<>();

//<xs:element name="uzivatelske_oznaceni" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{uzivatelske_oznaceni}" -->
  @JacksonXmlProperty(localName = "uzivatelske_oznaceni")
  public String uzivatelske_oznaceni;

  public void fillSolrFields(SolrInputDocument idoc, String pristupnost) {
    IndexUtils.setSecuredJSONField(idoc, this);
    IndexUtils.addSecuredFieldNonRepeat(idoc, "hlavni_katastr", hlavni_katastr.getValue(), pristupnost);
    IndexUtils.addSecuredFieldNonRepeat(idoc, "f_katastr", hlavni_katastr.getValue(), pristupnost);

    for (Vocab v : dalsi_katastr) {
      IndexUtils.addSecuredFieldNonRepeat(idoc, "dalsi_katastr", v.getValue(), pristupnost);
    }
    IndexUtils.addSecuredFieldNonRepeat(idoc, "uzivatelske_oznaceni", uzivatelske_oznaceni, pristupnost);

  }
}
