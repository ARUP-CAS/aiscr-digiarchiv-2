package cz.inovatika.arup.digiarchiv.web4.fedora.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.inovatika.arup.digiarchiv.web4.fedora.FedoraUtils;
import cz.inovatika.arup.digiarchiv.web4.index.IndexUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class Soubor {

//<xs:element name="id" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "soub-{id}" -->
  @JacksonXmlProperty(localName = "id")
  @Field
  public String id;

//<xs:element name="path" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{path}" -->
  @JacksonXmlProperty(localName = "path")
  @Field
  public String path = "neni";

//<xs:element name="nazev" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{nazev}" -->
  @JacksonXmlProperty(localName = "nazev")
  @Field
  public String nazev;

//<xs:element name="mimetype" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{mimetype}" -->
  @JacksonXmlProperty(localName = "mimetype")
  @Field
  public String mimetype;

//<xs:element name="rozsah" minOccurs="0" maxOccurs="1" type="xs:integer"/> <!-- "{rozsah}" -->
  @JacksonXmlProperty(localName = "rozsah")
  @Field
  public long rozsah;

//<xs:element name="size_mb" minOccurs="1" maxOccurs="1" type="xs:decimal"/> <!-- "{size_mb}" -->
  @JacksonXmlProperty(localName = "size_mb")
  @Field
  public float size_mb;

//<xs:element name="sha_512" minOccurs="1" maxOccurs="1" type="xs:string"/> <!-- "{sha_512}" -->
  @JacksonXmlProperty(localName = "sha_512")
  @Field
  public String sha_512;

//<xs:element name="historie" minOccurs="0" maxOccurs="unbounded" type="amcr:historieType"/> <!-- historie.historie_set -->
  @JacksonXmlProperty(localName = "historie")
  public List<Historie> historie = new ArrayList();

  public List<Distri> distribuce = new ArrayList();

  public void fillSolrFields(SolrInputDocument idoc) {
    IndexUtils.setDateStamp(idoc, id);
    IndexUtils.setDateStampFromHistory(idoc, historie);

    idoc.addField("soubor_id", id);
    idoc.addField("soubor_nazev", nazev);
    idoc.addField("soubor_filepath", path);
    idoc.addField("soubor_rozsah", rozsah);
    idoc.addField("soubor_size_mbytes", size_mb);
    idoc.addField("soubor_mimetype", mimetype);
    List<String> d = new ArrayList();
    distribuce.add(new Distri("orig", nazev, (long) Math.floor(size_mb*1024*1024), mimetype));

    for (Historie h : historie) {
      if ("DIST01".equals(h.typ_zmeny)) {
        d.add(h.poznamka);
      } else if ("DIST10".equals(h.typ_zmeny)) {
        d.remove(h.poznamka);
      }
    }
    for (String di : d) {
      String url = path + "/" + di;
      url = url.substring(url.indexOf("record"));
      try {
        JSONObject json = FedoraUtils.getFileMetadataByPath(url);
        Distri dis = new Distri(
                di,
                json.getJSONArray("http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#filename")
                        .getJSONObject(0).getString("@value"),
                json.getJSONArray("http://www.loc.gov/premis/rdf/v1#hasSize")
                        .getJSONObject(0).getLong("@value"),
                json.getJSONArray("http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#hasMimeType")
                        .getJSONObject(0).getString("@value")
        );
        distribuce.add(dis);
      } catch (Exception ex) {
        System.getLogger(Soubor.class.getName()).log(System.Logger.Level.ERROR, "Can't get metadata for distri", ex);
      }
    }
    idoc.addField("soubor_distri", d);
    IndexUtils.addJSONField(idoc, "soubor", this);

  }

  public SolrInputDocument createSolrDoc() {

    DocumentObjectBinder dob = new DocumentObjectBinder();
    SolrInputDocument idoc = dob.toSolrInputDocument(this);
    IndexUtils.setDateStamp(idoc, id);
    IndexUtils.setDateStampFromHistory(idoc, historie);
    return idoc;

  }
}

class Distri {

  public String path;
  public String filename;
  public long size;
  public String mimetype;

  public Distri(String path, String filename, long size, String mimetype) {
    this.path = path;
    this.filename = filename;
    this.size = size;
    this.mimetype = mimetype;
  }
}
