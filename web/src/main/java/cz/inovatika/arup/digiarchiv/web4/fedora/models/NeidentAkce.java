
package cz.inovatika.arup.digiarchiv.web4.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web4.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web4.index.SolrSearcher;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
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
        IndexUtils.addFieldNonRepeat(idoc, "f_kraj", SolrSearcher.getKrajByOkres(okres.getId()).getString("kraj")); 
        IndexUtils.addSecuredFieldNonRepeat(idoc, "f_katastr", katastr.getValue(), "A");

        for (Vocab v : vedouci) {
            IndexUtils.addRefField(kdoc, "vedouci", v);
            IndexUtils.addRefField(idoc, "f_vedouci", v);
        }
        
        JSONObject li = new JSONObject()
                .put("pristupnost", "A")
                .put("katastr", kdoc.getFieldValue("katastr"))
                .put("okres", kdoc.getFieldValue("okres"));
        
        SolrSearcher.addFieldNonRepeat(idoc, "location_info", li.toString());
        
        idoc.addField("neident_akce_rok_zahajeni", rok_zahajeni);
        idoc.addField("neident_akce_rok_ukonceni", rok_ukonceni);
        idoc.addField("neident_akce_lokalizace", lokalizace);
        idoc.addField("neident_akce_popis", popis);
        idoc.addField("neident_akce_poznamka", poznamka);
        idoc.addField("neident_akce_pian", pian);
       
        int datum_provedeni_od = 10000;
        int datum_provedeni_do = 0;
        if (rok_zahajeni != 0) {
          datum_provedeni_od = Math.min(datum_provedeni_od, rok_zahajeni);
          String ukonceni = "*";
          Calendar c1 = Calendar.getInstance();
          c1.set(datum_provedeni_od, 0, 1);
          SolrSearcher.addFieldNonRepeat(idoc, "datum_provedeni_od", c1.toInstant().toString());
          idoc.setField("datum_provedeni_od", c1.toInstant().toString());
          if (rok_ukonceni != 0) {
            datum_provedeni_do = Math.max(datum_provedeni_do, rok_ukonceni);
            Calendar c2 = Calendar.getInstance();
            c2.set(datum_provedeni_do, 11, 31);
            SolrSearcher.addFieldNonRepeat(idoc, "datum_provedeni_do", c2.toInstant().toString());
            idoc.setField("datum_provedeni_do", c2.toInstant().toString());
            if (!c2.before(c1)) {
              ukonceni = c2.toInstant().toString();
            }
          }
          idoc.setField("datum_provedeni", "[" + c1.toInstant().toString() + " TO " + ukonceni + "]");
        }
        
    }
}
