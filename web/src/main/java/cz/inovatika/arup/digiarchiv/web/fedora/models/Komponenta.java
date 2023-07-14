/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;

/**
 *
 * @author alberto
 */
public class Komponenta {

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
  @JacksonXmlProperty(localName = "ident_cely")
  @Field
  public String ident_cely;

//<xs:element name="obdobi" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{obdobi.ident_cely}" | "{obdobi.heslo}" -->
  @JacksonXmlProperty(localName = "obdobi")
  public Vocab obdobi;

//<xs:element name="jistota" minOccurs="0" maxOccurs="1" type="xs:boolean"/> <!-- "{jistota}" -->
  @JacksonXmlProperty(localName = "jistota")
  @Field
  public boolean jistota;

//<xs:element name="presna_datace" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{presna_datace}" -->
  @JacksonXmlProperty(localName = "presna_datace")
  @Field
  public String presna_datace;

//<xs:element name="areal" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{areal.ident_cely}" | "{areal.heslo}" -->
  @JacksonXmlProperty(localName = "areal")
  public Vocab areal;

//<xs:element name="aktivita" minOccurs="0" maxOccurs="unbounded" type="amcr:vocabType"/> <!-- "{aktivity.ident_cely}" | "{aktivity.heslo}" -->
  @JacksonXmlProperty(localName = "aktivita")
  public Vocab aktivita;

//<xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
  @JacksonXmlProperty(localName = "poznamka")
  @Field
  public String poznamka;

//<xs:element name="nalez_objekt" minOccurs="0" maxOccurs="unbounded" type="amcr:nalez_objektType"/> <!-- "{objekty}" -->
  @JacksonXmlProperty(localName = "nalez_objekt")
  public List<NalezObjekt> nalez_objekt = new ArrayList();

//<xs:element name="nalez_predmet" minOccurs="0" maxOccurs="unbounded" type="amcr:nalez_predmetType"/><!-- "{predmety}" -->
  @JacksonXmlProperty(localName = "nalez_predmet")
  public List<NalezPredmet> nalez_predmet = new ArrayList();

  public void fillSolrFields(SolrInputDocument idoc) {
    DocumentObjectBinder dob = new DocumentObjectBinder();
    SolrInputDocument kdoc = dob.toSolrInputDocument(this);
    IndexUtils.addVocabField(kdoc, "obdobi", obdobi);
    IndexUtils.addVocabField(kdoc, "areal", areal);
    IndexUtils.addVocabField(kdoc, "aktivita", aktivita);
    for (Map.Entry<String, SolrInputField> entry : kdoc.entrySet()) {
      idoc.setField("komponenta_" + entry.getKey(), entry.getValue().getValue());
    }
  }

}