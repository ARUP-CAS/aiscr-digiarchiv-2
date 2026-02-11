package cz.inovatika.arup.digiarchiv.web4.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web4.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web4.index.IndexUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

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
    public Date let_datum;

//<xs:element name="hodina_zacatek" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{hodina_zacatek}" -->
    @JacksonXmlProperty(localName = "hodina_zacatek")
    @Field
    public String let_hodina_zacatek;

//<xs:element name="hodina_konec" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{hodina_konec}" -->
    @JacksonXmlProperty(localName = "hodina_konec")
    @Field
    public String let_hodina_konec;

//<xs:element name="pozorovatel" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{pozorovatel.ident_cely}" | "{pozorovatel.vypis_cely}" -->
    @JacksonXmlProperty(localName = "pozorovatel")
    public Vocab let_pozorovatel;

//<xs:element name="organizace" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{organizace.ident_cely}" | "{organizace.nazev}" -->
    @JacksonXmlProperty(localName = "organizace")
    public Vocab let_organizace;

//<xs:element name="fotoaparat" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{fotoaparat}" -->
    @JacksonXmlProperty(localName = "fotoaparat")
    @Field
    public String let_fotoaparat;

//<xs:element name="pilot" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{pilot}" -->
    @JacksonXmlProperty(localName = "pilot")
    @Field
    public String let_pilot;

//<xs:element name="typ_letounu" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{typ_letounu}" -->
    @JacksonXmlProperty(localName = "typ_letounu")
    @Field
    public String let_typ_letounu;

//<xs:element name="ucel_letu" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{ucel_letu}" -->
    @JacksonXmlProperty(localName = "ucel_letu")
    @Field
    public String let_ucel_letu;

//<xs:element name="letiste_start" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{letiste_start.ident_cely}" | "{letiste_start.heslo}" -->
    @JacksonXmlProperty(localName = "letiste_start")
    public Vocab let_letiste_start;

//<xs:element name="letiste_cil" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{letiste_cil.ident_cely}" | "{letiste_cil.heslo}" -->
    @JacksonXmlProperty(localName = "letiste_cil")
    public Vocab let_letiste_cil;

//<xs:element name="pocasi" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{pocasi.ident_cely}" | "{pocasi.heslo}" -->
    @JacksonXmlProperty(localName = "pocasi")
    public Vocab let_pocasi;

//<xs:element name="dohlednost" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{dohlednost.ident_cely}" | "{dohlednost.heslo}" -->
    @JacksonXmlProperty(localName = "dohlednost")
    public Vocab let_dohlednost;

//<xs:element name="uzivatelske_oznaceni" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{uzivatelske_oznaceni}" -->
    @JacksonXmlProperty(localName = "uzivatelske_oznaceni")
    @Field
    public String let_uzivatelske_oznaceni;

//<xs:element name="dokument" minOccurs="0" maxOccurs="unbounded" type="amcr:refType"/> <!-- "{dokument_set.ident_cely}" | "{dokument_set.ident_cely}" -->
    @JacksonXmlProperty(localName = "dokument")
    public List<Vocab> let_dokument = new ArrayList();

    @Override
    public String coreName() {
        return "entities";
    }
    
    @Override
    public boolean isSearchable(){
        return true;
    }

    @Override
    public void fillSolrFields(SolrInputDocument idoc) {
        idoc.setField("searchable", true);
        IndexUtils.setDateStamp(idoc, ident_cely);
        IndexUtils.addRefField(idoc, "let_pozorovatel", let_pozorovatel);
        IndexUtils.addJSONField(idoc, "let_organizace", let_organizace);
        IndexUtils.addJSONField(idoc, "let_letiste_start", let_letiste_start);
        IndexUtils.addJSONField(idoc, "let_letiste_cil", let_letiste_cil);
        IndexUtils.addJSONField(idoc, "let_pocasi", let_pocasi);
        IndexUtils.addJSONField(idoc, "let_dohlednost", let_dohlednost);
        for (Vocab v : let_dokument) {
            IndexUtils.addVocabField(idoc, "let_dokument", v);
        }

    }

    @Override
    public boolean filterOAI(JSONObject user, SolrDocument doc) {
        return true;
    }

}
