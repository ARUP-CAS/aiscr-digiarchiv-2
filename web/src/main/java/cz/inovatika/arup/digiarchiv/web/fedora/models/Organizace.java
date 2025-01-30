package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@JacksonXmlRootElement(localName = "organizace")
public class Organizace implements FedoraModel {

  @Field
  public String entity = "organizace";

//      <xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
  @JacksonXmlProperty(localName = "ident_cely")
  @Field
  public String ident_cely;
  
//<xs:element name="nazev" minOccurs="1" maxOccurs="1" type="amcr:langstringType"/> <!-- "{nazev}" -->
  @JacksonXmlProperty(localName = "nazev")
  public Lang nazev;
  
//<xs:element name="nazev_en" minOccurs="0" maxOccurs="1" type="amcr:langstringType"/> <!-- "{nazev_en}" -->
  @JacksonXmlProperty(localName = "nazev_en")
  public Lang nazev_en;
  
//<xs:element name="nazev_zkraceny" minOccurs="1" maxOccurs="1" type="amcr:langstringType"/> <!-- "{nazev_zkraceny}" -->
  @JacksonXmlProperty(localName = "nazev_zkraceny")
  public Lang nazev_zkraceny;
  
//<xs:element name="nazev_zkraceny_en" minOccurs="1" maxOccurs="1" type="amcr:langstringType"/> <!-- "{nazev_zkraceny_en}" -->
  @JacksonXmlProperty(localName = "nazev_zkraceny_en")
  public Lang nazev_zkraceny_en;
  
//<xs:element name="typ_organizace" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ_organizace.ident_cely}" | "{typ_organizace.heslo}" -->
  @JacksonXmlProperty(localName = "typ_organizace")
  public Vocab typ_organizace;
  
//<xs:element name="oao" minOccurs="1" maxOccurs="1" type="xs:boolean"/> <!-- "{oao}" -->
  @JacksonXmlProperty(localName = "oao")
  @Field
  public boolean oao;
  
//<xs:element name="mesicu_do_zverejneni" minOccurs="1" maxOccurs="1" type="xs:integer"/> <!-- "{mesicu_do_zverejneni}" -->
  @JacksonXmlProperty(localName = "mesicu_do_zverejneni")
  @Field
  public long mesicu_do_zverejneni;
  
//<xs:element name="zverejneni_pristupnost" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{zverejneni_pristupnost.ident_cely}" | "{zverejneni_pristupnost.heslo}" -->
  @JacksonXmlProperty(localName = "zverejneni_pristupnost")
  public Vocab zverejneni_pristupnost;
  
//<xs:element name="email" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{email}" -->
  @JacksonXmlProperty(localName = "email")
  @Field
  public String email;
  
//<xs:element name="telefon" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{telefon}" -->
  @JacksonXmlProperty(localName = "telefon")
  @Field
  public String telefon;
  
//<xs:element name="adresa" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{adresa}" -->
  @JacksonXmlProperty(localName = "adresa")
  @Field
  public String adresa;
  
//<xs:element name="ico" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{ico}" -->
  @JacksonXmlProperty(localName = "ico")
  @Field
  public String ico;
  
//<xs:element name="soucast" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{soucast.ident_cely}" | "{soucast.nazev}" -->
  @JacksonXmlProperty(localName = "soucast")
  public Vocab soucast;
  
//<xs:element name="zanikla" minOccurs="1" maxOccurs="1" type="xs:boolean"/> <!-- "{zanikla}" -->
  @JacksonXmlProperty(localName = "zanikla")
  @Field
  public boolean zanikla;
  
  @JacksonXmlProperty(localName = "cteni_dokumentu")
  @Field
  public boolean cteni_dokumentu;

    @JacksonXmlProperty(localName = "historie")
    public List<Historie> historie = new ArrayList();
    
    @Override
    public boolean isSearchable(){
        return true;
    }
    
  @Override
  public void fillSolrFields(SolrInputDocument idoc) {
    IndexUtils.addLangField(idoc, "nazev", nazev);
    IndexUtils.addLangField(idoc, "nazev_en", nazev_en);
    IndexUtils.addLangField(idoc, "nazev_zkraceny", nazev_zkraceny);
    IndexUtils.addLangField(idoc, "nazev_zkraceny_en", nazev_zkraceny_en);
    IndexUtils.addVocabField(idoc, "typ_organizace", typ_organizace);
    IndexUtils.addVocabField(idoc, "zverejneni_pristupnost", zverejneni_pristupnost);
    IndexUtils.addVocabField(idoc, "soucast", soucast);
      IndexUtils.setDateStamp(idoc, ident_cely);
        IndexUtils.setDateStampFromHistory(idoc, historie);
  }

  @Override
  public String coreName() {
    return "organizations";
  }

    @Override
    public boolean filterOAI(JSONObject user, SolrDocument doc) {
        return true;
    }
  
}
