package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.util.Date;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;

/**
 *
 * @author alberto
 */
public class PIAN implements FedoraModel {

  @Field
  public String entity = "pian";

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
  @JacksonXmlProperty(localName = "ident_cely")
  @Field
  public String ident_cely;

//  <xs:element name="stav" minOccurs="1" maxOccurs="1" type="xs:integer"/> <!-- "{stav}" -->
  @JacksonXmlProperty(localName = "stav")
  @Field
  public long stav;

//  <xs:element name="typ" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ.ident_cely}" | "{typ.heslo}" -->
  @JacksonXmlProperty(localName = "typ")
  public Vocab typ;

//  <xs:element name="presnost" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{presnost.ident_cely}" | "{presnost.heslo}" -->
  @JacksonXmlProperty(localName = "presnost")
  public Vocab presnost;

//  <xs:element name="geom_system" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{geom_system}" -->
  @JacksonXmlProperty(localName = "geom_system")
  @Field
  public String geom_system;

//  <xs:element name="geom_updated_at" minOccurs="0" maxOccurs="1" type="xs:dateTime"/> <!-- "{geom_updated_at}" -->
  @JacksonXmlProperty(localName = "geom_updated_at")
  @Field
  public Date geom_updated_at;

//  <xs:element name="geom_sjtsk_updated_at" minOccurs="0" maxOccurs="1" type="xs:dateTime"/> <!-- "{geom_sjtsk_updated_at}" -->
  @JacksonXmlProperty(localName = "geom_sjtsk_updated_at")
  @Field
  public Date geom_sjtsk_updated_at;

//  <xs:element name="pristupnost_pom" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{pristupnost_pom.ident_cely}" | "{pristupnost_pom.heslo}" -->
  @JacksonXmlProperty(localName = "pristupnost_pom")
  public Vocab pristupnost;

//  <xs:element name="chranene_udaje" minOccurs="0" maxOccurs="1" type="amcr:pian-chranene_udajeType"/> <!-- SELF -->
  @JacksonXmlProperty(localName = "chranene_udaje")
  private PIANChraneneUdaje chranene_udaje;

//  <xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/> <!-- "{historie.historie_set}" -->
  @Override
  public boolean isOAI() {
    return true;
  }

  @Override
  public boolean isEntity() {
    return true;
  }

  @Override
  public boolean isHeslo() {
    return false;
  }

  @Override
  public SolrInputDocument createOAIDocument(String xml) {

    SolrInputDocument idoc = new SolrInputDocument();
    idoc.setField("ident_cely", ident_cely);
    idoc.setField("model", entity);
    idoc.setField("xml", xml);
    return idoc;
  }

  @Override
  public void fillSolrFields(SolrInputDocument idoc) {
    idoc.setField("searchable", !this.ident_cely.startsWith("N"));
    idoc.setField("pristupnost", SearchUtils.getPristupnostMap().get(pristupnost.getId()));

    IndexUtils.addVocabField(idoc, "typ", typ);
    IndexUtils.addVocabField(idoc, "presnost", presnost);

    if (chranene_udaje != null) {
      chranene_udaje.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
    }
  }

}

class PIANChraneneUdaje {

//<xs:element name="zm10" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{zm10.cislo}" -->
  @JacksonXmlProperty(localName = "zm10")
  public String zm10;

  //<xs:element name="geom_gml" minOccurs="0" maxOccurs="1" type="amcr:gmlType"/> <!-- ST_AsGML("{geom}") -->
  @JacksonXmlProperty(localName = "geom_gml")
  public Object geom_gml;

//<xs:element name="geom_wkt" minOccurs="0" maxOccurs="1" type="amcr:wktType"/> <!-- ST_SRID("{geom}") | ST_AsText("{geom}") -->
  @JacksonXmlProperty(localName = "geom_wkt")
  public WKT geom_wkt;

//<xs:element name="geom_sjtsk_gml" minOccurs="0" maxOccurs="1" type="amcr:gmlType"/> <!-- ST_AsGML("{geom_sjtsk}") -->
  @JacksonXmlProperty(localName = "geom_sjtsk_gml")
  public Object geom_sjtsk_gml;

//<xs:element name="geom_sjtsk_wkt" minOccurs="0" maxOccurs="1" type="amcr:wktType"/> <!-- ST_SRID("{geom_sjtsk}") | ST_AsText("{geom_sjtsk}") -->
  @JacksonXmlProperty(localName = "geom_sjtsk_wkt")
  public WKT geom_sjtsk_wkt;

  public void fillSolrFields(SolrInputDocument idoc, String pristupnost) {
    IndexUtils.addSecuredFieldNonRepeat(idoc, "f_pian_zm10", zm10, pristupnost);
    IndexUtils.addSecuredJSONField(idoc, this);
//    IndexUtils.addSecuredJSONField(idoc, "geom_gml", geom_gml);
//    IndexUtils.addSecuredJSONField(idoc, "geom_wkt", geom_wkt);
//    IndexUtils.addSecuredJSONField(idoc, "geom_sjtsk_gml", geom_sjtsk_gml);
//    IndexUtils.addSecuredJSONField(idoc, "geom_sjtsk_wkt", geom_sjtsk_wkt);

    if (geom_wkt != null) {

      String wktStr = geom_wkt.getValue();
      final WKTReader reader = new WKTReader();
      try {
        Geometry geometry = reader.read(wktStr);
        Point p = geometry.getCentroid();
        IndexUtils.addSecuredFieldNonRepeat(idoc, "centroid_e", p.getX(), pristupnost);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "centroid_n", p.getY(), pristupnost);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "loc", p.getY() + "," + p.getX(), pristupnost);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "loc_rpt", p.getY() + "," + p.getX(), pristupnost);
      } catch (Exception e) {
        throw new RuntimeException(String.format("Can't parse string %s as WKT", wktStr));
      }

    }

  }
}
