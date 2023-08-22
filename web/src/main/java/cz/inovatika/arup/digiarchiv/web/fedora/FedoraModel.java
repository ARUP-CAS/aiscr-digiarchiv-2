/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web.fedora;

import cz.inovatika.arup.digiarchiv.web.fedora.models.Projekt;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.fedora.models.ADB;
import cz.inovatika.arup.digiarchiv.web.fedora.models.ArcheologickyZaznam;
import cz.inovatika.arup.digiarchiv.web.fedora.models.Dokument;
import cz.inovatika.arup.digiarchiv.web.fedora.models.ExtZdroj;
import cz.inovatika.arup.digiarchiv.web.fedora.models.Heslo;
import cz.inovatika.arup.digiarchiv.web.fedora.models.Let;
import cz.inovatika.arup.digiarchiv.web.fedora.models.PIAN;
import cz.inovatika.arup.digiarchiv.web.fedora.models.SamostatnyNalez;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public interface FedoraModel {

    /**
     *
     * @param model
     * @return true for models that should expose in OAI
     */
    public static boolean isOAI(String model) {
        JSONArray sets = Options.getInstance().getJSONObject("OAI").getJSONArray("sets");
        for (int i = 0; i < sets.length(); i++) {
            if (model.equals(sets.getJSONObject(i).getString("spec"))) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @return the core to index to (entities, heslar, organizations)
     */
    public String coreName();

    /**
     * Set all fields for entities core
     *
     * @param idoc
     */
    public void fillSolrFields(SolrInputDocument idoc);

    /**
     * Find Fedora model name in xml
     *
     * @param xml Full xml string
     * @return FedoraModel name
     * @throws XMLStreamException
     * @throws IOException
     */
    public static String getModel(String xml) throws XMLStreamException, IOException {
        XMLInputFactory f = XMLInputFactory.newFactory();
        XMLStreamReader sr = f.createXMLStreamReader(new StringReader(xml));
//    XmlMapper xmlMapper = new XmlMapper();
//    xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        sr.nextTag();
        sr.nextTag();
        return sr.getLocalName();
//    while (!sr.getLocalName().equals("projekt")) {
//      sr.nextTag();
//    }
//    return (FedoraModel) xmlMapper.readValue(sr, clazz);
    }
    
    static XMLInputFactory _xmlFactory = XMLInputFactory.newFactory();
    

    /**
     * Deserialize amcr xml document to java class
     *
     * @param <T>
     * @param xml Full xml string
     * @param clazz The class to map to
     * @return Object implementing FedoraModel
     * @throws XMLStreamException
     * @throws IOException
     */
    public static <T> FedoraModel parseXml(String xml, Class<T> clazz) throws Exception {
        try {
            // XMLInputFactory f = XMLInputFactory.newFactory();
            XMLStreamReader sr = _xmlFactory.createXMLStreamReader(new StringReader(xml));
            JacksonXmlModule module = new JacksonXmlModule();
            module.setDefaultUseWrapper(false);
            XmlMapper xmlMapper = new XmlMapper(module);
            xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            xmlMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

            sr.nextTag();
            sr.nextTag();
            return (FedoraModel) xmlMapper.readValue(sr, clazz);
        } catch (IOException | XMLStreamException ex) {
            Logger.getLogger(FedoraModel.class.getName()).log(Level.SEVERE, "Error parsing {0}", xml);
            Logger.getLogger(FedoraModel.class.getName()).log(Level.SEVERE, null, ex);
            throw new Exception(ex);
        }
    }

    /**
     * Finds FedoraModel class by model name
     *
     * @param model
     * @return
     */
    public static Class getModelClass(String model) {
        switch (model) {
            case "adb":
                return ADB.class;
            case "let":
                return Let.class;
            case "ext_zdroj":
                return ExtZdroj.class;
            case "pian":
                return PIAN.class;
            case "archeologicky_zaznam":
                return ArcheologickyZaznam.class;
            case "projekt":
                return Projekt.class;
            case "samostatny_nalez":
                return SamostatnyNalez.class;
            case "heslo":
                return Heslo.class;
            case "organizace":
                return Heslo.class;
            case "dokument":
                return Dokument.class;
//      case "knihovna_3d":
//        return Dokument.class;
            default:
                return null;
        }
    }
}
