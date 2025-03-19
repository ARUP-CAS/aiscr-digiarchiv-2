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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.json.JSONObject;
import org.xml.sax.InputSource;

/**
 *
 * @author alberto
 */
public class OAIRequest {

    private static final Map<String, Transformer> dcTransformers = new HashMap<>();  
    private static Transformer forbiddenTransformer;
    private static Transformer emptyTransformer;
    private static final Map<String, Transformer> versionTransformers = new HashMap<>();

    private static Transformer getDcTransformer(String xmlns_amcr) throws TransformerConfigurationException {
        if (dcTransformers.get(xmlns_amcr) == null) {
            TransformerFactory factory = TransformerFactory.newInstance();
            String xsltStr; 
            try {
                xsltStr = FileUtils.readFileToString(Options.getInstance().getDCXslt(), "UTF-8");
                xsltStr = xsltStr.replace("##AMCRVERSION##", xmlns_amcr);
                Source xslt = new StreamSource(new StringReader(xsltStr));
                Transformer dcTransformer = factory.newTransformer(xslt);
                dcTransformer.setOutputProperty("omit-xml-declaration", "yes");
                dcTransformer.setParameter("base_url", Options.getInstance().getJSONObject("OAI").getString("baseUrl"));
                dcTransformers.put(xmlns_amcr, dcTransformer);
            } catch (IOException ex) {
                Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return dcTransformers.get(xmlns_amcr);
    }

    private static Transformer get404Transformer() throws TransformerConfigurationException {
        if (forbiddenTransformer == null) {
            TransformerFactory factory = TransformerFactory.newInstance();
            Source xslt = new StreamSource(Options.getInstance().getForbiddenXslt());
            forbiddenTransformer = factory.newTransformer(xslt);
            forbiddenTransformer.setOutputProperty("omit-xml-declaration", "yes");
        }
        return forbiddenTransformer;
    }

    private static Transformer getVersionTransformer(String version) throws TransformerConfigurationException {
        if (versionTransformers.get(version) == null) {
            //TransformerFactory factory = TransformerFactory.newDefaultInstance();
            TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();
            Source xslt = new StreamSource(Options.getInstance().getVersionXslt(version));
            Transformer t = factory.newTransformer(xslt);
            t.setOutputProperty("omit-xml-declaration", "yes");
            versionTransformers.put(version, t);
        }
        return versionTransformers.get(version);
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
        return "<?xml version=\"1.0\" encoding=\"utf-8\" ?>"
                + "<?xml-stylesheet type=\"text/xsl\" href=\"/oai2.xsl\" ?>\n"
                + "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" \n"
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
                + "xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n";
    }

    public static String responseDateTag() {
        return "<responseDate>" + ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT) + "</responseDate>\n";
    }

    public static String requestTag(HttpServletRequest req, String version) {
        StringBuilder ret = new StringBuilder();
        ret.append("<request ");
        for (String p : req.getParameterMap().keySet()) {
            ret.append(p).append("=\"").append(req.getParameter(p)).append("\" ");
        }
        ret.append(">").append(Options.getInstance().getJSONObject("OAI").getString("baseUrl")).append("/oai</request>\n");
        return ret.toString();
    }

    public static String identify(HttpServletRequest req, String version) {
        StringBuilder ret = new StringBuilder();
        ret.append(headerOAI())
                .append(responseDateTag())
                .append(requestTag(req, version))
                .append(Options.getInstance().getOAIIdentify())
                .append("</OAI-PMH>");
        return ret.toString();
    }

    public static String listSets(HttpServletRequest req, String version) {
        StringBuilder ret = new StringBuilder();
        ret.append(headerOAI())
                .append(responseDateTag())
                .append(requestTag(req, version))
                .append(Options.getInstance().getOAIListSets())
                .append("</OAI-PMH>");
        return ret.toString();
    }

    public static String metadataFormats(HttpServletRequest req, String version) {
        try {
            String prefix = Options.getInstance().getJSONObject("OAI").getString("baseUrl") + "/id/";
            String identifier = req.getParameter("identifier");
            if (identifier != null) {
                if (identifier.length() < prefix.length()) {
                    return idDoesNotExist(req, version);
                }
                String id = identifier.substring(prefix.length());
                SolrQuery query = new SolrQuery("*")
                        .addFilterQuery("ident_cely:\"" + id + "\"");
                QueryResponse resp = IndexUtils.getClientBin().query("oai", query);

                if (resp.getResults().getNumFound() == 0) {
                    return idDoesNotExist(req, version);
                }
            }

            StringBuilder ret = new StringBuilder();
            ret.append(headerOAI())
                    .append(responseDateTag())
                    .append(requestTag(req, version))
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
            data.put("expiration", d);
            IndexUtils.getClientBin().add("work", idoc);
            IndexUtils.getClientBin().commit("work");
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
            QueryResponse resp = IndexUtils.getClientBin().query("work", query);
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

    private static String idDoesNotExist(HttpServletRequest req, String version) {
        return OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                + "<request>" + Options.getInstance().getJSONObject("OAI").getString("baseUrl") + "</request>"
                + "<error code=\"idDoesNotExist\" />"
                + "</OAI-PMH>";
    }

    private static String noRecordsMatch(HttpServletRequest req, String version) {
        return OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                + "<request>" + Options.getInstance().getJSONObject("OAI").getString("baseUrl") + "</request>"
                + "<error code=\"noRecordsMatch\" />"
                + "</OAI-PMH>";
    }

    private static String badArgument(HttpServletRequest req, String version) {
        return OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                + "<request>" + Options.getInstance().getJSONObject("OAI").getString("baseUrl") + "</request>"
                + "<error code=\"badArgument\">Invalid arguments</error>"
                + "</OAI-PMH>";
    }

    private static String badArgument(HttpServletRequest req, String msg, String version) {
        return OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                + "<request>" + Options.getInstance().getJSONObject("OAI").getString("baseUrl") + "</request>"
                + "<error code=\"badArgument\">" + msg + "</error>"
                + "</OAI-PMH>";
    }

    private static boolean validSet(String set) {
        if ("akce".equals(set) || "lokalita".equals(set) || "knihovna_3d".equals(set)) {
            return false;
        }
        return Options.getInstance().getJSONObject("OAI").getJSONArray("sets").toList().contains(set);
    }

    public static String listRecords(HttpServletRequest req, boolean onlyIdentifiers, String version) {
        List<String> validParams = List.of("verb", "resumptionToken", "metadataPrefix", "from", "until", "set");
        List<String> params = Collections.list(req.getParameterNames());

        for (String name : params) {
            if (validParams.indexOf(name) < 0) {
                return badArgument(req, version);
            }
        }

        for (String name : validParams) {
            if (req.getParameter(name) != null && req.getParameterValues(name).length > 1) {
                return badArgument(req, version);
            }
            if (req.getParameter(name) != null && "".equals(req.getParameter(name))) {
                return badArgument(req, version);
            }
        }

        String resumptionToken = req.getParameter("resumptionToken");
        String metadataPrefix = req.getParameter("metadataPrefix");
        if (resumptionToken != null) {

            if (req.getParameterMap().size() > 2) {
                return badArgument(req, version);
            }

            JSONObject solrRt = retrieveResumptionToken(resumptionToken);
            if (solrRt != null) {
                // Build query with info in resumptionToken
                metadataPrefix = solrRt.getString("metadataPrefix");
            } else {
                String xml = OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                        + "<request>" + Options.getInstance().getJSONObject("OAI").getString("baseUrl") + "</request>"
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
            return badArgument(req, "multiple metadataPrefixes");
        }

        List<Object> metadataPrefixes = Options.getInstance().getJSONObject("OAI").getJSONArray("metadataPrefixes").toList();
        if (resumptionToken == null && !metadataPrefixes.contains(metadataPrefix)) {
            String xml = OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                    + "<request>" + Options.getInstance().getJSONObject("OAI").getString("baseUrl") + "</request>"
                    + "<error code=\"cannotDisseminateFormat\"/>"
                    + "</OAI-PMH>";
            return xml;
        }

        StringBuilder ret = new StringBuilder();
        JSONObject conf = Options.getInstance().getJSONObject("OAI");
        ret.append(headerOAI())
                .append(responseDateTag())
                .append(requestTag(req, version));
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
                model = model + "\\:*";
            } else if (model.equals("dokument")) {
                model = model + "*";
            } else if (!validSet(model)) {
                return badArgument(req, "Invalid set");
            } else {
                model = ClientUtils.escapeQueryChars(model);
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
                        until = solrRt.getString("until");
                    }

                } else {
                    String xml = OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                            + "<request>" + Options.getInstance().getJSONObject("OAI").getString("baseUrl") + "</request>"
                            + "<error code=\"badResumptionToken\"/>"
                            + "</OAI-PMH>";
                    return xml;
                }
            }

            query.addFilterQuery("model:"
                    + model);

            if (from != null && until != null && !"*".equals(from) && !"*".equals(until)) {
                if (from.length() != until.length()) {
                    return badArgument(req, "The request has different granularities for the from and until parameters.");
                }
            }
            if (from != null || until != null) {
                if (from == null || from.isBlank()) {
                    from = "*";
                } else if (from.length() < 11 && !"*".equals(from)) {
                    from = from + "T00:00:00Z";
                }
                if (until == null || until.isBlank()) {
                    until = "*";
                } else if (until.length() < 11 && !"*".equals(until)) {
                    until = until + "T23:59:59Z";
                }
                if (from.length() != until.length()) {

                }
                query.addFilterQuery("datestamp:[" + from + " TO " + until + "]");
            }
            query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursor);
            QueryResponse resp = IndexUtils.getClientBin().query("oai", query);

            SolrDocumentList docs = resp.getResults();
            if (docs.getNumFound() == 0) {
                return noRecordsMatch(req, version);
            }
            for (SolrDocument doc : docs) {
                appendRecord(ret, doc, req, onlyIdentifiers, metadataPrefix, version);
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
                        .append("expirationDate=\"")
                        .append(rt.getString("expiration"))
                        .append("\" ")
                        .append(oaiCursor)
                        .append(">")
                        .append(md5Hex)
                        .append("</resumptionToken>");

            }

        } catch (IllegalArgumentException ex) {
            Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
            return badArgument(req, version);
        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
            return badArgument(req, version);
        } catch (Exception ex) {
            Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
            return badArgument(req, version);
        }
        if (onlyIdentifiers) {
            ret.append("</ListIdentifiers>");
        } else {
            ret.append("</ListRecords>");
        }
        ret.append("</OAI-PMH>");
        return ret.toString();
    }

    public static String getRecord(HttpServletRequest req, String version) {

        List<String> validParams = List.of("verb", "metadataPrefix", "identifier");
        List<String> params = Collections.list(req.getParameterNames());

        for (String name : params) {
            if (validParams.indexOf(name) < 0) {
                return badArgument(req, version);
            }
        }

        String metadataPrefix = req.getParameter("metadataPrefix");
        if (metadataPrefix == null) {
            return badArgument(req, "metadataPrefix is missing");
        }
        if (req.getParameter("identifier") == null) {
            return badArgument(req, "identifier is missing");
        }
        String[] reqMetadataPrefixes = req.getParameterValues("metadataPrefix");
        if (reqMetadataPrefixes.length > 1) {
            return badArgument(req, "multiple metadataPrefixes");
        }
        List<Object> metadataPrefixes = Options.getInstance().getJSONObject("OAI").getJSONArray("metadataPrefixes").toList();
        if (!metadataPrefixes.contains(metadataPrefix)) {
            String xml = OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                    + "<request>" + Options.getInstance().getJSONObject("OAI").getString("baseUrl") + "</request>"
                    + "<error code=\"cannotDisseminateFormat\"/>"
                    + "</OAI-PMH>";
            return xml;
        }
        StringBuilder ret = new StringBuilder();
        ret.append(headerOAI())
                .append(responseDateTag())
                .append(requestTag(req, version));

        ret.append("<GetRecord>\n");
        try {
            String prefix = Options.getInstance().getJSONObject("OAI").getString("baseUrl") + "/id/";
            if (req.getParameter("identifier").length() < prefix.length()) {
                return idDoesNotExist(req, version);
            }
            String id = req.getParameter("identifier").substring(prefix.length());
            SolrQuery query = new SolrQuery("*")
                    .addFilterQuery("ident_cely:\"" + id + "\"");
            QueryResponse resp = IndexUtils.getClientBin().query("oai", query);

            if (resp.getResults().getNumFound() == 0) {
                return idDoesNotExist(req, version);
            }
            SolrDocument doc = resp.getResults().get(0);

            appendRecord(ret, doc, req, false, metadataPrefix, version);
        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
            return badArgument(req, version);
        } catch (Exception ex) {
            Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
            return badArgument(req, version);
        }
        ret.append("</GetRecord>\n</OAI-PMH>");
        return ret.toString();
    }

    private static void appendRecord(StringBuilder ret, SolrDocument doc, HttpServletRequest req, boolean onlyIdentifiers, String metadataPrefix, String version) {
        String id = (String) doc.getFieldValue("ident_cely");
        Date datestamp = (Date) doc.getFieldValue("datestamp");
        boolean isDeleted = false;
        if (doc.containsKey("is_deleted")) {
            isDeleted = (boolean) doc.getFieldValue("is_deleted");
        }
        String status = isDeleted ? " status=\"deleted\" " : "";

        String model = (String) doc.getFieldValue("model");
        if ("akce".equals(model) || "lokalita".equals(model)) {
            model = "archeologicky_zaznam:" + model;
        }
        ret.append("<record>");
        ret.append("<header").append(status).append(">\n")
                .append("<identifier>")
                .append(Options.getInstance().getJSONObject("OAI").getString("baseUrl"))
                .append("/id/")
                .append(id)
                .append("</identifier>\n")
                .append("<datestamp>")
                .append(datestamp.toInstant().toString())
                .append("</datestamp>\n");
        if (model != null) {
            ret.append("<setSpec>")
                    .append(model)
                    .append("</setSpec>\n");
        }

        // <setSpec>projekt</setSpec> <!-- "projekt" | "archeologicky_zaznam" | "let" | "adb" | "dokument" | "ext_zdroj" | "pian" | "samostatny_nalez" | "uzivatel" | "heslo" | "ruian_kraj" | "ruian_okres" | "ruian_katastr" | "organizace | "osoba -->
        ret.append("</header>\n");

        if (!onlyIdentifiers && !isDeleted) {

            ret.append("<metadata>\n");
            String xml = filter(req, doc);
            int pos1 = xml.indexOf("xmlns:amcr=");
            int pos2 = xml.indexOf("\"", pos1 + 14);
            String xmlns_amcr = xml.substring(pos1 + "xmlns:amcr=\"".length(), pos2);

            if (shoulTransformVersion(version, xmlns_amcr)) {
                try {
                    xml = transformByVersion(xml, version);
                } catch (TransformerException ex) {
                    Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (!xml.equals(ERROR_404_MSG) && "oai_dc".equals(metadataPrefix)) {
                try {
                    xml = transformToDC(xml, xmlns_amcr);
                } catch (TransformerException ex) {
                    Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            ret.append(xml);
            ret.append("</metadata>\n");
        }
        ret.append("</record>\n");
    }

    private static final String ERROR_404_MSG = "<amcr:amcr xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:gml=\"https://www.opengis.net/gml/3.2\" xmlns:amcr=\"https://api.aiscr.cz/schema/amcr/2.0/\" xsi:schemaLocation=\"https://api.aiscr.cz/schema/amcr/2.0/ https://api.aiscr.cz/schema/amcr/2.0/amcr.xsd http://www.opengis.net/gml/3.2 http://schemas.opengis.net/gml/3.2.1/gml.xsd\">\n"
            + "    HTTP/1.1 403 Forbidden\n"
            + "  </amcr:amcr>";

    private static String filter(HttpServletRequest req, SolrDocument doc) {
        // LoginServlet.organizace(req.getSession())
        String userPristupnost = LoginServlet.user(req).optString("pristupnost", "A");
        String userOrg = LoginServlet.organizace(req.getSession());

        String docPristupnost = (String) doc.getFieldValue("pristupnost");
        String projektOrg = (String) doc.getFirstValue("organizace");
        String model = (String) doc.getFieldValue("model");

        FedoraModel fm = FedoraModel.getFedoraModel(model);
        String xml = (String) doc.getFieldValue("xml");
        if (fm.filterOAI(LoginServlet.user(req), doc)) {
            long stav = 0;
            if (doc.containsKey("stav")) {
                stav = (long) doc.getFieldValue("stav");
            }
            String ret = xml;
            if (ret.contains("<amcr:oznamovatel>")
                    && ("C".compareToIgnoreCase(userPristupnost) > 0
                    || // A-B
                    ("C".equalsIgnoreCase(userPristupnost) && !(stav == 1 || userOrg.equals(projektOrg))))) {
// M-202204636
//projekt/oznamovatel
//-- A-B: nikdy
//-- C: stav = 1 OR projekt/organizace = {user}.organizace
//-- D-E: bez omezení
                while (ret.contains("<amcr:oznamovatel")) {
                    int pos1 = ret.indexOf("<amcr:oznamovatel>");
                    String s = ret.substring(0, pos1) + "<####>HTTP/1.1 403 Forbidden</####>";
                    int pos2 = ret.lastIndexOf("</amcr:oznamovatel>");
                    s += ret.substring(pos2 + "</amcr:oznamovatel>".length());
                    ret = s;
                }
                ret = ret.replaceAll("####", "amcr:oznamovatel");
            }

            if (ret.contains("<amcr:chranene_udaje>") && docPristupnost.compareToIgnoreCase(userPristupnost) > 0) {

                while (ret.contains("<amcr:chranene_udaje>")) {
                    int pos1 = ret.indexOf("<amcr:chranene_udaje>");
                    String s = ret.substring(0, pos1) + "<####>HTTP/1.1 403 Forbidden</####>";
                    int pos2 = ret.indexOf("</amcr:chranene_udaje>");
                    s += ret.substring(pos2 + "</amcr:chranene_udaje>".length());
                    ret = s;
                }
                return ret.replaceAll("####", "amcr:chranene_udaje");
            } else {
                return ret;
            }
        } else {
            //return ERROR_404_MSG;
            try {
                return transformToForbidden(xml);
            } catch (TransformerException ex) {
                Logger.getLogger(OAIRequest.class.getName()).log(Level.SEVERE, null, ex);
                return ERROR_404_MSG;
            }
        }

    }

    private static String transformToForbidden(String xml) throws TransformerException {

        Source text = new StreamSource(new StringReader(xml));
        StringWriter sw = new StringWriter();
        get404Transformer().transform(text, new StreamResult(sw));
        String e404 = sw.toString();
        return e404;
    }

    private static boolean shoulTransformVersion(String version, String xmlns_amcr ) {
        
        return (("/v2".equals(version) && "https://api.aiscr.cz/schema/amcr/2.1/".equals(xmlns_amcr)) 
                || ("/v2.1".equals(version) && "https://api.aiscr.cz/schema/amcr/2.0/".equals(xmlns_amcr)) );
    }

    private static String transformByVersion(String xml, String version) throws TransformerException {

        Source text = new SAXSource(new InputSource(new StringReader(xml)));
        StringWriter sw = new StringWriter();
        getVersionTransformer(version).transform(text, new StreamResult(sw));
        return sw.toString();
    }

    private static String transformToDC(String xml, String xmlns_amcr) throws TransformerException {

        Source text = new StreamSource(new StringReader(xml));
        StringWriter sw = new StringWriter();
                    
        getDcTransformer(xmlns_amcr).transform(text, new StreamResult(sw));

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
