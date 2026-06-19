
package cz.inovatika.arup.digiarchiv.web4.museion;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.solr.client.solrj.beans.Field;

/**
 *
 * @author alber
 */
public class AmcrEntita {
    
    
/**
 * <p>Java class for predmet complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
<xs:complexType name="amcrEntita">
<xs:sequence>
<xs:element name="id" type="xs:string"/>
</xs:sequence>
<xs:attribute name="typ" type="xs:string"/> 
</xs:complexType>
 * </pre>
 * 
 * 
 */
    
    @JacksonXmlProperty(localName = "id")
    public String id;
    
    @JacksonXmlProperty(isAttribute = true, localName = "typ")
    public String typ;
}
