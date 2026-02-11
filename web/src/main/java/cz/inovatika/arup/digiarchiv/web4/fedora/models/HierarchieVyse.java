package cz.inovatika.arup.digiarchiv.web4.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.apache.solr.client.solrj.beans.Field;

/**
 *
 * @author alberto
 */
@JacksonXmlRootElement(localName = "hierarchie_vyse")
public class HierarchieVyse {
//
//  <xs:element name="id" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "hier-{id}" -->
  @JacksonXmlProperty(localName = "id")
  @Field
  public String id;
  
//  <xs:element name="heslo_nadrazene" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{heslo_nadrazene.ident_cely}" | "{heslo_nadrazene.heslo}" -->
  @JacksonXmlProperty(localName = "heslo_nadrazene")
  @Field
  public Vocab heslo_nadrazene;
  
//  <xs:element name="typ" minOccurs="1" maxOccurs="1" type="amcr:langstringType"/> <!-- "{typ}" -->      
  @JacksonXmlProperty(localName = "typ")
  @Field
  public Lang typ;
  

}
