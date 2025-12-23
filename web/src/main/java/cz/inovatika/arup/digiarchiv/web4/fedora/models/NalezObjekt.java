package cz.inovatika.arup.digiarchiv.web4.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
/**
 *
 * @author alberto
 */
public class NalezObjekt {
  
//<xs:element name="id" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "nalo-{id}" -->
    @JacksonXmlProperty(localName = "id")
    public String id;
    
//<xs:element name="druh" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{druh.ident_cely}" | "{druh.heslo}" -->
    @JacksonXmlProperty(localName = "druh")
    public Vocab druh;
    
//<xs:element name="specifikace" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{specifikace.ident_cely}" | "{specifikace.heslo}" -->
    @JacksonXmlProperty(localName = "specifikace")
    public Vocab specifikace;
    
//<xs:element name="pocet" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{pocet}" -->
    @JacksonXmlProperty(localName = "pocet")
    public String pocet;
    
//<xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
    @JacksonXmlProperty(localName = "poznamka")
    public String poznamka;
    
  
}
