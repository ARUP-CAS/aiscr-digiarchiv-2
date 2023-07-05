package cz.inovatika.arup.digiarchiv.web.fedora;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author alberto
 */
/**
 *
 * <xs:complexType name="projektType">
 * <xs:sequence>
 * <xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/>
 * <!-- "{ident_cely}" -->
 * <xs:element name="stav" minOccurs="1" maxOccurs="1" type="xs:integer"/>
 * <!-- "{stav}" -->
 * <xs:element name="typ_projektu" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/>
 * <!-- "{typ_projektu.ident_cely}" | "{typ_projektu.heslo}" -->
 * <xs:element name="okres" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/>
 * <!-- "ruian-{hlavni_katastr.okres.kod}" | "{hlavni_katastr.okres.nazev}" -->
 * <xs:element name="podnet" minOccurs="0" maxOccurs="1" type="xs:string"/>
 * <!-- "{podnet}" -->
 * <xs:element name="planovane_zahajeni" minOccurs="0" maxOccurs="1" type="xs:string"/>
 * <!-- "{planovane_zahajeni_str}" -->
 * <xs:element name="vedouci_projektu" minOccurs="0" maxOccurs="1" type="amcr:refType"/>
 * <!-- "{vedouci_projektu.ident_cely}" | "{vedouci_projektu.vypis_cely}" -->
 * <xs:element name="organizace" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/>
 * <!-- "{organizace.ident_cely}" | "{organizace.nazev}" -->
 * <xs:element name="uzivatelske_oznaceni" minOccurs="0" maxOccurs="1" type="xs:string"/>
 * <!-- "{uzivatelske_oznaceni}" -->
 * <xs:element name="oznaceni_stavby" minOccurs="0" maxOccurs="1" type="xs:string"/>
 * <!-- "{oznaceni_stavby}" -->
 * <xs:element name="kulturni_pamatka" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/>
 * <!-- "{kulturni_pamatka.ident_cely}" | "{kulturni_pamatka.heslo}" -->
 * <xs:element name="datum_zahajeni" minOccurs="0" maxOccurs="1" type="xs:date"/>
 * <!-- "{datum_zahajeni}" -->
 * <xs:element name="datum_ukonceni" minOccurs="0" maxOccurs="1" type="xs:date"/>
 * <!-- "{datum_ukonceni}" -->
 * <xs:element name="termin_odevzdani_nz" minOccurs="0" maxOccurs="1" type="xs:date"/>
 * <!-- "{termin_odevzdani_nz}" -->
 * <xs:element name="pristupnost_pom" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/>
 * <!-- "{pristupnost.ident_cely}" | "{pristupnost.heslo}" -->
 * <xs:element name="chranene_udaje" minOccurs="0" maxOccurs="1" type="amcr:projekt-chranene_udajeType"/>
 * <!-- self -->
 * <xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/>
 * <!-- "{historie.historie_set}" -->
 * <xs:element name="oznamovatel" minOccurs="0" maxOccurs="1" type="amcr:oznamovatelType"/>
 * <!-- "{oznamovatel}" -->
 * <xs:element name="soubor" minOccurs="0" maxOccurs="unbounded" type="amcr:souborType"/>
 *  <!-- {soubory.soubory} -->
 * <xs:element name="archeologicky_zaznam" minOccurs="0" maxOccurs="unbounded" type="amcr:refType"/>
 * <!-- "{akce_set.archeologicky_zaznam.ident_cely}" | "{akce_set.archeologicky_zaznam.ident_cely}" -->
 * <xs:element name="samostatny_nalez" minOccurs="0" maxOccurs="unbounded" type="amcr:refType"/>
 * <!-- "{samostatne_nalezy.ident_cely}" | "{samostatne_nalezy.ident_cely}" -->
 * <xs:element name="dokument" minOccurs="0" maxOccurs="unbounded" type="amcr:refType"/>
 * <!-- "{casti_dokumentu.dokument.ident_cely}" | "{casti_dokumentu.dokument.ident_cely}" -->
 * </xs:sequence>
 * </xs:complexType>
 */
@JacksonXmlRootElement(localName = "projekt")
public class Projekt implements FedoraModel {

  @JacksonXmlProperty(localName = "ident_cely")
  @Field
  public String ident_cely;
  
  @JacksonXmlProperty(localName = "stav")
  @Field
  public long stav;
  
  @JacksonXmlProperty(localName = "pristupnost_pom")
  public Vocab pristupnost;
  
  

//  public static Projekt parse(String xml) throws Exception {
//    XMLInputFactory f = XMLInputFactory.newFactory();
//    // XMLInputFactory f = new WstxInputFactory();
//    //f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
//    XMLStreamReader sr = f.createXMLStreamReader(new StringReader(xml));
//    XmlMapper xmlMapper = new XmlMapper();
//    xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
//    sr.nextTag();
//    while (!sr.getLocalName().equals("projekt")) {
//      sr.nextTag();
//    }
//    return xmlMapper.readValue(sr, Projekt.class);
//  }

  @Override
  public SolrInputDocument createOAIDocument(String xml) {
    SolrInputDocument idoc = new SolrInputDocument();
    idoc.setField("ident_cely", ident_cely);
    idoc.setField("model", "projekt");
    idoc.setField("pristupnost", pristupnost.getId());
    idoc.setField("xml", xml);
    return idoc;
  }

  @Override
  public void fillEntityDocument(SolrInputDocument idoc) {
    idoc.setField("pristupnost", pristupnost.getId());
    
  }
}
