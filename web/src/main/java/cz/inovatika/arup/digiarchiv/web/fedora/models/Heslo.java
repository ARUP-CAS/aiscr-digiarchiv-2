package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@JacksonXmlRootElement(localName = "heslo")
public class Heslo implements FedoraModel {

  @Field
  public String entity = "heslo";

//  <xs:complexType name="hesloType">
//    <xs:sequence>
//    </xs:sequence>
//  </xs:complexType>
//      <xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
    @JacksonXmlProperty(localName = "ident_cely")
    @Field
    public String ident_cely;

//      <xs:element name="nazev_heslare" minOccurs="1" maxOccurs="1" type="amcr:langstringType"/> <!-- "{nazev_heslare.nazev}" -->
    @JacksonXmlProperty(localName = "nazev_heslare")
    public Lang nazev_heslare;

//      <xs:element name="heslo" minOccurs="1" maxOccurs="1" type="amcr:langstringType"/> <!-- "{heslo}" -->
    @JacksonXmlProperty(localName = "heslo")
    public Lang heslo;

//      <xs:element name="heslo_en" minOccurs="1" maxOccurs="1" type="amcr:langstringType"/> <!-- "{heslo_en}" -->
    @JacksonXmlProperty(localName = "heslo_en")
    public Lang heslo_en;

//      <xs:element name="popis" minOccurs="0" maxOccurs="1" type="amcr:langstringType"/> <!-- "{popis}" -->
    @JacksonXmlProperty(localName = "popis")
    @Field
    public Lang popis;

//      <xs:element name="popis_en" minOccurs="0" maxOccurs="1" type="amcr:langstringType"/> <!-- "{popis_en}" -->
    @JacksonXmlProperty(localName = "popis_en")
    @Field
    public Lang popis_en;

//      <xs:element name="zkratka" minOccurs="0" maxOccurs="1" type="amcr:langstringType"/> <!-- "{zkratka}" -->
    @JacksonXmlProperty(localName = "zkratka")
    public Lang zkratka;

//      <xs:element name="zkratka_en" minOccurs="0" maxOccurs="1" type="amcr:langstringType"/> <!-- "{zkratka_en}" -->
    @JacksonXmlProperty(localName = "zkratka_en")
    public Lang zkratka_en;

//      <xs:element name="razeni" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{razeni}" -->
    @JacksonXmlProperty(localName = "razeni")
    @Field
    public long razeni;

//      <xs:element name="hierarchie_vyse" minOccurs="0" maxOccurs="unbounded" type="amcr:hierarchie_vyseType"/> <!-- "{nadrazena_hesla}" -->
    @JacksonXmlProperty(localName = "hierarchie_vyse")
    private List<HierarchieVyse> hierarchie_vyse;

    public List<HierarchieVyse> getHierarchieVyse() {
        return hierarchie_vyse;
    }

//      <xs:element name="hierarchie_nize" minOccurs="0" maxOccurs="unbounded" type="amcr:hierarchie_nizeType"/> <!-- "{podrazena_hesla}" -->
    @JacksonXmlProperty(localName = "hierarchie_nize")
    private List<HierarchieNize> hierarchie_nize;

    public List<HierarchieNize> getHierarchieNize() {
        return hierarchie_nize;
    }

//  <xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/> <!-- "{historie.historie_set}" -->
    @JacksonXmlProperty(localName = "historie")
    public List<Historie> historie = new ArrayList();

//      <xs:element name="dokument_typ_material_rada" minOccurs="0" maxOccurs="unbounded" type="amcr:dokument_typ_material_radaType"/> <!-- "{dokument_typ_material_rada}" -->
//      <xs:element name="datace" minOccurs="0" maxOccurs="1" type="amcr:dataceType"/> <!-- "{datace_obdobi}" -->
//      <xs:element name="odkaz" minOccurs="0" maxOccurs="unbounded" type="amcr:odkazType"/> <!-- "{heslar_odkaz}" -->
    @Override
    public void fillSolrFields(SolrInputDocument idoc) {
        idoc.setField("cs", heslo.getValue());
//    if (heslo != null) {
//    }
        if (heslo_en != null) {
            idoc.setField("en", heslo_en.getValue());
        } else {
            idoc.setField("en", heslo.getValue());
        }
        if (nazev_heslare != null) {
            idoc.setField("nazev_heslare", nazev_heslare.getValue());
        }
        if (zkratka != null) {
            idoc.setField("zkratka", zkratka.getValue());
        }
        if (zkratka_en != null) {
            idoc.setField("zkratka_en", zkratka_en.getValue());
        }

//    if(hierarchie_nize != null) {
//      for(HierarchieNize hz: hierarchie_nize) {
//        idoc.setField("en", hz.);
//      }
//    }
        IndexUtils.setDateStamp(idoc, ident_cely);
        IndexUtils.setDateStampFromHistory(idoc, historie);
    }

    @Override
    public String coreName() {
        return "heslar";
    }

    @Override
    public String filterOAI(JSONObject user, SolrDocument doc) {
        return (String) doc.getFieldValue("xml");
    }

}
