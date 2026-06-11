
package cz.inovatika.arup.digiarchiv.web4.museion;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.solr.client.solrj.beans.Field;

/**
 *
 * @author alber
 */
public class Predmet {
    
    
/**
 * <p>Java class for predmet complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * <complexType name="predmet">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="aktivita" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="areal" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="cislo" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         <element name="cisloCes" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="cisloEvidCes" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="dataceUrceni" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="dataceVzniku" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="datumNabyti" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         <element name="datumNalezu" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         <element name="datumStav" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="datumZapisu" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         <element name="druhObjektu" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="fond" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="hloubka" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="kompletnost" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="komponenta" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="kontextObjekt" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="kontextPlocha" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="kontextStratigrafie" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="material" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="mnozstviSlovy" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="okolnosti" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="oznaceni" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="pocetCasti" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="pocetKusu" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         <element name="podsbirka" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         <element name="popis" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="popisCasti" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="popisStav" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="poznamkaUrceni" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="prirustkoveCislo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="rozmer" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="sbirka" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         <element name="stav" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="technika" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * </pre>
 * 
 * 
 */
    
    @JacksonXmlProperty(localName = "aktivita")
    public String aktivita;
    
    @JacksonXmlProperty(localName = "areal")
    public String areal;
    
    @JacksonXmlProperty(localName = "cislo")
    public String cislo;
    
    @JacksonXmlProperty(localName = "cisloCes")
    public String cisloCes;
    
    @JacksonXmlProperty(localName = "cisloEvidCes")
    public String cisloEvidCes;
    
    @JacksonXmlProperty(localName = "dataceUrceni")
    public String dataceUrceni;
    
    @JacksonXmlProperty(localName = "dataceVzniku")
    public String dataceVzniku;
    
    @JacksonXmlProperty(localName = "datumNabyti")
    public XMLGregorianCalendar datumNabyti;
    
    @JacksonXmlProperty(localName = "datumNalezu")
    public XMLGregorianCalendar datumNalezu;
    
    @JacksonXmlProperty(localName = "datumStav")
    public String datumStav;
    
    @JacksonXmlProperty(localName = "datumZapisu")
    public XMLGregorianCalendar datumZapisu;
    
    @JacksonXmlProperty(localName = "druhObjektu")
    public String druhObjektu;
    
    @JacksonXmlProperty(localName = "fond")
    public String fond;
    
    @JacksonXmlProperty(localName = "hloubka")
    public String hloubka;
    
    @JacksonXmlProperty(localName = "kompletnost")
    public String kompletnost;
    
    @JacksonXmlProperty(localName = "komponenta")
    public String komponenta;
    
    @JacksonXmlProperty(localName = "kontextObjekt")
    public String kontextObjekt;
    
    @JacksonXmlProperty(localName = "kontextPlocha")
    public String kontextPlocha;
    
    @JacksonXmlProperty(localName = "kontextStratigrafie")
    public String kontextStratigrafie;
    
    @JacksonXmlProperty(localName = "material")
    public String material;
    
    @JacksonXmlProperty(localName = "mnozstviSlovy")
    public String mnozstviSlovy;
    
    @JacksonXmlProperty(localName = "okolnosti")
    public String okolnosti;
    
    @JacksonXmlProperty(localName = "oznaceni")
    public String oznaceni;
    
    @JacksonXmlProperty(localName = "pocetCasti")
    public String pocetCasti;
    
    @JacksonXmlProperty(localName = "pocetKusu")
    public int pocetKusu;
    
    @JacksonXmlProperty(localName = "podsbirka")
    public String podsbirka;
    
    @JacksonXmlProperty(localName = "popis")
    public String popis;
    
    @JacksonXmlProperty(localName = "popisCasti")
    public String popisCasti;
    
    @JacksonXmlProperty(localName = "popisStav")
    public String popisStav;
    
    @JacksonXmlProperty(localName = "poznamkaUrceni")
    public String poznamkaUrceni;
    
    @JacksonXmlProperty(localName = "prirustkoveCislo")
    public String prirustkoveCislo;
    
    @JacksonXmlProperty(localName = "rozmer")
    public String rozmer;
    
    @JacksonXmlProperty(localName = "sbirka")
    public String sbirka;
    
    @JacksonXmlProperty(localName = "stav")
    public String stav;
    
    @JacksonXmlProperty(localName = "technika") 
    public String technika;
}
