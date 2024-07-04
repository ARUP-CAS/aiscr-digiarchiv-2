package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class ArcheologickyZaznam implements FedoraModel {

    @Field
    public String entity = "akce nebo lokalita";

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
    @JacksonXmlProperty(localName = "ident_cely")
    @Field
    public String ident_cely;

//  <xs:element name="stav" minOccurs="1" maxOccurs="1" type="xs:integer"/> <!-- "{stav}" -->
    @JacksonXmlProperty(localName = "stav")
    @Field
    public long stav;

//<xs:element name="okres" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "ruian-{hlavni_katastr.okres.kod}" | "{hlavni_katastr.okres.nazev}" -->
    @JacksonXmlProperty(localName = "okres")
    public Vocab az_okres;

//<xs:element name="pristupnost" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{pristupnost.ident_cely}" | "{pristupnost.heslo}" -->
    @JacksonXmlProperty(localName = "pristupnost")
    public Vocab pristupnost;

//<xs:element name="chranene_udaje" minOccurs="0" maxOccurs="1" type="amcr:az-chranene_udajeType"/> <!-- self -->
    @JacksonXmlProperty(localName = "chranene_udaje")
    private AZChraneneUdaje az_chranene_udaje;

//<xs:choice minOccurs="1" maxOccurs="1">
//  <xs:element name="akce" type="amcr:akceType"/> <!-- "{akce}" -->
    @JacksonXmlProperty(localName = "akce")
    public Akce az_akce;
//  <xs:element name="lokalita" type="amcr:lokalitaType"/> <!-- "{lokalita}" -->
    @JacksonXmlProperty(localName = "lokalita")
    public Lokalita az_lokalita;
//</xs:choice>

//<xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/> <!-- "{historie.historie_set}" -->
    @JacksonXmlProperty(localName = "historie")
    public List<Historie> historie = new ArrayList();

//<xs:element name="dokumentacni_jednotka" minOccurs="0" maxOccurs="unbounded" type="amcr:dokumentacni_jednotkaType"/> <!-- "{dokumentacni_jednotky_akce}" -->
    @JacksonXmlProperty(localName = "dokumentacni_jednotka")
    public List<DokumentacniJednotka> az_dokumentacni_jednotka = new ArrayList();

//<xs:element name="ext_odkaz" minOccurs="0" maxOccurs="unbounded" type="amcr:az-ext_odkazType"/> <!-- "{externi_odkazy}" -->
    @JacksonXmlProperty(localName = "ext_odkaz")
    public List<ExtOdkaz> az_ext_odkaz = new ArrayList();

//<xs:element name="dokument" minOccurs="0" maxOccurs="unbounded" type="amcr:refType"/> <!-- "{casti_dokumentu.dokument.ident_cely}" | "{casti_dokumentu.dokument.ident_cely}" -->
    @JacksonXmlProperty(localName = "dokument")
    public List<Vocab> az_dokument = new ArrayList();

    @Override
    public String coreName() {
        return "entities";
    }

    @Override
    public void fillSolrFields(SolrInputDocument idoc) throws Exception {

        entity = az_akce != null ? "akce" : "lokalita";
        idoc.setField("entity", entity);

        boolean searchable = stav == 3;
        idoc.setField("searchable", searchable);
        idoc.setField("pristupnost", SearchUtils.getPristupnostMap().get(pristupnost.getId()));
        IndexUtils.addRefField(idoc, "az_okres", az_okres);

        if (az_chranene_udaje != null) {
            az_chranene_udaje.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
        }

        for (Vocab v : az_dokument) {
            IndexUtils.addVocabField(idoc, "az_dokument", v);
        }

        for (ExtOdkaz v : az_ext_odkaz) {
            IndexUtils.addJSONField(idoc, "az_ext_odkaz", v);
        }

        IndexUtils.setDateStamp(idoc, ident_cely);
        IndexUtils.setDateStampFromHistory(idoc, historie);

        List<SolrInputDocument> djdocs = new ArrayList<>();
        try {
            for (DokumentacniJednotka dj : az_dokumentacni_jednotka) {
                SolrInputDocument djdoc = dj.createSolrDoc();
                djdocs.add(djdoc);

                IndexUtils.addJSONField(idoc, "az_dokumentacni_jednotka", dj);
                // choose dokumentacni_jednotka fields to put in idoc for akce/lokalita
                idoc.addField("az_dj", dj.ident_cely);
                idoc.addField("az_dj_pian", djdoc.getFieldValue("dj_pian"));
                idoc.addField("az_dj_adb", djdoc.getFieldValue("dj_adb"));
//                idoc.addField("az_dj_nazev", djdoc.getFieldValue("dj_nazev"));
//
//                idoc.addField("az_dj_komponenta_obdobi", djdoc.getFieldValue("dj_komponenta_obdobi"));
//                idoc.addField("az_dj_komponenta_presna_datace", djdoc.getFieldValue("dj_komponenta_presna_datace"));
//                idoc.addField("az_dj_komponenta_areal", djdoc.getFieldValue("dj_komponenta_areal"));
//                idoc.addField("az_dj_komponenta_aktivita", djdoc.getFieldValue("dj_komponenta_aktivita"));
//
//                idoc.addField("az_dj_komponenta_typ_nalezu", djdoc.getFieldValue("dj_komponenta_typ_nalezu"));
//                idoc.addField("az_dj_komponenta_nalez_objekt_druh", djdoc.getFieldValue("dj_komponenta_nalez_objekt_druh"));
//                idoc.addField("az_dj_komponenta_nalez_objekt_specifikace", djdoc.getFieldValue("dj_komponenta_nalez_objekt_specifikace"));
//                idoc.addField("az_dj_komponenta_nalez_predmet_druh", djdoc.getFieldValue("dj_komponenta_nalez_predmet_druh"));
//                idoc.addField("az_dj_komponenta_nalez_predmet_specifikace", djdoc.getFieldValue("dj_komponenta_nalez_predmet_specifikace"));

//                IndexUtils.addFieldNonRepeat(idoc, "az_dj_typ", djdoc.getFieldValue("dj_typ"));
                IndexUtils.addFieldNonRepeat(idoc, "f_typ_vyzkumu", djdoc.getFieldValue("dj_typ"));

                // add loc field by pian
                if (djdoc.getFieldValue("dj_pian") != null ) {
                    addPian(idoc, (String) djdoc.getFieldValue("dj_pian"), (String) idoc.getFieldValue("pristupnost"));
                }

                //add adb fields
                if (idoc.getFieldValue("dj_adb") != null ) {
                    addAdbFields(idoc, (String) djdoc.getFieldValue("dj_adb"));
                }
            }
            if (!djdocs.isEmpty()) {
                IndexUtils.getClientBin().add("entities", djdocs, 10);
            } 
        } catch (Exception ex) {
            Logger.getLogger(ArcheologickyZaznam.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (az_akce != null) {
            az_akce.fillSolrFields(idoc);
        }

        if (az_lokalita != null) {
            az_lokalita.fillSolrFields(idoc);
        }

        setFullText(idoc);
    }

    public void setFullText(SolrInputDocument idoc) {
        List<Object> indexFields = Options.getInstance().getJSONObject("indexFieldsByType").getJSONArray("archeologicky_zaznam").toList();

        String pr = (String) idoc.getFieldValue("pristupnost");
        List<String> prSufix = new ArrayList<>();

        if ("A".compareTo(pr) >= 0) {
            prSufix.add("A");
        }
        if ("B".compareTo(pr) >= 0) {
            prSufix.add("B");
        }
        if ("C".compareTo(pr) >= 0) {
            prSufix.add("C");
        }
        if ("D".compareTo(pr) >= 0) {
            prSufix.add("D");
        }

        Object[] fields = idoc.getFieldNames().toArray();
        for (Object f : fields) {
            String s = (String) f;

            // SolrSearcher.addSecuredFieldFacets(s, idoc, prSufix);
            if (indexFields.contains(s)) {
                for (String sufix : prSufix) {
                    IndexUtils.addFieldNonRepeat(idoc, "text_all_" + sufix, idoc.getFieldValues(s));
                }
            }
        }
        for (String sufix : prSufix) {
            IndexUtils.addRefField(idoc, "text_all_" + sufix, az_chranene_udaje.hlavni_katastr);
            for (Vocab v : az_chranene_udaje.dalsi_katastr) {
                IndexUtils.addRefField(idoc, "text_all_" + sufix, v);
            }

            IndexUtils.addFieldNonRepeat(idoc, "text_all_" + sufix, az_chranene_udaje.uzivatelske_oznaceni);
        }

        
        // Fields allways searchable
        String[] defFields = new String[]{"ident_cely", "dokument",
            "projekt"};
        for (String sufix : SolrSearcher.prSufixAll) {
            for (String field : defFields) {
                IndexUtils.addFieldNonRepeat(idoc, "text_all_" + sufix, idoc.getFieldValues(field));
            }
            IndexUtils.addRefField(idoc, "text_all_" + sufix, az_okres);
        }
    }

    private void addAdbFields(SolrInputDocument idoc, String ident_cely) throws Exception {
        SolrQuery query = new SolrQuery("ident_cely:\"" + ident_cely + "\"")
                .setFields("ident_cely,adb_podnet,adb_typ_sondy,adb_autor_popisu,adb_autor_revize");
        JSONObject json = SearchUtils.searchOrIndex(query, "entities", ident_cely);

        if (json.getJSONObject("response").getInt("numFound") > 0) {
            for (int d = 0; d < json.getJSONObject("response").getJSONArray("docs").length(); d++) {
                JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(d);

                for (String key : doc.keySet()) {
                    SolrSearcher.addFieldNonRepeat(idoc, "dj_adb_" + key, doc.opt(key));
                }
            }
        }
    } 

    private void addPian(SolrInputDocument idoc, String pian, String pristupnost) throws Exception {
        idoc.addField("pian_id", pian);
        SolrQuery query = new SolrQuery("ident_cely:\"" + pian + "\"")
                .setFields("*,pian_chranene_udaje:[json]");
        JSONObject json = SearchUtils.searchOrIndex(query, "entities", pian);

        if (json.getJSONObject("response").getInt("numFound") > 0) { 
            for (int d = 0; d < json.getJSONObject("response").getJSONArray("docs").length(); d++) {
                JSONObject pianDoc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(d);

            // IndexUtils.addSecuredFieldNonRepeat(idoc, "pian", pianDoc.toString(), pristupnost);
            IndexUtils.addFieldNonRepeat(idoc, "f_pian_typ", pianDoc.getString("pian_typ"));
            IndexUtils.addFieldNonRepeat(idoc, "f_pian_presnost", pianDoc.getString("pian_presnost"));
            IndexUtils.addSecuredFieldNonRepeat(idoc, "f_pian_zm10", pianDoc.getJSONObject("pian_chranene_udaje").getString("zm10"), pristupnost);
            
                for (String key : pianDoc.keySet()) {
                    switch (key) {
                        case "entity":
                        case "searchable":
                        case "_version_":
                        case "stav":
                        case "chranene_udaje":
                            break;
                        default:
                            // idoc.setField("dj_pian_" + key, pianDoc.opt(key));
                            if (key.startsWith("loc")) {
                                //SolrSearcher.addFieldNonRepeat(idoc, key, pianDoc.opt(key));
                                
                                
                                JSONArray val = pianDoc.optJSONArray(key);
                                for (int i = 0; i < val.length(); i++) {
                                    SolrSearcher.addFieldNonRepeat(idoc, key, val.opt(i));
                                }
                                
                                
                            } else if (key.startsWith("lat") || key.startsWith("lng")) {
                                // SolrSearcher.addFieldNonRepeat(idoc, "lng" + key.substring(3), pianDoc.opt(key));
                                JSONArray val = pianDoc.optJSONArray(key);
                                for (int i = 0; i < val.length(); i++) {
                                    SolrSearcher.addFieldNonRepeat(idoc, key, val.getBigDecimal(i).toString());
                                }

                            } else {
                                // SolrSearcher.addFieldNonRepeat(idoc, "dj_pian_" + key, pianDoc.opt(key));
                            }
                    }
                }
            }
        }
    }
    

    @Override
    public boolean filterOAI(JSONObject user, SolrDocument doc) {
        
        long st = (long) doc.getFieldValue("stav");
        String userPr = user.optString("pristupnost", "A");
        if (userPr.compareToIgnoreCase("A") > 0 || st == 3) {
            return true;
        } else {
            return false;
        }
    }

}

class AZChraneneUdaje {

    //<xs:element name="hlavni_katastr" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "ruian-{hlavni_katastr.kod}" | "{hlavni_katastr.nazev}" -->
    @JacksonXmlProperty(localName = "hlavni_katastr")
    public Vocab hlavni_katastr;

//<xs:element name="dalsi_katastr" minOccurs="0" maxOccurs="unbounded" type="amcr:vocabType"/> <!-- "ruian-{katastry.kod}" | "{katastry.nazev}" -->
    @JacksonXmlProperty(localName = "dalsi_katastr")
    public List<Vocab> dalsi_katastr = new ArrayList<>();

//<xs:element name="uzivatelske_oznaceni" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{uzivatelske_oznaceni}" -->
    @JacksonXmlProperty(localName = "uzivatelske_oznaceni")
    public String uzivatelske_oznaceni;

    public void fillSolrFields(SolrInputDocument idoc, String pristupnost) {
        IndexUtils.setSecuredJSONField(idoc, "az_chranene_udaje", this);
        // IndexUtils.addSecuredFieldNonRepeat(idoc, "hlavni_katastr", hlavni_katastr.getValue(), pristupnost);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "f_katastr", hlavni_katastr.getValue(), pristupnost);

        for (Vocab v : dalsi_katastr) {
            // IndexUtils.addSecuredFieldNonRepeat(idoc, "dalsi_katastr", v.getValue(), pristupnost);
            IndexUtils.addSecuredFieldNonRepeat(idoc, "f_katastr", v.getValue(), pristupnost);
        }
        // IndexUtils.addSecuredFieldNonRepeat(idoc, "az_chranene_udaje_uzivatelske_oznaceni", uzivatelske_oznaceni, pristupnost);
    }
}
