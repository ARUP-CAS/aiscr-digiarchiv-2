package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.util.Map.Entry;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;

/**
 *
 * @author alberto
 */
public class Lokalita {

//<xs:element name="typ_lokality" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ_lokality.ident_cely}" | "{typ_lokality.heslo}" -->
  @JacksonXmlProperty(localName = "typ_lokality")
  public Vocab typ_lokality;

//<xs:element name="druh" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{druh.ident_cely}" | "{druh.heslo}" -->
  @JacksonXmlProperty(localName = "druh")
  public Vocab druh;

//<xs:element name="zachovalost" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{zachovalost.ident_cely}" | "{zachovalost.heslo}" -->
  @JacksonXmlProperty(localName = "zachovalost")
  public Vocab zachovalost;

//<xs:element name="jistota" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{jistota.ident_cely}" | "{jistota.heslo}" -->
  @JacksonXmlProperty(localName = "jistota")
  public Vocab jistota;

//<xs:element name="chranene_udaje" minOccurs="0" maxOccurs="1" type="amcr:lok-chranene_udajeType"/> <!-- SELF -->  
  @JacksonXmlProperty(localName = "chranene_udaje")
  private LokalitaChraneneUdaje chranene_udaje;

  public void fillSolrFields(SolrInputDocument idoc) {
    SolrSearcher.addVocabField(idoc, "typ_lokality", typ_lokality);
    SolrSearcher.addVocabField(idoc, "druh", druh);
    SolrSearcher.addVocabField(idoc, "zachovalost", zachovalost);
    SolrSearcher.addVocabField(idoc, "jistota", jistota);
    
    if (chranene_udaje != null) {
      chranene_udaje.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
    }
    
  }

}

class LokalitaChraneneUdaje {
  
//<xs:element name="nazev" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{nazev}" -->
  @JacksonXmlProperty(localName = "nazev")
  public String nazev;
  
//<xs:element name="popis" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{popis}" -->
  @JacksonXmlProperty(localName = "popis")
  public String popis;
  
//<xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
  @JacksonXmlProperty(localName = "poznamka")
  public String poznamka;
  
  public void fillSolrFields(SolrInputDocument idoc, String pristupnost) {
    SolrSearcher.addSecuredFieldNonRepeat(idoc, "nazev", nazev, pristupnost);
    SolrSearcher.addSecuredFieldNonRepeat(idoc, "popis", popis, pristupnost);
    SolrSearcher.addSecuredFieldNonRepeat(idoc, "poznamka", poznamka, pristupnost);
  }

}
