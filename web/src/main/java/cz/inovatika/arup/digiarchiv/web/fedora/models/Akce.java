package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;

/**
 *
 * @author alberto
 */
public class Akce {

//<xs:element name="projekt" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{projekt.ident_cely}" | "{projekt.ident_cely}" -->
  @JacksonXmlProperty(localName = "projekt")
  public Vocab projekt;

//<xs:element name="typ" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{typ}" -->
  @JacksonXmlProperty(localName = "typ")
  @Field
  public String typ;

//<xs:element name="je_nz" minOccurs="1" maxOccurs="1" type="xs:boolean"/> <!-- "{je_nz}" -->
  @JacksonXmlProperty(localName = "je_nz")
  @Field
  public boolean je_nz;

//<xs:element name="odlozena_nz" minOccurs="1" maxOccurs="1" type="xs:boolean"/> <!-- "{odlozena_nz}" -->
  @JacksonXmlProperty(localName = "odlozena_nz")
  @Field
  public boolean odlozena_nz;

//<xs:element name="hlavni_vedouci" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{hlavni_vedouci.ident_cely}" | "{hlavni_vedouci.vypis_cely}" -->
  @JacksonXmlProperty(localName = "hlavni_vedouci")
  public Vocab hlavni_vedouci;

//<xs:element name="organizace" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{organizace.ident_cely}" | "{organizace.nazev}" -->
  @JacksonXmlProperty(localName = "organizace")
  public Vocab organizace;

//<xs:element name="vedouci_akce_ostatni" minOccurs="0" maxOccurs="unbounded" type="amcr:vedouci_akce_ostatniType"/> <!-- "{akcevedouci_set}" -->
  @JacksonXmlProperty(localName = "vedouci_akce_ostatni")
  public List<Object> vedouci_akce_ostatni = new ArrayList();

//<xs:element name="specifikace_data" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{specifikace_data.ident_cely}" | "{specifikace_data.heslo}" -->
  @JacksonXmlProperty(localName = "specifikace_data")
  public Vocab specifikace_data;

//<xs:element name="datum_zahajeni" minOccurs="0" maxOccurs="1" type="xs:date"/> <!-- "{datum_zahajeni}" -->
  @JacksonXmlProperty(localName = "datum_zahajeni")
  @Field
  public Date datum_zahajeni;

//<xs:element name="datum_ukonceni" minOccurs="0" maxOccurs="1" type="xs:date"/> <!-- "{datum_ukonceni}" -->
  @JacksonXmlProperty(localName = "datum_ukonceni")
  @Field
  public Date datum_ukonceni;

//<xs:element name="hlavni_typ" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{hlavni_typ.ident_cely}" | "{hlavni_typ.heslo}" -->
  @JacksonXmlProperty(localName = "hlavni_typ")
  public Vocab hlavni_typ;

//<xs:element name="vedlejsi_typ" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{vedlejsi_typ.ident_cely}" | "{vedlejsi_typ.heslo}" -->
  @JacksonXmlProperty(localName = "vedlejsi_typ")
  public Vocab vedlejsi_typ;

//<xs:element name="ulozeni_nalezu" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{ulozeni_nalezu}" -->
  @JacksonXmlProperty(localName = "ulozeni_nalezu")
  @Field
  public String ulozeni_nalezu;

//<xs:element name="ulozeni_dokumentace" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{ulozeni_dokumentace}" -->
  @JacksonXmlProperty(localName = "ulozeni_dokumentace")
  @Field
  public String ulozeni_dokumentace;

//<xs:element name="chranene_udaje" minOccurs="0" maxOccurs="1" type="amcr:akce-chranene_udajeType"/> <!-- SELF -->     
  @JacksonXmlProperty(localName = "chranene_udaje")
  private AkceChraneneUdaje chranene_udaje;

  public void fillSolrFields(SolrInputDocument idoc) {
    
    // idoc from archeologicky_zaznam.
    
    String pristupnost = (String) idoc.getFieldValue("pristupnost");
    DocumentObjectBinder dob = new DocumentObjectBinder();
    SolrInputDocument akceDoc = dob.toSolrInputDocument(this);
    //akceDoc.setField("entity", "akce");
    //akceDoc.setField("pristupnost", pristupnost);
    SolrSearcher.addVocabField(akceDoc, "projekt", projekt);
    SolrSearcher.addVocabField(akceDoc, "hlavni_vedouci", hlavni_vedouci);
    SolrSearcher.addVocabField(akceDoc, "organizace", organizace);
    SolrSearcher.addVocabField(akceDoc, "specifikace_data", specifikace_data);
    SolrSearcher.addVocabField(akceDoc, "hlavni_typ", hlavni_typ);
    SolrSearcher.addVocabField(akceDoc, "vedlejsi_typ", vedlejsi_typ);
    
    if (chranene_udaje != null) {
      chranene_udaje.fillSolrFields(akceDoc, pristupnost);
    }
    
    for (Entry<String,SolrInputField> entry : akceDoc.entrySet()) {
      idoc.setField(entry.getKey(), entry.getValue().getValue());
    }
    
  }

}

class AkceChraneneUdaje {
//<xs:element name="lokalizace_okolnosti" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{lokalizace_okolnosti}" -->
  @JacksonXmlProperty(localName = "lokalizace_okolnosti")
  public String lokalizace_okolnosti;
  
//<xs:element name="souhrn_upresneni" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{souhrn_upresneni}" -->
  @JacksonXmlProperty(localName = "souhrn_upresneni")
  public String souhrn_upresneni;
  
  public void fillSolrFields(SolrInputDocument idoc, String pristupnost) {
    SolrSearcher.addSecuredFieldNonRepeat(idoc, "lokalizace_okolnosti", lokalizace_okolnosti, pristupnost);
    SolrSearcher.addSecuredFieldNonRepeat(idoc, "souhrn_upresneni", souhrn_upresneni, pristupnost);
  }

}
