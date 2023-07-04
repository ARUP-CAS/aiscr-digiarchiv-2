/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web.oai;

import cz.inovatika.arup.digiarchiv.web.Options;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class OAIRequest {

  public static String headerOAI() {
    return "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n";
  }

  public static String responseDateTag() {
    return "<responseDate>" + ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT) + "</responseDate>";
  }

  public static String requestTag(HttpServletRequest req) {
    StringBuilder ret = new StringBuilder();
    ret.append("<request ");
    for (String p : req.getParameterMap().keySet()) {
      ret.append(p).append("=\"").append(req.getParameter(p)).append("\" ");
    }
    ret.append(">").append(req.getRequestURL()).append("</request>");
    return ret.toString();
  }

  public static String identify(HttpServletRequest req) {

//    JSONObject conf = Options.getInstance().getJSONObject("OAI");
//    String xml = headerOAI() + responseDateTag() + requestTag(req)
//            + "<Identify>"
//            + "<repositoryName>" + conf.getString("repositoryName") + "</repositoryName>"
//            + "<baseURL>" + req.getRequestURL() + "</baseURL>"
//            + "<protocolVersion>2.0</protocolVersion>"
//            + "<adminEmail>" + conf.getString("adminEmail") + "</adminEmail>"
//            + "<earliestDatestamp>2012-06-30T22:26:40Z</earliestDatestamp>"
//            + "<deletedRecord>persistent</deletedRecord>"
//            + "<granularity>YYYY-MM-DDThh:mm:ssZ</granularity>"
//            + "<description>"
//            + "<oai-identifier xmlns=\"http://www.openarchives.org/OAI/2.0/oai-identifier\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai-identifier http://www.openarchives.org/OAI/2.0/oai-identifier.xsd\">"
//            + "<scheme>oai</scheme>"
//            + "<repositoryIdentifier>aleph-nkp.cz</repositoryIdentifier>"
//            + "<delimiter>:</delimiter>"
//            + "<sampleIdentifier>oai:aleph-nkp.cz:NKC01-000000001</sampleIdentifier>"
//            + "</oai-identifier>"
//            + "</description>"
//            + "</Identify>"
//            + "</OAI-PMH>";

    return Options.getInstance().getOAIIdentify(); 

  }

  public static String listSets(HttpServletRequest req) {
    String xml = headerOAI() + responseDateTag() + requestTag(req)
            + "<ListSets>";
    JSONArray sets = Options.getInstance().getJSONObject("OAI").getJSONArray("sets");
    for (int i = 0; i < sets.length(); i++) {
      JSONObject set = sets.getJSONObject(i);
      xml += "<set>"
              + "<setSpec>" + set.getString("spec")+"</setSpec>"
              + "<setName>" + set.getString("name")+"</setName>"
              + "<setDescription><oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/  http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">";
      JSONArray descs = set.getJSONArray("description");
      for (int j = 0; j<descs.length(); j++) {
        xml += "<dc:description>" + descs.getString(j) + "</dc:description>"; 
      }
      
      xml += "</oai_dc:dc></setDescription></set>";
    }
    xml += "</ListSets>\n"
           + "</OAI-PMH>";
    return xml;
  }

  public static String metadataFormats(HttpServletRequest req) {
    String xml = headerOAI() + responseDateTag() + requestTag(req)
            + "<ListMetadataFormats>"
            + "<metadataFormat>"
            + "<metadataPrefix>oai_dc</metadataPrefix>"
            + "<schema>http://www.openarchives.org/OAI/2.0/oai_dc.xsd</schema>"
            + "<metadataNamespace>http://www.openarchives.org/OAI/2.0/oai_dc/</metadataNamespace>"
            + "</metadataFormat>"
            + "<metadataFormat><metadataPrefix>oai_amcr</metadataPrefix>"
            + "<schema>https://api.aiscr.cz/dapro/media/oai_amcr.xsd</schema>"
            + "<metadataNamespace>https://api.aiscr.cz/schema/oai_amcr/</metadataNamespace>"
            + "</metadataFormat>"
            + "</ListMetadataFormats>"
            + "</OAI-PMH>";
    return xml;

  }

  public static String listRecords(HttpServletRequest req, boolean onlyIdentifiers) {
    StringBuilder ret = new StringBuilder();
    ret.append(headerOAI())
            .append(responseDateTag())
            .append(requestTag(req));

    return ret.toString();

  }

  public static String getRecord(HttpServletRequest req) {
    StringBuilder ret = new StringBuilder();
    ret.append(headerOAI())
            .append(responseDateTag())
            .append(requestTag(req));
    return ret.toString();

  }
}
