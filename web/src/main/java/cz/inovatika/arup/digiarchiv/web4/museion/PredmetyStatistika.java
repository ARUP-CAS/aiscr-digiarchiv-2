
package cz.inovatika.arup.digiarchiv.web4.museion;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

/**
 *
 * @author alber
 */
@JacksonXmlRootElement(localName = "predmetyDleAmcrIdResponse")
public class PredmetyStatistika {
    
/**

 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="amcrIdPom" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         <element name="amcrIdSys" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         <element name="organizaceId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         <element name="pocetPomAkce" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         <element name="pocetPomCelkem" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         <element name="pocetPomProjekt" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         <element name="pocetPomSamNalez" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         <element name="pocetSysAkce" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         <element name="pocetSysCelkem" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         <element name="pocetSysProjekt" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         <element name="pocetSysSamNalez" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * 
 * 
 * 
 */
    
    @JacksonXmlProperty(localName = "amcrIdPom")
    public List<AmcrEntita> amcrIdPom;
    
    @JacksonXmlProperty(localName = "amcrIdSys")
    public List<AmcrEntita> amcrIdSys;
    
    @JacksonXmlProperty(localName = "organizaceId") 
    public String organizaceId;
    
    @JacksonXmlProperty(localName = "pocetPomAkce")
    public int pocetPomAkce;
    
    @JacksonXmlProperty(localName = "pocetPomCelkem")
    public int pocetPomCelkem;
    
    @JacksonXmlProperty(localName = "pocetPomProjekt")
    public int pocetPomProjekt;
    
    @JacksonXmlProperty(localName = "pocetPomSamNalez")
    public int pocetPomSamNalez;
    
    @JacksonXmlProperty(localName = "pocetSysAkce")
    public int pocetSysAkce;
    
    @JacksonXmlProperty(localName = "pocetSysCelkem")
    public int pocetSysCelkem;
    
    @JacksonXmlProperty(localName = "pocetSysProjekt")
    public int pocetSysProjekt;
    
    @JacksonXmlProperty(localName = "pocetSysSamNalez")
    public int pocetSysSamNalez;
    
    public String end_point; 
    
}
