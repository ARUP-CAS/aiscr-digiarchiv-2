package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class ExtZdroj implements FedoraModel {

    @Field
    public String entity = "ext_zdroj";

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
    @JacksonXmlProperty(localName = "ident_cely")
    @Field
    public String ident_cely;

//  <xs:element name="stav" minOccurs="1" maxOccurs="1" type="xs:integer"/> <!-- "{stav}" -->
    @JacksonXmlProperty(localName = "stav")
    @Field
    public long stav;

//  <xs:element name="typ" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ.ident_cely}" | "{typ.heslo}" -->
    @JacksonXmlProperty(localName = "typ")
    public Vocab typ;

//  <xs:element name="sysno" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{sysno}" -->
    @JacksonXmlProperty(localName = "sysno")
    @Field
    public String sysno;

//  <xs:element name="autor" minOccurs="0" maxOccurs="unbounded" type="amcr:autorType"/> <!-- "{externizdrojautor_set.autor.ident_cely}" | "{externizdrojautor_set.poradi}" | "{externizdrojautor_set.autor.vypis_cely}" -->
    @JacksonXmlProperty(localName = "autor")
    public List<Vocab> autor = new ArrayList();

//  <xs:element name="nazev" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{nazev}" -->
    @JacksonXmlProperty(localName = "nazev")
    @Field
    public String nazev;

//  <xs:element name="edice_rada" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{edice_rada}" -->
    @JacksonXmlProperty(localName = "edice_rada")
    @Field
    public String edice_rada;

//  <xs:element name="rok_vydani_vzniku" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{rok_vydani_vzniku}" -->
    @JacksonXmlProperty(localName = "rok_vydani_vzniku")
    @Field
    public String rok_vydani_vzniku;

//  <xs:element name="isbn" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{isbn}" -->
    @JacksonXmlProperty(localName = "isbn")
    @Field
    public String isbn;

//  <xs:element name="issn" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{issn}" -->
    @JacksonXmlProperty(localName = "issn")
    @Field
    public String issn;

//  <xs:element name="vydavatel" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{vydavatel}" -->
    @JacksonXmlProperty(localName = "vydavatel")
    @Field
    public String vydavatel;

//  <xs:element name="editor" minOccurs="0" maxOccurs="unbounded" type="amcr:autorType"/> <!-- "{externizdrojeditor_set.editor.ident_cely}" | "{externizdrojeditor_set.poradi}" | "{externizdrojeditor_set.editor.vypis_cely}" -->
    @JacksonXmlProperty(localName = "editor")
    public List<Vocab> editor = new ArrayList();

//  <xs:element name="sbornik_nazev" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{sbornik_nazev}" -->
    @JacksonXmlProperty(localName = "sbornik_nazev")
    @Field
    public String sbornik_nazev;

//  <xs:element name="misto" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{misto}" -->
    @JacksonXmlProperty(localName = "misto")
    @Field
    public String misto;

//  <xs:element name="casopis_denik_nazev" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{casopis_denik_nazev}" -->
    @JacksonXmlProperty(localName = "casopis_denik_nazev")
    @Field
    public String casopis_denik_nazev;

//  <xs:element name="casopis_rocnik" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{casopis_rocnik}" -->
    @JacksonXmlProperty(localName = "casopis_rocnik")
    @Field
    public String casopis_rocnik;

//  <xs:element name="datum_rd" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{datum_rd}" -->
    @JacksonXmlProperty(localName = "datum_rd")
    @Field
    public String datum_rd;

//  <xs:element name="paginace_titulu" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{paginace_titulu}" -->
    @JacksonXmlProperty(localName = "paginace_titulu")
    @Field
    public String paginace_titulu;

//  <xs:element name="link" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{link}" -->
    @JacksonXmlProperty(localName = "link")
    @Field
    public String link;

//  <xs:element name="typ_dokumentu" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ_dokumentu.ident_cely}" | "{typ_dokumentu.heslo}" -->
    @JacksonXmlProperty(localName = "typ_dokumentu")
    public Vocab typ_dokumentu;

//  <xs:element name="organizace" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{organizace.ident_cely}" | "{organizace.nazev}" -->
    @JacksonXmlProperty(localName = "organizace")
    public Vocab organizace;

//  <xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
    @JacksonXmlProperty(localName = "poznamka")
    @Field
    public String poznamka;

//  <xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/> <!-- "{historie.historie_set}" -->
    @JacksonXmlProperty(localName = "historie")
    public List<Historie> historie = new ArrayList();
    
//  <xs:element name="ext_odkaz" minOccurs="0" maxOccurs="unbounded" type="amcr:ez-ext_odkazType"/> <!-- "{externi_odkazy_zdroje}" -->
    @JacksonXmlProperty(localName = "ext_odkaz")
    //public List<Object> ext_odkaz = new ArrayList();
    public List<Object> ext_odkaz = new ArrayList();

    @Override
    public String coreName() {
        return "entities";
    }

    @Override
    public void fillSolrFields(SolrInputDocument idoc) {
        idoc.setField("searchable", true);
        IndexUtils.setDateStamp(idoc, ident_cely);
        IndexUtils.setDateStampFromHistory(idoc, historie);
        IndexUtils.addVocabField(idoc, "typ", typ);
        for (Vocab v : autor) {
            IndexUtils.addVocabField(idoc, "autor", v);
        }
        for (Vocab v : editor) {
            IndexUtils.addVocabField(idoc, "editor", v);
        }
        IndexUtils.addVocabField(idoc, "typ_dokumentu", typ_dokumentu);
        IndexUtils.addVocabField(idoc, "organizace", organizace);
        IndexUtils.addVocabField(idoc, "typ", typ);
        //idoc.addField("ext_odkaz", objectMapper.writeValueAsString(ext_odkaz));
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            idoc.addField("ext_odkaz", objectMapper.writeValueAsString(ext_odkaz));
        } catch (JsonProcessingException ex) {
            Logger.getLogger(ExtZdroj.class.getName()).log(Level.SEVERE, null, ex);
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
