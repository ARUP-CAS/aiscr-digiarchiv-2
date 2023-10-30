/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cz.inovatika.arup.digiarchiv.web.oai;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
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

    private static Transformer dcTransformer;

    private static Transformer getTransformer() throws TransformerConfigurationException {
        if (dcTransformer == null) {
            TransformerFactory factory = TransformerFactory.newInstance();
            Source xslt = new StreamSource(Options.getInstance().getDCXslt());
            dcTransformer = factory.newTransformer(xslt);
            dcTransformer.setOutputProperty("omit-xml-declaration", "yes");
        }
        return dcTransformer;
    }

    public static String headerOAI() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\" ?><OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n";
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
        return Options.getInstance().getOAIListSets();
    }

    public static String metadataFormats(HttpServletRequest req) {
        return Options.getInstance().getOAIListMetadataFormats();
    }

    public static String listRecords(HttpServletRequest req, boolean onlyIdentifiers) {
        String metadataPrefix = req.getParameter("metadataPrefix");
        if (metadataPrefix == null) {
            String xml = OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                    + "<request>" + req.getRequestURL() + "</request>"
                    + "<error code=\"badArgument\">metadataPrefix is missing</error>"
                    + "</OAI-PMH>";
            return xml;
        }
        List<Object> metadataPrefixes = Options.getInstance().getJSONObject("OAI").getJSONArray("metadataPrefixes").toList();
        if (!metadataPrefixes.contains(metadataPrefix)) {
            String xml = OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                    + "<request>" + req.getRequestURL() + "</request>"
                    + "<error code=\"cannotDisseminateFormat\"/>"
                    + "</OAI-PMH>";
            return xml;
        }
        StringBuilder ret = new StringBuilder();
        JSONObject conf = Options.getInstance().getJSONObject("OAI");
        ret.append(headerOAI())
                .append(responseDateTag())
                .append(requestTag(req));
        if (onlyIdentifiers) {
            ret.append("<ListIdentifiers>");
        } else {
            ret.append("<ListRecords>");
        }

        try {
            String model = req.getParameter("set");
            if (model == null) {
                model = "*";
            }
            String cursor = CursorMarkParams.CURSOR_MARK_START;
            String resumptionToken = req.getParameter("resumptionToken");
            String from = req.getParameter("from");
            String until = req.getParameter("until");
            // resumptionToken has format set:cursor
            if (resumptionToken != null) {
                cursor = resumptionToken.split(":", 2)[1];
                model = resumptionToken.split(":")[0];
            }

            SolrQuery query = new SolrQuery("*")
                    .setSort(SolrQuery.SortClause.create(conf.getString("orderField"), conf.getString("orderDirection")))
                    .addFilterQuery("model:" + model)
                    // .addFilterQuery("pristupnost:c")
                    // .addFilterQuery("stav:1")
                    .setRows(conf.getInt("recordsPerPage"));
            if (from != null || until != null) {
                if (from == null) {
                    from = "*";
                } else if (from.length() < 11) {
                    from = from + "T00:00:00Z";
                }
                if (until == null) {
                    until = "*";
                } else if (until.length() < 11) {
                    until = until + "T23:59:59Z";
                }
                query.addFilterQuery("datestamp:[" + from + " TO " + until + "]");
            }
            query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursor);
            QueryResponse resp = IndexUtils.getClient().query("oai", query);
            SolrDocumentList docs = resp.getResults();
            for (SolrDocument doc : docs) {
                appendRecord(ret, doc, req, onlyIdentifiers);
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
        if (onlyIdentifiers) {
            ret.append("</ListIdentifiers>");
        } else {
            ret.append("</ListRecords>");
        }
        ret.append("</OAI-PMH>");
        return ret.toString();
    }

    public static String getRecord(HttpServletRequest req) {
        String metadataPrefix = req.getParameter("metadataPrefix");
        if (metadataPrefix == null) {
            String xml = OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                    + "<request>" + req.getRequestURL() + "</request>"
                    + "<error code=\"badArgument\">metadataPrefix is missing</error>"
                    + "</OAI-PMH>";
            return xml;
        }
        List<Object> metadataPrefixes = Options.getInstance().getJSONObject("OAI").getJSONArray("metadataPrefixes").toList();
        if (!metadataPrefixes.contains(metadataPrefix)) {
            String xml = OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                    + "<request>" + req.getRequestURL() + "</request>"
                    + "<error code=\"cannotDisseminateFormat\"/>"
                    + "</OAI-PMH>";
            return xml;
        }
        StringBuilder ret = new StringBuilder();
        ret.append(headerOAI())
                .append(responseDateTag())
                .append(requestTag(req));
        try {
            String prefix = Options.getInstance().getJSONObject("OAI").getString("baseUrl") + "/id/";
            String id = req.getParameter("identifier").substring(prefix.length());
            SolrQuery query = new SolrQuery("*")
                    .addFilterQuery("ident_cely:\"" + id + "\"");
            QueryResponse resp = IndexUtils.getClient().query("oai", query);
            SolrDocument doc = resp.getResults().get(0);

            appendRecord(ret, doc, req, false);
        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
        ret.append("</OAI-PMH>");
        return ret.toString();
    }

    private static void appendRecord(StringBuilder ret, SolrDocument doc, HttpServletRequest req, boolean onlyIdentifiers) {
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

        if (!onlyIdentifiers) {

            ret.append("<metadata>");
            String xml = filter(req, doc);
            if (!xml.equals(ERROR_404_MSG) && "oai_dc".equals(req.getParameter("metadataPrefix"))) {
                try {
                    xml = transformToDC(xml);
                } catch (TransformerException ex) {
                    Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            ret.append(xml);
            ret.append("</metadata>");
        }
        ret.append("</record>");
    }
    
    private static final String ERROR_404_MSG = "HTTP/1.1 403 Forbidden";

    private static String filter(HttpServletRequest req, SolrDocument doc) {
        // LoginServlet.organizace(req.getSession())
        String userPristupnost = LoginServlet.user(req).optString("pristupnost", "A");
        String docPristupnost = (String) doc.getFieldValue("pristupnost");
        String model = (String) doc.getFieldValue("model");
        FedoraModel fm = FedoraModel.getFedoraModel(model);
        if (fm.filterOAI(LoginServlet.user(req), doc)) {
            String xml = (String) doc.getFieldValue("xml");
            if (xml.contains("<amcr:chranene_udaje>") && docPristupnost.compareToIgnoreCase(userPristupnost) > 0) {
                String ret = xml;
                while (ret.contains("<amcr:chranene_udaje>")) {
                    int pos1 = ret.indexOf("<amcr:chranene_udaje>");
                    String s = ret.substring(0, pos1);
                    int pos2 = ret.indexOf("</amcr:chranene_udaje>");
                    s += ret.substring(pos2 + "</amcr:chranene_udaje>".length());
                    ret = s;
                }
                return ret;
            } else {
                return xml;
            }
        } else {
            return ERROR_404_MSG;
        }

    }

    private static String transformToDC(String xml) throws TransformerException {

        Source text = new StreamSource(new StringReader(xml));
        StringWriter sw = new StringWriter();
        getTransformer().transform(text, new StreamResult(sw));
        return sw.toString();
    }
}
