/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.incad.arup.thumbnailsgenerator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class Indexer {

    public static final Logger LOGGER = Logger.getLogger(Indexer.class.getName());

    private final String DEFAULT_HOST = "http://localhost:8983/solr/";

    Options opts;
    PDFThumbsGenerator pdfGen;
    int imgGenerated;
    int totalDocs = 0;

    SolrClient dokumentClient;
    SolrClient exportClient;
    SolrClient relationsClient;
    DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("YYYY-MM-dd'T'hh:mm:ss'Z'")
            .withZone(ZoneId.systemDefault());

    public Indexer(boolean forced) {

        try {
            opts = Options.getInstance();

            pdfGen = new PDFThumbsGenerator(forced);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

    }

    public String host() {
        try {
            Options opts = Options.getInstance();
            return opts.getString("solrhost", DEFAULT_HOST);
        } catch (JSONException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return DEFAULT_HOST;
    }

    public SolrClient getClient(String core) throws IOException {
        LOGGER.log(Level.INFO, "Getting solr client at {0} ", String.format("%s%s",
                host(),
                core));
        SolrClient server = new HttpSolrClient.Builder(String.format("%s%s",
                host(),
                core)).build();
        return server;
    }
    
    public JSONObject updateForEntities(boolean overwrite, boolean onlyThumbs) throws IOException, MalformedURLException, URISyntaxException, InterruptedException {
        String lastUpdate = readUpdateTime();
        String fq = "datestamp:[" + lastUpdate + " TO *]";
        return createForEntities(overwrite, onlyThumbs, fq);
    }
    
    public JSONObject createForEntities(boolean overwrite, boolean onlyThumbs, String fq) throws IOException, MalformedURLException, URISyntaxException, InterruptedException {
        String sort = "ident_cely";
        int rows = 200;
        SolrQuery query = new SolrQuery("*");
            // query.addFilterQuery("searchable:true");
            query.addFilterQuery("soubor_id:*");
            query.setFields("ident_cely,soubor");
            query.set("wt", "json");
            query.setRows(rows);
            query.setSort(SolrQuery.SortClause.asc(sort));
            
            if (fq != null) {
                query.addFilterQuery(fq);
            }
        return createFromEntities( query, overwrite, onlyThumbs, rows);
    }
    
    public JSONObject createForUsed(boolean overwrite, boolean onlyThumbs, String fq) throws IOException, MalformedURLException, URISyntaxException, InterruptedException {
        String sort = "ident_cely";
        int rows = 200;
        SolrQuery query = new SolrQuery("*");
            query.addFilterQuery("searchable:true");
            query.addFilterQuery("soubor_id:*");
            query.setFields("ident_cely,soubor");
            query.set("wt", "json");
            query.setRows(rows);
            query.setSort(SolrQuery.SortClause.asc(sort));
            
            if (fq != null) {
                query.addFilterQuery(fq);
            }
        return createFromEntities( query, overwrite, onlyThumbs, rows);
    }
    
    public JSONObject createForUnused(boolean overwrite, boolean onlyThumbs, String fq) throws IOException, MalformedURLException, URISyntaxException, InterruptedException {
        String sort = "ident_cely";
        int rows = 200;
        SolrQuery query = new SolrQuery("*");
            query.addFilterQuery("searchable:false");
            query.addFilterQuery("soubor_id:*");
            query.setFields("ident_cely,soubor");
            query.set("wt", "json");
            query.setRows(rows);
            query.setSort(SolrQuery.SortClause.asc(sort));
            
            if (fq != null) {
                query.addFilterQuery(fq);
            }
        return createFromEntities( query, overwrite, onlyThumbs, rows);
    }

    public JSONObject createFromEntities(SolrQuery query, boolean overwrite, boolean onlyThumbs, int rows) throws IOException, MalformedURLException, URISyntaxException, InterruptedException {
        Instant start = Instant.now();
        // Date start = new Date();
        totalDocs = 0;

        try {
            File file = new File(Options.getInstance().getString("thumbsDir") + File.separator + "skipped.txt");
            FileUtils.writeStringToFile(file, "Create thums started at " + start.toString() + System.getProperty("line.separator"), "UTF-8", true);
            dokumentClient = getClient("entities/");

            String cursorMark = CursorMarkParams.CURSOR_MARK_START;

            boolean done = false;
            QueryResponse rsp = null;

            while (!done) {
                query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
                try {
                    rsp = dokumentClient.query(query);
                } catch (SolrServerException e) {
                    LOGGER.log(Level.SEVERE, null, e);

                    Date end = new Date();

                    String msg = String.format("Generate thumbs finished with error. Thumbs :%1$d", totalDocs);
                    LOGGER.log(Level.INFO, msg);

                    JSONObject jo = new JSONObject();
                    jo.put("result", "error");
                    jo.put("error", e.toString());
                    jo.put("total docs", totalDocs);
                    jo.put("ellapsed time", FormatUtils.formatInterval(end.getTime() - start.toEpochMilli()));
                    return jo;
                }

                SolrDocumentList docs = rsp.getResults();
                
                for (SolrDocument doc : docs) {
                    JSONArray ja = new JSONArray(doc.getFieldValues("soubor"));
                    for (int i = 0; i < ja.length(); i++) {
                        JSONObject json = new JSONObject(ja.getString(i));
                        createThumbFromJSON(json, overwrite, false, onlyThumbs);
                    }
                }
                //totalDocs += rsp.getResults().size();
                LOGGER.log(Level.INFO, "Currently {0} files processed", totalDocs);

                String nextCursorMark = rsp.getNextCursorMark();
                if (cursorMark.equals(nextCursorMark) || rsp.getResults().size() < rows) {
                    done = true;
                } else {
                    cursorMark = nextCursorMark;
                }
            }

            Date end = new Date();
            String msg = String.format("Generate thumbs finished. Files processed: %1$d. Pdf thumbs: %2$d. Image thumbs: %3$d. Time: %4$tF",
                    totalDocs, pdfGen.generated, imgGenerated, end);
            FileUtils.writeStringToFile(file, msg + System.getProperty("line.separator"), "UTF-8", true);
            LOGGER.log(Level.INFO, msg);
            JSONObject jo = new JSONObject();
            jo.put("result", "Update success");
            jo.put("total thumbs", totalDocs);
            jo.put("ellapsed time", FormatUtils.formatInterval(end.getTime() - start.toEpochMilli()));
            dokumentClient.close();
            
            writeUpdateTime(start);
            return jo;

        } catch (IOException | JSONException ex) {
            LOGGER.log(Level.SEVERE, null, ex);

            Date end = new Date();

            String msg = String.format("Generate thumbs finished with errors. Thumbs: %1$d", totalDocs);
            File file = new File(Options.getInstance().getString("thumbsDir") + File.separator + "skipped.txt");
            FileUtils.writeStringToFile(file, msg + System.getProperty("line.separator"), "UTF-8", true);
            LOGGER.log(Level.INFO, msg);

            JSONObject jo = new JSONObject();
            jo.put("result", "error");
            jo.put("error", ex.toString());
            jo.put("total docs", totalDocs);
            jo.put("ellapsed time", FormatUtils.formatInterval(end.getTime() - start.toEpochMilli()));
            return jo;
        }
    }

    public JSONObject createForEntityRecord(String ident_cely) throws IOException, MalformedURLException, URISyntaxException, InterruptedException {
        Date start = new Date();
        totalDocs = 0;

        try {
            File file = new File(Options.getInstance().getString("thumbsDir") + File.separator + "skipped.txt");
            LOGGER.log(Level.INFO, Options.CONFIG_DIR);
            LOGGER.log(Level.INFO, Options.getInstance().getString("thumbsDir"));
            FileUtils.writeStringToFile(file, "Create thums started at " + start.toString() + System.getProperty("line.separator"), "UTF-8", true);
            dokumentClient = getClient("entities/");
            int rows = 200;
            SolrQuery query = new SolrQuery();
            // query.addFilterQuery("entity:dokument");
            query.setQuery("ident_cely:\"" + ident_cely + "\"");
            query.setFields("ident_cely,soubor");
            query.set("wt", "json");
            query.setRows(rows);

            QueryResponse rsp = null;

            try {
                rsp = dokumentClient.query(query);
            } catch (SolrServerException e) {
                LOGGER.log(Level.SEVERE, null, e);

                Date end = new Date();

                String msg = String.format("Generate thumbs finished with error. Thumbs :%1$d", totalDocs);
                LOGGER.log(Level.INFO, msg);

                JSONObject jo = new JSONObject();
                jo.put("result", "error");
                jo.put("error", e.toString());
                jo.put("total docs", totalDocs);
                jo.put("ellapsed time", FormatUtils.formatInterval(end.getTime() - start.getTime()));
                return jo;
            }

            SolrDocumentList docs = rsp.getResults();
            for (SolrDocument doc : docs) {
                JSONArray ja = new JSONArray(doc.getFieldValues("soubor"));
                for (int i = 0; i < ja.length(); i++) {
                    JSONObject json = new JSONObject(ja.getString(i));
                    createThumbFromJSON(json, true, false, false);
                }
            }
            //totalDocs += rsp.getResults().size();
            LOGGER.log(Level.INFO, "Currently {0} files processed", totalDocs);

            Date end = new Date();
            String msg = String.format("Generate thumbs finished. Files processed: %1$d. Pdf thumbs: %2$d. Image thumbs: %3$d. Time: %4$tF",
                    totalDocs, pdfGen.generated, imgGenerated, end);
            FileUtils.writeStringToFile(file, msg + System.getProperty("line.separator"), "UTF-8", true);
            LOGGER.log(Level.INFO, msg);
            JSONObject jo = new JSONObject();
            jo.put("result", "Update success");
            jo.put("total thumbs", totalDocs);
            jo.put("ellapsed time", FormatUtils.formatInterval(end.getTime() - start.getTime()));
            dokumentClient.close();
            return jo;

        } catch (IOException | JSONException ex) {
            LOGGER.log(Level.SEVERE, null, ex);

            Date end = new Date();

            String msg = String.format("Generate thumbs finished with errors. Thumbs: %1$d", totalDocs);
            File file = new File(Options.getInstance().getString("thumbsDir") + File.separator + "skipped.txt");
            FileUtils.writeStringToFile(file, msg + System.getProperty("line.separator"), "UTF-8", true);
            LOGGER.log(Level.INFO, msg);

            JSONObject jo = new JSONObject();
            jo.put("result", "error");
            jo.put("error", ex.toString());
            jo.put("total docs", totalDocs);
            jo.put("ellapsed time", FormatUtils.formatInterval(end.getTime() - start.getTime()));
            return jo;
        }
    }

    public JSONObject createThumbs(boolean overwrite, boolean onlyThumbs) throws IOException {
        return createThumbs(overwrite, onlyThumbs, null);
    }

    public JSONObject createThumbs(boolean overwrite, boolean onlyThumbs, String fq) throws IOException {
        Date start = new Date();
        totalDocs = 0;

        try {
            File file = new File(Options.getInstance().getString("thumbsDir") + File.separator + "skipped.txt");
            FileUtils.writeStringToFile(file, "Create thums started at " + start.toString() + System.getProperty("line.separator"), "UTF-8", true);
            relationsClient = getClient("soubor/");
            String sort = "path";
            int rows = 200;
            SolrQuery query = new SolrQuery("*");
            // query.addFilterQuery("dokument:[* TO *]");
            query.addFilterQuery("-(dokument:\"\" AND samostatny_nalez:\"\")");
            query.addFilterQuery("-dokument:X*");
            query.addFilterQuery("-dokument:ZA*");
            query.addFilterQuery("-dokument:ZL*");
            if (fq != null) {
                query.addFilterQuery(fq);
            }
            query.setRows(rows);
            query.setSort("path", SolrQuery.ORDER.asc);
            // query.setTimeAllowed(0);

            String cursorMark = CursorMarkParams.CURSOR_MARK_START;

            boolean done = false;
            QueryResponse rsp = null;
            LOGGER.log(Level.INFO, "Searching soubor with query {0}", query.toQueryString());

            while (!done) {
                query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
                try {
                    rsp = relationsClient.query(query);
                    LOGGER.log(Level.INFO, "Core soubor has {0} records", rsp.getResults().getNumFound());
                } catch (SolrServerException e) {
                    LOGGER.log(Level.SEVERE, null, e);

                    Date end = new Date();

                    String msg = String.format("Generate thumbs finished with error. Thumbs :%1$d", totalDocs);
                    LOGGER.log(Level.INFO, msg);

                    JSONObject jo = new JSONObject();
                    jo.put("result", "error");
                    jo.put("error", e.toString());
                    jo.put("total docs", totalDocs);
                    jo.put("ellapsed time", FormatUtils.formatInterval(end.getTime() - start.getTime()));
                    return jo;

                }

                createThumbs(rsp.getResults(), overwrite, false, onlyThumbs);
                //totalDocs += rsp.getResults().size();
                LOGGER.log(Level.INFO, "Currently {0} files processed", totalDocs);

                String nextCursorMark = rsp.getNextCursorMark();
                if (cursorMark.equals(nextCursorMark) || rsp.getResults().size() < rows) {
                    done = true;
                } else {
                    cursorMark = nextCursorMark;
                }
            }

            Date end = new Date();
            String msg = String.format("Generate thumbs finished. Files processed: %1$d. Pdf thumbs: %2$d. Image thumbs: %3$d. Time: %4$tF",
                    totalDocs, pdfGen.generated, imgGenerated, end);
            FileUtils.writeStringToFile(file, msg + System.getProperty("line.separator"), "UTF-8", true);
            LOGGER.log(Level.INFO, msg);
            JSONObject jo = new JSONObject();
            jo.put("result", "Update success");
            jo.put("total thumbs", totalDocs);
            jo.put("ellapsed time", FormatUtils.formatInterval(end.getTime() - start.getTime()));
            relationsClient.close();
            return jo;

        } catch (IOException | JSONException ex) {
            LOGGER.log(Level.SEVERE, null, ex);

            Date end = new Date();

            String msg = String.format("Generate thumbs finished with errors. Thumbs: %1$d", totalDocs);
            File file = new File(Options.getInstance().getString("thumbsDir") + File.separator + "skipped.txt");
            FileUtils.writeStringToFile(file, msg + System.getProperty("line.separator"), "UTF-8", true);
            LOGGER.log(Level.INFO, msg);

            JSONObject jo = new JSONObject();
            jo.put("result", "error");
            jo.put("error", ex.toString());
            jo.put("total docs", totalDocs);
            jo.put("ellapsed time", FormatUtils.formatInterval(end.getTime() - start.getTime()));
            return jo;
        }
    }

    private void createThumbFromSolrDoc(SolrDocument doc, boolean overwrite, boolean force, boolean onlyThumbs) throws MalformedURLException, IOException, URISyntaxException, InterruptedException {

        String path = doc.getFirstValue("nazev").toString();
        // String path = doc.getFirstValue("path").toString();
        String url = doc.getFirstValue("path").toString() + "/orig";
        url = url.substring(url.indexOf("record"));

        String mimetype = doc.getFirstValue("mimetype").toString();
        if (overwrite || !ImageSupport.thumbExists(path)) {
            // File f = FedoraUtils.requestFile(url, imagesDir).toFile();
            // InputStream is = FedoraUtils.requestInputStream(url);
            byte[] is = FedoraUtils.requestBytes(url);
            //if (!f.exists()) {
            //   LOGGER.log(Level.FINE, "File {0} doesn't exists", path);
            if (is == null) {
                LOGGER.log(Level.FINE, "File {0} doesn't exists", path);
            } else {
                String msg = String.format("Currently Files processed: %1$d. Pdf thumbs: %2$d. Image thumbs: %3$d.",
                        totalDocs, pdfGen.generated, imgGenerated);
                LOGGER.log(Level.INFO, "processing file {0}. {1}", new Object[]{path, msg});
                if ("application/pdf".equals(mimetype)) {
                    // pdfGen.processFile(f, force, onlyThumbs);
                    pdfGen.processBytes(is, path, force, onlyThumbs);
                    // pdfGen.processInputStream(is, path, force, onlyThumbs);
                } else {
                    // ImageSupport.thumbnailzeImg(is, path, onlyThumbs);
                    imgGenerated++;
                }
            }
        }
        totalDocs++;
    }

    private void createThumbFromJSON(JSONObject json, boolean overwrite, boolean force, boolean onlyThumbs) throws MalformedURLException, IOException, URISyntaxException, InterruptedException {

        String path = json.getString("path");
        String id = json.getString("id");
        String sha = json.optString("sha_512");
        String url = path + "/orig";
        url = url.substring(url.indexOf("record"));
        String mimetype = json.getString("mimetype");
        if ("application/pdf".equals(mimetype)) {
            if (overwrite || ImageSupport.shaChanged(id, sha)) {
                byte[] is = FedoraUtils.requestBytes(url);
                if (is == null) {
                    LOGGER.log(Level.INFO, "File {0} doesn't exists", path);
                } else {
                    String msg = String.format("Currently Files processed: %1$d. Pdf thumbs: %2$d. Image thumbs: %3$d.",
                            totalDocs, pdfGen.generated, imgGenerated);
                    LOGGER.log(Level.INFO, "processing file {0}. {1}", new Object[]{id, msg});
                    // pdfGen.processFile(f, force, onlyThumbs);
                    pdfGen.processBytes(is, id, true, onlyThumbs);
                    ImageSupport.writeSha(id, sha);
                }
            }
        } else {
            // ImageSupport.thumbnailzeImg(is, path, onlyThumbs);
            imgGenerated++;
        }
        totalDocs++;
    }

    public void createThumb(String id, boolean onlySmall, boolean force, boolean onlyThumbs) {
        try {

            relationsClient = getClient("soubor/");
            SolrQuery query = new SolrQuery();
            //query.setRequestHandler(core);
            // query.setQuery("nazev:\"" + nazev + "\"");
            query.setQuery("id:\"" + id + "\"");

            Options opts = Options.getInstance();

            SolrDocumentList docs = relationsClient.query(query).getResults();
            if (docs.getNumFound() == 0) {
                LOGGER.log(Level.WARNING, "{0} not found", id);
                return;
            }
            SolrDocument doc = docs.get(0);
            createThumbFromSolrDoc(doc, true, force, onlyThumbs);
            relationsClient.close();

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void createThumbs(SolrDocumentList docs, boolean overwrite, boolean force, boolean onlyThumbs) {
        try {
            Options opts = Options.getInstance();
            //ImageSupport.initCount();

            for (SolrDocument doc : docs) {
                createThumbFromSolrDoc(doc, overwrite, force, onlyThumbs);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public boolean existsInIndex(String filepath) {
        boolean exists = true;
        try {

            SolrQuery query = new SolrQuery();
            //query.setRequestHandler(core);
            query.setQuery("path:\"" + filepath + "\"");

            exists = relationsClient.query(query).getResults().getNumFound() > 0;

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return exists;
    }

    public void checkFolder(String folder, boolean remove) {
        try {
            relationsClient = getClient("soubor/");
            Options opts = Options.getInstance();
            //String thumbsDir = opts.getString("thumbsDir");

            boolean existsInIndex = existsInIndex(folder);
            boolean existsInDisk = new File(ImageSupport.getDestDir(folder)).exists();

            System.out.println("Folder exists: " + existsInDisk);
            System.out.println("Exists in index: " + existsInIndex);
            relationsClient.close();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

    }

    public void checkDirs(boolean remove) {
        try {
            relationsClient = getClient("soubor/");
            Options opts = Options.getInstance();
            String thumbsDir = opts.getString("thumbsDir");
            File file = new File(thumbsDir + File.separator + "toberemoved." + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".txt");
            FileUtils.writeStringToFile(file, "Report: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    + System.getProperty("line.separator") + System.getProperty("line.separator"), "UTF-8", false);
            Path path = Paths.get(thumbsDir);

            int total = 0;
            //File file2 = new File(thumbsDir + File.separator + "toberemoved2." + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".txt");

            List<Path> paths = listFiles(path);

            for (Path x : paths) {
                String fileName = x.getFileName().toString().replace("_thumb.jpg", "");
                boolean exists = existsInIndex(fileName);
                // System.out.println(x.toString().replace("_thumb.jpg", "") + " -> " + exists);
                if (!exists) {
                    try {
                        File medium = new File(x.toString().replace("_thumb.jpg", "_medium.jpg"));
                        if (remove) {
                            FileUtils.delete(x.toFile());
                            if (medium.exists()) {
                                FileUtils.delete(medium);
                            }
                        }
                        FileUtils.writeStringToFile(file, x.toString() + System.getProperty("line.separator"), "UTF-8", true);
                        FileUtils.writeStringToFile(file, medium.getName() + System.getProperty("line.separator"), "UTF-8", true);
                        total++;
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                }

            }

            List<Path> pdfs = listPdfFiles(path);
            for (Path x : pdfs) {
                String fileName = x.getFileName().toString();
                boolean exists = existsInIndex(fileName);
                if (!exists) {
                    try {
                        if (remove) {
                            FileUtils.deleteQuietly(x.toFile());
                        }
                        total++;
                        FileUtils.writeStringToFile(file, fileName + System.getProperty("line.separator"), "UTF-8", true);
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                }
            }

            if (remove) {
                deleteEmptyDirs(path);
            }

            System.out.println("Total files found: " + paths.size());
            System.out.println("Total dirs to remove: " + total);
            System.out.println("Files to be removed in " + file.getAbsolutePath());
            relationsClient.close();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private List<Path> listFiles(Path path) throws IOException {
        List<Path> result;
        try (Stream<Path> walk = Files.walk(path)) {
            result = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith("_thumb.jpg"))
                    .collect(Collectors.toList());
        }
        return result;

    }

    private List<Path> listPdfFiles(Path path) throws IOException {
        List<Path> result;
        try (Stream<Path> walk = Files.walk(path)) {
            result = walk
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().endsWith(".pdf"))
                    .collect(Collectors.toList());
        }
        return result;

    }

    private List<Path> listNonEmptyDirectories(Path path) throws IOException {
        List<Path> result;

        try (Stream<Path> walk = Files.walk(path)) {
            result = walk.filter(Files::isDirectory)
                    .filter(p -> (!FileUtils.listFiles(p.toFile(), new String[]{"jpg"}, false).isEmpty())
                    && p.getFileName().toString().endsWith(".pdf"))
                    .collect(Collectors.toList());
        }
        return result;

    }

    private void deleteEmptyDirs(Path path) throws IOException {

        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .filter(File::isDirectory)
                .filter(p -> {
                    try {
                        return FileUtils.isEmptyDirectory(p);
                    } catch (IOException ex) {
                        Logger.getLogger(Indexer.class.getName()).log(Level.SEVERE, null, ex);
                        return false;
                    }
                })
                .forEach(File::delete);

    }
    
    private String readUpdateTime() {

    try {
      File file = new File(opts.getString("thumbsDir") + File.separator + "update.txt");
      if (file.exists()) {
        return FileUtils.readFileToString(file, "UTF-8");
      } else {
        return "*";
      }
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return "*";
    }
  }

  private void writeUpdateTime(Instant date) {
    try {
        
        
      File file = new File(opts.getString("thumbsDir") + File.separator + "update.txt");
      FileUtils.writeStringToFile(file, formatter.format(date), "UTF-8", false);
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

}
