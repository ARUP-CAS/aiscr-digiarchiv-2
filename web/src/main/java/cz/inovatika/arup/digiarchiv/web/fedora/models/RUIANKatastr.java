package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.util.Date;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class RUIANKatastr implements FedoraModel {

    @Field
    public String entity = "ruian_katastr"; 
  
//xs:element name="kod" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "ruian-{kod}" -->
    @JacksonXmlProperty(localName = "kod")
    @Field
    public String kod;

//<xs:element name="nazev" minOccurs="1" maxOccurs="1" type="amcr:langstringType"/> <!-- "{nazev}" -->
    @JacksonXmlProperty(localName = "nazev")
    @Field
    public String nazev;

//<xs:element name="nazev_en" minOccurs="1" maxOccurs="1" type="amcr:langstringType"/> <!-- "{nazev_en}" -->
    @JacksonXmlProperty(localName = "nazev_en")
    @Field
    public String nazev_en;

//<xs:element name="pian" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{pian}" -->
    @JacksonXmlProperty(localName = "pian")
    public Vocab pian;

//<xs:element name="okres" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "ruian-{kraj.kod}" | "{kraj.nazev}" -->
    @JacksonXmlProperty(localName = "okres")
    public Vocab okres;

//<xs:element name="definicni_bod_gml" minOccurs="1" maxOccurs="1" type="amcr:gmlType"/> <!-- ST_AsGML("{definicni_bod}") -->
    @JacksonXmlProperty(localName = "definicni_bod_gml")
    public Object definicni_bod_gml;

//<xs:element name="definicni_bod_wkt" minOccurs="1" maxOccurs="1" type="amcr:wktType"/> <!-- ST_SRID("{definicni_bod}") | ST_AsText("{definicni_bod}") -->
    @JacksonXmlProperty(localName = "definicni_bod_wkt")
    public Object definicni_bod_wkt;

//<xs:element name="hranice_gml" minOccurs="1" maxOccurs="1" type="amcr:gmlType"/> <!-- ST_AsGML("{hranice}") -->
    @JacksonXmlProperty(localName = "hranice_gml")
    public Object hranice_gml;

//<xs:element name="hranice_wkt" minOccurs="1" maxOccurs="1" type="amcr:wktType"/> <!-- ST_SRID("{hranice}") | ST_AsText("{hranice}") -->
    @JacksonXmlProperty(localName = "hranice_wkt")
    public Object hranice_wkt;
    
    @Override
    public boolean isSearchable(){
        return true;
    } 

    @Override
    public void fillSolrFields(SolrInputDocument idoc) {
        idoc.setField("ident_cely", kod);
        IndexUtils.addVocabField(idoc, "okres", okres);
        idoc.setField("okres_nazev", SolrSearcher.getOkresNazev(okres.getId()));
        JSONObject kraj = SolrSearcher.getKrajNazev(okres.getId());
        idoc.setField("kraj_nazev", kraj.getString("nazev"));
        idoc.setField("kraj", kraj.getString("kod"));
        IndexUtils.addRefField(idoc, "pian", pian);
        IndexUtils.setDateStamp(idoc, kod);   
    }
 
    @Override
    public String coreName() {
        return "ruian";
    }

    @Override
    public boolean filterOAI(JSONObject user, SolrDocument doc) {
        return true;
    }

}
