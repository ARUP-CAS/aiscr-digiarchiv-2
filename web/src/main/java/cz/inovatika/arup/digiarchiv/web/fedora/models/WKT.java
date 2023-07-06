/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

/**
 *
 * @author alberto
 */
public class WKT {

  @JacksonXmlProperty(isAttribute = true, localName = "EPSG")
  private long epsg;
  @JacksonXmlText
  private String value;

  public long getEpsg() {
    return epsg;
  }

  public void setKey(String epsg) {
    this.epsg = epsg;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

}
