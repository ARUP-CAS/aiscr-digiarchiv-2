package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.util.ArrayList;
import java.util.List;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author alberto
 */
public class Lokalita {

//<xs:element name="typ_lokality" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ_lokality.ident_cely}" | "{typ_lokality.heslo}" -->
    @JacksonXmlProperty(localName = "typ_lokality")
    public Vocab lokalita_typ_lokality;

//<xs:element name="druh" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{druh.ident_cely}" | "{druh.heslo}" -->
    @JacksonXmlProperty(localName = "druh")
    public Vocab lokalita_druh;

//<xs:element name="zachovalost" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{zachovalost.ident_cely}" | "{zachovalost.heslo}" -->
    @JacksonXmlProperty(localName = "zachovalost")
    public Vocab lokalita_zachovalost;

//<xs:element name="jistota" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{jistota.ident_cely}" | "{jistota.heslo}" -->
    @JacksonXmlProperty(localName = "jistota")
    public Vocab lokalita_jistota;

//<xs:element name="chranene_udaje" minOccurs="0" maxOccurs="1" type="amcr:lok-chranene_udajeType"/> <!-- SELF -->  
    @JacksonXmlProperty(localName = "chranene_udaje")
    private LokalitaChraneneUdaje lokalita_chranene_udaje;

    public void fillSolrFields(SolrInputDocument idoc) {
        IndexUtils.addVocabField(idoc, "lokalita_typ_lokality", lokalita_typ_lokality);
        IndexUtils.addVocabField(idoc, "f_typ_lokality", lokalita_typ_lokality);
        IndexUtils.addVocabField(idoc, "lokalita_druh", lokalita_druh);
        IndexUtils.addVocabField(idoc, "f_druh_lokality", lokalita_druh);
        IndexUtils.addVocabField(idoc, "lokalita_zachovalost", lokalita_zachovalost);
        IndexUtils.addRefField(idoc, "lokalita_zachovalost_human", lokalita_zachovalost);
        IndexUtils.addVocabField(idoc, "lokalita_jistota", lokalita_jistota);

        if (lokalita_chranene_udaje != null) {
            lokalita_chranene_udaje.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
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
        List<Object> indexFields = Options.getInstance().getJSONObject("fields").getJSONObject("lokalita").getJSONArray("facets").toList();
        // List<String> prSufixAll = new ArrayList<>();
        
        for (Object f : indexFields) {
            String s = (String) f;
            String dest = s.split(":")[0];
            String orig = s.split(":")[1];
            IndexUtils.addByPath(idoc, orig, dest, prSufix);
//            if (idoc.containsKey(orig)) {
//                IndexUtils.addFieldNonRepeat(idoc, dest, idoc.getFieldValues(orig));
//            } 
//            for (String sufix : prSufix) {
//                if (idoc.containsKey(orig + "_" + sufix)) {
//                    IndexUtils.addFieldNonRepeat(idoc, dest + "_" + sufix, idoc.getFieldValues(orig + "_" + sufix));
//                }
//            }
        }
    }

    private void setFullText(SolrInputDocument idoc, List<String> prSufix) {
        List<Object> indexFields = Options.getInstance().getJSONObject("fields").getJSONObject("lokalita").getJSONArray("full_text").toList();
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

        for (String sufix : SolrSearcher.prSufixAll) {
            IndexUtils.addRefField(idoc, "text_all_" + sufix, lokalita_druh);
            IndexUtils.addRefField(idoc, "text_all_" + sufix, lokalita_typ_lokality);
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
        IndexUtils.setSecuredJSONFieldPrefix(idoc, "lokalita", this);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "text_all", nazev, pristupnost);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "text_all", popis, pristupnost);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "text_all", poznamka, pristupnost);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "lokalita_nazev", nazev, pristupnost);
        idoc.addField("nazev_sort", nazev);  
    }

}
