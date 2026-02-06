package cz.inovatika.arup.digiarchiv.web4.fedora;

import com.fasterxml.jackson.core.JsonProcessingException;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.Projekt;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import cz.inovatika.arup.digiarchiv.web4.Options;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.ADB;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.ArcheologickyZaznam;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.Dokument;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.ExtZdroj;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.Heslo;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.Let;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.Organizace;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.Osoba;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.PIAN;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.RUIANKatastr;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.RUIANKraj;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.RUIANOkres;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.SamostatnyNalez;
import cz.inovatika.arup.digiarchiv.web4.fedora.models.Uzivatel;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public interface FedoraModel {
            
    
    public static XmlMapper xmlMapper = new XmlMapper(); 
        
    /**
     *
     * @param val
     * @return xml 
     * @throws com.fasterxml.jackson.core.JsonProcessingException 
     */
    public static Object getAsXml(Object val) throws JsonProcessingException {
        if (val == null) {
            return val;
        }
        String xml = xmlMapper.writeValueAsString(val);
        xml = xml.replaceAll("<LinkedHashMap>", "");
        xml = xml.replaceAll("</LinkedHashMap>", "");
        xml = xml.replaceAll("><srsName>", " srsName=\"");
        return xml.replaceAll("</srsName>", "\">");
    }

    /**
     *
     * @param model
     * @return true for models that should expose in OAI
     */
    public static boolean isOAI(String model) {
        JSONArray sets = Options.getInstance().getJSONObject("OAI").getJSONArray("sets");
        List<Object> l = sets.toList();
        return l.contains(model) || "knihovna_3d".equals(model);
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
    public void fillSolrFields(SolrInputDocument idoc) throws Exception;

    /**
     * @return true if record should be indexed
     */
    public boolean isSearchable();

    /**
     * Filter oai record based on security
     *
     * @param user
     * @param doc
     * @return 
     */
    public boolean filterOAI(JSONObject user, SolrDocument doc);

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
        } catch (Exception ex) {
            Logger.getLogger(FedoraModel.class.getName()).log(Level.SEVERE, "Error parsing {0}", xml);
            // Logger.getLogger(FedoraModel.class.getName()).log(Level.SEVERE, null, ex);
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
                return Organizace.class;
            case "dokument":
                return Dokument.class;
            case "uzivatel":
                return Uzivatel.class;
            case "osoba":
                return Osoba.class;
            case "ruian_kraj":
                return RUIANKraj.class;
            case "ruian_okres":
                return RUIANOkres.class;
            case "ruian_katastr":
                return RUIANKatastr.class;
//      case "knihovna_3d":
//        return Dokument.class;
            default:
                return null;
        }
    }
    
    
    /**
     * Returns FedoraModel instance by model name
     *
     * @param model
     * @return
     */
    public static FedoraModel getFedoraModel(String model) {
        switch (model) {
            case "adb":
                return new ADB();
            case "let":
                return new Let();
            case "ext_zdroj":
                return new ExtZdroj();
            case "pian":
                return new PIAN();
            case "archeologicky_zaznam":
            case "archeologicky_zaznam:akce":
            case "archeologicky_zaznam:lokalita":
            case "akce":
            case "lokalita":
                return new ArcheologickyZaznam();
            case "projekt":
                return new Projekt();
            case "samostatny_nalez":
                return new SamostatnyNalez();
            case "heslo":
            case "heslo:adb_podnet":
                case "heslo:adb_typ":
                case "heslo:akce_typ":
                case "heslo:akce_typ_kat":
                case "heslo:aktivita":
                case "heslo:areal":
                case "heslo:areal_kat":
                case "heslo:datum_specifikace":
                case "heslo:dohlednost":
                case "heslo:dok_jednotka_typ":
                case "heslo:dokument_format":
                case "heslo:dokument_licence":
                case "heslo:dokument_material":
                case "heslo:dokument_nahrada":
                case "heslo:dokument_rada":
                case "heslo:dokument_typ":
                case "heslo:dokument_ulozeni":
                case "heslo:dokument_zachovalost":
                case "heslo:ext_zdroj_typ":
                case "heslo:jazyk":
                case "heslo:jistota_urceni":
                case "heslo:letfoto_tvar":
                case "heslo:letiste":
                case "heslo:lokalita_druh":
                case "heslo:lokalita_druh_kat":
                case "heslo:lokalita_typ":
                case "heslo:nalez_typ":
                case "heslo:nalezove_okolnosti":
                case "heslo:obdobi":
                case "heslo:obdobi_kat":
                case "heslo:objekt_druh":
                case "heslo:objekt_druh_kat":
                case "heslo:objekt_specifikace":
                case "heslo:objekt_specifikace_kat":
                case "heslo:organizace_typ":
                case "heslo:pamatkova_ochrana":
                case "heslo:pian_presnost":
                case "heslo:pian_typ":
                case "heslo:pocasi":
                case "heslo:posudek_typ":
                case "heslo:posudek_typ_kat":
                case "heslo:predmet_druh":
                case "heslo:predmet_druh_kat":
                case "heslo:predmet_specifikace":
                case "heslo:pristupnost":
                case "heslo:projekt_typ":
                case "heslo:stav_dochovani":
                case "heslo:udalost_typ":
                case "heslo:vyskovy_bod_typ":
                case "heslo:zeme":                
                return new Heslo();
            case "organizace":
                return new Organizace();
            case "dokument":
            case "dokument:3d":
            case "knihovna_3d":
                return new Dokument();
            case "uzivatel":
                return new Uzivatel();
            case "osoba":
                return new Osoba();
            case "ruian_kraj":
                return new RUIANKraj();
            case "ruian_okres":
                return new RUIANOkres();
            case "ruian_katastr":
                return new RUIANKatastr();
            default:
                return null;
        }
    }
}
