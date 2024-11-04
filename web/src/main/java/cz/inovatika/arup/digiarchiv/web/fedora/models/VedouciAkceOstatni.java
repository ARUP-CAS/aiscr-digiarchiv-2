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
public class VedouciAkceOstatni {
//<xs:element name="id" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "vedo-{id}" -->
  @JacksonXmlProperty(localName = "id")
  public String id;
  
//<xs:element name="vedouci" minOccurs="1" maxOccurs="1" type="amcr:refType"/> <!-- "{vedouci.ident_cely}" | "{vedouci.vypis_cely}" -->
  @JacksonXmlProperty(localName = "vedouci")
  public Vocab vedouci;
  
//<xs:element name="organizace" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{organizace.ident_cely}" | "{organizace.nazev}" -->
  @JacksonXmlProperty(localName = "organizace")
  public Vocab organizace;



}
