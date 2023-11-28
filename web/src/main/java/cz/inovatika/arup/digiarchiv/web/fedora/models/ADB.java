package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class ADB implements FedoraModel {

    @Field
    public String entity = "adb";

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
    @JacksonXmlProperty(localName = "ident_cely")
    @Field
    public String ident_cely;

//<xs:element name="dokumentacni_jednotka" minOccurs="1" maxOccurs="1" type="amcr:refType"/> <!-- "{dokumentacni_jednotka.archeologicky_zaznam.ident_cely}" | "{dokumentacni_jednotka.ident_cely}" -->
    @JacksonXmlProperty(localName = "dokumentacni_jednotka")
    public Vocab dokumentacni_jednotka;

//<xs:element name="stav_pom" minOccurs="1" maxOccurs="1" type="xs:integer"/> <!-- "{dokumentacni_jednotka.archeologicky_zaznam.stav}" -->
    @JacksonXmlProperty(localName = "stav_pom")
    @Field
    public int stav_pom;

//<xs:element name="typ_sondy" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ_sondy.ident_cely}" | "{typ_sondy.heslo}" -->
    @JacksonXmlProperty(localName = "typ_sondy")
    public Vocab typ_sondy;

//<xs:element name="podnet" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{podnet.ident_cely}" | "{podnet.heslo}" -->
    @JacksonXmlProperty(localName = "podnet")
    public Vocab podnet;

//<xs:element name="stratigraficke_jednotky" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{stratigraficke_jednotky}" -->
    @JacksonXmlProperty(localName = "stratigraficke_jednotky")
    @Field
    public String stratigraficke_jednotky;

//<xs:element name="autor_popisu" minOccurs="1" maxOccurs="1" type="amcr:refType"/> <!-- "{autor_popisu.ident_cely}" | "{autor_popisu.vypis_cely}" -->
    @JacksonXmlProperty(localName = "autor_popisu")
    public Vocab autor_popisu;

//<xs:element name="rok_popisu" minOccurs="1" maxOccurs="1" type="xs:integer"/> <!-- "{rok_popisu}" -->
    @JacksonXmlProperty(localName = "rok_popisu")
    @Field
    public int rok_popisu;

//<xs:element name="autor_revize" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{autor_revize.ident_cely}" | "{autor_revize.vypis_cely}" -->
    @JacksonXmlProperty(localName = "autor_revize")
    public Vocab autor_revize;

//<xs:element name="rok_revize" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{rok_revize}" -->
    @JacksonXmlProperty(localName = "rok_revize")
    @Field
    public int rok_revize;

//<xs:element name="sm5" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{sm5.mapno}" -->
    @JacksonXmlProperty(localName = "sm5")
    @Field
    public String sm5;

//<xs:element name="pristupnost_pom" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{dokumentacni_jednotka.archeologicky_zaznam.pristupnost.ident_cely}" | "{dokumentacni_jednotka.archeologicky_zaznam.pristupnost.heslo}" -->
    @JacksonXmlProperty(localName = "pristupnost_pom")
    public Vocab pristupnost;

//<xs:element name="chranene_udaje" minOccurs="0" maxOccurs="1" type="amcr:adb-chranene_udajeType"/> <!-- SELF -->
    @JacksonXmlProperty(localName = "chranene_udaje")
    private ADBChraneneUdaje chranene_udaje;

    @Override
    public String coreName() {
        return "entities";
    }

    @Override
    public void fillSolrFields(SolrInputDocument idoc) {
        idoc.setField("pristupnost", SearchUtils.getPristupnostMap().get(pristupnost.getId()));
        idoc.setField("searchable", true);
        idoc.setField("stav", stav_pom);
        IndexUtils.setDateStamp(idoc, ident_cely);
        IndexUtils.addVocabField(idoc, "dokumentacni_jednotka", dokumentacni_jednotka);
        IndexUtils.addVocabField(idoc, "typ_sondy", typ_sondy);
        IndexUtils.addVocabField(idoc, "podnet", podnet);
        IndexUtils.addVocabField(idoc, "autor_popisu", autor_popisu);
        IndexUtils.addVocabField(idoc, "autor_revize", autor_revize);

        if (chranene_udaje != null) {
            chranene_udaje.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"), ident_cely);
        }
    }

    @Override
    public boolean filterOAI(JSONObject user, SolrDocument doc) {
//-- A: stav = 3
//-- B-E: bez omezení 
        long st = (long) doc.getFieldValue("stav");
        String userPr = user.optString("pristupnost", "A");
        if (userPr.compareToIgnoreCase("B") >= 0) {
            return true;
        } else if (userPr.compareToIgnoreCase("B") <= 0 && st == 3) {
            return true;
        } else {
            return false;
        }
    }

}

class ADBChraneneUdaje {

//<xs:element name="uzivatelske_oznaceni_sondy" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{uzivatelske_oznaceni_sondy}" -->
    @JacksonXmlProperty(localName = "uzivatelske_oznaceni_sondy")
    public String uzivatelske_oznaceni_sondy;

//<xs:element name="trat" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{trat}" -->
    @JacksonXmlProperty(localName = "trat")
    public String trat;

//<xs:element name="cislo_popisne" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{cislo_popisne}" -->
    @JacksonXmlProperty(localName = "cislo_popisne")
    public String cislo_popisne;

//<xs:element name="parcelni_cislo" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{parcelni_cislo}" -->
    @JacksonXmlProperty(localName = "parcelni_cislo")
    public String parcelni_cislo;

//<xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
    @JacksonXmlProperty(localName = "poznamka")
    public String poznamka;

//<xs:element name="vyskovy_bod" minOccurs="0" maxOccurs="unbounded" type="amcr:vyskovy_bodType"/> <!-- "{vyskove_body}" -->
    @JacksonXmlProperty(localName = "vyskovy_bod")
    public List<VyskovyBod> vyskovy_bod = new ArrayList();

    public void fillSolrFields(SolrInputDocument idoc, String pristupnost, String ident_cely) {
        IndexUtils.addSecuredFieldNonRepeat(idoc, "uzivatelske_oznaceni_sondy", uzivatelske_oznaceni_sondy, pristupnost);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "trat", trat, pristupnost);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "cislo_popisne", cislo_popisne, pristupnost);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "parcelni_cislo", parcelni_cislo, pristupnost);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "poznamka", poznamka, pristupnost);

        IndexUtils.setSecuredJSONField(idoc, this);

        // VyskovyBod as solr document
        List<SolrInputDocument> idocs = new ArrayList<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            for (VyskovyBod vb : vyskovy_bod) {
                SolrInputDocument vbdoc = new SolrInputDocument();
                vbdoc.setField("entity", "vyskovy_bod");
                vbdoc.setField("searchable", true);
                vbdoc.setField("ident_cely", vb.ident_cely);
                vbdoc.setField("parent", ident_cely);
                vbdoc.setField("typ", vb.typ);
                vbdoc.setField("geom_gml", objectMapper.writeValueAsString(vb.geom_gml));
                vbdoc.setField("geom_wkt", objectMapper.writeValueAsString(vb.geom_wkt));

                idocs.add(vbdoc);
                IndexUtils.addSecuredFieldNonRepeat(idoc, "vyskovy_bod", vb.ident_cely, pristupnost);
            }
            if (!idocs.isEmpty()) {
                IndexUtils.getClient().add("entities", idocs, 10);
            }
        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(ADBChraneneUdaje.class.getName()).log(Level.SEVERE, null, ex);
        }

//      for (VyskovyBod v: vyskovy_bod) {
//        try {
//          ObjectMapper objectMapper = new ObjectMapper();
//          idoc.addField("sec_vyskovy_bod", objectMapper.writeValueAsString(v));
//        } catch (JsonProcessingException ex) {
//          Logger.getLogger(ADBChraneneUdaje.class.getName()).log(Level.SEVERE, null, ex);
//        }
//      }
    }
}
