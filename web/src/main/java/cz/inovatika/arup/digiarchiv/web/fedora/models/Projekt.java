package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;

/**
 *
 * @author alberto
 */


@JacksonXmlRootElement(localName = "projekt")
public class Projekt implements FedoraModel {
  
  @Field
  public String entity = "projekt";

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
  @JacksonXmlProperty(localName = "ident_cely")
  @Field
  public String ident_cely;
  
//<xs:element name="stav" minOccurs="1" maxOccurs="1" type="xs:integer"/> <!-- "{stav}" -->
  @JacksonXmlProperty(localName = "stav")
  @Field
  public long stav;
  
//<xs:element name="typ_projektu" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ_projektu.ident_cely}" | "{typ_projektu.heslo}" -->
  @JacksonXmlProperty(localName = "typ_projektu")
  public Vocab typ_projektu;
  
//<xs:element name="okres" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "ruian-{hlavni_katastr.okres.kod}" | "{hlavni_katastr.okres.nazev}" -->
  @JacksonXmlProperty(localName = "okres")
  public Vocab okres;
  
//<xs:element name="podnet" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{podnet}" -->
  @JacksonXmlProperty(localName = "podnet")
  @Field
  public String podnet;
  
//<xs:element name="planovane_zahajeni" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{planovane_zahajeni_str}" -->
  @JacksonXmlProperty(localName = "planovane_zahajeni")
  @Field
  public String planovane_zahajeni;
  
//<xs:element name="vedouci_projektu" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{vedouci_projektu.ident_cely}" | "{vedouci_projektu.vypis_cely}" -->
  @JacksonXmlProperty(localName = "vedouci_projektu")
  public Vocab vedouci_projektu;
  
//<xs:element name="organizace" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{organizace.ident_cely}" | "{organizace.nazev}" -->
  @JacksonXmlProperty(localName = "organizace")
  public Vocab organizace;
  
//<xs:element name="uzivatelske_oznaceni" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{uzivatelske_oznaceni}" -->
  @JacksonXmlProperty(localName = "uzivatelske_oznaceni")
  @Field
  public String uzivatelske_oznaceni;
  
//<xs:element name="oznaceni_stavby" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{oznaceni_stavby}" -->
  @JacksonXmlProperty(localName = "oznaceni_stavby")
  @Field
  public String oznaceni_stavby;
  
//<xs:element name="kulturni_pamatka" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{kulturni_pamatka.ident_cely}" | "{kulturni_pamatka.heslo}" -->
  @JacksonXmlProperty(localName = "kulturni_pamatka")
  public Vocab kulturni_pamatka;
  
//<xs:element name="datum_zahajeni" minOccurs="0" maxOccurs="1" type="xs:date"/> <!-- "{datum_zahajeni}" -->
  @JacksonXmlProperty(localName = "datum_zahajeni")
  @Field
  public Date datum_zahajeni;
  
//<xs:element name="datum_ukonceni" minOccurs="0" maxOccurs="1" type="xs:date"/> <!-- "{datum_ukonceni}" -->
  @JacksonXmlProperty(localName = "datum_ukonceni")
  @Field
  public Date datum_ukonceni;
  
//<xs:element name="termin_odevzdani_nz" minOccurs="0" maxOccurs="1" type="xs:date"/> <!-- "{termin_odevzdani_nz}" -->
  @JacksonXmlProperty(localName = "termin_odevzdani_nz")
  @Field
  public Date termin_odevzdani_nz;
  
//<xs:element name="pristupnost_pom" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{pristupnost.ident_cely}" | "{pristupnost.heslo}" -->
  @JacksonXmlProperty(localName = "pristupnost_pom")
  public Vocab pristupnost;
  
//<xs:element name="chranene_udaje" minOccurs="0" maxOccurs="1" type="amcr:projekt-chranene_udajeType"/> <!-- self -->
  @JacksonXmlProperty(localName = "chranene_udaje")
  public ProjektChraneneUdaje chranene_udaje;
  
//<xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/> <!-- "{historie.historie_set}" -->
  @JacksonXmlProperty(localName = "historie")
  public List<Historie> historie = new ArrayList();

//<xs:element name="oznamovatel" minOccurs="0" maxOccurs="1" type="amcr:oznamovatelType"/> <!-- "{oznamovatel}" -->
  @JacksonXmlProperty(localName = "oznamovatel")
  public Oznamovatel oznamovatel;
  
//<xs:element name="soubor" minOccurs="0" maxOccurs="unbounded" type="amcr:souborType"/>  <!-- {soubory.soubory} -->
  @JacksonXmlProperty(localName = "soubor")
  public List<Soubor> soubor = new ArrayList();

//<xs:element name="archeologicky_zaznam" minOccurs="0" maxOccurs="unbounded" type="amcr:refType"/> <!-- "{akce_set.archeologicky_zaznam.ident_cely}" | "{akce_set.archeologicky_zaznam.ident_cely}" -->
  @JacksonXmlProperty(localName = "archeologicky_zaznam")
  public List<Vocab> archeologicky_zaznam = new ArrayList();
  
//<xs:element name="samostatny_nalez" minOccurs="0" maxOccurs="unbounded" type="amcr:refType"/> <!-- "{samostatne_nalezy.ident_cely}" | "{samostatne_nalezy.ident_cely}" -->
  @JacksonXmlProperty(localName = "samostatny_nalez")
  public List<Vocab> samostatny_nalez = new ArrayList();
  
//<xs:element name="dokument" minOccurs="0" maxOccurs="unbounded" type="amcr:refType"/> <!-- "{casti_dokumentu.dokument.ident_cely}" | "{casti_dokumentu.dokument.ident_cely}" -->
  @JacksonXmlProperty(localName = "dokument")
  public List<Vocab> dokument = new ArrayList();
  

  @Override
  public SolrInputDocument createOAIDocument(String xml) {
    SolrInputDocument idoc = new SolrInputDocument();
    idoc.setField("ident_cely", ident_cely);
    idoc.setField("model", "projekt");
    idoc.setField("pristupnost", SearchUtils.getPristupnostMap().get(pristupnost.getId()));
    idoc.setField("xml", xml);
    return idoc;
  }

  @Override
  public void fillSolrFields(SolrInputDocument idoc) {
    idoc.setField("pristupnost", SearchUtils.getPristupnostMap().get(pristupnost.getId()));
    idoc.setField("searchable", true);
    IndexUtils.setDateStamp(idoc, historie);
    
    IndexUtils.addVocabField(idoc, "okres", okres);
    idoc.setField("typ_projektu", typ_projektu.getId());
    IndexUtils.addVocabField(idoc, "vedouci_projektu", vedouci_projektu);
    IndexUtils.addVocabField(idoc, "organizace", organizace);
    IndexUtils.addVocabField(idoc, "kulturni_pamatka", kulturni_pamatka);
    
    
    for(Vocab v : archeologicky_zaznam) {
      idoc.setField("archeologicky_zaznam", v.getValue());
    }
    
    for(Vocab v : samostatny_nalez) {
      idoc.setField("samostatny_nalez", v.getValue());
    }
    
    for(Vocab v : dokument) {
      idoc.setField("dokument", v.getValue());
    }
    
    List<SolrInputDocument> idocs = new ArrayList<>();
    try {
      for (Soubor s : soubor) {
        SolrInputDocument djdoc = s.createSolrDoc();
        idocs.add(djdoc);
        IndexUtils.addJSONField(idoc, "soubor", s);
      }
      if (!idocs.isEmpty()) {
        IndexUtils.getClient().add("soubor", idocs, 10);
      }
    } catch (SolrServerException | IOException ex) {
      Logger.getLogger(Projekt.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    if (chranene_udaje != null) {
      chranene_udaje.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
    }
  }

  @Override
  public boolean isOAI() {
    return true;
  }

  @Override
  public String coreName() {
    return "entities";
  }
}

class ProjektChraneneUdaje {
  
//<xs:element name="hlavni_katastr" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "ruian-{hlavni_katastr.kod}" | "{hlavni_katastr.nazev}" -->
  @JacksonXmlProperty(localName = "hlavni_katastr")
  public Vocab hlavni_katastr;
  
//<xs:element name="dalsi_katastr" minOccurs="0" maxOccurs="unbounded" type="amcr:vocabType"/> <!-- "ruian-{katastry.kod}" | "{katastry.nazev}" -->
  @JacksonXmlProperty(localName = "dalsi_katastr")
  public List<Vocab> dalsi_katastr = new ArrayList<>();
  
//<xs:element name="lokalizace" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{lokalizace}" -->
  @JacksonXmlProperty(localName = "lokalizace")
  public String lokalizace;
  
//<xs:element name="parcelni_cislo" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{parcelni_cislo}" -->
  @JacksonXmlProperty(localName = "parcelni_cislo")
  public String parcelni_cislo;
  
//<xs:element name="geom_gml" minOccurs="0" maxOccurs="1" type="amcr:gmlType"/> <!-- ST_AsGML("{geom}") -->
//<xs:element name="geom_wkt" minOccurs="0" maxOccurs="1" type="amcr:wktType"/> <!-- ST_SRID("{geom}") | ST_AsText("{geom}") -->
  @JacksonXmlProperty(localName = "geom_wkt")
  public WKT geom_wkt;
  
//<xs:element name="kulturni_pamatka_cislo" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{kulturni_pamatka_cislo}" -->
  @JacksonXmlProperty(localName = "kulturni_pamatka_cislo")
  public String kulturni_pamatka_cislo;
  
//<xs:element name="kulturni_pamatka_popis" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{kulturni_pamatka_popis}" -->
  @JacksonXmlProperty(localName = "kulturni_pamatka_popis")
  public String kulturni_pamatka_popis;
  
  public void fillSolrFields(SolrInputDocument idoc, String pristupnost) {
    IndexUtils.setSecuredJSONField(idoc, this);
    
    IndexUtils.addSecuredFieldNonRepeat(idoc, "hlavni_katastr", hlavni_katastr.getValue(), pristupnost);
    
    for (Vocab v: dalsi_katastr) {
      IndexUtils.addSecuredFieldNonRepeat(idoc, "dalsi_katastr", v.getValue(), pristupnost);
    }

//    IndexUtils.addSecuredFieldNonRepeat(idoc, "lokalizace", lokalizace, pristupnost);
//    IndexUtils.addSecuredFieldNonRepeat(idoc, "parcelni_cislo", parcelni_cislo, pristupnost);
//    IndexUtils.addSecuredFieldNonRepeat(idoc, "kulturni_pamatka_cislo", kulturni_pamatka_cislo, pristupnost);
//    IndexUtils.addSecuredFieldNonRepeat(idoc, "kulturni_pamatka_popis", kulturni_pamatka_popis, pristupnost);
    
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
