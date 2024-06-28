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
  public Vocab dokumentacni_jednotka_pian;

//<xs:element name="typ" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ.ident_cely}" | "{typ.heslo}" -->
  @JacksonXmlProperty(localName = "typ")
  public Vocab dokumentacni_jednotka_typ;

//<xs:element name="negativni_jednotka" minOccurs="1" maxOccurs="1" type="xs:boolean"/> <!-- "{negativni_jednotka}" -->
  @JacksonXmlProperty(localName = "negativni_jednotka")
  @Field
  public boolean dokumentacni_jednotka_negativni_jednotka;

//<xs:element name="nazev" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{nazev}" -->
  @JacksonXmlProperty(localName = "nazev")
  @Field
  public String dokumentacni_jednotka_nazev;

//<xs:element name="adb" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{Adb.ident_cely}" | "{Adb.ident_cely}" -->
  @JacksonXmlProperty(localName = "adb")
  public Vocab dokumentacni_jednotka_adb;

//<xs:element name="komponenta" minOccurs="0" maxOccurs="unbounded" type="amcr:komponentaType"/> <!-- "{komponenty.komponenty}" -->
  @JacksonXmlProperty(localName = "komponenta")
  public List<Komponenta> dokumentacni_jednotka_komponenta = new ArrayList();
  
  public SolrInputDocument createSolrDoc() {
    
    DocumentObjectBinder dob = new DocumentObjectBinder();
    SolrInputDocument idoc = dob.toSolrInputDocument(this);
    //akceDoc.setField("entity", "akce");
    //akceDoc.setField("pristupnost", pristupnost);
    IndexUtils.addVocabField(idoc, "dokumentacni_jednotka_pian", dokumentacni_jednotka_pian);
    IndexUtils.addVocabField(idoc, "dokumentacni_jednotka_typ", dokumentacni_jednotka_typ);
    
    IndexUtils.addVocabField(idoc, "dokumentacni_jednotka_adb", dokumentacni_jednotka_adb);
    
    for (Komponenta k : dokumentacni_jednotka_komponenta) {
      IndexUtils.addJSONField(idoc, "dokumentacni_jednotka_komponenta", k);
      k.fillSolrFields(idoc, "dokumentacni_jednotka");
    }
    
    return idoc;
    
  }

  public void fillSolrFields(SolrInputDocument idoc) {
    
    // IndexUtils.addVocabField(idoc, "typ", typ);
    
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      idoc.addField("dokumentacni_jednotka", objectMapper.writeValueAsString(this));
      // IndexUtils.addFieldNonRepeat(idoc, "dokumentacni_jednotka", objectMapper.writeValueAsString(this));
    } catch (JsonProcessingException ex) {
      Logger.getLogger(DokumentacniJednotka.class.getName()).log(Level.SEVERE, null, ex);
    }
    for (Komponenta k : dokumentacni_jednotka_komponenta) {
      k.fillSolrFields(idoc, "dokumentacni_jednotka");
    }
    
    if(dokumentacni_jednotka_pian != null) {
      // Add fields from PIAN.
    }
  }

}
