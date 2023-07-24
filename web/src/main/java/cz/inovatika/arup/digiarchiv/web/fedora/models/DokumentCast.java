/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;

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

    public void fillSolrFields(SolrInputDocument idoc) {

        IndexUtils.addJSONField(idoc, "dokument_cast", this);
        idoc.addField("dokument_cast_ident_cely", ident_cely);
        IndexUtils.addVocabField(idoc, "dokument_cast_archeologicky_zaznam", archeologicky_zaznam);
        IndexUtils.addVocabField(idoc, "dokument_cast_projekt", projekt);
        idoc.addField("dokument_cast_poznamka", poznamka);
        
        for (Komponenta k : komponenta) {
            IndexUtils.addJSONField(idoc, "komponenta", komponenta);
            k.fillSolrFields(idoc);
        }
        
        if (neident_akce != null) {
            neident_akce.fillSolrFields(idoc);
        }
        
    }

}
