package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
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
public class Let implements FedoraModel {

  @Field
  public String entity = "let";

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
  @JacksonXmlProperty(localName = "ident_cely")
  @Field
  public String ident_cely;      
  
//<xs:element name="datum" minOccurs="0" maxOccurs="1" type="xs:date"/> <!-- "{datum}" -->
  @JacksonXmlProperty(localName = "datum")
  @Field
  public Date datum;     
  
//<xs:element name="hodina_zacatek" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{hodina_zacatek}" -->
  @JacksonXmlProperty(localName = "hodina_zacatek")
  @Field
  public String hodina_zacatek;      
  
//<xs:element name="hodina_konec" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{hodina_konec}" -->
  @JacksonXmlProperty(localName = "hodina_konec")
  @Field
  public String hodina_konec;      
  
//<xs:element name="pozorovatel" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{pozorovatel.ident_cely}" | "{pozorovatel.vypis_cely}" -->
  @JacksonXmlProperty(localName = "pozorovatel")
  public Vocab pozorovatel;      
  
//<xs:element name="organizace" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{organizace.ident_cely}" | "{organizace.nazev}" -->
  @JacksonXmlProperty(localName = "organizace")
  public Vocab organizace;      
  
//<xs:element name="fotoaparat" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{fotoaparat}" -->
  @JacksonXmlProperty(localName = "fotoaparat")
  @Field
  public String fotoaparat;      
  
//<xs:element name="pilot" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{pilot}" -->
  @JacksonXmlProperty(localName = "pilot")
  @Field
  public String pilot;      
  
//<xs:element name="typ_letounu" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{typ_letounu}" -->
  @JacksonXmlProperty(localName = "typ_letounu")
  @Field
  public String typ_letounu;   
  
//<xs:element name="ucel_letu" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{ucel_letu}" -->
  @JacksonXmlProperty(localName = "ucel_letu")
  @Field
  public String ucel_letu;      
  
//<xs:element name="letiste_start" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{letiste_start.ident_cely}" | "{letiste_start.heslo}" -->
  @JacksonXmlProperty(localName = "letiste_start")
  public Vocab letiste_start;      
  
//<xs:element name="letiste_cil" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{letiste_cil.ident_cely}" | "{letiste_cil.heslo}" -->
  @JacksonXmlProperty(localName = "letiste_cil")
  public Vocab letiste_cil;      
  
//<xs:element name="pocasi" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{pocasi.ident_cely}" | "{pocasi.heslo}" -->
  @JacksonXmlProperty(localName = "pocasi")
  public Vocab pocasi;      
  
//<xs:element name="dohlednost" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{dohlednost.ident_cely}" | "{dohlednost.heslo}" -->
  @JacksonXmlProperty(localName = "dohlednost")
  public Vocab dohlednost;      
  
//<xs:element name="uzivatelske_oznaceni" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{uzivatelske_oznaceni}" -->
  @JacksonXmlProperty(localName = "uzivatelske_oznaceni")
  @Field
  public String uzivatelske_oznaceni;      
  
//<xs:element name="dokument" minOccurs="0" maxOccurs="unbounded" type="amcr:refType"/> <!-- "{dokument_set.ident_cely}" | "{dokument_set.ident_cely}" -->
  @JacksonXmlProperty(localName = "dokument")
  public List<Vocab> dokument = new ArrayList();      
  
  
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
    idoc.setField("searchable", true);

    IndexUtils.addVocabField(idoc, "pozorovatel", pozorovatel);
    IndexUtils.addVocabField(idoc, "organizace", organizace);
    IndexUtils.addVocabField(idoc, "letiste_start", letiste_start);
    IndexUtils.addVocabField(idoc, "letiste_cil", letiste_cil);
    IndexUtils.addVocabField(idoc, "pocasi", pocasi);
    IndexUtils.addVocabField(idoc, "dohlednost", dohlednost);
    for (Vocab v : dokument) {
      IndexUtils.addVocabField(idoc, "dokument", v);
    }
    
  }

}
