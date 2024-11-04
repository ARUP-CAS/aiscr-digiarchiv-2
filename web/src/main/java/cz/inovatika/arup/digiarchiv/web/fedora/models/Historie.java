package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.Date;
import org.apache.solr.client.solrj.beans.Field;

/**
 *
 * @author alberto
 */
public class Historie {

//<xs:element name="id" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "nalo-{id}" -->
  @JacksonXmlProperty(localName = "id")
  @Field
  public String id;

//<xs:element name="datum_zmeny" minOccurs="1" maxOccurs="1" type="xs:dateTime"/> <!-- "{datum_zmeny}" -->
  @JacksonXmlProperty(localName = "datum_zmeny")
  @Field
  public Date datum_zmeny;

//<xs:element name="uzivatel" minOccurs="1" maxOccurs="1" type="amcr:refType"/> <!-- "{uzivatel.ident_cely}" | "{uzivatel.ident_cely}" -->
  @JacksonXmlProperty(localName = "uzivatel")
  @Field
  public Vocab uzivatel;
  
//<xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
  @JacksonXmlProperty(localName = "poznamka")
  @Field
  public String poznamka;
  
//<xs:element name="typ_zmeny" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{typ_zmeny}" -->
  @JacksonXmlProperty(localName = "typ_zmeny")
  @Field
  public String typ_zmeny;

}
