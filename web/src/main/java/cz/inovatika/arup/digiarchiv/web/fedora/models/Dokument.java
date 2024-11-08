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
import java.util.Arrays;
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

/**
 *
 * @author alberto
 */
@JacksonXmlRootElement(localName = "dokument")
public class Dokument implements FedoraModel {

    @Field
    public String entity = "dokument";

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
    @JacksonXmlProperty(localName = "ident_cely")
    @Field
    public String ident_cely;

//<xs:element name="stav" minOccurs="1" maxOccurs="1" type="xs:integer"/> <!-- "{stav}" -->
    @Field
    public long stav;

//<xs:element name="pristupnost" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{pristupnost.ident_cely}" | "{pristupnost.heslo}" -->
    @JacksonXmlProperty(localName = "pristupnost")
    public Vocab pristupnost;

//<xs:element name="let" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{let.ident_cely}" | "{let.ident_cely}" -->
    @JacksonXmlProperty(localName = "let")
    public Vocab dokument_let;

//<xs:element name="typ_dokumentu" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ_dokumentu.ident_cely}" | "{typ_dokumentu.heslo}" -->
    @JacksonXmlProperty(localName = "typ_dokumentu")
    public Vocab dokument_typ_dokumentu;

//<xs:element name="material_originalu" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{material_originalu.ident_cely}" | "{material_originalu.heslo}" -->
    @JacksonXmlProperty(localName = "material_originalu")
    public Vocab dokument_material_originalu;

//<xs:element name="rada" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{rada.ident_cely}" | "{rada.heslo}" -->
    @JacksonXmlProperty(localName = "rada")
    public Vocab dokument_rada;

//<xs:element name="posudek" minOccurs="0" maxOccurs="unbounded" type="amcr:vocabType"/> <!-- "{posudky.ident_cely}" | "{posudky.heslo}" -->
    @JacksonXmlProperty(localName = "posudek")
    public List<Vocab> dokument_posudek = new ArrayList();

//<xs:element name="autor" minOccurs="0" maxOccurs="unbounded" type="amcr:autorType"/> <!-- "{dokumentautor_set.autor.ident_cely}"  | "{dokumentautor_set.poradi}" | "{dokumentautor_set.autor.vypis_cely}" -->
    @JacksonXmlProperty(localName = "autor")
    public List<Vocab> dokument_autor = new ArrayList();

//<xs:element name="rok_vzniku" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{rok_vzniku}" -->
    @JacksonXmlProperty(localName = "rok_vzniku")
    @Field
    public int dokument_rok_vzniku;

//<xs:element name="organizace" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{organizace.ident_cely}" | "{organizace.nazev}" -->
    @JacksonXmlProperty(localName = "organizace")
    public Vocab dokument_organizace;

//<xs:element name="datum_zverejneni" minOccurs="0" maxOccurs="1" type="xs:date"/> <!-- "{datum_zverejneni}" -->
    @JacksonXmlProperty(localName = "datum_zverejneni")
    @Field
    public Date dokument_datum_zverejneni;

//<xs:element name="jazyk_dokumentu" minOccurs="0" maxOccurs="unbounded" type="amcr:vocabType"/> <!-- "{jazyky.ident_cely}" | "{jazyky.heslo}" -->
    @JacksonXmlProperty(localName = "jazyk_dokumentu")
    public List<Vocab> dokument_jazyk_dokumentu = new ArrayList();

//<xs:element name="ulozeni_originalu" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{ulozeni_originalu.ident_cely}" | "{ulozeni_originalu.heslo}" -->
    @JacksonXmlProperty(localName = "ulozeni_originalu")
    public Vocab dokument_ulozeni_originalu;

//<xs:element name="oznaceni_originalu" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{oznaceni_originalu}" -->
    @JacksonXmlProperty(localName = "oznaceni_originalu")
    @Field
    public String dokument_oznaceni_originalu;

//<xs:element name="popis" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{popis}" -->
    @JacksonXmlProperty(localName = "popis")
    @Field
    public String dokument_popis;

//<xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
    @JacksonXmlProperty(localName = "poznamka")
    @Field
    public String dokument_poznamka;

//<xs:element name="licence" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{licence.ident_cely}" | "{licence.heslo}" -->
    @JacksonXmlProperty(localName = "licence")
    public Vocab dokument_licence;

//<xs:element name="osoba" minOccurs="0" maxOccurs="unbounded" type="amcr:refType"/> <!-- "{osoby.ident_cely}" | "{osoby.vypis_cely}" -->
    @JacksonXmlProperty(localName = "osoba")
    public List<Vocab> dokument_osoba = new ArrayList();

//<xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/> <!-- "{historie.historie_set}" -->  
    @JacksonXmlProperty(localName = "historie")
    public List<Historie> historie = new ArrayList();

//<xs:element name="soubor" minOccurs="0" maxOccurs="unbounded" type="amcr:souborType"/>  <!-- "{soubory.soubory}" -->  
    @JacksonXmlProperty(localName = "soubor")
    public List<Soubor> soubor = new ArrayList();

//<xs:element name="extra_data" minOccurs="0" maxOccurs="1" type="amcr:extra_dataType"/> <!-- "{extra_data}" -->
    @JacksonXmlProperty(localName = "extra_data")
    public ExtraData dokument_extra_data;

//<xs:element name="tvar" minOccurs="0" maxOccurs="unbounded" type="amcr:tvarType"/> <!-- "{tvar_set}" -->
    @JacksonXmlProperty(localName = "tvar")
    public List<Tvar> dokument_tvar = new ArrayList();

//<xs:element name="dokument_cast" minOccurs="0" maxOccurs="unbounded" type="amcr:dokument_castType"/> <!-- "{casti}" -->
    @JacksonXmlProperty(localName = "dokument_cast")
    public List<DokumentCast> dokument_cast = new ArrayList();

    @Override
    public String coreName() {
        return "entities";
    }
    
    @Override
    public boolean isSearchable(){
        return stav == 3;
    }

    @Override
    public void fillSolrFields(SolrInputDocument idoc) throws Exception {
        idoc.setField("pristupnost", SearchUtils.getPristupnostMap().get(pristupnost.getId()));
        boolean searchable = stav == 3;
        idoc.setField("searchable", searchable);
        IndexUtils.setDateStamp(idoc, ident_cely);
        IndexUtils.setDateStampFromHistory(idoc, historie);
        for (Historie h: historie) {
            IndexUtils.addJSONField(idoc, "historie", h);
        }
        

        entity = (dokument_rada.getId().toUpperCase().equals("HES-000870")) ? "knihovna_3d" : "dokument";
        idoc.setField("entity", entity);

        IndexUtils.addVocabField(idoc, "dokument_typ_dokumentu", dokument_typ_dokumentu);
        String kategorie = Options.getInstance().getJSONObject("kategoriet").optString(dokument_typ_dokumentu.getId(), dokument_typ_dokumentu.getId());
        SolrSearcher.addFieldNonRepeat(idoc, "dokument_kategorie_dokumentu", kategorie);

        IndexUtils.addVocabField(idoc, "dokument_material_originalu", dokument_material_originalu);
        IndexUtils.addVocabField(idoc, "dokument_rada", dokument_rada);

        for (Vocab v : dokument_posudek) {
            IndexUtils.addVocabField(idoc, "dokument_posudek", v);
        }

        for (Vocab v : dokument_autor) {
            IndexUtils.addRefField(idoc, "dokument_autor", v); 
        }
        
        if (!dokument_autor.isEmpty()) {
            IndexUtils.addRefField(idoc, "autor_sort", dokument_autor.get(0));
        }
        
        
        IndexUtils.addVocabField(idoc, "dokument_organizace", dokument_organizace);
        IndexUtils.addRefField(idoc, "organizace_sort", dokument_organizace);

        for (Vocab v : dokument_jazyk_dokumentu) {
            IndexUtils.addVocabField(idoc, "dokument_jazyk_dokumentu", v);
        }
        IndexUtils.addVocabField(idoc, "dokument_ulozeni_originalu", dokument_ulozeni_originalu);
        IndexUtils.addVocabField(idoc, "dokument_licence", dokument_licence);
        
        for (Vocab v : dokument_osoba) {
            IndexUtils.addRefField(idoc, "dokument_osoba", v);
        }

//        List<SolrInputDocument> idocs = new ArrayList<>();
        try {
            for (Soubor s : soubor) {
//                SolrInputDocument djdoc = s.createSolrDoc();
//                idocs.add(djdoc);
                IndexUtils.addJSONField(idoc, "soubor", s);
                idoc.addField("soubor_id", s.id);
                idoc.addField("soubor_nazev", s.nazev);
                idoc.addField("soubor_filepath", s.path);
                idoc.addField("soubor_rozsah", s.rozsah);
                idoc.addField("soubor_size_mbytes", s.size_mb);
    
            }
//            if (!idocs.isEmpty()) {
//                IndexUtils.getClientBin().add("soubor", idocs, 10);
//            }
        } catch (Exception ex) {
            Logger.getLogger(Dokument.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (Tvar tv : dokument_tvar) {
            IndexUtils.addJSONField(idoc, "dokument_tvar", tv);
            idoc.addField("tvar_poznamka", tv.poznamka);
            //IndexUtils.addVocabField(idoc, "tvar_tvar", tv.tvar);
        }

        if (dokument_extra_data != null) {
            dokument_extra_data.fillSolrFields(idoc, "dukument", (String) idoc.getFieldValue("pristupnost"));
            IndexUtils.addJSONField(idoc, "dokument_extra_data", dokument_extra_data);
        }

        for (DokumentCast dc : dokument_cast) {
            dc.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
            // IndexUtils.addJSONField(idoc, "dokument_cast", dc);
            if (idoc.containsKey("dokument_cast_akce")) {
                for (Object val : idoc.getFieldValues("dokument_cast_akce")) {
                    processAkce(idoc, (String) val);
                }
            }
        }

        if (dokument_let != null) {
            addLet(idoc);
        }
        setFacets(idoc);
        setFullText(idoc);
    }

    private void processAkce(SolrInputDocument idoc, String id) throws Exception {

        SolrQuery query = new SolrQuery("ident_cely:\"" + id + "\"")
                //.addFilterQuery("searchable:true")
                .setFields("searchable,az_chranene_udaje,az_okres,pristupnost,f_typ_vyzkumu");
        JSONObject json = SearchUtils.searchOrIndex(query, "entities", id);
        if (json.getJSONObject("response").getInt("numFound") > 0) {
            JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
            
            if (doc.optBoolean("searchable")) {
                if (doc.has("f_typ_vyzkumu")) {
                    
                    Object val = doc.get("f_typ_vyzkumu");
                    if (val instanceof JSONArray) {
                        JSONArray ja = (JSONArray) val;
                        for (int j = 0; j < ja.length(); j++) {
                             SolrSearcher.addFieldNonRepeat(idoc, "f_typ_vyzkumu", ja.get(j));
                        }
                    } else {
                        SolrSearcher.addFieldNonRepeat(idoc, "f_typ_vyzkumu", val);
                    }
                        
                    // SolrSearcher.addFieldNonRepeat(idoc, "f_typ_vyzkumu", doc.get("f_typ_vyzkumu"));
                }
//                String pr = doc.getString("pristupnost");
//                SolrSearcher.addFieldNonRepeat(idoc, "az_okres", doc.get("az_okres"));
//                SolrSearcher.addSecuredFieldNonRepeat(idoc, "dokument_az_chranene_udaje", doc.get("az_chranene_udaje"), pr);
            }
        }
    }

    private void addLet(SolrInputDocument idoc) throws Exception {

        IndexUtils.addVocabField(idoc, "dokument_let_ident_cely", dokument_let);
        SolrQuery query = new SolrQuery("ident_cely:\"" + dokument_let.getId() + "\"");
        JSONObject json = SearchUtils.searchOrIndex(query, "entities", dokument_let.getId());
        if (json.getJSONObject("response").getInt("numFound") > 0) {
            for (int d = 0; d < json.getJSONObject("response").getJSONArray("docs").length(); d++) {
                JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(d);
                SolrSearcher.addFieldNonRepeat(idoc, "dokument_let", doc.toString());
                for (String key : doc.keySet()) {
                    if (key.startsWith("let_")) {
                        SolrSearcher.addFieldNonRepeat(idoc, key, doc.opt(key));
                    }
                    
                }
            }
        }
    }
    
    public void setFacets(SolrInputDocument idoc) {
        List<Object> indexFields = Options.getInstance().getJSONObject("fields").getJSONObject("dokument").getJSONArray("facets").toList();
        // List<String> prSufixAll = new ArrayList<>();
        
        for (Object f : indexFields) {
            String s = (String) f;
            String dest = s.split(":")[0];
            String orig = s.split(":")[1];
            IndexUtils.addByPath(idoc, orig, dest, Arrays.asList(SolrSearcher.prSufixAll));
        }
        
    }

    public void setFullText(SolrInputDocument idoc) {
        List<Object> indexFields = Options.getInstance().getJSONObject("fields").getJSONObject("dokument").getJSONArray("full_text").toList();

        for (Object f : indexFields) {
            String s = (String) f;
            for (String sufix : SolrSearcher.prSufixAll) {
                if (idoc.containsKey(s)) {
                    IndexUtils.addFieldNonRepeat(idoc, "text_all_" + sufix, idoc.getFieldValues(s));
                }
                if (idoc.containsKey(s + "_" + sufix)) {
                    IndexUtils.addFieldNonRepeat(idoc, "text_all_" + sufix, idoc.getFieldValues(s + "_" + sufix));
                }
            }
        } 
        for (String sufix : SolrSearcher.prSufixAll) {
            IndexUtils.addRefField(idoc, "text_all_" + sufix, dokument_typ_dokumentu);
            IndexUtils.addRefField(idoc, "text_all_" + sufix, dokument_material_originalu);
            IndexUtils.addRefField(idoc, "text_all_" + sufix, dokument_rada);
            IndexUtils.addRefField(idoc, "text_all_" + sufix, dokument_organizace);
            for (Vocab v : dokument_osoba) {
                IndexUtils.addRefField(idoc, "text_all_" + sufix, v);
            }
            for (Vocab v : dokument_jazyk_dokumentu) {
                IndexUtils.addRefField(idoc, "text_all_" + sufix, v);
            }

            IndexUtils.addRefField(idoc, "text_all_" + sufix, dokument_ulozeni_originalu);
            for (Vocab v : dokument_posudek) {
                IndexUtils.addRefField(idoc, "text_all_" + sufix, v);
            }
            for (Soubor v : soubor) {
                IndexUtils.addFieldNonRepeat(idoc, "text_all_" + sufix, v.nazev);
            }
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
