package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;

/**
 *
 * @author alberto
 */
@JacksonXmlRootElement(localName = "projekt")
public class Projekt implements FedoraModel {


    public String fieldPrefix = "projekt_";
    
    @Field
    public String entity = "projekt";

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
    @JacksonXmlProperty(localName = "ident_cely")
    @Field
    public String ident_cely;

//<xs:element name="stav" minOccurs="1" maxOccurs="1" type="xs:integer"/> <!-- "{stav}" -->
    @JacksonXmlProperty(localName = "stav")
    @Field
    public long stav;

//<xs:element name="typ_projektu" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ_projektu.ident_cely}" | "{typ_projektu.heslo}" -->
    @JacksonXmlProperty(localName = "typ_projektu")
    public Vocab projekt_typ_projektu;

//<xs:element name="okres" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "ruian-{hlavni_katastr.okres.kod}" | "{hlavni_katastr.okres.nazev}" -->
    @JacksonXmlProperty(localName = "okres")
    public Vocab projekt_okres;

//<xs:element name="podnet" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{podnet}" -->
    @JacksonXmlProperty(localName = "podnet")
    @Field
    public String projekt_podnet;

//<xs:element name="planovane_zahajeni" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{planovane_zahajeni_str}" -->
    @JacksonXmlProperty(localName = "planovane_zahajeni")
    @Field
    public String projekt_planovane_zahajeni;

//<xs:element name="vedouci_projektu" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{vedouci_projektu.ident_cely}" | "{vedouci_projektu.vypis_cely}" -->
    @JacksonXmlProperty(localName = "vedouci_projektu")
    public Vocab projekt_vedouci_projektu;

//<xs:element name="organizace" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{organizace.ident_cely}" | "{organizace.nazev}" -->
    @JacksonXmlProperty(localName = "organizace")
    public Vocab projekt_organizace;

//<xs:element name="uzivatelske_oznaceni" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{uzivatelske_oznaceni}" -->
    @JacksonXmlProperty(localName = "uzivatelske_oznaceni")
    @Field
    public String projekt_uzivatelske_oznaceni;

//<xs:element name="oznaceni_stavby" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{oznaceni_stavby}" -->
    @JacksonXmlProperty(localName = "oznaceni_stavby")
    @Field
    public String projekt_oznaceni_stavby;

//<xs:element name="kulturni_pamatka" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{kulturni_pamatka.ident_cely}" | "{kulturni_pamatka.heslo}" -->
    @JacksonXmlProperty(localName = "kulturni_pamatka")
    public Vocab projekt_kulturni_pamatka;

//<xs:element name="datum_zahajeni" minOccurs="0" maxOccurs="1" type="xs:date"/> <!-- "{datum_zahajeni}" -->
    @JacksonXmlProperty(localName = "datum_zahajeni")
    @Field
    public Date projekt_datum_zahajeni;

//<xs:element name="datum_ukonceni" minOccurs="0" maxOccurs="1" type="xs:date"/> <!-- "{datum_ukonceni}" -->
    @JacksonXmlProperty(localName = "datum_ukonceni")
    @Field
    public Date projekt_datum_ukonceni;

//<xs:element name="termin_odevzdani_nz" minOccurs="0" maxOccurs="1" type="xs:date"/> <!-- "{termin_odevzdani_nz}" -->
    @JacksonXmlProperty(localName = "termin_odevzdani_nz")
    @Field
    public Date projekt_termin_odevzdani_nz;

//<xs:element name="pristupnost_pom" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{pristupnost.ident_cely}" | "{pristupnost.heslo}" -->
    @JacksonXmlProperty(localName = "pristupnost_pom")
    public Vocab pristupnost;

//<xs:element name="chranene_udaje" minOccurs="0" maxOccurs="1" type="amcr:projekt-chranene_udajeType"/> <!-- self -->
    @JacksonXmlProperty(localName = "chranene_udaje")
    private ProjektChraneneUdaje projekt_chranene_udaje;

//<xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/> <!-- "{historie.historie_set}" -->
    @JacksonXmlProperty(localName = "historie")
    public List<Historie> historie = new ArrayList();

//<xs:element name="oznamovatel" minOccurs="0" maxOccurs="1" type="amcr:oznamovatelType"/> <!-- "{oznamovatel}" -->
    @JacksonXmlProperty(localName = "oznamovatel")
    public Oznamovatel projekt_oznamovatel;

//<xs:element name="soubor" minOccurs="0" maxOccurs="unbounded" type="amcr:souborType"/>  <!-- {soubory.soubory} -->
    @JacksonXmlProperty(localName = "soubor")
    public List<Soubor> soubor = new ArrayList();

//<xs:element name="archeologicky_zaznam" minOccurs="0" maxOccurs="unbounded" type="amcr:refType"/> <!-- "{akce_set.archeologicky_zaznam.ident_cely}" | "{akce_set.archeologicky_zaznam.ident_cely}" -->
    @JacksonXmlProperty(localName = "archeologicky_zaznam")
    public List<Vocab> projekt_archeologicky_zaznam = new ArrayList();

//<xs:element name="samostatny_nalez" minOccurs="0" maxOccurs="unbounded" type="amcr:refType"/> <!-- "{samostatne_nalezy.ident_cely}" | "{samostatne_nalezy.ident_cely}" -->
    @JacksonXmlProperty(localName = "samostatny_nalez")
    public List<Vocab> projekt_samostatny_nalez = new ArrayList();

//<xs:element name="dokument" minOccurs="0" maxOccurs="unbounded" type="amcr:refType"/> <!-- "{casti_dokumentu.dokument.ident_cely}" | "{casti_dokumentu.dokument.ident_cely}" -->
    @JacksonXmlProperty(localName = "dokument")
    public List<Vocab> projekt_dokument = new ArrayList();
    
    @Override
    public boolean isSearchable(){
        return !projekt_archeologicky_zaznam.isEmpty() || !projekt_samostatny_nalez.isEmpty();
    }

    @Override
    public void fillSolrFields(SolrInputDocument idoc) throws Exception {
        idoc.setField("pristupnost", SearchUtils.getPristupnostMap().get(pristupnost.getId()));
        boolean searchable = !projekt_archeologicky_zaznam.isEmpty() || !projekt_samostatny_nalez.isEmpty();
        idoc.setField("searchable", searchable);
        IndexUtils.setDateStamp(idoc, ident_cely);
        IndexUtils.setDateStampFromHistory(idoc, historie);

        IndexUtils.addJSONField(idoc, "projekt_oznamovatel", projekt_oznamovatel);
        
        IndexUtils.addRefField(idoc, "projekt_okres", projekt_okres);
        IndexUtils.addVocabField(idoc, "projekt_typ_projektu", projekt_typ_projektu);
        IndexUtils.addRefField(idoc, "projekt_vedouci_projektu", projekt_vedouci_projektu);
        IndexUtils.addVocabField(idoc, "projekt_organizace", projekt_organizace);
        IndexUtils.addVocabField(idoc, "projekt_kulturni_pamatka", projekt_kulturni_pamatka);

        if (projekt_datum_zahajeni != null) {
            IndexUtils.addFieldNonRepeat(idoc, "projekt_datum_zahajeni_od", projekt_datum_zahajeni);
            IndexUtils.addFieldNonRepeat(idoc, "projekt_datum_zahajeni_do", projekt_datum_zahajeni);
            String ukonceni = "*";
            if (projekt_datum_ukonceni != null) {
                IndexUtils.addFieldNonRepeat(idoc, "projekt_datum_provedeni_do", projekt_datum_ukonceni);
                IndexUtils.addFieldNonRepeat(idoc, "projekt_datum_ukonceni", projekt_datum_ukonceni);
                if (!projekt_datum_ukonceni.before(projekt_datum_zahajeni)) {
                    ukonceni = projekt_datum_ukonceni.toInstant().toString();
                }
            }
            IndexUtils.addFieldNonRepeat(idoc, "projekt_datum_provedeni", "[" + projekt_datum_zahajeni.toInstant().toString() + " TO " + ukonceni + "]");
        }

        for (Vocab v : projekt_archeologicky_zaznam) {
            idoc.addField("projekt_archeologicky_zaznam", v.getValue());
            addArch(idoc, v.getValue());
        }

        for (Vocab v : projekt_samostatny_nalez) {
            idoc.addField("projekt_samostatny_nalez", v.getValue());
        }

        for (Vocab v : projekt_dokument) {
            idoc.setField("projekt_dokument", v.getValue());
        }

        List<SolrInputDocument> idocs = new ArrayList<>();
        try {
            for (Soubor s : soubor) {
                SolrInputDocument djdoc = s.createSolrDoc();
                idocs.add(djdoc);
                IndexUtils.addJSONField(idoc, "soubor", s);
            }
            if (!idocs.isEmpty()) {
                IndexUtils.getClientBin().add("soubor", idocs, 10);
            }
        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(Projekt.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (projekt_chranene_udaje != null) {
            projekt_chranene_udaje.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
        }
        
        setSortFields(idoc);
        setFacets(idoc, SolrSearcher.getSufixesByLevel((String) idoc.getFieldValue("pristupnost")));
        setFullText(idoc);
    }
    
    private void setSortFields(SolrInputDocument idoc) {
        
        IndexUtils.addRefField(idoc, "autor_sort", projekt_vedouci_projektu);
        IndexUtils.addRefField(idoc, "organizace_sort", projekt_organizace);
        IndexUtils.addRefField(idoc, "okres_sort", projekt_okres);
        if (projekt_chranene_udaje != null && projekt_chranene_udaje.hlavni_katastr != null)
        IndexUtils.addRefField(idoc, "katastr_sort", projekt_chranene_udaje.hlavni_katastr);
        
    }
    
    public void setFacets(SolrInputDocument idoc, List<String> prSufix) {
        List<Object> indexFields = Options.getInstance().getJSONObject("fields").getJSONObject("projekt").getJSONArray("facets").toList();
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

    public void setFullText(SolrInputDocument idoc) {
        List<Object> indexFields = Options.getInstance().getJSONObject("fields").getJSONObject("projekt").getJSONArray("full_text").toList();

        Object[] fields = idoc.getFieldNames().toArray();
        for (Object f : fields) {
            String s = (String) f;
            // SolrSearcher.addSecuredFieldFacets(s, idoc, prSufix);

            if (indexFields.contains(s)) {
                for (String sufix : SolrSearcher.prSufixAll) {
                    IndexUtils.addFieldNonRepeat(idoc, "text_all_" + sufix, idoc.getFieldValues(s));
                }
            }
        }
        
        // Add value of vocab fields
        for (String sufix : SolrSearcher.prSufixAll) {
            idoc.addField("text_all_" + sufix, ident_cely);
            IndexUtils.addRefField(idoc, "text_all_" + sufix, projekt_kulturni_pamatka);
            IndexUtils.addRefField(idoc, "text_all_" + sufix, projekt_okres);
            IndexUtils.addRefField(idoc, "text_all_" + sufix, projekt_organizace);
            IndexUtils.addRefField(idoc, "text_all_" + sufix, projekt_typ_projektu);
            IndexUtils.addRefField(idoc, "text_all_" + sufix, projekt_vedouci_projektu);
        }
    }

    private void addArch(SolrInputDocument idoc, String az) throws Exception {
        
        String[] facetFields = new String[]{"f_areal", 
            "f_obdobi", 
            "f_aktivita", 
            "f_typ_nalezu", 
            "f_druh_nalezu", 
            //"dokumentacni_jednotka_komponenta_nalez_objekt_druh", 
            //"dokumentacni_jednotka_komponenta_nalez_predmet_druh", 
            "f_specifikace", 
            "f_dj_typ", 
            "f_typ_vyzkumu"};
        SolrQuery query = new SolrQuery("ident_cely:\"" + az + "\"").
                setFields("pian_id,pristupnost");
        for (String f : facetFields) {
            query.addField(f);
        }
        JSONObject json = SearchUtils.searchOrIndex(query, "entities", az);

        if (json.getJSONObject("response").getInt("numFound") > 0) {

            for (int d = 0; d < json.getJSONObject("response").getJSONArray("docs").length(); d++) {
                JSONObject azDoc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(d);
                String pristupnost = azDoc.getString("pristupnost");
                if (azDoc.has("pian_id")) {
                    JSONArray pians = azDoc.getJSONArray("pian_id");
                    for (int j = 0; j < pians.length(); j++) {
                        // idoc.addField("pian_id", pians.optString(j));
                        addPian(idoc, pristupnost, pians.optString(j));
                    }
                }

                for (String f : facetFields) {
                    if (azDoc.has(f)) {
                        SolrSearcher.addFieldNonRepeat(idoc, f, azDoc.get(f));
                    }
                }

            }
        }
    }

    private void addPian(SolrInputDocument idoc, String pristupnost, String pian_id) throws Exception {
        SolrQuery query = new SolrQuery("ident_cely:\"" + pian_id + "\"")
                .setFields("pian_typ,pian_presnost,pian_chranene_udaje:[json]");
        JSONObject json = SearchUtils.searchOrIndex(query, "entities", pian_id);

        if (json.getJSONObject("response").getInt("numFound") > 0) {
            JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
            SolrSearcher.addFieldNonRepeat(idoc, "pian_id", pian_id);
            IndexUtils.addFieldNonRepeat(idoc, "f_pian_typ", doc.getString("pian_typ"));
            IndexUtils.addFieldNonRepeat(idoc, "f_pian_presnost", doc.getString("pian_presnost"));
            IndexUtils.addSecuredFieldNonRepeat(idoc, "f_pian_zm10", doc.getJSONObject("pian_chranene_udaje").getString("zm10"), pristupnost);
        }

    }

    @Override
    public String coreName() {
        return "entities";
    }

    @Override
    public boolean filterOAI(JSONObject user, SolrDocument doc) {
//-- A-B: stav = 6
//-- C: stav >= 1
//-- D-E: bez omezení 
        long st = (long) doc.getFieldValue("stav");
        String userPr = user.optString("pristupnost", "A");
        if (userPr.compareToIgnoreCase("D") >= 0) {
            return true;
        } else if (userPr.equalsIgnoreCase("C") && st >= 1) {
            return true;
        } else if (userPr.compareToIgnoreCase("B") <= 0 && st == 6) {
            return true;
        } else {
            return false;
        }
    }
}

class ProjektChraneneUdaje {
    
    public static final Logger LOGGER = Logger.getLogger(ProjektChraneneUdaje.class.getName());

//<xs:element name="hlavni_katastr" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "ruian-{hlavni_katastr.kod}" | "{hlavni_katastr.nazev}" -->
    @JacksonXmlProperty(localName = "hlavni_katastr")
    public Vocab hlavni_katastr;

//<xs:element name="dalsi_katastr" minOccurs="0" maxOccurs="unbounded" type="amcr:vocabType"/> <!-- "ruian-{katastry.kod}" | "{katastry.nazev}" -->
    @JacksonXmlProperty(localName = "dalsi_katastr")
    public List<Vocab> dalsi_katastr = new ArrayList<>();

//<xs:element name="lokalizace" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{lokalizace}" -->
    @JacksonXmlProperty(localName = "lokalizace")
    public String lokalizace;

//<xs:element name="parcelni_cislo" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{parcelni_cislo}" -->
    @JacksonXmlProperty(localName = "parcelni_cislo")
    public String parcelni_cislo;

//<xs:element name="geom_gml" minOccurs="0" maxOccurs="1" type="amcr:gmlType"/> <!-- ST_AsGML("{geom}") -->
//<xs:element name="geom_wkt" minOccurs="0" maxOccurs="1" type="amcr:wktType"/> <!-- ST_SRID("{geom}") | ST_AsText("{geom}") -->
    @JacksonXmlProperty(localName = "geom_wkt")
    public WKT geom_wkt;

//<xs:element name="kulturni_pamatka_cislo" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{kulturni_pamatka_cislo}" -->
    @JacksonXmlProperty(localName = "kulturni_pamatka_cislo")
    public String kulturni_pamatka_cislo;

//<xs:element name="kulturni_pamatka_popis" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{kulturni_pamatka_popis}" -->
    @JacksonXmlProperty(localName = "kulturni_pamatka_popis")
    public String kulturni_pamatka_popis;

    public void fillSolrFields(SolrInputDocument idoc, String pristupnost) {
        IndexUtils.setSecuredJSONField(idoc, "projekt_chranene_udaje", this);

        IndexUtils.addSecuredFieldNonRepeat(idoc, "projekt_hlavni_katastr", hlavni_katastr.getValue(), pristupnost);
        IndexUtils.addSecuredFieldNonRepeat(idoc, "f_katastr", hlavni_katastr.getValue(), pristupnost);

        for (Vocab v : dalsi_katastr) {
            IndexUtils.addSecuredFieldNonRepeat(idoc, "projekt_dalsi_katastr", v.getValue(), pristupnost);
            IndexUtils.addSecuredFieldNonRepeat(idoc, "f_katastr", v.getValue(), pristupnost);
        }

        if (geom_wkt != null) {

            String wktStr = geom_wkt.getValue();
            if (!wktStr.isBlank()) {
                final WKTReader reader = new WKTReader();
                try {
                    Geometry geometry = reader.read(wktStr);
                    Point p = geometry.getCentroid();
                    IndexUtils.addSecuredFieldNonRepeat(idoc, "lng", p.getX(), pristupnost);
                    IndexUtils.addSecuredFieldNonRepeat(idoc, "lat", p.getY(), pristupnost);
                    IndexUtils.addSecuredFieldNonRepeat(idoc, "loc", p.getY() + "," + p.getX(), pristupnost);
                    IndexUtils.addSecuredFieldNonRepeat(idoc, "loc_rpt", p.getY() + "," + p.getX(), pristupnost);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Can't parse string {0} as WKT", wktStr);
                    // throw new RuntimeException(String.format("Can't parse string %s as WKT", wktStr));
                    
                }
                
            }

        }
    }
}
