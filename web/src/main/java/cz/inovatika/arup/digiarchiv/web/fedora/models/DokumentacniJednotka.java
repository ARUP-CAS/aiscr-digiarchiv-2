/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author alberto
 */
public class DokumentacniJednotka {

  @Field
  public String entity = "dokumentacni_jednotka";

  @Field
  public boolean searchable = true;

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
  @JacksonXmlProperty(localName = "ident_cely")
  @Field
  public String ident_cely;

//<xs:element name="pian" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{pian.ident_cely}" | "{pian.ident_cely}" -->
  @JacksonXmlProperty(localName = "pian") 
  public Vocab pian;

//<xs:element name="typ" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ.ident_cely}" | "{typ.heslo}" -->
  @JacksonXmlProperty(localName = "typ")
  public Vocab typ;

//<xs:element name="negativni_jednotka" minOccurs="1" maxOccurs="1" type="xs:boolean"/> <!-- "{negativni_jednotka}" -->
  @JacksonXmlProperty(localName = "negativni_jednotka")
  @Field
  public boolean negativni_jednotka;

//<xs:element name="nazev" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{nazev}" -->
  @JacksonXmlProperty(localName = "nazev")
  @Field
  public String nazev;

//<xs:element name="adb" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{Adb.ident_cely}" | "{Adb.ident_cely}" -->
  @JacksonXmlProperty(localName = "adb")
  public Vocab adb;

//<xs:element name="komponenta" minOccurs="0" maxOccurs="unbounded" type="amcr:komponentaType"/> <!-- "{komponenty.komponenty}" -->
  @JacksonXmlProperty(localName = "komponenta")
  public List<Komponenta> komponenta = new ArrayList();
  
  public SolrInputDocument createSolrDoc() {
    
    DocumentObjectBinder dob = new DocumentObjectBinder();
    SolrInputDocument idoc = dob.toSolrInputDocument(this);
    //akceDoc.setField("entity", "akce");
    //akceDoc.setField("pristupnost", pristupnost);
    IndexUtils.addVocabField(idoc, "pian", pian);
    IndexUtils.addVocabField(idoc, "typ", typ);
    
    IndexUtils.addVocabField(idoc, "adb", adb);
    
    for (Komponenta k : komponenta) {
      IndexUtils.addJSONField(idoc, "komponenta", komponenta);
      k.fillSolrFields(idoc, "dok_jednotka");
    }
    
    return idoc;
    
  }

  public void fillSolrFields(SolrInputDocument idoc) {
    
    // IndexUtils.addVocabField(idoc, "typ", typ);
    
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      idoc.addField("dok_jednotka", objectMapper.writeValueAsString(this));
      // IndexUtils.addFieldNonRepeat(idoc, "dok_jednotka", objectMapper.writeValueAsString(this));
    } catch (JsonProcessingException ex) {
      Logger.getLogger(DokumentacniJednotka.class.getName()).log(Level.SEVERE, null, ex);
    }
    for (Komponenta k : komponenta) {
      k.fillSolrFields(idoc, "dok_jednotka");
    }
    
    if(pian != null) {
      // Add fields from PIAN.
    }
  }

}
