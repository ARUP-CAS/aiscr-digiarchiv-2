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
    public Vocab okres;

//<xs:element name="pristupnost" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{pristupnost.ident_cely}" | "{pristupnost.heslo}" -->
    @JacksonXmlProperty(localName = "pristupnost")
    public Vocab pristupnost;

//<xs:element name="chranene_udaje" minOccurs="0" maxOccurs="1" type="amcr:az-chranene_udajeType"/> <!-- self -->
    @JacksonXmlProperty(localName = "chranene_udaje")
    private AZChraneneUdaje chranene_udaje;

//<xs:choice minOccurs="1" maxOccurs="1">
//  <xs:element name="akce" type="amcr:akceType"/> <!-- "{akce}" -->
    @JacksonXmlProperty(localName = "akce")
    public Akce akce;
//  <xs:element name="lokalita" type="amcr:lokalitaType"/> <!-- "{lokalita}" -->
    @JacksonXmlProperty(localName = "lokalita")
    public Lokalita lokalita;
//</xs:choice>

//<xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/> <!-- "{historie.historie_set}" -->
    @JacksonXmlProperty(localName = "historie")
    public List<Historie> historie = new ArrayList();

//<xs:element name="dokumentacni_jednotka" minOccurs="0" maxOccurs="unbounded" type="amcr:dokumentacni_jednotkaType"/> <!-- "{dokumentacni_jednotky_akce}" -->
    @JacksonXmlProperty(localName = "dokumentacni_jednotka")
    public List<DokumentacniJednotka> dokumentacni_jednotka = new ArrayList();

//<xs:element name="ext_odkaz" minOccurs="0" maxOccurs="unbounded" type="amcr:az-ext_odkazType"/> <!-- "{externi_odkazy}" -->
    @JacksonXmlProperty(localName = "ext_odkaz")
    public List<ExtOdkaz> ext_odkaz = new ArrayList();

//<xs:element name="dokument" minOccurs="0" maxOccurs="unbounded" type="amcr:refType"/> <!-- "{casti_dokumentu.dokument.ident_cely}" | "{casti_dokumentu.dokument.ident_cely}" -->
    @JacksonXmlProperty(localName = "dokument")
    public List<Vocab> dokument = new ArrayList();

    @Override
    public String coreName() {
        return "entities";
    }

    @Override
    public void fillSolrFields(SolrInputDocument idoc) {

        entity = akce != null ? "akce" : "lokalita";
        idoc.setField("entity", entity);

        boolean searchable = stav == 3;
        idoc.setField("searchable", searchable);
        idoc.setField("pristupnost", SearchUtils.getPristupnostMap().get(pristupnost.getId()));
        IndexUtils.addRefField(idoc, "okres", okres);

        if (chranene_udaje != null) {
            chranene_udaje.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
        }

        for (Vocab v : dokument) {
            IndexUtils.addVocabField(idoc, "dokument", v);
        }

        for (ExtOdkaz v : ext_odkaz) {
            IndexUtils.addJSONField(idoc, "ext_odkaz", v);
        }

        IndexUtils.setDateStamp(idoc, historie);

        List<SolrInputDocument> djdocs = new ArrayList<>();
        try {
            for (DokumentacniJednotka dj : dokumentacni_jednotka) {
                SolrInputDocument djdoc = dj.createSolrDoc();
                djdocs.add(djdoc);

                IndexUtils.addJSONField(idoc, "dokumentacni_jednotka", dj);
                // choose dokumentacni_jednotka fields to put in idoc for akce/lokalita
                idoc.addField("dokumentacni_jednotka_ident_cely", dj.ident_cely);
                idoc.addField("dokumentacni_jednotka_pian", djdoc.getFieldValue("pian"));
                idoc.addField("dokumentacni_jednotka_adb", djdoc.getFieldValue("adb"));
                idoc.addField("dokumentacni_jednotka_nazev", djdoc.getFieldValue("nazev"));

                idoc.addField("dokumentacni_jednotka_komponenta_obdobi", djdoc.getFieldValue("komponenta_obdobi"));
                idoc.addField("dokumentacni_jednotka_komponenta_presna_datace", djdoc.getFieldValue("komponenta_presna_datace"));
                idoc.addField("dokumentacni_jednotka_komponenta_areal", djdoc.getFieldValue("komponenta_areal"));
                idoc.addField("dokumentacni_jednotka_komponenta_aktivita", djdoc.getFieldValue("komponenta_aktivita"));

                idoc.addField("dokumentacni_jednotka_komponenta_typ_nalezu", djdoc.getFieldValue("komponenta_typ_nalezu"));
                idoc.addField("dokumentacni_jednotka_komponenta_nalez_objekt_druh", djdoc.getFieldValue("komponenta_nalez_objekt_druh"));
                idoc.addField("dokumentacni_jednotka_komponenta_nalez_objekt_specifikace", djdoc.getFieldValue("komponenta_nalez_objekt_specifikace"));
                idoc.addField("dokumentacni_jednotka_komponenta_nalez_predmet_druh", djdoc.getFieldValue("komponenta_nalez_predmet_druh"));
                idoc.addField("dokumentacni_jednotka_komponenta_nalez_predmet_specifikace", djdoc.getFieldValue("komponenta_nalez_predmet_specifikace"));

                IndexUtils.addFieldNonRepeat(idoc, "dokumentacni_jednotka_typ", djdoc.getFieldValue("typ"));

                // add loc field by pian
                addPian(idoc, (String) djdoc.getFieldValue("pian"));

                //add adb fields
                addAdbFields(idoc, (String) djdoc.getFieldValue("adb"));
            }
            if (!djdocs.isEmpty()) {
                IndexUtils.getClient().add("entities", djdocs, 10);
            }
        } catch (Exception ex) {
            Logger.getLogger(ArcheologickyZaznam.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (akce != null) {
            akce.fillSolrFields(idoc);
        }

        if (lokalita != null) {
            lokalita.fillSolrFields(idoc);
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
            IndexUtils.addRefField(idoc, "text_all_" + sufix, chranene_udaje.hlavni_katastr);
            for (Vocab v : chranene_udaje.dalsi_katastr) {
                IndexUtils.addRefField(idoc, "text_all_" + sufix, v);
            }

            IndexUtils.addFieldNonRepeat(idoc, "text_all_" + sufix, chranene_udaje.uzivatelske_oznaceni);
        }

        List<String> prSufixAll = new ArrayList<>();
        prSufixAll.add("A");
        prSufixAll.add("B");
        prSufixAll.add("C");
        prSufixAll.add("D");
        // Fields allways searchable
        String[] defFields = new String[]{"ident_cely", "dokument",
            "projekt"};
        for (String sufix : prSufixAll) {
            for (String field : defFields) {
                IndexUtils.addFieldNonRepeat(idoc, "text_all_" + sufix, idoc.getFieldValues(field));
            }
            IndexUtils.addRefField(idoc, "text_all_" + sufix, okres);
        }

        if (akce != null) {
            akce.setFullText(idoc);
        }

        if (lokalita != null) {
            lokalita.setFullText(idoc);
        }
    }

    private void addAdbFields(SolrInputDocument idoc, String ident_cely) {
        SolrQuery query = new SolrQuery("ident_cely:\"" + ident_cely + "\"")
                .setFields("ident_cely,podnet,typ_sondy,autor_popisu,autor_revize");
        JSONObject json = SearchUtils.json(query, IndexUtils.getClient(), "entities");

        if (json.getJSONObject("response").getInt("numFound") > 0) {
            for (int d = 0; d < json.getJSONObject("response").getJSONArray("docs").length(); d++) {
                JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(d);

                for (String key : doc.keySet()) {
                    SolrSearcher.addFieldNonRepeat(idoc, "dokumentacni_jednotka_adb_" + key, doc.opt(key));
                }
            }
        }
    }

    private void addPian(SolrInputDocument idoc, String pian) {
        SolrQuery query = new SolrQuery("ident_cely:\"" + pian + "\"");
        JSONObject json = SearchUtils.json(query, IndexUtils.getClient(), "entities");

        if (json.getJSONObject("response").getInt("numFound") > 0) {
            idoc.addField("pian_id", pian);
            for (int d = 0; d < json.getJSONObject("response").getJSONArray("docs").length(); d++) {
                JSONObject pianDoc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(d);
//        JSONObject cu = new JSONObject((String)idoc.getFieldValue("chranene_udaje"));
//        cu.put("pian", pianDoc);
//        idoc.setField("chranene_udaje", cu.toString());
                for (String key : pianDoc.keySet()) {
                    switch (key) {
                        case "entity":
                        case "searchable":
                        case "_version_":
                        case "stav":
                        case "chranene_udaje":
                            break;
                        default:
                            // idoc.setField("dokumentacni_jednotka_pian_" + key, pianDoc.opt(key));
                            if (key.startsWith("loc")) {
                                SolrSearcher.addFieldNonRepeat(idoc, key, pianDoc.opt(key));
                            } else if (key.startsWith("lat") || key.startsWith("lng")) {
                                // SolrSearcher.addFieldNonRepeat(idoc, "lng" + key.substring(3), pianDoc.opt(key));
                                JSONArray val = pianDoc.optJSONArray(key);
                                for (int i = 0; i < val.length(); i++) {
                                    SolrSearcher.addFieldNonRepeat(idoc, key, val.getBigDecimal(i).toString());
                                }

                            } else {
                                // SolrSearcher.addFieldNonRepeat(idoc, "dokumentacni_jednotka_pian_" + key, pianDoc.opt(key));
                            }
                    }
                }
            }
        }
    }

    @Override
    public String filterOAI(String userPristupnost, SolrDocument doc) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
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
        IndexUtils.setSecuredJSONField(idoc, this);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "hlavni_katastr", hlavni_katastr.getValue(), pristupnost);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "f_katastr", hlavni_katastr.getValue(), pristupnost);

        for (Vocab v : dalsi_katastr) {
            IndexUtils.addSecuredFieldNonRepeat(idoc, "dalsi_katastr", v.getValue(), pristupnost);
            IndexUtils.addSecuredFieldNonRepeat(idoc, "f_katastr", v.getValue(), pristupnost);
        }
        IndexUtils.addSecuredFieldNonRepeat(idoc, "uzivatelske_oznaceni", uzivatelske_oznaceni, pristupnost);

    }
}
