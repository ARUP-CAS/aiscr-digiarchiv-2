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

//<xs:element name="let" minOccurs="0" maxOccurs="1" type="amcr:refType"/> <!-- "{let.ident_cely}" | "{let.ident_cely}" -->
    @JacksonXmlProperty(localName = "let")
    public Vocab let;

//<xs:element name="stav" minOccurs="1" maxOccurs="1" type="xs:integer"/> <!-- "{stav}" -->
    @Field
    public long stav;

//<xs:element name="typ_dokumentu" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{typ_dokumentu.ident_cely}" | "{typ_dokumentu.heslo}" -->
    @JacksonXmlProperty(localName = "typ_dokumentu")
    public Vocab typ_dokumentu;

//<xs:element name="material_originalu" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{material_originalu.ident_cely}" | "{material_originalu.heslo}" -->
    @JacksonXmlProperty(localName = "material_originalu")
    public Vocab material_originalu;

//<xs:element name="rada" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{rada.ident_cely}" | "{rada.heslo}" -->
    @JacksonXmlProperty(localName = "rada")
    public Vocab rada;

//<xs:element name="posudek" minOccurs="0" maxOccurs="unbounded" type="amcr:vocabType"/> <!-- "{posudky.ident_cely}" | "{posudky.heslo}" -->
    @JacksonXmlProperty(localName = "posudek")
    public List<Vocab> posudek = new ArrayList();

//<xs:element name="autor" minOccurs="0" maxOccurs="unbounded" type="amcr:autorType"/> <!-- "{dokumentautor_set.autor.ident_cely}"  | "{dokumentautor_set.poradi}" | "{dokumentautor_set.autor.vypis_cely}" -->
  @JacksonXmlProperty(localName = "autor")
  public List<Vocab> autor = new ArrayList();
  
//<xs:element name="rok_vzniku" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{rok_vzniku}" -->
    @JacksonXmlProperty(localName = "rok_vzniku")
    @Field
    public int rok_vzniku;

//<xs:element name="organizace" minOccurs="1" maxOccurs="1" type="amcr:vocabType"/> <!-- "{organizace.ident_cely}" | "{organizace.nazev}" -->
    @JacksonXmlProperty(localName = "organizace")
    public Vocab organizace;

//<xs:element name="pristupnost" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{pristupnost.ident_cely}" | "{pristupnost.heslo}" -->
    @JacksonXmlProperty(localName = "pristupnost")
    public Vocab pristupnost;

//<xs:element name="datum_zverejneni" minOccurs="0" maxOccurs="1" type="xs:date"/> <!-- "{datum_zverejneni}" -->
    @JacksonXmlProperty(localName = "datum_zverejneni")
    @Field
    public Date datum_zverejneni;

//<xs:element name="jazyk_dokumentu" minOccurs="0" maxOccurs="unbounded" type="amcr:vocabType"/> <!-- "{jazyky.ident_cely}" | "{jazyky.heslo}" -->
    @JacksonXmlProperty(localName = "jazyk_dokumentu")
    public List<Vocab> jazyk_dokumentu = new ArrayList();

//<xs:element name="ulozeni_originalu" minOccurs="0" maxOccurs="1" type="amcr:vocabType"/> <!-- "{ulozeni_originalu.ident_cely}" | "{ulozeni_originalu.heslo}" -->
    @JacksonXmlProperty(localName = "ulozeni_originalu")
    public Vocab ulozeni_originalu;

//<xs:element name="oznaceni_originalu" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{oznaceni_originalu}" -->
    @JacksonXmlProperty(localName = "oznaceni_originalu")
    @Field
    public String oznaceni_originalu;

//<xs:element name="popis" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{popis}" -->
    @JacksonXmlProperty(localName = "popis")
    @Field
    public String popis;

//<xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
    @JacksonXmlProperty(localName = "poznamka")
    @Field
    public String poznamka;

//<xs:element name="licence" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{licence}" -->
    @JacksonXmlProperty(localName = "licence")
    @Field
    public String licence;

//<xs:element name="osoba" minOccurs="0" maxOccurs="unbounded" type="amcr:refType"/> <!-- "{osoby.ident_cely}" | "{osoby.vypis_cely}" -->
    @JacksonXmlProperty(localName = "osoba")
    public List<Vocab> osoba = new ArrayList();

//<xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/> <!-- "{historie.historie_set}" -->  
    @JacksonXmlProperty(localName = "historie")
    public List<Historie> historie = new ArrayList();

//<xs:element name="soubor" minOccurs="0" maxOccurs="unbounded" type="amcr:souborType"/>  <!-- "{soubory.soubory}" -->  
    @JacksonXmlProperty(localName = "soubor")
    public List<Soubor> soubor = new ArrayList();

//<xs:element name="extra_data" minOccurs="0" maxOccurs="1" type="amcr:extra_dataType"/> <!-- "{extra_data}" -->
    @JacksonXmlProperty(localName = "extra_data")
    public ExtraData extra_data;

//<xs:element name="tvar" minOccurs="0" maxOccurs="unbounded" type="amcr:tvarType"/> <!-- "{tvar_set}" -->
    @JacksonXmlProperty(localName = "tvar")
    public List<Tvar> tvar = new ArrayList();

//<xs:element name="dokument_cast" minOccurs="0" maxOccurs="unbounded" type="amcr:dokument_castType"/> <!-- "{casti}" -->
    @JacksonXmlProperty(localName = "dokument_cast")
    public List<DokumentCast> dokument_cast = new ArrayList();

    @Override
    public String coreName() {
        return "entities";
    }

    @Override
    public void fillSolrFields(SolrInputDocument idoc) {
        idoc.setField("pristupnost", SearchUtils.getPristupnostMap().get(pristupnost.getId()));
        boolean searchable = stav == 3;
        idoc.setField("searchable", searchable);
        IndexUtils.setDateStamp(idoc, historie);
        
        entity = "3D".equals(rada.getValue()) ? "knihovna_3d" : "dokument";
        idoc.setField("entity", entity);

        IndexUtils.addVocabField(idoc, "typ_dokumentu", typ_dokumentu);
        IndexUtils.addVocabField(idoc, "material_originalu", material_originalu);
        IndexUtils.addVocabField(idoc, "rada", rada);

        for (Vocab v: posudek) {
            IndexUtils.addVocabField(idoc, "posudek", v);
        }

        for (Vocab v: autor) {
            IndexUtils.addRefField(idoc, "autor", v);
        }
        IndexUtils.addVocabField(idoc, "organizace", organizace);

        for (Vocab v: jazyk_dokumentu) {
            IndexUtils.addVocabField(idoc, "jazyk_dokumentu", v);
        }
        IndexUtils.addVocabField(idoc, "ulozeni_originalu", ulozeni_originalu);
        

        for (Vocab v: osoba) {
            IndexUtils.addRefField(idoc, "osoba", v);
        }
        
        
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
                IndexUtils.getClient().add("soubor", idocs, 10);
            }
        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(Dokument.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (Tvar tv : tvar) {
            IndexUtils.addJSONField(idoc, "tvar", tv);
            idoc.addField("tvar_poznamka", tv.poznamka);
            IndexUtils.addVocabField(idoc, "tvar_tvar", tv.tvar);
        }

        if (extra_data != null) {
            extra_data.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
        }

        for (DokumentCast dc: dokument_cast) {
            // IndexUtils.addJSONField(idoc, "dokument_cast", dc);
            dc.fillSolrFields(idoc, (String) idoc.getFieldValue("pristupnost"));
        }
        

        if (let != null) {
            addLet(idoc);
        }

        setFullText(idoc);
    }
    
    private void addLet(SolrInputDocument idoc) {
        
        IndexUtils.addVocabField(idoc, "let", let); 
        SolrQuery query = new SolrQuery("ident_cely:\"" + let.getId() + "\"")
            .setFields("ident_cely",
                        "datum",
                        "pozorovatel",
                        "organizace",
                        "fotoaparat",
                        "pilot",
                        "typ_letounu",
                        "ucel_letu",
                        "letiste_start", 
                        "letiste_cil", 
                        "pocasi", 
                        "dohlednost", 
                        "uzivatelske_oznaceni");
        JSONObject json = SearchUtils.json(query, IndexUtils.getClient(), "entities");
        if (json.getJSONObject("response").getInt("numFound") > 0) {
            for (int d = 0; d < json.getJSONObject("response").getJSONArray("docs").length(); d++) {
                JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(d);
                for (String key : doc.keySet()) {
                    SolrSearcher.addFieldNonRepeat(idoc, "let_" + key, doc.opt(key));
                }
            }
        }
    }

    public void setFullText(SolrInputDocument idoc) {
        Object[] fields = idoc.getFieldNames().toArray();
        List<Object> indexFields = Options.getInstance().getJSONObject("indexFieldsByType").getJSONArray("dokument").toList();
        List<String> prSufix = new ArrayList<>();
        prSufix.add("A");
        prSufix.add("B");
        prSufix.add("C");
        prSufix.add("D");

        for (Object f : fields) {
            String s = (String) f;

            // SolrSearcher.addSecuredFieldFacets(s, idoc, prSufix);
            if (indexFields.contains(s)) {
                for (String sufix : prSufix) {
                    SolrSearcher.addFieldNonRepeat(idoc, "text_all_" + sufix, idoc.getFieldValues(s));
                }
            }

        }
                for (String sufix : prSufix) {
                    IndexUtils.addRefField(idoc, "text_all_" + sufix, typ_dokumentu);
                    IndexUtils.addRefField(idoc, "text_all_" + sufix, material_originalu);
                    IndexUtils.addRefField(idoc, "text_all_" + sufix, rada);
                    IndexUtils.addRefField(idoc, "text_all_" + sufix, organizace);
                    for (Vocab v: osoba) {
                        IndexUtils.addRefField(idoc, "text_all_" + sufix, v);
                    }
                    for (Vocab v: jazyk_dokumentu) {
                        IndexUtils.addRefField(idoc, "text_all_" + sufix, v);
                    }
                    
                    IndexUtils.addRefField(idoc, "text_all_" + sufix, ulozeni_originalu);
                    for (Vocab v: posudek) {
                        IndexUtils.addRefField(idoc, "text_all_" + sufix, v);
                    }
                    for (Soubor v: soubor) {
                        IndexUtils.addFieldNonRepeat(idoc, "text_all_" + sufix, v.nazev);
                    }
                }
    }

    @Override
    public String filterOAI(JSONObject user, SolrDocument doc) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
