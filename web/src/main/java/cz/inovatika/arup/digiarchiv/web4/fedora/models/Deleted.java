package cz.inovatika.arup.digiarchiv.web4.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import cz.inovatika.arup.digiarchiv.web4.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web4.index.IndexUtils;
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
public class Deleted implements FedoraModel {

  @Field
  public String entity = "deleted";

  public String ident_cely;
  
//<xs:element name="jmeno" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{first_name}" -->
    @JacksonXmlProperty(localName = "jmeno")
    @Field
    public String jmeno;

//<xs:element name="prijmeni" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{last_name}" -->
    @JacksonXmlProperty(localName = "prijmeni")
    @Field
    public String prijmeni;
    
//<xs:element name="vypis" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{vypis}" -->
    @JacksonXmlProperty(localName = "vypis")
    @Field
    public String vypis;
    
//<xs:element name="vypis_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{vypis_cely}" -->
    @JacksonXmlProperty(localName = "vypis_cely")
    @Field
    public String vypis_cely;
    
//<xs:element name="rok_narozeni" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{rok_narozeni}" -->
    @JacksonXmlProperty(localName = "rok_narozeni")
    @Field
    public int rok_narozeni;
    
//<xs:element name="rok_umrti" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{rok_umrti}" -->
    @JacksonXmlProperty(localName = "rok_umrti")
    @Field
    public int rok_umrti;
    
//<xs:element name="rodne_prijmeni" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{rodne_prijmeni}" -->
    @JacksonXmlProperty(localName = "rodne_prijmeni")
    @Field
    public String rodne_prijmeni;
    

    @JacksonXmlProperty(localName = "historie")
    public List<Historie> historie = new ArrayList();
    
    @Override
    public boolean isSearchable(){
        return true;
    }
    
  @Override
  public void fillSolrFields(SolrInputDocument idoc) {
      IndexUtils.setDateStamp(idoc, ident_cely);
        IndexUtils.setDateStampFromHistory(idoc, historie);
  }

  @Override
  public String coreName() {
    return "osoba";
  }

    @Override
    public boolean filterOAI(JSONObject user, SolrDocument doc) {
        return true;
    }
  
}
