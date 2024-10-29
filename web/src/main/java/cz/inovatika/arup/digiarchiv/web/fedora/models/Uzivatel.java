package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class Uzivatel implements FedoraModel {

    @Field
    public String entity = "uzivatel"; 

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
    @JacksonXmlProperty(localName = "ident_cely")
    @Field
    public String ident_cely;

//<xs:element name="jmeno" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{first_name}" -->
    @JacksonXmlProperty(localName = "jmeno")
    @Field
    public String jmeno;

//<xs:element name="prijmeni" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{last_name}" -->
    @JacksonXmlProperty(localName = "prijmeni")
    @Field
    public String prijmeni;

//<xs:element name="email" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{email}" -->
    @JacksonXmlProperty(localName = "email")
    @Field
    public String email;

//<xs:element name="osoba" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{osoba.ident_cely}" | "{osoba.vypis_cely}" -->
    @JacksonXmlProperty(localName = "osoba")
    public Vocab osoba;

//<xs:element name="organizace" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{organizace.ident_cely}" | "{organizace.nazev}" -->
    @JacksonXmlProperty(localName = "organizace")
    public Vocab organizace;

//<xs:element name="jazyk" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{jazyk}" -->
    @JacksonXmlProperty(localName = "jazyk")
    @Field
    public String jazyk;

//<xs:element name="telefon" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{telefon}" -->
    @JacksonXmlProperty(localName = "telefon")
    @Field
    public String telefon;

//<xs:element name="aktivni" minOccurs="1" maxOccurs="1" type="xs:boolean"/> <!-- "{is_active}" -->
    @JacksonXmlProperty(localName = "aktivni")
    @Field
    public boolean aktivni;

//<xs:element name="admin" minOccurs="1" maxOccurs="1" type="xs:boolean"/> <!-- "{is_staff}" -->
    @JacksonXmlProperty(localName = "admin")
    @Field
    public boolean admin;

//<xs:element name="superadmin" minOccurs="1" maxOccurs="1" type="xs:boolean"/> <!-- "{is_superuser}" -->   
    @JacksonXmlProperty(localName = "superadmin")
    @Field
    public boolean superadmin;

//<xs:element name="skupina" minOccurs="1" maxOccurs="unbounded" type="amcr:langstringType"/> <!-- "{groups.name}" -->
    @JacksonXmlProperty(localName = "skupina")
    public List<Lang> skupina = new ArrayList();

//<xs:element name="notifikace" minOccurs="0" maxOccurs="unbounded" type="xs:string"/> <!-- "{notification_types.ident_cely}" -->
    @JacksonXmlProperty(localName = "notifikace")
    @Field
    public List<String> notifikace = new ArrayList();

//<xs:element name="hlidaci_pes" minOccurs="0" maxOccurs="unbounded" type="amcr:vocabType"/> <!-- "{pes_set}" -->
    @JacksonXmlProperty(localName = "hlidaci_pes")
    public List<Vocab> hlidaci_pes = new ArrayList();

//<xs:element name="datum_registrace" minOccurs="1" maxOccurs="1" type="xs:dateTime"/> <!-- "{date_joined}" -->
    @JacksonXmlProperty(localName = "datum_registrace")
    @Field
    public Date datum_registrace;

//<xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/> <!-- "{history_vazba.historie_set}" -->
    @JacksonXmlProperty(localName = "historie")
    public List<Historie> historie = new ArrayList();

//<xs:element name="spoluprace_nadrizeni" minOccurs="0" maxOccurs="unbounded" type="amcr:spoluprace_nadrizeniType"/> <!-- "{spoluprace_badatelu}" -->
//<xs:element name="spoluprace_podrizeni" minOccurs="0" maxOccurs="unbounded" type="amcr:spoluprace_podrizeniType"/> <!-- "{spoluprace_archeologu}" -->
    @JacksonXmlProperty(localName = "pristupnost")
    @Field
    public String pristupnost;

    @Override
    public void fillSolrFields(SolrInputDocument idoc) {
        IndexUtils.setDateStamp(idoc, ident_cely);
        IndexUtils.setDateStampFromHistory(idoc, historie);
        IndexUtils.addVocabField(idoc, "organizace", organizace);
    }
    
    @Override
    public boolean isSearchable(){
        return true;
    }

    @Override
    public String coreName() {
        return "uzivatel";
    }

    public SolrInputDocument createSolrDoc() {

        DocumentObjectBinder dob = new DocumentObjectBinder();
        SolrInputDocument idoc = dob.toSolrInputDocument(this);
        IndexUtils.setDateStamp(idoc, ident_cely);
        IndexUtils.setDateStampFromHistory(idoc, historie);
        return idoc;

    }

    public void setPristupnost() {
//A = Anonym (tj. uživatel bez autentizace)
//B = <amcr:skupina>Badatel</amcr:skupina>
//C = <amcr:skupina>Archeolog</amcr:skupina>
//D = <amcr:skupina>Archivář</amcr:skupina>
//E = <amcr:skupina>Administrátor</amcr:skupina>

        for (Lang lang : skupina) {
            if ("Administrátor".equalsIgnoreCase(lang.getValue())) {
                pristupnost = "E";
                return;
            }
            if ("Archivář".equalsIgnoreCase(lang.getValue())) {
                pristupnost = "D";
                return;
            }
            if ("Archeolog".equalsIgnoreCase(lang.getValue())) {
                pristupnost = "C";
                return;
            }
            if ("Badatel".equalsIgnoreCase(lang.getValue())) {
                pristupnost = "B";
                return;
            }
        }
        pristupnost = "A";

    }

    @Override
    public boolean filterOAI(JSONObject user, SolrDocument doc) {
        
//-- A: nikdy
//-- B-C: ident_cely = {user}.ident_cely
//-- D-E: bez omezení
        String userPr = user.optString("pristupnost", "A");
        if (userPr.equalsIgnoreCase("A")) {
            return false;
        } else if (userPr.compareToIgnoreCase("D") >= 0) {
            return true;
        } else if (userPr.compareToIgnoreCase("C") <= 0 && user.getString("ident_cely").equals(doc.getFirstValue("ident_cely"))) {
            return true;
        } else {
            return false;
        }
    }

}
