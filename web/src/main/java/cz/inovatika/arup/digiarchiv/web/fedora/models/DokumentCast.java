package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web.index.SolrSearcher;
import java.util.ArrayList;
import java.util.List;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class DokumentCast {

//<xs:element name="ident_cely" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{ident_cely}" -->
    @JacksonXmlProperty(localName = "ident_cely")
    @Field
    public String ident_cely;

//<xs:element name="archeologicky_zaznam" type="amcr:refType" minOccurs="0" maxOccurs="1" /> <!-- "{archeologicky_zaznam.ident_cely}" | "{archeologicky_zaznam.ident_cely}" -->
    @JacksonXmlProperty(localName = "archeologicky_zaznam")
    public Vocab archeologicky_zaznam;

//<xs:element name="projekt" type="amcr:refType" minOccurs="0" maxOccurs="1" /> <!-- "{projekt.ident_cely}" | "{projekt.ident_cely}" -->
    @JacksonXmlProperty(localName = "projekt")
    public Vocab projekt;

//<xs:element name="poznamka" minOccurs="0" maxOccurs="1" type="xs:string"/> <!-- "{poznamka}" -->
    @JacksonXmlProperty(localName = "poznamka")
    @Field
    public String poznamka;

//<xs:element name="komponenta" minOccurs="0" maxOccurs="unbounded" type="amcr:komponentaType"/> <!-- "{komponenty.komponenty}" -->
    @JacksonXmlProperty(localName = "komponenta")
    public List<Komponenta> komponenta = new ArrayList<>();

//<xs:element name="neident_akce" minOccurs="0" maxOccurs="1" type="amcr:neident_akceType"/> <!-- "{neident_akce}" -->
    @JacksonXmlProperty(localName = "neident_akce")
    public NeidentAkce neident_akce;

    public List<String> location_info = new ArrayList();

    public void fillSolrFields(SolrInputDocument idoc, String pristupnost) throws Exception {

        IndexUtils.addJSONField(idoc, "dokument_cast", this);
        // idoc.addField("dokument_cast_ident_cely", ident_cely);
        IndexUtils.addVocabField(idoc, "dokument_cast_archeologicky_zaznam", archeologicky_zaznam);
        IndexUtils.addVocabField(idoc, "dokument_cast_projekt", projekt);
        // idoc.addField("dokument_cast_poznamka", poznamka);

        for (Komponenta k : komponenta) {
            // IndexUtils.addJSONField(idoc, "komponenta", k);
             k.fillSolrFields(idoc, "dokument_cast");
        }

        if (neident_akce != null) {
            IndexUtils.addJSONField(idoc, "dokument_cast_neident_akce", neident_akce);
            neident_akce.fillSolrFields(idoc, pristupnost.toUpperCase());
        }

        if (archeologicky_zaznam != null) {
            addLocation(idoc, pristupnost.toUpperCase());
            // SolrSearcher.addFieldNonRepeat(idoc, "location_info", location_info);
            //idoc.addField("location_info", location_info);
        }
    }

    private void addLocation(SolrInputDocument idoc, String pristupnost) throws Exception {
        SolrQuery query = new SolrQuery("ident_cely:\"" + archeologicky_zaznam.getId() + "\"")
                .setFields("*,katastr:hlavni_katastr_" + pristupnost, "az_okres,pristupnost");
        
        JSONObject json = SearchUtils.searchOrIndex(query, "entities", archeologicky_zaznam.getId());

        if (json.getJSONObject("response").getInt("numFound") > 0) {
            JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
            idoc.addField("dokument_cast_" + doc.getString("entity"), archeologicky_zaznam.getId());
            
            idoc.addField("f_vedouci", doc.optString("akce_hlavni_vedouci"));

            if (doc.has("katastr")) {
                // SolrSearcher.addFieldNonRepeat(idoc, "dokument_cast_katastr", doc.getString("katastr"));
                IndexUtils.addSecuredFieldNonRepeat(idoc, "f_katastr", doc.getString("katastr"), pristupnost);
                // SolrSearcher.addFieldNonRepeat(idoc, "dokument_cast_okres", doc.getString("az_okres"));
                IndexUtils.addFieldNonRepeat(idoc, "f_okres", doc.getString("az_okres"));
                JSONObject li = new JSONObject()
                        .put("pristupnost", doc.getString("pristupnost"))
                        .put("katastr", doc.getString("katastr"))
                        .put("okres", doc.getString("okres"));
                
                if (!location_info.contains(li.toString())) {
                    location_info.add(li.toString());
                    SolrSearcher.addFieldNonRepeat(idoc, "location_info", li.toString());
                }
            }

            for (String key : doc.keySet()) {
                if (key.startsWith("loc")) {
                    SolrSearcher.addFieldNonRepeat(idoc, key, doc.opt(key));
                } else if (key.startsWith("lat") || key.startsWith("lng")) {
                    // SolrSearcher.addFieldNonRepeat(idoc, "lng" + key.substring(3), pianDoc.opt(key));
                    JSONArray val = doc.optJSONArray(key);
                    for (int i = 0; i < val.length(); i++) {
                        SolrSearcher.addFieldNonRepeat(idoc, key, val.getBigDecimal(i).toString());
                    }

                } else if (key.equals("pian_id")) {
                    // SolrSearcher.addFieldNonRepeat(idoc, "lng" + key.substring(3), pianDoc.opt(key));
                    JSONArray val = doc.optJSONArray(key);
                    for (int i = 0; i < val.length(); i++) {
                        // SolrSearcher.addFieldNonRepeat(idoc, key, val.opt(i));
                        addPian(idoc, val.optString(i), pristupnost);
                    }

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
                                SolrSearcher.addFieldNonRepeat(idoc, key, pianDoc.opt(key));
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

}
