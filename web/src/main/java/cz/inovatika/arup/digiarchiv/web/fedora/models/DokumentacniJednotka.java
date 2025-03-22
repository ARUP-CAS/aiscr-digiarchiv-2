package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraUtils;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

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
  public Vocab dj_pian;

//<xs:element name="typ" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ.ident_cely}" | "{typ.heslo}" -->
  @JacksonXmlProperty(localName = "typ")
  public Vocab dj_typ;

//<xs:element name="negativni_jednotka" minOccurs="1" maxOccurs="1" type="xs:boolean"/> <!-- "{negativni_jednotka}" -->
  @JacksonXmlProperty(localName = "negativni_jednotka")
  @Field
  public boolean dj_negativni_jednotka;

//<xs:element name="nazev" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{nazev}" -->
  @JacksonXmlProperty(localName = "nazev")
  @Field
  public String dj_nazev;

//<xs:element name="adb" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{Adb.ident_cely}" | "{Adb.ident_cely}" -->
  @JacksonXmlProperty(localName = "adb")
  public Vocab adb_ident_cely;
  
  public ADB dj_adb;

//<xs:element name="komponenta" minOccurs="0" maxOccurs="unbounded" type="amcr:komponentaType"/> <!-- "{komponenty.komponenty}" -->
  @JacksonXmlProperty(localName = "komponenta")
  public List<Komponenta> dj_komponenta = new ArrayList();
  
  public SolrInputDocument createSolrDoc() {
    
    DocumentObjectBinder dob = new DocumentObjectBinder();
    SolrInputDocument idoc = dob.toSolrInputDocument(this);
    //akceDoc.setField("entity", "akce");
    //akceDoc.setField("pristupnost", pristupnost);
    IndexUtils.addVocabField(idoc, "dj_pian", dj_pian);
    IndexUtils.addVocabField(idoc, "dj_typ", dj_typ);
    
    IndexUtils.addVocabField(idoc, "dj_adb", adb_ident_cely);
    if (adb_ident_cely != null) {
        try {
            String xml = FedoraUtils.requestXml("record/" + adb_ident_cely.getId() + "/metadata");
            dj_adb = (ADB) FedoraModel.parseXml(xml, ADB.class);
        } catch (Exception ex) {
            Logger.getLogger(DokumentacniJednotka.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    for (Komponenta k : dj_komponenta) {
      k.fillSolrFields(idoc, "dokumentacni_jednotka");
      IndexUtils.addJSONField(idoc, "dj_komponenta", k);
    }
    
    return idoc;
    
  }

  public void fillSolrFields(SolrInputDocument idoc) {
    
    // IndexUtils.addVocabField(idoc, "typ", typ);
    
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      idoc.addField("dokumentacni_jednotka", objectMapper.writeValueAsString(this));
    } catch (JsonProcessingException ex) {
      Logger.getLogger(DokumentacniJednotka.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    IndexUtils.addFieldNonRepeat(idoc, "dj_ident_cely", ident_cely);
    IndexUtils.addFieldNonRepeat(idoc, "dj_nazev", dj_nazev);
    IndexUtils.addFieldNonRepeat(idoc, "dj_negativni_jednotka", dj_negativni_jednotka);
    for (Komponenta k : dj_komponenta) {
      k.fillSolrFields(idoc, "dokumentacni_jednotka");
    }
    
    if(dj_pian != null) {
      // Add fields from PIAN.
    }
  }

}
