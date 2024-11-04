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
public class Lang {

  @JacksonXmlProperty(isAttribute = true, localName = "lang")
  private String lang;
  @JacksonXmlText
  private String value;

  public String getLang() {
    return lang;
  }

  public void setKey(String lang) {
    this.lang = lang;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

}
