package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author alberto
 */
@JacksonXmlRootElement(localName = "samostatny_nalez")
public class SamostatnyNalez implements FedoraModel {

  @Field
  public String entity = "samostatny_nalez";

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
  @JacksonXmlProperty(localName = "ident_cely")
  @Field
  public String ident_cely;

//<xs:element name="evidencni_cislo" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{evidencni_cislo}" -->
  @JacksonXmlProperty(localName = "evidencni_cislo")
  @Field
  public String evidencni_cislo;

//<xs:element name="projekt" minOccurs="1" maxOccurs="1" type="amcr:refType"/> <!-- "{projekt.ident_cely}" | "{projekt.ident_cely}" -->
  @JacksonXmlProperty(localName = "projekt")
  public Vocab projekt;

//<xs:element name="okres" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "ruian-{katastr.okres.kod}" | "{katastr.okres.nazev}" -->
  @JacksonXmlProperty(localName = "okres")
  public Vocab okres;

//<xs:element name="hloubka" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{hloubka}" -->
  @JacksonXmlProperty(localName = "hloubka")
  @Field
  public long hloubka;

//<xs:element name="okolnosti" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{okolnosti.ident_cely}" | "{okolnosti.heslo}" -->
  @JacksonXmlProperty(localName = "okolnosti")
  public Vocab okolnosti;

//<xs:element name="obdobi" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{obdobi.ident_cely}" | "{obdobi.heslo}" -->
  @JacksonXmlProperty(localName = "obdobi")
  public Vocab obdobi;

//<xs:element name="presna_datace" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{presna_datace}" -->
  @JacksonXmlProperty(localName = "presna_datace")
  @Field
  public String presna_datace;

//<xs:element name="druh_nalezu" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{druh_nalezu.ident_cely}" | "{druh_nalezu.heslo}" -->
  @JacksonXmlProperty(localName = "druh_nalezu")
  public Vocab druh_nalezu;

//<xs:element name="specifikace" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{specifikace.ident_cely}" | "{specifikace.heslo}" -->
  @JacksonXmlProperty(localName = "specifikace")
  public Vocab specifikace;

//<xs:element name="pocet" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{pocet}" -->
  @JacksonXmlProperty(localName = "pocet")
  @Field
  public String pocet;

//<xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
  @JacksonXmlProperty(localName = "poznamka")
  @Field
  public String poznamka;

//<xs:element name="nalezce" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{nalezce.ident_cely}" | "{nalezce.vypis_cely}" -->
  @JacksonXmlProperty(localName = "nalezce")
  public Vocab nalezce;

//<xs:element name="datum_nalezu" minOccurs="0" maxOccurs="1" type="xs:date"/> <!-- "{datum_nalezu}" -->
  @JacksonXmlProperty(localName = "datum_nalezu")
  @Field
  public Date datum_nalezu;

//<xs:element name="stav" minOccurs="1" maxOccurs="1" type="xs:integer"/> <!-- "{stav}" -->
  @JacksonXmlProperty(localName = "stav")
  @Field
  public long stav;

//<xs:element name="predano" minOccurs="0" maxOccurs="1" type="xs:boolean"/> <!-- "{predano}" -->
  @JacksonXmlProperty(localName = "predano")
  @Field
  public boolean predano;

//<xs:element name="predano_organizace" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{predano_organizace.ident_cely}" | "{predano_organizace.nazev}" -->
  @JacksonXmlProperty(localName = "predano_organizace")
  public Vocab predano_organizace;

//<xs:element name="geom_system" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{geom_system}" -->
  @JacksonXmlProperty(localName = "geom_system")
  @Field
  public String geom_system;

//<xs:element name="geom_updated_at" minOccurs="0" maxOccurs="1" type="xs:dateTime"/> <!-- "{geom_updated_at}" -->
  @JacksonXmlProperty(localName = "geom_updated_at")
  @Field
  public Date geom_updated_at;

//<xs:element name="geom_sjtsk_updated_at" minOccurs="0" maxOccurs="1" type="xs:dateTime"/> <!-- "{geom_sjtsk_updated_at}" -->
  @JacksonXmlProperty(localName = "geom_sjtsk_updated_at")
  @Field
  public Date geom_sjtsk_updated_at;

//<xs:element name="pristupnost" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{pristupnost.ident_cely}" | "{pristupnost.heslo}" -->
  @JacksonXmlProperty(localName = "pristupnost")
  public Vocab pristupnost;

//<xs:element name="chranene_udaje" minOccurs="0" maxOccurs="1" type="amcr:sn-chranene_udajeType"/> <!-- SELF -->
  @JacksonXmlProperty(localName = "chranene_udaje")
  public SnChraneneUdaje chranene_udaje;

//<xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/> <!-- "{historie.historie_set}" -->  
//<xs:element name="soubor" minOccurs="0" maxOccurs="unbounded" type="amcr:souborType"/>  <!-- "{soubory.soubory}" -->  
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
    idoc.setField("pristupnost", SearchUtils.getPristupnostMap().get(pristupnost.getId()));
    idoc.setField("xml", xml);
    return idoc;
  }

  @Override
  public void fillSolrFields(SolrInputDocument idoc) {
    idoc.setField("pristupnost", SearchUtils.getPristupnostMap().get(pristupnost.getId()));
    boolean searchable = stav == 4;
    idoc.setField("searchable", searchable);
    
    SolrSearcher.addVocabField(idoc, "okres", okres);
    SolrSearcher.addVocabField(idoc, "projekt", projekt);
    SolrSearcher.addVocabField(idoc, "okolnosti", okolnosti);
    SolrSearcher.addVocabField(idoc, "obdobi", obdobi);
    SolrSearcher.addVocabField(idoc, "druh_nalezu", druh_nalezu);
    SolrSearcher.addVocabField(idoc, "specifikace", specifikace);
    SolrSearcher.addVocabField(idoc, "nalezce", nalezce);
  
    if (chranene_udaje != null) {
      chranene_udaje.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
    }
  }
  
  
}


  class SnChraneneUdaje {

//<xs:element name="katastr" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "ruian-{katastr.kod}" | "{katastr.nazev}" -->
    @JacksonXmlProperty(localName = "katastr")
    public Vocab katastr;

//<xs:element name="lokalizace" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{lokalizace}" -->
    @JacksonXmlProperty(localName = "lokalizace")
    public String lokalizace;

//<xs:element name="geom_gml" minOccurs="0" maxOccurs="1" type="amcr:gmlType"/> <!-- ST_AsGML("{geom}") -->
//<xs:element name="geom_wkt" minOccurs="0" maxOccurs="1" type="amcr:wktType"/> <!-- ST_SRID("{geom}") | ST_AsText("{geom}") -->
    @JacksonXmlProperty(localName = "geom_wkt")
    public WKT geom_wkt;

//<xs:element name="geom_sjtsk_gml" minOccurs="0" maxOccurs="1" type="amcr:gmlType"/> <!-- ST_AsGML("{geom_sjtsk}") -->
//<xs:element name="geom_sjtsk_wkt" minOccurs="0" maxOccurs="1" type="amcr:wktType"/> <!-- ST_SRID("{geom_sjtsk}") | ST_AsText("{geom_sjtsk}") -->
    public void fillSolrFields(SolrInputDocument idoc, String pristupnost) {
      if (katastr != null) {
        idoc.setField("sec_katastr", katastr.getValue());
        SolrSearcher.addSecuredFieldNonRepeat(idoc, "f_katastr", katastr.getValue(), pristupnost);
      }

      if (lokalizace != null) {
        SolrSearcher.addSecuredFieldNonRepeat(idoc, "f_lokalizace", lokalizace, pristupnost);
      }

      if (geom_wkt != null) {
        idoc.setField("sec_geom_wkt", geom_wkt.getValue());
      }

    }
  }
