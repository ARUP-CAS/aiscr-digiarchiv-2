package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.apache.solr.client.solrj.beans.Field;

/**
 *
 * @author alberto
 */
public class ChraneneUdaje {
//
//  <xs:element name="id" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "hier-{id}" -->
  @JacksonXmlProperty(localName = "id")
  @Field
  public String id;
  
//  <xs:element name="heslo_podrazene" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{heslo_nadrazene.ident_cely}" | "{heslo_podrazene.heslo}" -->
  @JacksonXmlProperty(localName = "heslo_podrazene")
  @Field
  public Vocab heslo_podrazene;
  
//  <xs:element name="typ" minOccurs="1" maxOccurs="1" type="amcr:langstringType"/> <!-- "{typ}" -->      
  @JacksonXmlProperty(localName = "typ")
  @Field
  public Lang typ;
  
}
