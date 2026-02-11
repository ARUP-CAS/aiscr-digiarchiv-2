package cz.inovatika.arup.digiarchiv.web4.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.apache.solr.client.solrj.beans.Field;

/**
 *
 * @author alberto
 */
public class ExtOdkaz {

//<xs:element name="id" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "nalo-{id}" -->
  @JacksonXmlProperty(localName = "id")
  @Field
  public String id;

//<xs:element name="ext_zdroj" minOccurs="1" maxOccurs="1" type="amcr:refType"/> <!-- "{externi_zdroj.ident_cely}" | "{externi_zdroj.ident_cely}" -->
  @JacksonXmlProperty(localName = "ext_zdroj")
  public Vocab ext_zdroj;

//<xs:element name="paginace" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{paginace}" -->
  @JacksonXmlProperty(localName = "paginace")
  @Field
  public String paginace;

}
