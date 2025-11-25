package cz.inovatika.arup.digiarchiv.web4.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web4.index.IndexUtils;
import cz.inovatika.arup.digiarchiv.web4.index.SearchUtils;
import cz.inovatika.arup.digiarchiv.web4.index.SolrSearcher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class DokumentCast {

    @Field
    public String entity = "dokument_cast";

    @Field
    public boolean searchable = true;

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

    // public List<String> location_info = new ArrayList();
    public void fillSolrFields(SolrInputDocument idoc, String pristupnost) throws Exception {

        DocumentObjectBinder dob = new DocumentObjectBinder();
        SolrInputDocument kdoc = dob.toSolrInputDocument(this);

        IndexUtils.addVocabField(kdoc, "dokument_cast_archeologicky_zaznam", archeologicky_zaznam);
        IndexUtils.addVocabField(kdoc, "dokument_cast_projekt", projekt);
        IndexUtils.addVocabField(idoc, "dokument_cast_archeologicky_zaznam", archeologicky_zaznam);
        IndexUtils.addVocabField(idoc, "dokument_cast_projekt", projekt);
        IndexUtils.addFieldNonRepeat(idoc, "dokument_cast_ident_cely", ident_cely);

        for (Komponenta k : komponenta) {
            k.fillSolrFields(idoc, "dokument_cast");
            IndexUtils.addJSONField(kdoc, "dokument_cast_komponenta", k);
        }

        if (neident_akce != null) {
            neident_akce.fillSolrFields(idoc, pristupnost.toUpperCase());
            IndexUtils.addJSONField(kdoc, "dokument_cast_neident_akce", neident_akce);
            //IndexUtils.addJSONField(idoc, "dokument_cast_neident_akce", neident_akce);
        }

        if (archeologicky_zaznam != null) {
            addLocation(idoc, pristupnost.toUpperCase());
        }
        if (projekt != null) {
            addProjekt(idoc, pristupnost.toUpperCase());
        }
        // IndexUtils.addFieldNonRepeat(idoc, "location_info", location_info);
        IndexUtils.addJSONField(idoc, "dokument_cast", this);

        try {
            IndexUtils.addAndCommit("entities", kdoc);
        } catch (Exception ex) {
            Logger.getLogger(Komponenta.class.getName()).log(Level.SEVERE, "Error indexing dokument_cast {0}", ident_cely);
            // Logger.getLogger(Komponenta.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addProjekt(SolrInputDocument idoc, String pristupnost) {
        SolrQuery query = new SolrQuery("ident_cely:\"" + projekt.getId() + "\"")
                .setFields("*,projekt_chranene_udaje:[json]");
        try {
            JSONObject json = SearchUtils.searchOrIndex(query, "entities", projekt.getId());
            if (json.getJSONObject("response").getInt("numFound") > 0) {
                JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
                if (doc.has("projekt_chranene_udaje")) {

                    JSONObject pr_chranene_udaje = doc.getJSONObject("projekt_chranene_udaje");
                    String k = pr_chranene_udaje.getJSONObject("hlavni_katastr").optString("value");
                    IndexUtils.addSecuredFieldNonRepeat(idoc,
                            "f_katastr",
                            k,
                            doc.getString("pristupnost"));
                    IndexUtils.addFieldNonRepeat(idoc, "f_okres", doc.getString("projekt_okres"));
                    JSONArray f_kraj = pr_chranene_udaje.optJSONArray("f_kraj");
                    for (int j = 0; j < f_kraj.length(); j++) {
                        IndexUtils.addFieldNonRepeat(idoc, "f_kraj", f_kraj.getString(j)); 
                    }
                    

                    JSONObject li = new JSONObject()
                            .put("pristupnost", doc.getString("pristupnost"))
                            .put("katastr", k)
                            .put("okres", doc.getString("projekt_okres"));
                    IndexUtils.addFieldNonRepeat(idoc, "location_info", li.toString());

                    JSONArray dalsi_katastr = pr_chranene_udaje.getJSONArray("dalsi_katastr");

                    for (int j = 0; j < dalsi_katastr.length(); j++) {
                        k = dalsi_katastr.getJSONObject(j).optString("value");
                        if (k != null) {
                            SolrSearcher.addSecuredFieldNonRepeat(idoc, "f_katastr", k, doc.getString("pristupnost"));
                            //String okres = SolrSearcher.getOkresNazevByKatastr(dalsi_katastr.getJSONObject(j).optString("id"));
                            
                            JSONObject kat = SolrSearcher.getOkresNazevByKatastr(dalsi_katastr.getJSONObject(j).optString("id"));
                            String okres = kat.getString("okres_nazev");
                            String kraj = kat.getString("kraj_nazev");
            
            
                            IndexUtils.addFieldNonRepeat(idoc, "f_okres", okres); 
                            IndexUtils.addFieldNonRepeat(idoc, "f_kraj", kraj);
                            JSONObject li2 = new JSONObject()
                                    .put("pristupnost", doc.getString("pristupnost"))
                                    .put("katastr", k)
                                    .put("okres", okres);
                            IndexUtils.addFieldNonRepeat(idoc, "location_info", li2.toString());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(DokumentCast.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addLocation(SolrInputDocument idoc, String pristupnost) throws Exception {
        SolrQuery query = new SolrQuery("ident_cely:\"" + archeologicky_zaznam.getId() + "\"")
                .setFields("*,az_okres,pristupnost,az_chranene_udaje:[json] ");

        JSONObject json = SearchUtils.searchOrIndex(query, "entities", archeologicky_zaznam.getId());

        if (json.getJSONObject("response").getInt("numFound") > 0) {
            JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
            idoc.addField("dokument_cast_" + doc.getString("entity"), archeologicky_zaznam.getId());

            idoc.addField("f_vedouci", doc.optString("akce_hlavni_vedouci", null));
            IndexUtils.addFieldNonRepeat(idoc, "f_okres", doc.getString("az_okres"));

            String k = null;
            JSONArray dalsi_katastr = new JSONArray();
            if (doc.has("az_chranene_udaje")) {

                JSONObject az_chranene_udaje = doc.getJSONObject("az_chranene_udaje");
                k = az_chranene_udaje.getJSONObject("hlavni_katastr").optString("value");
                IndexUtils.addSecuredFieldNonRepeat(idoc,
                        "f_katastr",
                        k,
                        doc.getString("pristupnost"));

                JSONObject li = new JSONObject()
                        .put("pristupnost", doc.getString("pristupnost"))
                        .put("katastr", k)
                        .put("okres", doc.getString("az_okres"));
                IndexUtils.addFieldNonRepeat(idoc, "location_info", li.toString());

                dalsi_katastr = az_chranene_udaje.getJSONArray("dalsi_katastr");
                for (int j = 0; j < dalsi_katastr.length(); j++) {
                    k = dalsi_katastr.getJSONObject(j).optString("value");
                    if (k != null) {
                        SolrSearcher.addSecuredFieldNonRepeat(idoc, "f_katastr", k, doc.getString("pristupnost"));
                        JSONObject kat = SolrSearcher.getOkresNazevByKatastr(dalsi_katastr.getJSONObject(j).optString("id"));
                        String okres = kat.getString("okres_nazev");
                        String kraj = kat.getString("kraj_nazev");
                        IndexUtils.addFieldNonRepeat(idoc, "f_okres", okres);
                        IndexUtils.addFieldNonRepeat(idoc, "f_kraj", kraj);
                        JSONObject li2 = new JSONObject()
                                .put("pristupnost", doc.getString("pristupnost"))
                                .put("katastr", k)
                                .put("okres", okres);
                        IndexUtils.addFieldNonRepeat(idoc, "location_info", li2.toString());
                    }
                }

            }

            for (String key : doc.keySet()) {
                if (key.startsWith("loc") && !doc.has("pian_id")) {
                    //SolrSearcher.addFieldNonRepeat(idoc, key, doc.opt(key));
                    JSONArray val = doc.optJSONArray(key);
                    for (int i = 0; i < val.length(); i++) {
                        SolrSearcher.addFieldNonRepeat(idoc, key, val.opt(i));
                    }
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
        idoc.addField("pian_ident_cely", pian);
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

}
