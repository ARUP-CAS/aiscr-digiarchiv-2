package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;

/**
 *
 * @author alberto
 */
@JacksonXmlRootElement(localName = "samostatny_nalez")
public class SamostatnyNalez implements FedoraModel {

    @Field
    public String entity = "samostatny_nalez";

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
    @JacksonXmlProperty(localName = "ident_cely")
    @Field
    public String ident_cely;

//<xs:element name="evidencni_cislo" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{evidencni_cislo}" -->
    @JacksonXmlProperty(localName = "evidencni_cislo")
    @Field
    public String evidencni_cislo;

//<xs:element name="projekt" minOccurs="1" maxOccurs="1" type="amcr:refType"/> <!-- "{projekt.ident_cely}" | "{projekt.ident_cely}" -->
    @JacksonXmlProperty(localName = "projekt")
    public Vocab projekt;

//<xs:element name="okres" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "ruian-{katastr.okres.kod}" | "{katastr.okres.nazev}" -->
    @JacksonXmlProperty(localName = "okres")
    public Vocab okres;

//<xs:element name="hloubka" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{hloubka}" -->
    @JacksonXmlProperty(localName = "hloubka")
    @Field
    public long hloubka;

//<xs:element name="okolnosti" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{okolnosti.ident_cely}" | "{okolnosti.heslo}" -->
    @JacksonXmlProperty(localName = "okolnosti")
    public Vocab okolnosti;

//<xs:element name="obdobi" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{obdobi.ident_cely}" | "{obdobi.heslo}" -->
    @JacksonXmlProperty(localName = "obdobi")
    public Vocab obdobi;

//<xs:element name="presna_datace" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{presna_datace}" -->
    @JacksonXmlProperty(localName = "presna_datace")
    @Field
    public String presna_datace;

//<xs:element name="druh_nalezu" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{druh_nalezu.ident_cely}" | "{druh_nalezu.heslo}" -->
    @JacksonXmlProperty(localName = "druh_nalezu")
    public Vocab druh_nalezu;

//<xs:element name="specifikace" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{specifikace.ident_cely}" | "{specifikace.heslo}" -->
    @JacksonXmlProperty(localName = "specifikace")
    public Vocab specifikace;

//<xs:element name="pocet" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{pocet}" -->
    @JacksonXmlProperty(localName = "pocet")
    @Field
    public String pocet;

//<xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
    @JacksonXmlProperty(localName = "poznamka")
    @Field
    public String poznamka;

//<xs:element name="nalezce" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{nalezce.ident_cely}" | "{nalezce.vypis_cely}" -->
    @JacksonXmlProperty(localName = "nalezce")
    public Vocab nalezce;

//<xs:element name="datum_nalezu" minOccurs="0" maxOccurs="1" type="xs:date"/> <!-- "{datum_nalezu}" -->
    @JacksonXmlProperty(localName = "datum_nalezu")
    @Field
    public Date datum_nalezu;

//<xs:element name="stav" minOccurs="1" maxOccurs="1" type="xs:integer"/> <!-- "{stav}" -->
    @JacksonXmlProperty(localName = "stav")
    @Field
    public long stav;

//<xs:element name="predano" minOccurs="0" maxOccurs="1" type="xs:boolean"/> <!-- "{predano}" -->
    @JacksonXmlProperty(localName = "predano")
    @Field
    public boolean predano;

//<xs:element name="predano_organizace" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{predano_organizace.ident_cely}" | "{predano_organizace.nazev}" -->
    @JacksonXmlProperty(localName = "predano_organizace")
    public Vocab predano_organizace;

//<xs:element name="geom_system" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{geom_system}" -->
    @JacksonXmlProperty(localName = "geom_system")
    @Field
    public String geom_system;

//<xs:element name="geom_updated_at" minOccurs="0" maxOccurs="1" type="xs:dateTime"/> <!-- "{geom_updated_at}" -->
    @JacksonXmlProperty(localName = "geom_updated_at")
    @Field
    public Date geom_updated_at;

//<xs:element name="geom_sjtsk_updated_at" minOccurs="0" maxOccurs="1" type="xs:dateTime"/> <!-- "{geom_sjtsk_updated_at}" -->
    @JacksonXmlProperty(localName = "geom_sjtsk_updated_at")
    @Field
    public Date geom_sjtsk_updated_at;

//<xs:element name="pristupnost" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{pristupnost.ident_cely}" | "{pristupnost.heslo}" -->
    @JacksonXmlProperty(localName = "pristupnost")
    public Vocab pristupnost;

//<xs:element name="chranene_udaje" minOccurs="0" maxOccurs="1" type="amcr:sn-chranene_udajeType"/> <!-- SELF -->
    @JacksonXmlProperty(localName = "chranene_udaje")
    private SnChraneneUdaje chranene_udaje;

//<xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/> <!-- "{historie.historie_set}" -->  
    @JacksonXmlProperty(localName = "historie")
    public List<Historie> historie = new ArrayList();

//<xs:element name="soubor" minOccurs="0" maxOccurs="unbounded" type="amcr:souborType"/>  <!-- "{soubory.soubory}" -->  
    @JacksonXmlProperty(localName = "soubor")
    public List<Soubor> soubor = new ArrayList();

    @Override
    public String coreName() {
        return "entities";
    }

    @Override
    public void fillSolrFields(SolrInputDocument idoc) {
        idoc.setField("pristupnost", SearchUtils.getPristupnostMap().get(pristupnost.getId()));
        boolean searchable = stav == 4;
        idoc.setField("searchable", searchable);
        IndexUtils.setDateStamp(idoc, ident_cely);
        IndexUtils.setDateStampFromHistory(idoc, historie);

        IndexUtils.addRefField(idoc, "okres", okres);
        IndexUtils.addVocabField(idoc, "projekt", projekt);
        IndexUtils.addVocabField(idoc, "okolnosti", okolnosti);
        IndexUtils.addVocabField(idoc, "f_nalezove_okolnosti", okolnosti);
        IndexUtils.addVocabField(idoc, "obdobi", obdobi);
        IndexUtils.addVocabField(idoc, "f_obdobi", obdobi);
        IndexUtils.addVocabField(idoc, "druh_nalezu", druh_nalezu);
        IndexUtils.addVocabField(idoc, "specifikace", specifikace);
        IndexUtils.addVocabField(idoc, "predano_organizace", predano_organizace);
        IndexUtils.addVocabField(idoc, "nalezce", nalezce);

        List<SolrInputDocument> idocs = new ArrayList<>();
        try {
            for (Soubor s : soubor) {
                SolrInputDocument djdoc = s.createSolrDoc();
                idocs.add(djdoc);
                IndexUtils.addJSONField(idoc, "soubor", s);
                idoc.addField("soubor_nazev", s.nazev);
                idoc.addField("soubor_filepath", s.path);

            }
            if (!idocs.isEmpty()) {
                // IndexUtils.getClient().add("soubor", idocs, 10);
            }
        } catch (Exception ex) {
            Logger.getLogger(SamostatnyNalez.class.getName()).log(Level.SEVERE, null, ex); 
        }
        if (chranene_udaje != null) {
            chranene_udaje.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
        }
        setFullText(idoc);
    }

    public void setFullText(SolrInputDocument idoc) {
        List<Object> indexFields = Options.getInstance().getJSONObject("indexFieldsByType").getJSONArray("samostatny_nalez").toList();
        // List<String> prSufixAll = new ArrayList<>();
        String pr = (String) idoc.getFieldValue("pristupnost");
        List<String> prSufix = new ArrayList<>();

        if ("A".compareTo(pr) == 0) {
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

        List<String> prSufixAll = new ArrayList<>();
        prSufixAll.add("A");
        prSufixAll.add("B");
        prSufixAll.add("C");
        prSufixAll.add("D");

        Object[] fields = idoc.getFieldNames().toArray();
        for (Object f : fields) {
            String s = (String) f;
            // SolrSearcher.addSecuredFieldFacets(s, idoc, prSufix);
            if (indexFields.contains(s)) {
                for (String sufix : prSufixAll) {
                    IndexUtils.addFieldNonRepeat(idoc, "text_all_" + sufix, idoc.getFieldValues(s));
                }
            }
        }
        
        // Add value of vocab fields
        for (String sufix : prSufixAll) {
            IndexUtils.addRefField(idoc, "text_all_" + sufix, okres);
            IndexUtils.addRefField(idoc, "text_all_" + sufix, specifikace);
            IndexUtils.addRefField(idoc, "text_all_" + sufix, nalezce);
            IndexUtils.addRefField(idoc, "text_all_" + sufix, obdobi);
            IndexUtils.addRefField(idoc, "text_all_" + sufix, druh_nalezu);
        }
    }

    @Override
    public boolean filterOAI(JSONObject user, SolrDocument doc) {
        long st = (long) doc.getFieldValue("stav");
        String userPr = user.optString("pristupnost", "A");
        String userId = user.optString("ident_cely", "A");
        if (userPr.compareToIgnoreCase("C") > 0) {
            return true;
        } else if (st == 4) {
            return true;
        } else if (userPr.equalsIgnoreCase("C") && 
                (("SN01".equals((String) doc.getFieldValue("historie_typ_zmeny")) && 
                userId.equals((String) doc.getFieldValue("historie_uzivatel"))) || 
                ("SN01".equals((String) doc.getFieldValue("historie_typ_zmeny")) && 
                "SN01".equals((String) doc.getFieldValue("historie_uzivatel"))))) {
            return true;
        } else if (userPr.equalsIgnoreCase("B") && 
                "SN01".equals((String) doc.getFieldValue("historie_typ_zmeny")) && 
                userId.equals((String) doc.getFieldValue("historie_uzivatel"))) {
            // historie[typ_zmeny='SN01']/uzivatel = {user}.ident_cely
            return true;
        } else {
            return false;
        }
    }

}

class SnChraneneUdaje {

//<xs:element name="katastr" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "ruian-{katastr.kod}" | "{katastr.nazev}" -->
    @JacksonXmlProperty(localName = "katastr")
    public Vocab katastr;

//<xs:element name="lokalizace" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{lokalizace}" -->
    @JacksonXmlProperty(localName = "lokalizace")
    public String lokalizace;

//<xs:element name="geom_gml" minOccurs="0" maxOccurs="1" type="amcr:gmlType"/> <!-- ST_AsGML("{geom}") -->
    @JacksonXmlProperty(localName = "geom_gml")
    public Object geom_gml;

//<xs:element name="geom_wkt" minOccurs="0" maxOccurs="1" type="amcr:wktType"/> <!-- ST_SRID("{geom}") | ST_AsText("{geom}") -->
    @JacksonXmlProperty(localName = "geom_wkt")
    public WKT geom_wkt;

//<xs:element name="geom_sjtsk_gml" minOccurs="0" maxOccurs="1" type="amcr:gmlType"/> <!-- ST_AsGML("{geom_sjtsk}") -->
    @JacksonXmlProperty(localName = "geom_sjtsk_gml")
    public Object geom_sjtsk_gml;

//<xs:element name="geom_sjtsk_wkt" minOccurs="0" maxOccurs="1" type="amcr:wktType"/> <!-- ST_SRID("{geom_sjtsk}") | ST_AsText("{geom_sjtsk}") -->
    @JacksonXmlProperty(localName = "geom_sjtsk_wkt")
    public WKT geom_sjtsk_wkt;

    public void fillSolrFields(SolrInputDocument idoc, String pristupnost) {

        IndexUtils.setSecuredJSONField(idoc, this);

        if (katastr != null) {
            IndexUtils.addSecuredFieldNonRepeat(idoc, "f_katastr", katastr.getValue(), pristupnost);
        }

        if (lokalizace != null) {
            IndexUtils.addSecuredFieldNonRepeat(idoc, "f_lokalizace", lokalizace, pristupnost);
        }

        if (geom_wkt != null) {

            String wktStr = geom_wkt.getValue();
            final WKTReader reader = new WKTReader();
            try {
                Geometry geometry = reader.read(wktStr);
                Point p = geometry.getCentroid();
                IndexUtils.addSecuredFieldNonRepeat(idoc, "lng", p.getX(), pristupnost);
                IndexUtils.addSecuredFieldNonRepeat(idoc, "lat", p.getY(), pristupnost);
                IndexUtils.addSecuredFieldNonRepeat(idoc, "loc", p.getY() + "," + p.getX(), pristupnost);
                IndexUtils.addSecuredFieldNonRepeat(idoc, "loc_rpt", p.getY() + "," + p.getX(), pristupnost);
            } catch (Exception e) {
                throw new RuntimeException(String.format("Can't parse string %s as WKT", wktStr));
            }

        }

    }
}
