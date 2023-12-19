package cz.inovatika.arup.digiarchiv.web.oai;

import cz.inovatika.arup.digiarchiv.web.LoginServlet;
import cz.inovatika.arup.digiarchiv.web.Options;
import cz.inovatika.arup.digiarchiv.web.fedora.FedoraModel;
import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * @author alberto
 */
public class OAIRequest {

    private static Transformer dcTransformer;
    private static Transformer emptyTransformer;

    private static Transformer getTransformer() throws TransformerConfigurationException {
        if (dcTransformer == null) {
            TransformerFactory factory = TransformerFactory.newInstance();
            Source xslt = new StreamSource(Options.getInstance().getDCXslt());
            dcTransformer = factory.newTransformer(xslt);
            dcTransformer.setOutputProperty("omit-xml-declaration", "yes");
        }
        return dcTransformer;
    }

    private static Transformer getTransformer2() throws TransformerConfigurationException {
        if (emptyTransformer == null) {
            TransformerFactory factory = TransformerFactory.newInstance();
            Source xslt = new StreamSource(Options.getInstance().getEmptyXslt());
            emptyTransformer = factory.newTransformer(xslt);
            emptyTransformer.setOutputProperty("omit-xml-declaration", "yes");
        }
        return emptyTransformer;
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
        StringBuilder ret = new StringBuilder();
        ret.append(headerOAI())
                .append(responseDateTag())
                .append(requestTag(req))
                .append(Options.getInstance().getOAIIdentify())
                .append("</OAI-PMH>");
        return ret.toString();
    }

    public static String listSets(HttpServletRequest req) {
        StringBuilder ret = new StringBuilder();
        ret.append(headerOAI())
                .append(responseDateTag())
                .append(requestTag(req))
                .append(Options.getInstance().getOAIListSets())
                .append("</OAI-PMH>");
        return ret.toString();
    }

    public static String metadataFormats(HttpServletRequest req) {
        try {
            String prefix = Options.getInstance().getJSONObject("OAI").getString("baseUrl") + "/id/";
            String id = req.getParameter("identifier").substring(prefix.length());
            SolrQuery query = new SolrQuery("*")
                    .addFilterQuery("ident_cely:\"" + id + "\"");
            QueryResponse resp = IndexUtils.getClient().query("oai", query);

            if (resp.getResults().getNumFound() == 0) {
                return idDoesNotExist(req);
            }

            StringBuilder ret = new StringBuilder();
            ret.append(headerOAI())
                    .append(responseDateTag())
                    .append(requestTag(req))
                    .append(Options.getInstance().getOAIListMetadataFormats())
                    .append("</OAI-PMH>");
            return ret.toString();
        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
            return ex.toString();
        }
    }

    private static void storeResumptionToken(String token, JSONObject data) {
        try {
            SolrInputDocument idoc = new SolrInputDocument();
            idoc.setField("id", token);
            idoc.setField("type", "resumptionToken");
            String d = ZonedDateTime
                    .now(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.SECONDS)
                    .plus(Duration.of(Options.getInstance().getJSONObject("OAI").getInt("resumptionTokenExpires"), ChronoUnit.MINUTES))
                    .format(DateTimeFormatter.ISO_INSTANT);
            data.put("expires", d);
            idoc.setField("data", data.toString());
            idoc.setField("expiration", d);
            IndexUtils.getClient().add("work", idoc);
            IndexUtils.getClient().commit("work");
        } catch (Exception ex) {
            Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static JSONObject retrieveResumptionToken(String resumptionToken) {
        try {
            SolrQuery query = new SolrQuery("id:" + resumptionToken)
                    .setFields("data:[json]")
                    .addFilterQuery("type:resumptionToken")
                    .addFilterQuery("expiration:[NOW TO *]");
            QueryResponse resp = IndexUtils.getClient().query("work", query);
            SolrDocumentList docs = resp.getResults();
            if (docs.isEmpty()) {
                return null;
            }
            SolrDocument doc = docs.get(0);
            return new JSONObject((String) doc.getFirstValue("data"));
        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private static String idDoesNotExist(HttpServletRequest req) {
        return OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                + "<request>" + req.getRequestURL() + "</request>"
                + "<error code=\"idDoesNotExist\" />"
                + "</OAI-PMH>";
    }

    private static String noRecordsMatch(HttpServletRequest req) {
        return OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                + "<request>" + req.getRequestURL() + "</request>"
                + "<error code=\"noRecordsMatch\" />"
                + "</OAI-PMH>";
    }

    private static String badArgument(HttpServletRequest req) {
        return OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                + "<request>" + req.getRequestURL() + "</request>"
                + "<error code=\"badArgument\">Invalid arguments</error>"
                + "</OAI-PMH>";
    }

    private static String badArgument(HttpServletRequest req, String msg) {
        return OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                + "<request>" + req.getRequestURL() + "</request>"
                + "<error code=\"badArgument\">" + msg + "</error>"
                + "</OAI-PMH>";
    }

    public static String listRecords(HttpServletRequest req, boolean onlyIdentifiers) {
        List<String> validParams = List.of("verb", "resumptionToken", "metadataPrefix", "from", "until", "set");
        List<String> params = Collections.list(req.getParameterNames());

        for (String name : params) {
            if (validParams.indexOf(name) < 0) {
                return badArgument(req);
            }
        }

        for (String name : validParams) {
            if (req.getParameter(name) != null && req.getParameterValues(name).length > 1) {
                return badArgument(req);
            }
            if (req.getParameter(name) != null && "".equals(req.getParameter(name))) {
                return badArgument(req);
            }
        }

        String resumptionToken = req.getParameter("resumptionToken");
        String metadataPrefix = req.getParameter("metadataPrefix");
        if (resumptionToken != null) {

            if (req.getParameterMap().size() > 2) {
                return badArgument(req);
            }

            JSONObject solrRt = retrieveResumptionToken(resumptionToken);
            if (solrRt != null) {
                // Build query with info in resumptionToken
                metadataPrefix = solrRt.getString("metadataPrefix");
            } else {
                String xml = OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                        + "<request>" + req.getRequestURL() + "</request>"
                        + "<error code=\"badResumptionToken\"/>"
                        + "</OAI-PMH>";
                return xml;
            }
        }
        if (metadataPrefix == null && resumptionToken == null) {
            return badArgument(req, "metadataPrefix is missing");
        }
        String[] reqMetadataPrefixes = req.getParameterValues("metadataPrefix");
        if (resumptionToken == null && reqMetadataPrefixes != null && reqMetadataPrefixes.length > 1) {
            String xml = OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                    + "<request>" + req.getRequestURL() + "</request>"
                    + "<error code=\"badArgument\">multiple metadataPrefixes</error>"
                    + "</OAI-PMH>";
            return xml;
        }

        List<Object> metadataPrefixes = Options.getInstance().getJSONObject("OAI").getJSONArray("metadataPrefixes").toList();
        if (resumptionToken == null && !metadataPrefixes.contains(metadataPrefix)) {
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
            String from = req.getParameter("from");
            String until = req.getParameter("until");
            long page = 0;
            if (model == null) {
                model = "*";
            } else if (model.equals("archeologicky_zaznam")) {
                model = "(akce OR lokalita)";
            } else if (model.startsWith("archeologicky_zaznam:")) {
                model = model.substring("archeologicky_zaznam:".length());
            }
            String cursor = CursorMarkParams.CURSOR_MARK_START;
            SolrQuery query = new SolrQuery("*")
                    .setSort(SolrQuery.SortClause.create(conf.getString("orderField"), conf.getString("orderDirection")))
                    .addSort(SolrQuery.SortClause.create("ident_cely", "asc"))
                    // .addFilterQuery("pristupnost:c")
                    // .addFilterQuery("stav:1")
                    .setRows(conf.getInt("recordsPerPage"));

            if (resumptionToken != null) {
                JSONObject solrRt = retrieveResumptionToken(resumptionToken);
                if (solrRt != null) {
                    // Build query with info in resumptionToken
                    metadataPrefix = solrRt.getString("metadataPrefix");
                    model = solrRt.getString("model");
                    cursor = solrRt.getString("nextCursorMark");
                    page = solrRt.getLong("page") + 1;
                    if (solrRt.has("from")) {
                        from = solrRt.getString("from");
                    }
                    if (solrRt.has("until")) {
                        from = solrRt.getString("until");
                    }

                } else {
                    String xml = OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                            + "<request>" + req.getRequestURL() + "</request>"
                            + "<error code=\"badResumptionToken\"/>"
                            + "</OAI-PMH>";
                    return xml;
                }
            }

            query.addFilterQuery("model:" + model);

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
            if (docs.getNumFound() == 0) {
                return noRecordsMatch(req);
            }
            for (SolrDocument doc : docs) {
                appendRecord(ret, doc, req, onlyIdentifiers, metadataPrefix);
            }

            String nextCursorMark = resp.getNextCursorMark();
            String oaiCursor = " cursor=\"" + (page * conf.getInt("recordsPerPage")) + "\"";

            JSONObject rt = new JSONObject();
            rt.put("model", model);
            rt.put("metadataPrefix", metadataPrefix);
            rt.put("from", from);
            rt.put("until", until);
            rt.put("numFound", docs.getNumFound());
            rt.put("page", page);
            rt.put("nextCursorMark", nextCursorMark);
            String md5Hex = DigestUtils.md5Hex(rt.toString()).toUpperCase();
            storeResumptionToken(md5Hex, rt);

            // String nextCursorMark = resp.getNextCursorMark();
            if (!cursor.equals(nextCursorMark) && docs.getNumFound() > conf.getInt("recordsPerPage")) {
                ret.append("<resumptionToken ")
                        .append("completeListSize=\"")
                        .append(docs.getNumFound())
                        .append("\" ")
                        .append(oaiCursor)
                        .append(">")
                        .append(md5Hex)
                        .append("</resumptionToken>");
            }

        } catch (IllegalArgumentException ex) {
            Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
            String xml = OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                    + "<request>" + req.getRequestURL() + "</request>"
                    + "<error code=\"badResumptionToken\"/>"
                    + "</OAI-PMH>";
            return xml;
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

        List<String> validParams = List.of("verb", "metadataPrefix", "identifier");
        List<String> params = Collections.list(req.getParameterNames());

        for (String name : params) {
            if (validParams.indexOf(name) < 0) {
                return badArgument(req);
            }
        }

        String metadataPrefix = req.getParameter("metadataPrefix");
        if (metadataPrefix == null) {
            return badArgument(req, "metadataPrefix is missing");
        }
        String[] reqMetadataPrefixes = req.getParameterValues("metadataPrefix");
        if (reqMetadataPrefixes.length > 1) {
            return badArgument(req, "multiple metadataPrefixes");
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
            if (req.getParameter("identifier").length() < prefix.length()) {
                return idDoesNotExist(req);
            }
            String id = req.getParameter("identifier").substring(prefix.length());
            SolrQuery query = new SolrQuery("*")
                    .addFilterQuery("ident_cely:\"" + id + "\"");
            QueryResponse resp = IndexUtils.getClient().query("oai", query);

            if (resp.getResults().getNumFound() == 0) {
                return idDoesNotExist(req);
            }
            SolrDocument doc = resp.getResults().get(0);

            appendRecord(ret, doc, req, false, metadataPrefix);
        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
        ret.append("</OAI-PMH>");
        return ret.toString();
    }

    private static void appendRecord(StringBuilder ret, SolrDocument doc, HttpServletRequest req, boolean onlyIdentifiers, String metadataPrefix) {
        String id = (String) doc.getFieldValue("ident_cely");
        Date datestamp = (Date) doc.getFieldValue("datestamp");
        boolean isDeleted = false;
        if (doc.containsKey("is_deleted")) {
            isDeleted = (boolean) doc.getFieldValue("is_deleted");
        }
        String status = isDeleted ? " status=\"deleted\"" : "";

        String model = (String) doc.getFieldValue("model");
        if (model.equals("akce") || model.equals("lokalita") ) {
            model = "archeologicky_zaznam:" + model;
        } 

        ret.append("<record>");
        ret.append("<header").append(status).append(" >")
                .append("<identifier>")
                .append(Options.getInstance().getJSONObject("OAI").getString("baseUrl"))
                .append("/id/")
                .append(id)
                .append("</identifier>")
                .append("<datestamp>")
                .append(datestamp.toInstant().toString())
                .append("</datestamp>")
                .append("<setSpec>")
                .append(model)
                .append("</setSpec>");

        // <setSpec>projekt</setSpec> <!-- "projekt" | "archeologicky_zaznam" | "let" | "adb" | "dokument" | "ext_zdroj" | "pian" | "samostatny_nalez" | "uzivatel" | "heslo" | "ruian_kraj" | "ruian_okres" | "ruian_katastr" | "organizace | "osoba -->
        ret.append("</header>");

        if (!onlyIdentifiers && !isDeleted) {

            ret.append("<metadata>");
            String xml = filter(req, doc);
            if (!xml.equals(ERROR_404_MSG) && "oai_dc".equals(metadataPrefix)) {
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

        Pattern emptyValueTag = Pattern.compile("\\s*<dc:\\w+.*/>");
        Pattern emptyTagMultiLine = Pattern.compile("\\s*<\\w+>\n*\\s*</\\w+>");

        String dc = sw.toString();
        dc = emptyValueTag.matcher(dc).replaceAll("");

        while (dc.length() != (dc = emptyTagMultiLine.matcher(dc).replaceAll("")).length()) {
        }

        return dc;

        // return sw.toString();
//        String dc = sw.toString();
//        Source dc2 = new StreamSource(new StringReader(dc));
//        StringWriter sw2 = new StringWriter();
//        getTransformer2().transform(dc2, new StreamResult(sw2));
//        return sw2.toString();
    }

}
