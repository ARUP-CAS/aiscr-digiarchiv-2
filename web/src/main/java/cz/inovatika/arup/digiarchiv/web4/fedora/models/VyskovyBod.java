package cz.inovatika.arup.digiarchiv.web4.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web4.fedora.FedoraModel;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class VyskovyBod implements FedoraModel {
  
//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
  @JacksonXmlProperty(localName = "ident_cely")
  public String ident_cely;
  
//<xs:element name="typ" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ.ident_cely}" | "{typ.heslo}" -->
    @JacksonXmlProperty(localName = "typ")
    public Vocab typ;
    
//<xs:element name="geom_gml" minOccurs="1" maxOccurs="1" type="amcr:gmlType"/> <!-- ST_AsGML("{geom}") -->
    @JacksonXmlProperty(localName = "geom_gml")
    public Object geom_gml;
    
//<xs:element name="geom_wkt" minOccurs="1" maxOccurs="1" type="amcr:wktType"/> <!-- ST_SRID("{geom}") | ST_AsText("{geom}") -->
    @JacksonXmlProperty(localName = "geom_wkt")
    public WKT geom_wkt;

    @Override
    public String coreName() {
        return "entities";
    }
    
    @Override
    public boolean isSearchable(){
        return true;
    }

    @Override
    public void fillSolrFields(SolrInputDocument idoc) throws Exception {
        
    }

    @Override
    public boolean filterOAI(JSONObject user, SolrDocument doc) {
        return true;
    }
    
    
}
