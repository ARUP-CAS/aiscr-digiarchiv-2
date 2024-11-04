package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.apache.solr.client.solrj.beans.Field;

/**
 *
 * @author alberto
 */
public class Oznamovatel {
  
//<xs:element name="oznamovatel" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{oznamovatel}" -->
  @JacksonXmlProperty(localName = "oznamovatel")
  @Field
  public String oznamovatel;
  
//<xs:element name="odpovedna_osoba" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{odpovedna_osoba}" -->
  @JacksonXmlProperty(localName = "odpovedna_osoba")
  @Field
  public String odpovedna_osoba;
  
//<xs:element name="adresa" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{adresa}" -->
  @JacksonXmlProperty(localName = "adresa")
  @Field
  public String adresa;
  
//<xs:element name="telefon" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{telefon}" -->
  @JacksonXmlProperty(localName = "telefon")
  @Field
  public String telefon;
  
//<xs:element name="email" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{email}" -->
  @JacksonXmlProperty(localName = "email")
  @Field
  public String email;
  
  // <xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
  @JacksonXmlProperty(localName = "poznamka")
  @Field
  public String poznamka;
  
}
