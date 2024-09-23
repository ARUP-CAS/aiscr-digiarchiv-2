package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;

/**
 *
 * @author alberto
 */
public class Akce {

//<xs:element name="projekt" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{projekt.ident_cely}" | "{projekt.ident_cely}" -->
    @JacksonXmlProperty(localName = "projekt")
    public Vocab akce_projekt;

//<xs:element name="typ" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{typ}" -->
    @JacksonXmlProperty(localName = "typ")
    @Field
    public String akce_typ;

//<xs:element name="je_nz" minOccurs="1" maxOccurs="1" type="xs:boolean"/> <!-- "{je_nz}" -->
    @JacksonXmlProperty(localName = "je_nz")
    @Field
    public boolean akce_je_nz;

//<xs:element name="odlozena_nz" minOccurs="1" maxOccurs="1" type="xs:boolean"/> <!-- "{odlozena_nz}" -->
    @JacksonXmlProperty(localName = "odlozena_nz")
    @Field
    public boolean akce_odlozena_nz;

//<xs:element name="hlavni_vedouci" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{hlavni_vedouci.ident_cely}" | "{hlavni_vedouci.vypis_cely}" -->
    @JacksonXmlProperty(localName = "hlavni_vedouci")
    public Vocab akce_hlavni_vedouci;

//<xs:element name="organizace" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{organizace.ident_cely}" | "{organizace.nazev}" -->
    @JacksonXmlProperty(localName = "organizace")
    public Vocab akce_organizace;

//<xs:element name="vedouci_akce_ostatni" minOccurs="0" maxOccurs="unbounded" type="amcr:vedouci_akce_ostatniType"/> <!-- "{akcevedouci_set}" -->
    @JacksonXmlProperty(localName = "vedouci_akce_ostatni")
    public List<VedouciAkceOstatni> akce_vedouci_akce_ostatni = new ArrayList();

//<xs:element name="specifikace_data" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{specifikace_data.ident_cely}" | "{specifikace_data.heslo}" -->
    @JacksonXmlProperty(localName = "specifikace_data")
    public Vocab akce_specifikace_data;

//<xs:element name="datum_zahajeni" minOccurs="0" maxOccurs="1" type="xs:date"/> <!-- "{datum_zahajeni}" -->
    @JacksonXmlProperty(localName = "datum_zahajeni")
    @Field
    public Date akce_datum_zahajeni;

//<xs:element name="datum_ukonceni" minOccurs="0" maxOccurs="1" type="xs:date"/> <!-- "{datum_ukonceni}" -->
    @JacksonXmlProperty(localName = "datum_ukonceni")
    @Field
    public Date akce_datum_ukonceni;

//<xs:element name="hlavni_typ" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{hlavni_typ.ident_cely}" | "{hlavni_typ.heslo}" -->
    @JacksonXmlProperty(localName = "hlavni_typ")
    public Vocab akce_hlavni_typ;

//<xs:element name="vedlejsi_typ" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{vedlejsi_typ.ident_cely}" | "{vedlejsi_typ.heslo}" -->
    @JacksonXmlProperty(localName = "vedlejsi_typ")
    public Vocab akce_vedlejsi_typ;

//<xs:element name="ulozeni_nalezu" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{ulozeni_nalezu}" -->
    @JacksonXmlProperty(localName = "ulozeni_nalezu")
    @Field
    public String akce_ulozeni_nalezu;

//<xs:element name="ulozeni_dokumentace" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{ulozeni_dokumentace}" -->
    @JacksonXmlProperty(localName = "ulozeni_dokumentace")
    @Field
    public String akce_ulozeni_dokumentace;

//<xs:element name="chranene_udaje" minOccurs="0" maxOccurs="1" type="amcr:akce-chranene_udajeType"/> <!-- SELF -->     
    @JacksonXmlProperty(localName = "chranene_udaje")
    private AkceChraneneUdaje akce_chranene_udaje;

    public void fillSolrFields(SolrInputDocument idoc) {

        // idoc from archeologicky_zaznam.
        String pristupnost = (String) idoc.getFieldValue("pristupnost");
        DocumentObjectBinder dob = new DocumentObjectBinder();
        SolrInputDocument akceDoc = dob.toSolrInputDocument(this);
        //akceDoc.setField("entity", "akce");
        //akceDoc.setField("pristupnost", pristupnost);
        IndexUtils.addVocabField(akceDoc, "akce_projekt", akce_projekt);
        IndexUtils.addRefField(akceDoc, "akce_hlavni_vedouci", akce_hlavni_vedouci);
        IndexUtils.addRefField(idoc, "autor_sort", akce_hlavni_vedouci);
        IndexUtils.addVocabField(akceDoc, "akce_organizace", akce_organizace);
        IndexUtils.addRefField(idoc, "organizace_sort", akce_organizace);
        IndexUtils.addVocabField(akceDoc, "akce_specifikace_data", akce_specifikace_data);
        IndexUtils.addVocabField(akceDoc, "akce_hlavni_typ", akce_hlavni_typ);
        IndexUtils.addVocabField(akceDoc, "akce_vedlejsi_typ", akce_vedlejsi_typ);

        for (VedouciAkceOstatni o : akce_vedouci_akce_ostatni) {
            IndexUtils.addJSONField(akceDoc, "akce_vedouci_akce_ostatni", o);
        }

        if (akce_chranene_udaje != null) {
            akce_chranene_udaje.fillSolrFields(akceDoc, pristupnost);
        }

        for (Entry<String, SolrInputField> entry : akceDoc.entrySet()) {
            idoc.setField(entry.getKey(), entry.getValue().getValue());
        }

        if (akce_datum_zahajeni != null) {
            SolrSearcher.addFieldNonRepeat(idoc, "datum_provedeni_od", akce_datum_zahajeni);
            String ukonceni = "*";
            if (akce_datum_ukonceni != null) {
                SolrSearcher.addFieldNonRepeat(idoc, "datum_provedeni_do", akce_datum_ukonceni);
                if (!akce_datum_ukonceni.before(akce_datum_zahajeni)) {
                    ukonceni = akce_datum_ukonceni.toInstant().toString();
                }
            }
            SolrSearcher.addFieldNonRepeat(idoc, "datum_provedeni", "[" + akce_datum_zahajeni.toInstant().toString() + " TO " + ukonceni + "]");
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
        List<Object> indexFields = Options.getInstance().getJSONObject("fields").getJSONObject("akce").getJSONArray("facets").toList();
        // List<String> prSufixAll = new ArrayList<>();

        for (Object f : indexFields) {
            String s = (String) f;
            String dest = s.split(":")[0];
            String orig = s.split(":")[1];
            IndexUtils.addByPath(idoc, orig, dest, prSufix);
        }

    }

    private void setFullText(SolrInputDocument idoc, List<String> prSufix) {
        List<Object> indexFields = Options.getInstance().getJSONObject("fields").getJSONObject("akce").getJSONArray("full_text").toList();

        for (Object f : indexFields) {
            String s = (String) f;
            if (idoc.containsKey(s)) {
                IndexUtils.addSecuredFieldNonRepeat(idoc, "text_all", idoc.getFieldValues(s), prSufix);
            }
            for (String sufix : prSufix) {
                if (idoc.containsKey(s + "_" + sufix)) {
                    IndexUtils.addFieldNonRepeat(idoc, "text_all_" + sufix, idoc.getFieldValues(s + "_" + sufix));
                }
            }
        }

        for (String sufix : prSufix) {
            IndexUtils.addRefField(idoc, "text_all_" + sufix, akce_hlavni_typ);
            IndexUtils.addRefField(idoc, "text_all_" + sufix, akce_vedlejsi_typ);
//            for (VedouciAkceOstatni o : akce_vedouci_akce_ostatni) {
//                IndexUtils.addRefField(idoc, "text_all_" + sufix, o.organizace);
//                IndexUtils.addRefField(idoc, "text_all_" + sufix, o.vedouci);
//            }
        }

        for (String sufix : SolrSearcher.prSufixAll) {
            IndexUtils.addRefField(idoc, "text_all_" + sufix, akce_organizace);
            IndexUtils.addRefField(idoc, "text_all_" + sufix, akce_specifikace_data);
        }

    }

}

class AkceChraneneUdaje {
//<xs:element name="lokalizace_okolnosti" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{lokalizace_okolnosti}" -->

    @JacksonXmlProperty(localName = "lokalizace_okolnosti")
    public String lokalizace_okolnosti;

//<xs:element name="souhrn_upresneni" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{souhrn_upresneni}" -->
    @JacksonXmlProperty(localName = "souhrn_upresneni")
    public String souhrn_upresneni;

    public void fillSolrFields(SolrInputDocument idoc, String pristupnost) {
        IndexUtils.setSecuredJSONFieldPrefix(idoc, "akce", this);
        IndexUtils.setSecuredField(idoc, "akce_lokalizace_okolnosti", lokalizace_okolnosti, pristupnost);
//        IndexUtils.setSecuredField(idoc, "akce_chranene_udaje_souhrn_upresneni", souhrn_upresneni, pristupnost);
    }

}
