/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web.fedora;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author alberto
 */
public interface FedoraModel {
  
  public SolrInputDocument createOAIDocument(String xml);
  public SolrInputDocument createEntityDocument();
  
  public static <T> FedoraModel parseXml(String xml, Class<T> clazz) throws XMLStreamException, IOException{
    XMLInputFactory f = XMLInputFactory.newFactory();
    // XMLInputFactory f = new WstxInputFactory();
    //f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    XMLStreamReader sr = f.createXMLStreamReader(new StringReader(xml));
    XmlMapper xmlMapper = new XmlMapper();
    xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    sr.nextTag();
    while (!sr.getLocalName().equals("projekt")) {
      sr.nextTag();
    }
    return (FedoraModel) xmlMapper.readValue(sr, clazz);
  }
  
  public static Class getModelClass(String model) {
    switch (model) {
      case "projekt":
        return Projekt.class;
//      case "samostatny_nalez":
//        return SamostatniNalez.class;
//      case "knihovna_3d":
//        return Dokument.class;
      default:
        return null;
    }
  }
}
