package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author alberto
 */
public class ADB implements FedoraModel {

  @Field
  public String entity = "adb";

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
  @JacksonXmlProperty(localName = "ident_cely")
  @Field
  public String ident_cely;

//<xs:element name="dokumentacni_jednotka" minOccurs="1" maxOccurs="1" type="amcr:refType"/> <!-- "{dokumentacni_jednotka.archeologicky_zaznam.ident_cely}" | "{dokumentacni_jednotka.ident_cely}" -->
  @JacksonXmlProperty(localName = "dokumentacni_jednotka")
  public Vocab dokumentacni_jednotka;

//<xs:element name="stav_pom" minOccurs="1" maxOccurs="1" type="xs:integer"/> <!-- "{dokumentacni_jednotka.archeologicky_zaznam.stav}" -->
  @JacksonXmlProperty(localName = "stav_pom")
  @Field
  public int stav_pom;

//<xs:element name="typ_sondy" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ_sondy.ident_cely}" | "{typ_sondy.heslo}" -->
  @JacksonXmlProperty(localName = "typ_sondy")
  public Vocab typ_sondy;

//<xs:element name="podnet" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{podnet.ident_cely}" | "{podnet.heslo}" -->
  @JacksonXmlProperty(localName = "podnet")
  public Vocab podnet;

//<xs:element name="stratigraficke_jednotky" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{stratigraficke_jednotky}" -->
  @JacksonXmlProperty(localName = "stratigraficke_jednotky")
  @Field
  public String stratigraficke_jednotky;

//<xs:element name="autor_popisu" minOccurs="1" maxOccurs="1" type="amcr:refType"/> <!-- "{autor_popisu.ident_cely}" | "{autor_popisu.vypis_cely}" -->
  @JacksonXmlProperty(localName = "autor_popisu")
  public Vocab autor_popisu;

//<xs:element name="rok_popisu" minOccurs="1" maxOccurs="1" type="xs:integer"/> <!-- "{rok_popisu}" -->
  @JacksonXmlProperty(localName = "rok_popisu")
  @Field
  public int rok_popisu;

//<xs:element name="autor_revize" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{autor_revize.ident_cely}" | "{autor_revize.vypis_cely}" -->
  @JacksonXmlProperty(localName = "autor_revize")
  public Vocab autor_revize;

//<xs:element name="rok_revize" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{rok_revize}" -->
  @JacksonXmlProperty(localName = "rok_revize")
  @Field
  public int rok_revize;

//<xs:element name="sm5" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{sm5.mapno}" -->
  @JacksonXmlProperty(localName = "sm5")
  @Field
  public String sm5;

//<xs:element name="pristupnost_pom" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{dokumentacni_jednotka.archeologicky_zaznam.pristupnost.ident_cely}" | "{dokumentacni_jednotka.archeologicky_zaznam.pristupnost.heslo}" -->
  @JacksonXmlProperty(localName = "pristupnost_pom")
  public Vocab pristupnost;

//<xs:element name="chranene_udaje" minOccurs="0" maxOccurs="1" type="amcr:adb-chranene_udajeType"/> <!-- SELF -->
  @JacksonXmlProperty(localName = "chranene_udaje")
  private ADBChraneneUdaje chranene_udaje;

      
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
    idoc.setField("searchable", true);
    
    SolrSearcher.addVocabField(idoc, "dokumentacni_jednotka", dokumentacni_jednotka);
    SolrSearcher.addVocabField(idoc, "typ_sondy", typ_sondy);
    SolrSearcher.addVocabField(idoc, "podnet", podnet);
    SolrSearcher.addVocabField(idoc, "autor_popisu", autor_popisu);
    SolrSearcher.addVocabField(idoc, "autor_revize", autor_revize);
  
    if (chranene_udaje != null) {
      chranene_udaje.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
    }
  }
  
  
}


  class ADBChraneneUdaje {
    
//<xs:element name="uzivatelske_oznaceni_sondy" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{uzivatelske_oznaceni_sondy}" -->
    @JacksonXmlProperty(localName = "uzivatelske_oznaceni_sondy")
    public String uzivatelske_oznaceni_sondy;

//<xs:element name="trat" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{trat}" -->
    @JacksonXmlProperty(localName = "trat")
    public String trat;

//<xs:element name="cislo_popisne" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{cislo_popisne}" -->
    @JacksonXmlProperty(localName = "cislo_popisne")
    public String cislo_popisne;

//<xs:element name="parcelni_cislo" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{parcelni_cislo}" -->
    @JacksonXmlProperty(localName = "parcelni_cislo")
    public String parcelni_cislo;

//<xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
    @JacksonXmlProperty(localName = "poznamka")
    public String poznamka;

//<xs:element name="vyskovy_bod" minOccurs="0" maxOccurs="unbounded" type="amcr:vyskovy_bodType"/> <!-- "{vyskove_body}" -->
    @JacksonXmlProperty(localName = "vyskovy_bod")
    public List<VyskovyBod> vyskovy_bod = new ArrayList();

    
    public void fillSolrFields(SolrInputDocument idoc, String pristupnost) {
      SolrSearcher.addSecuredFieldNonRepeat(idoc, "uzivatelske_oznaceni_sondy", uzivatelske_oznaceni_sondy, pristupnost);
      SolrSearcher.addSecuredFieldNonRepeat(idoc, "trat", trat, pristupnost);
      SolrSearcher.addSecuredFieldNonRepeat(idoc, "cislo_popisne", cislo_popisne, pristupnost);
      SolrSearcher.addSecuredFieldNonRepeat(idoc, "parcelni_cislo", parcelni_cislo, pristupnost);
      SolrSearcher.addSecuredFieldNonRepeat(idoc, "poznamka", poznamka, pristupnost);
      for (VyskovyBod v: vyskovy_bod) {
        try {
          ObjectMapper objectMapper = new ObjectMapper();
          idoc.addField("sec_vyskovy_bod", objectMapper.writeValueAsString(v));
        } catch (JsonProcessingException ex) {
          Logger.getLogger(ADBChraneneUdaje.class.getName()).log(Level.SEVERE, null, ex);
        }
      }

    }
  }
