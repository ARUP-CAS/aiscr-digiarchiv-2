package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;

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
  public Object akce;
//  <xs:element name="lokalita" type="amcr:lokalitaType"/> <!-- "{lokalita}" -->
  @JacksonXmlProperty(localName = "lokalita")
  public Object lokalita;
//</xs:choice>

//<xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/> <!-- "{historie.historie_set}" -->
//<xs:element name="dokumentacni_jednotka" minOccurs="0" maxOccurs="unbounded" type="amcr:dokumentacni_jednotkaType"/> <!-- "{dokumentacni_jednotky_akce}" -->
  @JacksonXmlProperty(localName = "dokumentacni_jednotka")
  public List<Object> dokumentacni_jednotka = new ArrayList();
  
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

    boolean searchable = true;

    switch (calcEntity) {

      case "akce":
        searchable = stav == 4 || stav == 5 || stav == 8;
        break;
      case "loaklita":
        searchable = stav == 3;
        break;
    }
    idoc.setField("searchable", searchable);
    idoc.setField("pristupnost", SearchUtils.getPristupnostMap().get(pristupnost.getId()));
    SolrSearcher.addVocabField(idoc, "okres", okres);
    
    for (Vocab v : dokument) {
      SolrSearcher.addVocabField(idoc, "dokument", v);
    }
    
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      idoc.addField("dokumentacni_jednotka", objectMapper.writeValueAsString(dokumentacni_jednotka));
    } catch (JsonProcessingException ex) {
      Logger.getLogger(ArcheologickyZaznam.class.getName()).log(Level.SEVERE, null, ex);
    }
    if (chranene_udaje != null) {
      chranene_udaje.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
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
    SolrSearcher.addSecuredFieldNonRepeat(idoc, "hlavni_katastr", hlavni_katastr.getValue(), pristupnost);

    for (Vocab v : dalsi_katastr) {
      SolrSearcher.addSecuredFieldNonRepeat(idoc, "dalsi_katastr", v.getValue(), pristupnost);
    }
    SolrSearcher.addSecuredFieldNonRepeat(idoc, "uzivatelske_oznaceni", uzivatelske_oznaceni, pristupnost);

  }
}
