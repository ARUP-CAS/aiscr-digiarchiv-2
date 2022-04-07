/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.incad.arup.thumbnailsgenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
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
    } catch (JSONException | IOException ex) {
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

  public JSONObject createForUsed(boolean overwrite, boolean onlyThumbs) throws IOException {
    Date start = new Date();
    totalDocs = 0;

    try {
      File file = new File(Options.getInstance().getString("thumbsDir") + File.separator + "skipped.txt");
      FileUtils.writeStringToFile(file, "Create thums started at " + start.toString() + System.getProperty("line.separator"), "UTF-8", true);
      dokumentClient = getClient("entities/");
      String sort = "path";
      int rows = 200;
      SolrQuery query = new SolrQuery();
      query.addFilterQuery("entity:dokument");
      query.setQuery("soubor:[* TO *]");
      query.setFields("soubor");
      query.set("wt", "json");
      query.setRows(rows);
      query.setSort(SolrQuery.SortClause.asc(sort));
      query.setTimeAllowed(0);

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
          jo.put("ellapsed time", FormatUtils.formatInterval(end.getTime() - start.getTime()));
          return jo;

        }

        SolrDocumentList docs = rsp.getResults();
        for (SolrDocument doc : docs) {
          JSONArray ja = new JSONArray(doc.getFirstValue("soubor").toString());
          for (int i = 0; i < ja.length(); i++) {
            JSONObject json = ja.getJSONObject(i);
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

  private void createThumbFromSolrDoc(SolrDocument doc, boolean overwrite, boolean force, boolean onlyThumbs) {

    String imagesDir = opts.getString("imagesDir");
    String nazev = doc.getFirstValue("nazev").toString();
    String path = doc.getFirstValue("filepath").toString();
    String mimetype = doc.getFirstValue("mimetype").toString();
    if (overwrite || !ImageSupport.thumbExists(path)) {
      //if (overwrite || !ImageSupport.folderExists(nazev)) {

      File f = new File(imagesDir + path);
      if (!f.exists()) {
        LOGGER.log(Level.FINE, "File {0} doesn't exists", f);
      } else {
        String msg = String.format("Currently Files processed: %1$d. Pdf thumbs: %2$d. Image thumbs: %3$d.",
                totalDocs, pdfGen.generated, imgGenerated);
        LOGGER.log(Level.INFO, "processing file {0}. {1}", new Object[]{f, msg});
        if ("application/pdf".equals(mimetype)) {
          pdfGen.processFile(f, force, onlyThumbs);
//                            ImageSupport.thumbnailPdfPage(f, 0, nazev);
//                            ImageSupport.mediumPdf(f, nazev);
        } else {
          ImageSupport.thumbnailzeImg(f, path, onlyThumbs);
          imgGenerated++;
        }
      }
    }
    totalDocs++;
  }

  private void createThumbFromJSON(JSONObject json, boolean overwrite, boolean force, boolean onlyThumbs) {

    String imagesDir = opts.getString("imagesDir");
    String nazev = json.getJSONArray("nazev").getString(0);
    String path = json.getJSONArray("filepath").getString(0);
    String mimetype = json.getJSONArray("mimetype").getString(0);
    if (overwrite || !ImageSupport.thumbExists(path)) {
      //if (overwrite || !ImageSupport.folderExists(nazev)) {

      File f = new File(imagesDir + path);
      if (!f.exists()) {
        LOGGER.log(Level.FINE, "File {0} doesn't exists", f);
      } else {
        String msg = String.format("Currently Files processed: %1$d. Pdf thumbs: %2$d. Image thumbs: %3$d.",
                totalDocs, pdfGen.generated, imgGenerated);
        LOGGER.log(Level.INFO, "processing file {0}. {1}", new Object[]{f, msg});
        if ("application/pdf".equals(mimetype)) {
          pdfGen.processFile(f, force, onlyThumbs);
//                            ImageSupport.thumbnailPdfPage(f, 0, nazev);
//                            ImageSupport.mediumPdf(f, nazev);
        } else {
          ImageSupport.thumbnailzeImg(f, path, onlyThumbs);
          imgGenerated++;
        }
      }
    }
    totalDocs++;
  }

  public void createThumb(String nazev, boolean onlySmall, boolean force, boolean onlyThumbs) {
    try {

      relationsClient = getClient("soubor/");
      SolrQuery query = new SolrQuery();
      //query.setRequestHandler(core);
      query.setQuery("nazev:\"" + nazev + "\"");

      Options opts = Options.getInstance();

      SolrDocumentList docs = relationsClient.query(query).getResults();
      if (docs.getNumFound() == 0) {
        LOGGER.log(Level.WARNING, "{0} not found", nazev);
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
      String imagesDir = opts.getString("imagesDir");
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
      query.setQuery("filepath:\"" + filepath + "\"");

      exists = relationsClient.query(query).getResults().getNumFound() > 0;

    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    return exists;
  }

  public void checkDirs(boolean remove) {
    try {
      relationsClient = getClient("soubor/");
      Options opts = Options.getInstance();
      String thumbsDir = opts.getString("thumbsDir");
      File file = new File(thumbsDir + File.separator + "toberemoved." + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)+ ".txt");
      Path path = Paths.get(thumbsDir);
      List<Path> paths = listFiles(path);
      int total = 0;
      for (Path x: paths) {
        String fileName = x.getFileName().toString().replace("_thumb.jpg", "");
        boolean exists = existsInIndex(fileName);
        // System.out.println(x.toString().replace("_thumb.jpg", "") + " -> " + exists);
        if (!exists) {
          try {
            // System.out.println(x.getParent());
            // String dirName = x.getParent().toString();
            if (remove) {
              FileUtils.delete(x.toFile());
              FileUtils.delete(new File(x.toString().replace("_thumb.jpg", "_medium.jpg")));
              if (fileName.endsWith(".pdf")) {
                FileUtils.deleteDirectory(new File(x.toString().replace("_thumb.jpg", "")));
              }
            }
            total++;
            FileUtils.writeStringToFile(file, x.toString() + System.getProperty("line.separator"), "UTF-8", true);
            FileUtils.writeStringToFile(file, x.toString().replace("_thumb.jpg", "_medium.jpg") + System.getProperty("line.separator"), "UTF-8", true);
            if (fileName.endsWith(".pdf")) {
              FileUtils.writeStringToFile(file, x.toString().replace("_thumb.jpg", "") + System.getProperty("line.separator"), "UTF-8", true);
            }
          } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
          }
        }
        
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
      result = walk.filter(Files::isRegularFile)
              .filter(p -> p.getFileName().toString().endsWith("_thumb.jpg"))
              .collect(Collectors.toList());
    }
    return result;

  }

}
