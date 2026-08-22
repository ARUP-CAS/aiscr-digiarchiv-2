package cz.inovatika.arup.digiarchiv.web4.museion;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

@JacksonXmlRootElement(localName = "predmetyDleAmcrIdResponse")
public class PredmetyDleAmcr {
    
/**
 * <complexType name="predmetyDleAmcrIdResponse">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="organizaceId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         <element name="pocetPom" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         <element name="pocetSys" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         <element name="predmetPom" type="{http://www.museion.cz/NalezyAmcrService}predmet" maxOccurs="unbounded" minOccurs="0"/>
 *         <element name="predmetSys" type="{http://www.museion.cz/NalezyAmcrService}predmet" maxOccurs="unbounded" minOccurs="0"/>
<xs:element name="pristup" type="xs:string"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * </pre>
 * 
 * 
 */
    
//    @JacksonXmlProperty(localName = "pristup")
//    public String pristup;
    
    @JacksonXmlProperty(localName = "organizaceId") 
    public String organizaceId;
    
    @JacksonXmlProperty(localName = "pocetPom")
    public String pocetPom;
    
    @JacksonXmlProperty(localName = "pocetSys")
    public String pocetSys;
    
    @JacksonXmlProperty(localName = "predmetSys")
    public List<Predmet> predmetSys;
    
    @JacksonXmlProperty(localName = "predmetPom")
    public List<Predmet> predmetPom;
    
}
