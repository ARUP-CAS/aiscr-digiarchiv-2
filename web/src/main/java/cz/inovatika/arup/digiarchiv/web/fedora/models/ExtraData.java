package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;

/**
 *
 * @author alberto
 */
public class ExtraData {

    @Field
    public String entity = "extra_data";

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
    @JacksonXmlProperty(localName = "ident_cely")
    @Field
    public String ident_cely;

//  <xs:element name="stav" minOccurs="1" maxOccurs="1" type="xs:integer"/> <!-- "{stav}" -->
    @JacksonXmlProperty(localName = "stav")
    @Field
    public long stav;

//<xs:element name="cislo_objektu" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{cislo_objektu}" -->
    @JacksonXmlProperty(localName = "cislo_objektu")
    @Field
    public String cislo_objektu;

//<xs:element name="format" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{format.ident_cely}" | "{format.heslo}" -->
    @JacksonXmlProperty(localName = "format")
    public Vocab format;

//<xs:element name="meritko" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{meritko}" -->
    @JacksonXmlProperty(localName = "meritko")
    @Field
    public String meritko;

//<xs:element name="vyska" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{vyska}" -->
    @JacksonXmlProperty(localName = "vyska")
    @Field
    public long vyska;

//<xs:element name="sirka" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{sirka}" -->
    @JacksonXmlProperty(localName = "sirka")
    @Field
    public long sirka;

//<xs:element name="zachovalost" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{zachovalost.ident_cely}" | "{zachovalost.heslo}" -->
    @JacksonXmlProperty(localName = "zachovalost")
    public Vocab zachovalost;

//<xs:element name="nahrada" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{nahrada.ident_cely}" | "{nahrada.heslo}" -->
    @JacksonXmlProperty(localName = "nahrada")
    public Vocab nahrada;

//<xs:element name="pocet_variant_originalu" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{pocet_variant_originalu}" -->
    @JacksonXmlProperty(localName = "pocet_variant_originalu")
    @Field
    public long pocet_variant_originalu;

//<xs:element name="odkaz" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{odkaz}" -->
    @JacksonXmlProperty(localName = "odkaz")
    @Field
    public String odkaz;

//<xs:element name="datum_vzniku" minOccurs="0" maxOccurs="1" type="xs:date"/> <!-- "{datum_vzniku}" -->
    @JacksonXmlProperty(localName = "datum_vzniku")
    @Field
    public Date datum_vzniku;

//<xs:element name="udalost_typ" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{udalost_typ.ident_cely}" | "{udalost_typ.heslo}" -->
    @JacksonXmlProperty(localName = "udalost_typ")
    public Vocab udalost_typ;

//<xs:element name="udalost" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{udalost}" -->
    @JacksonXmlProperty(localName = "udalost")
    @Field
    public String udalost;

//<xs:element name="zeme" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{zeme.ident_cely}" | "{zeme.heslo}" -->
    @JacksonXmlProperty(localName = "zeme")
    public Vocab zeme;

//<xs:element name="region" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{region}" -->
    @JacksonXmlProperty(localName = "region")
    @Field
    public String region;

//<xs:element name="rok_od" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{rok_od}" -->
    @JacksonXmlProperty(localName = "rok_od")
    @Field
    public long rok_od;

//<xs:element name="rok_do" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{rok_do}" -->
    @JacksonXmlProperty(localName = "rok_do")
    @Field
    public long rok_do;

//<xs:element name="duveryhodnost" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{duveryhodnost}" -->
    @JacksonXmlProperty(localName = "duveryhodnost")
    @Field
    public long duveryhodnost;

//<xs:element name="geom_gml" minOccurs="0" maxOccurs="1" type="amcr:gmlType"/> <!-- ST_AsGML("{geom}") -->
    @JacksonXmlProperty(localName = "geom_gml")
    public Object geom_gml;

//<xs:element name="geom_wkt" minOccurs="0" maxOccurs="1" type="amcr:wktType"/> <!-- ST_SRID("{geom}") | ST_AsText("{geom}") -->
    @JacksonXmlProperty(localName = "geom_wkt")
    public WKT geom_wkt;

    public void fillSolrFields(SolrInputDocument idoc, String prefix, String pristupnost) {
//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//            idoc.addField(prefix + "_" + "extra_data", objectMapper.writeValueAsString(this));
//        } catch (JsonProcessingException ex) {
//            Logger.getLogger(DokumentacniJednotka.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        IndexUtils.addVocabField(idoc, "extra_data_format", format);
//        IndexUtils.addVocabField(idoc, "extra_data_zachovalost", zachovalost);
//        IndexUtils.addVocabField(idoc, "extra_data_nahrada", nahrada);
//        IndexUtils.addVocabField(idoc, "extra_data_udalost_typ", udalost_typ);
//        IndexUtils.addVocabField(idoc, "extra_data_zeme", zeme);
//        IndexUtils.addJSONField(idoc, "extra_data_geom_gml", geom_gml);
//        IndexUtils.addJSONField(idoc, "extra_data_geom_wkt", geom_wkt);
        if (geom_wkt != null) {

            String wktStr = geom_wkt.getValue();
            final WKTReader reader = new WKTReader();
            try {
                Geometry geometry = reader.read(wktStr);
                Point p = geometry.getCentroid();
                IndexUtils.addSecuredFieldNonRepeat(idoc, "lng", p.getX(), pristupnost);
                IndexUtils.addSecuredFieldNonRepeat(idoc, "lat", p.getY(), pristupnost);
                IndexUtils.addSecuredFieldNonRepeat(idoc, "loc", p.getY() + "," + p.getX(), pristupnost);
                IndexUtils.addSecuredFieldNonRepeat(idoc, "loc_rpt", p.getY() + "," + p.getX(), pristupnost);
            } catch (Exception e) {
                throw new RuntimeException(String.format("Can't parse string %s as WKT", wktStr));
            }

        } 

    }

}
