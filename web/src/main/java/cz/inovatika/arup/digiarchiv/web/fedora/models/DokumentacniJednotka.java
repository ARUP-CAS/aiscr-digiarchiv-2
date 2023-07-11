/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author alberto
 */
public class DokumentacniJednotka {

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
  @JacksonXmlProperty(localName = "ident_cely")
  public String ident_cely;

//<xs:element name="pian" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{pian.ident_cely}" | "{pian.ident_cely}" -->
  @JacksonXmlProperty(localName = "pian")
  public Vocab pian;

//<xs:element name="typ" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ.ident_cely}" | "{typ.heslo}" -->
  @JacksonXmlProperty(localName = "typ")
  public Vocab typ;

//<xs:element name="negativni_jednotka" minOccurs="1" maxOccurs="1" type="xs:boolean"/> <!-- "{negativni_jednotka}" -->
  @JacksonXmlProperty(localName = "negativni_jednotka")
  public boolean negativni_jednotka;

//<xs:element name="nazev" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{nazev}" -->
  @JacksonXmlProperty(localName = "nazev")
  public String nazev;

//<xs:element name="adb" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{Adb.ident_cely}" | "{Adb.ident_cely}" -->
  @JacksonXmlProperty(localName = "adb")
  public Vocab adb;

//<xs:element name="komponenta" minOccurs="0" maxOccurs="unbounded" type="amcr:komponentaType"/> <!-- "{komponenty.komponenty}" -->
  @JacksonXmlProperty(localName = "komponenta")
  public List<Komponenta> komponenta = new ArrayList();

  public void fillSolrFields(SolrInputDocument idoc) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      // idoc.addField("dokumentacni_jednotka", objectMapper.writeValueAsString(this));
      SolrSearcher.addFieldNonRepeat(idoc, "dok_jednotka", objectMapper.writeValueAsString(this));
    } catch (JsonProcessingException ex) {
      Logger.getLogger(DokumentacniJednotka.class.getName()).log(Level.SEVERE, null, ex);
    }
    for (Komponenta k : komponenta) {
      k.fillSolrFields(idoc);
    }
    
    if(pian != null) {
      // Add fields from PIAN.
    }
  }

}
