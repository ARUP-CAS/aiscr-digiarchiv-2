/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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

    public void fillSolrFields(SolrInputDocument idoc, String pristupnost) {

        IndexUtils.addJSONField(idoc, "dokument_cast", this);
        idoc.addField("dokument_cast_ident_cely", ident_cely);
        IndexUtils.addVocabField(idoc, "dokument_cast_archeologicky_zaznam", archeologicky_zaznam);
        IndexUtils.addVocabField(idoc, "dokument_cast_projekt", projekt);
        idoc.addField("dokument_cast_poznamka", poznamka);

        for (Komponenta k : komponenta) {
            IndexUtils.addJSONField(idoc, "komponenta", k);
            k.fillSolrFields(idoc);
        }

        if (neident_akce != null) {
            IndexUtils.addJSONField(idoc, "neident_akce", neident_akce);
            neident_akce.fillSolrFields(idoc, pristupnost.toUpperCase());
        }

        if (archeologicky_zaznam != null) {
            addLocation(idoc, pristupnost.toUpperCase());
            // SolrSearcher.addFieldNonRepeat(idoc, "location_info", location_info);
            //idoc.addField("location_info", location_info);
        }
    }

    private void addLocation(SolrInputDocument idoc, String pristupnost) {
        SolrQuery query = new SolrQuery("ident_cely:\"" + archeologicky_zaznam.getId() + "\"")
                .setFields("*,katastr:hlavni_katastr_" + pristupnost, "okres,pristupnost");
        JSONObject json = SearchUtils.json(query, IndexUtils.getClient(), "entities");

        if (json.getJSONObject("response").getInt("numFound") > 0) {
            JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
            idoc.addField("dokument_cast_" + doc.getString("entity"), archeologicky_zaznam.getId());

            if (doc.has("katastr")) {
                SolrSearcher.addFieldNonRepeat(idoc, "dokument_cast_katastr", doc.getString("katastr"));
                IndexUtils.addSecuredFieldNonRepeat(idoc, "f_katastr", doc.getString("katastr"), pristupnost);
                SolrSearcher.addFieldNonRepeat(idoc, "dokument_cast_okres", doc.getString("okres"));
                IndexUtils.addFieldNonRepeat(idoc, "f_okres", doc.getString("okres"));
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
                        addPian(idoc, pristupnost, val.optString(i));
                    }

                }
            }
        }
    }
    
    private void addPian(SolrInputDocument idoc, String pristupnost, String pian_id) {
        SolrQuery query = new SolrQuery("ident_cely:\"" + pian_id + "\"")
                .setFields("typ,presnost,chranene_udaje:[json]");
        JSONObject json = SearchUtils.json(query, IndexUtils.getClient(), "entities");
        if (json.getJSONObject("response").getInt("numFound") > 0) {
            JSONObject doc = json.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
            SolrSearcher.addFieldNonRepeat(idoc, "pian_id", pian_id);
            IndexUtils.addSecuredFieldNonRepeat(idoc, "f_pian_typ", doc.getJSONArray("typ").getString(0), pristupnost);
            IndexUtils.addSecuredFieldNonRepeat(idoc, "f_pian_presnost", doc.getString("presnost"), pristupnost);
            IndexUtils.addSecuredFieldNonRepeat(idoc, "f_pian_zm10", doc.getJSONObject("chranene_udaje").getString("zm10"), pristupnost);
        }
    }

}
