
package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class NeidentAkce {

//<xs:element name="okres" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "ruian-{katastr.okres.kod}" | "{katastr.okres.nazev}" -->
    @JacksonXmlProperty(localName = "okres")
    public Vocab okres;

//<xs:element name="katastr" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "ruian-{katastr.kod}" | "{katastr.nazev}" -->
    @JacksonXmlProperty(localName = "katastr")
    public Vocab katastr;

//<xs:element name="vedouci" minOccurs="0" maxOccurs="unbounded" type="amcr:refType"/> <!-- "{vedouci.ident_cely}" | "{vedouci.vypis_cely}" -->
    @JacksonXmlProperty(localName = "vedouci")
    public List<Vocab> vedouci = new ArrayList<>();

//<xs:element name="rok_zahajeni" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{rok_zahajeni}" -->
    @JacksonXmlProperty(localName = "rok_zahajeni")
    @Field
    public int rok_zahajeni;

//<xs:element name="rok_ukonceni" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{rok_ukonceni}" -->
    @JacksonXmlProperty(localName = "rok_ukonceni")
    @Field
    public int rok_ukonceni;

//<xs:element name="lokalizace" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{lokalizace}" -->
    @JacksonXmlProperty(localName = "lokalizace")
    @Field
    public String lokalizace;

//<xs:element name="popis" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{popis}" -->
    @JacksonXmlProperty(localName = "popis")
    @Field
    public String popis;

//<xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
    @JacksonXmlProperty(localName = "poznamka")
    @Field
    public String poznamka;

//<xs:element name="pian" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{pian}" -->
    @JacksonXmlProperty(localName = "pian")
    @Field
    public String pian; 

    public void fillSolrFields(SolrInputDocument idoc, String pristupnost) {
        DocumentObjectBinder dob = new DocumentObjectBinder();
        SolrInputDocument kdoc = dob.toSolrInputDocument(this);
        IndexUtils.addRefField(kdoc, "okres", okres);
        IndexUtils.addRefField(kdoc, "katastr", katastr);
        IndexUtils.addFieldNonRepeat(idoc, "f_okres", okres.getValue());
        IndexUtils.addSecuredFieldNonRepeat(idoc, "f_katastr", katastr.getValue(), pristupnost);

        for (Vocab v : vedouci) {
            IndexUtils.addRefField(kdoc, "vedouci", v);
        }

        JSONObject li = new JSONObject()
                .put("pristupnost", pristupnost)
                .put("katastr", kdoc.getFieldValue("katastr"))
                .put("okres", kdoc.getFieldValue("okres"));
        idoc.addField("location_info", li.toString());
        

        for (Map.Entry<String, SolrInputField> entry : kdoc.entrySet()) {
            idoc.setField("neident_akce_" + entry.getKey(), entry.getValue().getValue());
        }
    }
}
