package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.json.JSONObject;

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
  public Vocab pian_typ;

//  <xs:element name="presnost" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{presnost.ident_cely}" | "{presnost.heslo}" -->
  @JacksonXmlProperty(localName = "presnost")
  public Vocab pian_presnost;

//  <xs:element name="geom_system" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{geom_system}" -->
  @JacksonXmlProperty(localName = "geom_system")
  @Field
  public String pian_geom_system;

//  <xs:element name="geom_updated_at" minOccurs="0" maxOccurs="1" type="xs:dateTime"/> <!-- "{geom_updated_at}" -->
  @JacksonXmlProperty(localName = "geom_updated_at")
  @Field
  public Date pian_geom_updated_at;

//  <xs:element name="geom_sjtsk_updated_at" minOccurs="0" maxOccurs="1" type="xs:dateTime"/> <!-- "{geom_sjtsk_updated_at}" -->
  @JacksonXmlProperty(localName = "geom_sjtsk_updated_at")
  @Field
  public Date pian_geom_sjtsk_updated_at;

//  <xs:element name="pristupnost_pom" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{pristupnost_pom.ident_cely}" | "{pristupnost_pom.heslo}" -->
  @JacksonXmlProperty(localName = "pristupnost_pom")
  public Vocab pristupnost;

//  <xs:element name="chranene_udaje" minOccurs="0" maxOccurs="1" type="amcr:pian-chranene_udajeType"/> <!-- SELF -->
  @JacksonXmlProperty(localName = "chranene_udaje")
  private PIANChraneneUdaje pian_chranene_udaje;

//  <xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/> <!-- "{historie.historie_set}" -->
    @JacksonXmlProperty(localName = "historie")
    public List<Historie> historie = new ArrayList();
  
  @Override
  public String coreName() {
    return "entities";
  }

  @Override
  public void fillSolrFields(SolrInputDocument idoc) {
    idoc.setField("searchable", !this.ident_cely.startsWith("N"));
    idoc.setField("pristupnost", SearchUtils.getPristupnostMap().get(pristupnost.getId()));
    IndexUtils.setDateStamp(idoc, ident_cely);
        IndexUtils.setDateStampFromHistory(idoc, historie);
    
    IndexUtils.addVocabField(idoc, "pian_typ", pian_typ);
    IndexUtils.addVocabField(idoc, "pian_presnost", pian_presnost);

    if (pian_chranene_udaje != null) {
      pian_chranene_udaje.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
    }
    // Add value of vocab fields
    for (String sufix : SolrSearcher.prSufixAll) {
        idoc.addField("text_all_" + sufix, ident_cely);
        IndexUtils.addRefField(idoc, "text_all_" + sufix, pian_typ);
    }
  }

    @Override  
    public boolean filterOAI(JSONObject user, SolrDocument doc) {
//-- A: stav = 2
//-- B-E: bez omezení
        long st = (long) doc.getFieldValue("stav");
        String userPr = user.optString("pristupnost", "A");
        if (userPr.compareToIgnoreCase("B") >= 0) {
            return true;
        } else if (userPr.compareToIgnoreCase("B") <= 0 && st == 2) {
            return true;
        } else {
            return false;
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
    IndexUtils.addSecuredFieldNonRepeat(idoc, "f_pian_chranene_udaje_zm10", zm10, pristupnost);
    IndexUtils.setSecuredJSONField(idoc, "pian_chranene_udaje", this);

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
