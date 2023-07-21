package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author alberto
 */
public class Soubor implements FedoraModel {
  
//<xs:element name="id" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "soub-{id}" -->
  @JacksonXmlProperty(localName = "id")
  @Field
  public String id;
  
//<xs:element name="path" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{path}" -->
  @JacksonXmlProperty(localName = "path")
  @Field
  public String path = "neni";
  
//<xs:element name="nazev" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{nazev}" -->
  @JacksonXmlProperty(localName = "nazev")
  @Field
  public String nazev;
  
//<xs:element name="mimetype" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{mimetype}" -->
  @JacksonXmlProperty(localName = "mimetype")
  @Field
  public String mimetype;
  
//<xs:element name="rozsah" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{rozsah}" -->
  @JacksonXmlProperty(localName = "rozsah")
  @Field
  public long rozsah;
  
//<xs:element name="size_mb" minOccurs="1" maxOccurs="1" type="xs:decimal"/> <!-- "{size_mb}" -->
  @JacksonXmlProperty(localName = "size_mb")
  @Field
  public float size_mb;
  
//<xs:element name="sha_512" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{sha_512}" -->
  @JacksonXmlProperty(localName = "sha_512")
  @Field
  public String sha_512;
  
//<xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/> <!-- historie.historie_set -->
  @JacksonXmlProperty(localName = "historie")
  public List<Historie> historie = new ArrayList();
  

  @Override
  public SolrInputDocument createOAIDocument(String xml) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void fillSolrFields(SolrInputDocument idoc) {
      IndexUtils.setDateStamp(idoc, historie);
  }

  @Override
  public boolean isOAI() {
    return false;
  }

  @Override
  public String coreName() {
    return "soubor";
  }
  
  public SolrInputDocument createSolrDoc() {
    
    DocumentObjectBinder dob = new DocumentObjectBinder();
    SolrInputDocument idoc = dob.toSolrInputDocument(this);
    IndexUtils.setDateStamp(idoc, historie);
    return idoc;
    
  }
  
}
