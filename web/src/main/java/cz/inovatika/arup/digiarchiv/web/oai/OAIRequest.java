/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web.oai;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;
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
        return Options.getInstance().getOAIIdentify();
    }

    public static String listSets(HttpServletRequest req) {
        String xml = headerOAI() + responseDateTag() + requestTag(req)
                + "<ListSets>";
        JSONArray sets = Options.getInstance().getJSONObject("OAI").getJSONArray("sets");
        for (int i = 0; i < sets.length(); i++) {
            JSONObject set = sets.getJSONObject(i);
            xml += "<set>"
                    + "<setSpec>" + set.getString("spec") + "</setSpec>"
                    + "<setName>" + set.getString("name") + "</setName>"
                    + "<setDescription><oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/  http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">";
            JSONArray descs = set.getJSONArray("description");
            for (int j = 0; j < descs.length(); j++) {
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
        JSONObject conf = Options.getInstance().getJSONObject("OAI");
        ret.append(headerOAI())
                .append(responseDateTag())
                .append(requestTag(req))
                .append("<ListRecords>");
        try {
            String model = req.getParameter("set");
            String cursor =  CursorMarkParams.CURSOR_MARK_START;
            String resumptionToken = req.getParameter("resumptionToken");
            // resumptionToken has format set:cursor
            if (resumptionToken != null) {
                model = resumptionToken.split(":", 1)[1];
                cursor = resumptionToken.split(":")[0];
            } 
            
            SolrQuery query = new SolrQuery("*")
                    .setSort(SolrQuery.SortClause.create(conf.getString("orderField"), conf.getString("orderDirection")))
                    .addFilterQuery("model:\"" + model + "\"")
                    .setRows(conf.getInt("recordsPerPage"));
                query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursor);
            QueryResponse resp = IndexUtils.getClient().query("oai", query); 
            SolrDocumentList docs = resp.getResults();
            for (SolrDocument doc : docs) {
                appendRecord(ret, doc, req);
            }
            
            String nextCursorMark = resp.getNextCursorMark();
            if (!cursor.equals(nextCursorMark) && docs.getNumFound() > conf.getInt("recordsPerPage")) {
              ret.append("<resumptionToken ")
                      .append("completeListSize=\"")
                      .append(docs.getNumFound())
                      .append("\" >")
                      .append(model)
                      .append(":")
                      .append(nextCursorMark)
                      .append("</resumptionToken>");
            }
            
        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
        ret.append("</ListRecords>").append("</OAI-PMH>");
        return ret.toString();
    }

    public static String getRecord(HttpServletRequest req) {
        StringBuilder ret = new StringBuilder();
        ret.append(headerOAI())
                .append(responseDateTag())
                .append(requestTag(req));
        try {
            String id = req.getParameter("id");
            SolrQuery query = new SolrQuery("*")
                    .addFilterQuery("ident_cely:\"" + id + "\"");
            QueryResponse resp = IndexUtils.getClient().query("oai", query);
            SolrDocument doc = resp.getResults().get(0);

            appendRecord(ret, doc, req);
        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
        ret.append("</OAI-PMH>");
        return ret.toString();
    }
    
    private static void appendRecord(StringBuilder ret, SolrDocument doc, HttpServletRequest req) {
        String id = (String) doc.getFieldValue("ident_cely");
        Date datestamp = (Date) doc.getFieldValue("datestamp");
        ret.append("<record>");
            ret.append("<header>")
                    .append("<identifier>")
                    .append(Options.getInstance().getJSONObject("OAI").getString("baseUrl"))
                    .append("/id/")
                    .append(id)
                    .append("</identifier>")
                    .append("<datestamp>")
                    .append(datestamp.toInstant().toString())
                    .append("</datestamp>")
                    .append("<setSpec>")
                    .append((String) doc.getFieldValue("model"))
                    .append("</setSpec>");

            // <setSpec>projekt</setSpec> <!-- "projekt" | "archeologicky_zaznam" | "let" | "adb" | "dokument" | "ext_zdroj" | "pian" | "samostatny_nalez" | "uzivatel" | "heslo" | "ruian_kraj" | "ruian_okres" | "ruian_katastr" | "organizace | "osoba -->
            ret.append("</header>");

            ret.append("<metadata>");
            ret.append(filter(req, (String) doc.getFieldValue("pristupnost"), (String) doc.getFieldValue("xml")));
            ret.append("</metadata>");
            ret.append("</record>");
    }

    private static String filter(HttpServletRequest req, String docPristupnost, String xml) {
        // LoginServlet.organizace(req.getSession())
        String userPristupnost = LoginServlet.pristupnost(req.getSession());

        if (xml.contains("<amcr:chranene_udaje>") && docPristupnost.compareToIgnoreCase(userPristupnost) > 0) {
            String ret = xml.substring(0, xml.indexOf("<amcr:chranene_udaje>"));
            ret += xml.substring(xml.indexOf("</amcr:chranene_udaje>") + "</amcr:chranene_udaje>".length());
            return ret;
        } else {
            return xml;
        }
        
    }
}
