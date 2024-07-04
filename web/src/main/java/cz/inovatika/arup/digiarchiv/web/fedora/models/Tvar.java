
package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 *
 * @author alberto
 */
public class Tvar {

//<xs:element name="id" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "tvar-{id}" -->
  @JacksonXmlProperty(localName = "id")
  public String id;
  
//<xs:element name="tvar" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{tvar.ident_cely}" | "{tvar.heslo}" -->
  @JacksonXmlProperty(localName = "tvar")
  public Vocab tvar;
  
//<xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
  @JacksonXmlProperty(localName = "poznamka")
  public String poznamka;
  



}
