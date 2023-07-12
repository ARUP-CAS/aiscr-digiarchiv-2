/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 *
 * @author alberto
 */
public class VyskovyBod {
  
//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
  @JacksonXmlProperty(localName = "ident_cely")
  public String ident_cely;
  
//<xs:element name="typ" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ.ident_cely}" | "{typ.heslo}" -->
    @JacksonXmlProperty(localName = "typ")
    public String typ;
    
//<xs:element name="geom_gml" minOccurs="1" maxOccurs="1" type="amcr:gmlType"/> <!-- ST_AsGML("{geom}") -->
    @JacksonXmlProperty(localName = "geom_gml")
    public Object geom_gml;
    
//<xs:element name="geom_wkt" minOccurs="1" maxOccurs="1" type="amcr:wktType"/> <!-- ST_SRID("{geom}") | ST_AsText("{geom}") -->
    @JacksonXmlProperty(localName = "geom_wkt")
    public WKT geom_wkt;
    
}
