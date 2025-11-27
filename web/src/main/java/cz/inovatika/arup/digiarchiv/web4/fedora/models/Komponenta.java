package cz.inovatika.arup.digiarchiv.web4.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web4.index.IndexUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author alberto
 */
public class Komponenta {

    @Field
    public String entity = "komponenta";

    @Field
    public boolean searchable = true;

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
    @JacksonXmlProperty(localName = "ident_cely")
    @Field
    public String ident_cely;

//<xs:element name="obdobi" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{obdobi.ident_cely}" | "{obdobi.heslo}" -->
    @JacksonXmlProperty(localName = "obdobi")
    public Vocab komponenta_obdobi;

//<xs:element name="jistota" minOccurs="0" maxOccurs="1" type="xs:boolean"/> <!-- "{jistota}" -->
    @JacksonXmlProperty(localName = "jistota")
    public String komponenta_jistota;

//<xs:element name="presna_datace" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{presna_datace}" -->
    @JacksonXmlProperty(localName = "presna_datace")
    @Field
    public String komponenta_presna_datace;

//<xs:element name="areal" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{areal.ident_cely}" | "{areal.heslo}" -->
    @JacksonXmlProperty(localName = "areal")
    public Vocab komponenta_areal;

//<xs:element name="aktivita" minOccurs="0" maxOccurs="unbounded" type="amcr:vocabType"/> <!-- "{aktivity.ident_cely}" | "{aktivity.heslo}" -->
    @JacksonXmlProperty(localName = "aktivita")
    public List<Vocab> komponenta_aktivita = new ArrayList();

//<xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
    @JacksonXmlProperty(localName = "poznamka")
    @Field
    public String komponenta_poznamka;

//<xs:element name="nalez_objekt" minOccurs="0" maxOccurs="unbounded" type="amcr:nalez_objektType"/> <!-- "{objekty}" -->
    @JacksonXmlProperty(localName = "nalez_objekt")
    public List<NalezObjekt> komponenta_nalez_objekt = new ArrayList();

//<xs:element name="nalez_predmet" minOccurs="0" maxOccurs="unbounded" type="amcr:nalez_predmetType"/><!-- "{predmety}" -->
    @JacksonXmlProperty(localName = "nalez_predmet")
    public List<NalezPredmet> komponenta_nalez_predmet = new ArrayList();

    @Field
    public String komponenta_typ_nalezu;

    public void fillSolrFields(SolrInputDocument idoc, String prefix) {
        DocumentObjectBinder dob = new DocumentObjectBinder();
        SolrInputDocument kdoc = dob.toSolrInputDocument(this);
        IndexUtils.addJSONField(kdoc, "komponenta_obdobi", komponenta_obdobi);
        IndexUtils.addJSONField(kdoc, "komponenta_areal", komponenta_areal);
        for (Vocab a : komponenta_aktivita) {
            // IndexUtils.addVocabField(kdoc, "aktivita", a);
            IndexUtils.addJSONField(kdoc, "komponenta_aktivita", a);
        }
        if (!komponenta_nalez_objekt.isEmpty()) {
            // komponenta_typ_nalezu = "objekt";
            komponenta_typ_nalezu = IndexUtils.getTypNalezu("objekt");
        }
        for (NalezObjekt no : komponenta_nalez_objekt) {
            IndexUtils.addJSONField(kdoc, "komponenta_nalez_objekt", no);
            idoc.addField("nalez_dokumentu_pocet", no.pocet);
            idoc.addField("nalez_dokumentu_poznamka", no.poznamka);
        }
        if (!komponenta_nalez_predmet.isEmpty()) {
            // komponenta_typ_nalezu = "predmet";
            komponenta_typ_nalezu = "HES-001126";
        }
        for (NalezPredmet np : komponenta_nalez_predmet) {
            IndexUtils.addJSONField(kdoc, "komponenta_nalez_predmet", np);
            idoc.addField("nalez_dokumentu_pocet", np.pocet);
            idoc.addField("nalez_dokumentu_poznamka", np.poznamka);
        }
        kdoc.setField("komponenta_typ_nalezu", komponenta_typ_nalezu);

        idoc.addField("komponenta_dokument_ident_cely", ident_cely);
        if (komponenta_jistota != null) {
            kdoc.addField("komponenta_jistota", komponenta_jistota);
            idoc.addField("komponenta_dokument_jistota", komponenta_jistota);
            idoc.addField("dokument_cast_komponenta_dokument_jistota", komponenta_jistota);
        } else {
            kdoc.removeField("komponenta_jistota");
        }
        idoc.addField("komponenta_dokument_presna_datace", komponenta_presna_datace);
        idoc.addField("komponenta_dokument_poznamka", komponenta_poznamka);

        try {
            IndexUtils.addAndCommit("entities", kdoc);
        } catch (Exception ex) {
            Logger.getLogger(Komponenta.class.getName()).log(Level.SEVERE, "Error indexing komponenta {0}", ident_cely);
            // Logger.getLogger(Komponenta.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
