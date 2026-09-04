package cz.inovatika.arup.digiarchiv.web4.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web4.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web4.fedora.FedoraUtils;
import cz.inovatika.arup.digiarchiv.web4.index.IndexUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class DokumentacniJednotka implements FedoraModel {

    @Field
    public String entity = "dokumentacni_jednotka";

    @Field
    public boolean searchable = true;

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
    @JacksonXmlProperty(localName = "ident_cely")
    @Field
    public String ident_cely;

//<xs:element name="pian" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{pian.ident_cely}" | "{pian.ident_cely}" -->
    @JacksonXmlProperty(localName = "pian")
    public Vocab dj_pian;

//<xs:element name="typ" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ.ident_cely}" | "{typ.heslo}" -->
    @JacksonXmlProperty(localName = "typ")
    public Vocab dj_typ;

//<xs:element name="negativni_jednotka" minOccurs="1" maxOccurs="1" type="xs:boolean"/> <!-- "{negativni_jednotka}" -->
    @JacksonXmlProperty(localName = "negativni_jednotka")
    @Field
    public boolean dj_negativni_jednotka;

//<xs:element name="nazev" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{nazev}" -->
    @JacksonXmlProperty(localName = "nazev")
    @Field
    public String dj_nazev;

//<xs:element name="adb" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{Adb.ident_cely}" | "{Adb.ident_cely}" -->
    @JacksonXmlProperty(localName = "adb")
    public Vocab adb_ident_cely;

    public ADB dj_adb;
    // public JSONObject dj_adb;

//<xs:element name="komponenta" minOccurs="0" maxOccurs="unbounded" type="amcr:komponentaType"/> <!-- "{komponenty.komponenty}" -->
    @JacksonXmlProperty(localName = "komponenta")
    public List<Komponenta> dj_komponenta = new ArrayList();

    public SolrInputDocument createSolrDoc(SolrInputDocument idoc) {

        if (adb_ident_cely != null) {
            try {
                String xml = FedoraUtils.requestXml("record/" + adb_ident_cely.getId() + "/metadata");
                dj_adb = (ADB) FedoraModel.parseXml(xml, ADB.class);
                for (VyskovyBod vb : dj_adb.adb_chranene_udaje.vyskovy_bod) {
                    vb.geom_gml = FedoraModel.getAsXml(vb.geom_gml);
                }
            } catch (Exception ex) {
                Logger.getLogger(DokumentacniJednotka.class.getName()).log(Level.SEVERE, "", ex);
            }
        }

        DocumentObjectBinder dob = new DocumentObjectBinder();
        for (Komponenta k : dj_komponenta) {
            k.setKategorie();
        }
        SolrInputDocument djdoc = dob.toSolrInputDocument(this);
        djdoc.setField("pristupnost", idoc.getFieldValue("pristupnost"));
        djdoc.setField("stav", idoc.getFieldValue("stav"));
        IndexUtils.addVocabField(djdoc, "dj_pian", dj_pian);
        IndexUtils.addVocabField(djdoc, "dj_typ", dj_typ);
        IndexUtils.addJSONField(djdoc, "dj_adb", dj_adb);
        djdoc.setField("f_okres", idoc.getFieldValues("f_okres"));

        for (Komponenta k : dj_komponenta) {
            k.fillSolrFields(idoc, djdoc, "dokumentacni_jednotka");
            IndexUtils.addJSONField(djdoc, "dj_komponenta", k);
        }
        djdoc.removeField("dj_adb");
        IndexUtils.addVocabField(djdoc, "dj_adb", adb_ident_cely);

        return djdoc;

    }

    @Override
    public String coreName() {
        return "entities";
    }

    @Override
    public boolean isSearchable() {
        return true;
    }

    @Override
    public void fillSolrFields(SolrInputDocument idoc) throws Exception {

    }

    @Override
    public boolean filterOAI(JSONObject user, SolrDocument doc) {
        long st = ((Number) doc.getFieldValue("stav")).longValue();
        String userPr = user.optString("pristupnost", "A");
        if (userPr.compareToIgnoreCase("A") > 0 || st == 3) {
            return true;
        } else {
            return false;
        }
    }
}
