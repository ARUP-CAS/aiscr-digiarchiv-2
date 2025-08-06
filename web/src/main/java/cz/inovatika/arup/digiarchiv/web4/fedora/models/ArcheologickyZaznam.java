package cz.inovatika.arup.digiarchiv.web4.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web4.Options;
import cz.inovatika.arup.digiarchiv.web4.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web4.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web4.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web4.index.SolrSearcher;
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
    public boolean isSearchable() {
        return stav == 3;
    }

    @Override
    public void fillSolrFields(SolrInputDocument idoc) throws Exception {

        entity = az_akce != null ? "akce" : "lokalita";
        idoc.setField("entity", entity);

        boolean searchable = stav == 3;
        idoc.setField("searchable", searchable);
        idoc.setField("pristupnost", SearchUtils.getPristupnostMap().get(pristupnost.getId()));
        IndexUtils.addRefField(idoc, "az_okres", az_okres);
        IndexUtils.addRefField(idoc, "okres_sort", az_okres);
        String kraj_nazev = SolrSearcher.getKrajByOkres(az_okres.getId()).getString("kraj_nazev");
        IndexUtils.addFieldNonRepeat(idoc, "f_kraj", kraj_nazev);  

        if (az_chranene_udaje != null) {
            az_chranene_udaje.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
        }

        for (Vocab v : az_dokument) {
            IndexUtils.addVocabField(idoc, "az_dokument", v);
        }

        for (ExtOdkaz v : az_ext_odkaz) {
            IndexUtils.addJSONField(idoc, "az_ext_odkaz", v);
            IndexUtils.addRefField(idoc, "az_ext_zdroj", v.ext_zdroj);
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
                idoc.addField("az_dj_negativni_jednotka", djdoc.getFieldValue("dj_negativni_jednotka"));
                idoc.addField("az_dj_nazev", dj.dj_nazev);

                for (Komponenta k : dj.dj_komponenta) {
                    idoc.addField("komponenta_ident_cely", k.ident_cely);
                    idoc.addField("komponenta_dokument_presna_datace", k.komponenta_presna_datace);
                }
                if (az_akce != null) {
                    IndexUtils.addFieldNonRepeat(idoc, "f_dj_typ", djdoc.getFieldValue("dj_typ"));
                }

                // add loc field by pian
                if (djdoc.getFieldValue("dj_pian") != null) {
                    addPian(idoc, (String) djdoc.getFieldValue("dj_pian"), (String) idoc.getFieldValue("pristupnost"));
                }

                //add adb fields
                if (djdoc.getFieldValue("dj_adb") != null) {
                    // addAdbFields(idoc, (String) djdoc.getFieldValue("dj_adb"));
                }
            }
            if (!djdocs.isEmpty()) {
                IndexUtils.getClientBinIndex().add("entities", djdocs, 10);
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
        
        setFacets(idoc, prSufix);
        setFullText(idoc, prSufix);
    }
    
    public void setFacets(SolrInputDocument idoc, List<String> prSufix) {
        List<Object> indexFields = Options.getInstance().getJSONObject("fields").getJSONObject("archeologicky_zaznam").getJSONArray("facets").toList();
        // List<String> prSufixAll = new ArrayList<>();

        for (Object f : indexFields) {
            String s = (String) f;
            String dest = s.split(":")[0];
            String orig = s.split(":")[1];
            IndexUtils.addByPath(idoc, orig, dest, prSufix, false);
        }

    }

    public void setFullText(SolrInputDocument idoc, List<String> prSufix) {
        List<Object> indexFields = Options.getInstance().getJSONObject("fields").getJSONObject("archeologicky_zaznam").getJSONArray("full_text").toList();

        for (Object f : indexFields) {
            String s = (String) f;
            if (s.contains(".")) {
                IndexUtils.addByPath(idoc, s, "text_all", prSufix, true);
            } else {
                for (String sufix : prSufix) {
                    if (idoc.containsKey(s)) {
                        IndexUtils.addFieldNonRepeat(idoc, "text_all_" + sufix, idoc.getFieldValues(s));
                    }
                    if (idoc.containsKey(s + "_" + sufix)) {
                        IndexUtils.addFieldNonRepeat(idoc, "text_all_" + sufix, idoc.getFieldValues(s + "_" + sufix));
                    }
                }

            }
        }

//        Object[] fields = idoc.getFieldNames().toArray();
//        for (Object f : fields) {
//            String s = (String) f;
//            if (s.contains(".")) {
//                IndexUtils.addByPath(idoc, s, "text_all", prSufix, true);
//            } else {
//                if (indexFields.contains(s)) {
//                    for (String sufix : prSufix) {
//                        IndexUtils.addFieldNonRepeat(idoc, "text_all_" + sufix, idoc.getFieldValues(s));
//                    }
//                }
//            }
//        }
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
                //.setFields("ident_cely,adb_podnet,adb_typ_sondy,adb_autor_popisu,adb_autor_revize,adb_vyskovy_bod_typ_A,adb_vyskovy_bod_typ_B,adb_vyskovy_bod_typ_C,adb_vyskovy_bod_typ_D");
                .setFields("*");
        JSONObject json = SearchUtils.searchOrIndex(query, "entities", ident_cely);

        if (json.getJSONObject("response").getInt("numFound") > 0) {
            for (int d = 0; d < json.getJSONObject("response").getJSONArray("docs").length(); d++) {
                JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(d);
//                for (String key : doc.keySet()) {
//                    SolrSearcher.addFieldNonRepeat(idoc, "az_dj_" + key, doc.opt(key));
//                }
                IndexUtils.addFieldNonRepeat(idoc, "f_adb_typ_sondy", doc.optString("adb_typ_sondy", null));
                IndexUtils.addFieldNonRepeat(idoc, "f_adb_podnet", doc.optString("adb_podnet", null));
                IndexUtils.addFieldNonRepeat(idoc, "f_autor", doc.optString("adb_autor_revize", null));
                IndexUtils.addFieldNonRepeat(idoc, "az_adb_rok_popisu", doc.optIntegerObject("adb_rok_popisu", null));
                IndexUtils.addFieldNonRepeat(idoc, "az_adb_rok_revize", doc.optIntegerObject("adb_rok_revize", null));
                IndexUtils.addFieldNonRepeat(idoc, "vyskovy_bod_ident_cely", doc.opt("vyskovy_bod_ident_cely"));

                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_uzivatelske_oznaceni_sondy_A", doc.opt("adb_chranene_udaje_uzivatelske_oznaceni_sondy_A"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_uzivatelske_oznaceni_sondy_B", doc.opt("adb_chranene_udaje_uzivatelske_oznaceni_sondy_B"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_uzivatelske_oznaceni_sondy_C", doc.opt("adb_chranene_udaje_uzivatelske_oznaceni_sondy_C"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_uzivatelske_oznaceni_sondy_D", doc.opt("adb_chranene_udaje_uzivatelske_oznaceni_sondy_D"));

                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_trat_A", doc.opt("adb_chranene_udaje_trat_A"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_trat_B", doc.opt("adb_chranene_udaje_trat_B"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_trat_C", doc.opt("adb_chranene_udaje_trat_C"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_trat_D", doc.opt("adb_chranene_udaje_trat_D"));

                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_cislo_popisne_A", doc.opt("adb_chranene_udaje_cislo_popisne_A"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_cislo_popisne_B", doc.opt("adb_chranene_udaje_cislo_popisne_B"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_cislo_popisne_C", doc.opt("adb_chranene_udaje_cislo_popisne_C"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_cislo_popisne_D", doc.opt("adb_chranene_udaje_cislo_popisne_D"));

                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_parcelni_cislo_A", doc.opt("adb_chranene_udaje_parcelni_cislo_A"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_parcelni_cislo_B", doc.opt("adb_chranene_udaje_parcelni_cislo_B"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_parcelni_cislo_C", doc.opt("adb_chranene_udaje_parcelni_cislo_C"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_parcelni_cislo_D", doc.opt("adb_chranene_udaje_parcelni_cislo_D"));

                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_poznamka_A", doc.opt("adb_chranene_udaje_poznamka_A"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_poznamka_B", doc.opt("adb_chranene_udaje_poznamka_B"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_poznamka_C", doc.opt("adb_chranene_udaje_poznamka_C"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_poznamka_D", doc.opt("adb_chranene_udaje_poznamka_D"));

                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_vyskovy_bod_typ_A", doc.opt("adb_vyskovy_bod_typ_A"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_vyskovy_bod_typ_B", doc.opt("adb_vyskovy_bod_typ_B"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_vyskovy_bod_typ_C", doc.opt("adb_vyskovy_bod_typ_C"));
                IndexUtils.addFieldNonRepeatByJSONVal(idoc, "adb_vyskovy_bod_typ_D", doc.opt("adb_vyskovy_bod_typ_D"));
            }
        }
    }

    private void addPian(SolrInputDocument idoc, String pian, String pristupnost) throws Exception {
        idoc.addField("pian_id", pian);
        idoc.addField("pian_ident_cely", pian);
        SolrQuery query = new SolrQuery("ident_cely:\"" + pian + "\"")
                .setFields("*,pian_chranene_udaje:[json]");
        JSONObject json = SearchUtils.searchOrIndex(query, "entities", pian);

        if (json.getJSONObject("response").getInt("numFound") > 0) {
            for (int d = 0; d < json.getJSONObject("response").getJSONArray("docs").length(); d++) {
                JSONObject pianDoc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(d);

                // IndexUtils.setSecuredJSONField(idoc, "pian", pianDoc, pristupnost);
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

    public List<String> okresy = new ArrayList<>();
    public List<String> kraje = new ArrayList<>();

    public void fillSolrFields(SolrInputDocument idoc, String pristupnost) {
        for (Vocab v : dalsi_katastr) {
            String ruian = v.getId();
            JSONObject k = SolrSearcher.getOkresNazevByKatastr(ruian);
            String okres = k.getString("okres_nazev");
            if (!okresy.contains(okres)) {
                okresy.add(okres);
                IndexUtils.addFieldNonRepeat(idoc, "f_okres", okres);
            }
            String kraj = k.getString("kraj_nazev");
            if (!kraje.contains(kraj)) {
                kraje.add(kraj);
                IndexUtils.addFieldNonRepeat(idoc, "f_kraj", kraj);
            }
        }

        IndexUtils.setSecuredJSONField(idoc, "az_chranene_udaje", this); 
        IndexUtils.addSecuredFieldNonRepeat(idoc, "f_katastr", hlavni_katastr.getValue(), pristupnost);
        
//            JSONObject k = SolrSearcher.getOkresNazevByKatastr(hlavni_katastr.getId());
//            IndexUtils.addFieldNonRepeat(idoc, "f_kraj", k.getString("kraj_nazev"));
            
        IndexUtils.addRefField(idoc, "katastr_sort", hlavni_katastr);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "f_uzivatelske_oznaceni", uzivatelske_oznaceni, pristupnost);

        for (Vocab v : dalsi_katastr) {
            IndexUtils.addSecuredFieldNonRepeat(idoc, "f_katastr", v.getValue(), pristupnost);
            
            IndexUtils.addFieldNonRepeat(idoc, "f_okres", SolrSearcher.getOkresNazevByKatastr(v.getId()).getString("okres_nazev"));
        }
    }
}
