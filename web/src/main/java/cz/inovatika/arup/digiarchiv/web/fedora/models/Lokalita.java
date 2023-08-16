package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.util.ArrayList;
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
public class Lokalita {

//<xs:element name="typ_lokality" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ_lokality.ident_cely}" | "{typ_lokality.heslo}" -->
    @JacksonXmlProperty(localName = "typ_lokality")
    public Vocab typ_lokality;

//<xs:element name="druh" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{druh.ident_cely}" | "{druh.heslo}" -->
    @JacksonXmlProperty(localName = "druh")
    public Vocab druh;

//<xs:element name="zachovalost" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{zachovalost.ident_cely}" | "{zachovalost.heslo}" -->
    @JacksonXmlProperty(localName = "zachovalost")
    public Vocab zachovalost;

//<xs:element name="jistota" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{jistota.ident_cely}" | "{jistota.heslo}" -->
    @JacksonXmlProperty(localName = "jistota")
    public Vocab jistota;

//<xs:element name="chranene_udaje" minOccurs="0" maxOccurs="1" type="amcr:lok-chranene_udajeType"/> <!-- SELF -->  
    @JacksonXmlProperty(localName = "chranene_udaje")
    private LokalitaChraneneUdaje chranene_udaje;

    public void fillSolrFields(SolrInputDocument idoc) {
        IndexUtils.addVocabField(idoc, "typ_lokality", typ_lokality);
        IndexUtils.addVocabField(idoc, "druh", druh);
        IndexUtils.addVocabField(idoc, "zachovalost", zachovalost);
        IndexUtils.addVocabField(idoc, "jistota", jistota);

        if (chranene_udaje != null) {
            chranene_udaje.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
        }

    }

    public void setFullText(SolrInputDocument idoc) {
        List<Object> indexFields = Options.getInstance().getJSONObject("indexFieldsByType").getJSONArray("lokalita").toList();
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

        // Fields allways searchable
        List<String> prSufixAll = new ArrayList<>();
        prSufixAll.add("A");
        prSufixAll.add("B");
        prSufixAll.add("C");
        prSufixAll.add("D");

        for (String sufix : prSufixAll) {
            IndexUtils.addRefField(idoc, "text_all_" + sufix, druh);
            IndexUtils.addRefField(idoc, "text_all_" + sufix, typ_lokality);
        }
    }

}

class LokalitaChraneneUdaje {

//<xs:element name="nazev" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{nazev}" -->
    @JacksonXmlProperty(localName = "nazev")
    public String nazev;

//<xs:element name="popis" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{popis}" -->
    @JacksonXmlProperty(localName = "popis")
    public String popis;

//<xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
    @JacksonXmlProperty(localName = "poznamka")
    public String poznamka;

    public void fillSolrFields(SolrInputDocument idoc, String pristupnost) {
        IndexUtils.addSecuredFieldNonRepeat(idoc, "nazev", nazev, pristupnost);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "popis", popis, pristupnost);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "poznamka", poznamka, pristupnost);
    }

}
