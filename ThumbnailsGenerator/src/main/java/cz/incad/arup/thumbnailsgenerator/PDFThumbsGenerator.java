/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.incad.arup.thumbnailsgenerator;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.json.JSONException;

/**
 *
 * @author alberto
 */
public class PDFThumbsGenerator {

  public static final Logger LOGGER = Logger.getLogger(PDFThumbsGenerator.class.getName());

  public int generated;
  Options opts;
  int maxPixels;
  int maxMedium;

  int t_width;
  int t_height;

  List<String> unprocessables;

  public PDFThumbsGenerator(boolean forced) {

    try {
      opts = Options.getInstance();
      maxPixels = opts.getInt("maxPixels", 2000 * 2000);
      maxMedium = opts.getInt("mediumHeight", 1000);

      t_width = opts.getInt("thumbWidth", 100);
      t_height = opts.getInt("thumbHeight", 100);

      unprocessables = readUnprocessable();

      //Test if the file was last processed before crash;
      String lastProcessed = readProcessing();
      if (!forced) {
        if (!"".equals(lastProcessed) && !unprocessables.contains(lastProcessed)) {
          LOGGER.log(Level.INFO, "Last attemp to generate file {0} failed. Writing to unprocessables.txt", lastProcessed);
          writeUnprocessable(lastProcessed);
          writeProcessing("");
          return;
        }
      }

      generated = 0;
    } catch (JSONException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  public void processFile(File f, boolean force, boolean onlyThumbs) {

    if (!force) {
      //Test if the file was last processed before crash;
      String lastProcessed = readProcessing();
      if (f.getName().equals(lastProcessed)) {
        LOGGER.log(Level.INFO, "Last attemp to generate file {0} failed. Writing to unprocessables.txt. Skipping it", f.getName());
        writeUnprocessable(f.getName());
        writeProcessing("");
        return;
      }

      if (unprocessables.contains(f.getName())) {
        LOGGER.log(Level.INFO, "File {0} is in unprocessables.txt. Skipping it", f.getName());
        return;
      }
      writeProcessing(f.getName());
    }
    LOGGER.log(Level.INFO, "Generating thumbs for pdf {0}", f);

    try {
      int pageCounter = 0;
      try (PDDocument document = Loader.loadPDF(f)) {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        for (PDPage page : document.getPages()) {
          LOGGER.log(Level.FINE, "page {0}", pageCounter + 1);

//                        getImagesFromResources(page.getResources());
          BufferedImage bim = getImageFromPage(pdfRenderer, page.getMediaBox(), pageCounter, f.getName());
          if (bim != null) {
            if (pageCounter == 0) {
              thumbnailPdfPage(bim, f.getName());
            }

            if (onlyThumbs) {
              break;
            }
            processPage(bim, pageCounter, f.getName());
          }

          pageCounter++;
        }
        writeProcessing("");
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, f.getName() + " has error: {0}", ex);
        LOGGER.log(Level.SEVERE, null, ex);
        ImageSupport.writeSkipped(pageCounter, f.getName(), ex.toString());
      } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, f.getName() + " has error: {0}", ex);
        LOGGER.log(Level.SEVERE, null, ex);
        ImageSupport.writeSkipped(pageCounter, f.getName(), ex.toString());
      }
    } catch (JSONException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  public void processBytes(byte[] bytes, String name, boolean force, boolean onlyThumbs) {

    if (!force) {
      //Test if the file was last processed before crash;
      String lastProcessed = readProcessing();
      if (name.equals(lastProcessed)) {
        LOGGER.log(Level.INFO, "Last attemp to generate file {0} failed. Writing to unprocessables.txt. Skipping it", name);
        writeUnprocessable(name);
        writeProcessing("");
        return;
      }

      if (unprocessables.contains(name)) {
        LOGGER.log(Level.INFO, "File {0} is in unprocessables.txt. Skipping it", name);
        return;
      }
      writeProcessing(name);
    }
    LOGGER.log(Level.INFO, "Generating thumbs for pdf {0}", name);

    try {
      int pageCounter = 0;
      try (PDDocument document = Loader.loadPDF(bytes)) {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        for (PDPage page : document.getPages()) {
          LOGGER.log(Level.FINE, "page {0}", pageCounter + 1);

//                        getImagesFromResources(page.getResources());
          BufferedImage bim = getImageFromPage(pdfRenderer, page.getMediaBox(), pageCounter, name);
          if (bim != null) {
            if (pageCounter == 0) {
              thumbnailPdfPage(bim, name);
            }

            if (onlyThumbs) {
              break;
            }
            processPage(bim, pageCounter, name);
          }

          pageCounter++;
        }
        writeProcessing("");
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, name + " has error: {0}", ex);
        LOGGER.log(Level.SEVERE, null, ex);
        ImageSupport.writeSkipped(pageCounter, name, ex.toString());
      } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, name + " has error: {0}", ex);
        LOGGER.log(Level.SEVERE, null, ex);
        ImageSupport.writeSkipped(pageCounter, name, ex.toString());
      }
    } catch (JSONException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

//  public void processBytes(byte[] is, String name, boolean force, boolean onlyThumbs) {
//
//    if (!force) {
//      //Test if the file was last processed before crash;
//      String lastProcessed = readProcessing();
//      if (name.equals(lastProcessed)) {
//        LOGGER.log(Level.INFO, "Last attemp to generate file {0} failed. Writing to unprocessables.txt. Skipping it", name);
//        writeUnprocessable(name);
//        writeProcessing("");
//        return;
//      }
//
//      if (unprocessables.contains(name)) {
//        LOGGER.log(Level.INFO, "File {0} is in unprocessables.txt. Skipping it", name);
//        return;
//      }
//      writeProcessing(name);
//    }
//    LOGGER.log(Level.INFO, "Generating thumbs for pdf {0}", name);
//
//    try {
//      int pageCounter = 0;
//      try (PDDocument document = PDDocument.load(is)) {
//        PDFRenderer pdfRenderer = new PDFRenderer(document);
//        for (PDPage page : document.getPages()) {
//          LOGGER.log(Level.FINE, "page {0}", pageCounter + 1);
//
////                        getImagesFromResources(page.getResources());
//          BufferedImage bim = getImageFromPage(pdfRenderer, page.getMediaBox(), pageCounter, name);
//          if (bim != null) {
//            if (pageCounter == 0) {
//              thumbnailPdfPage(bim, name);
//            }
//
//            if (onlyThumbs) {
//              break;
//            }
//            processPage(bim, pageCounter, name);
//          }
//
//          pageCounter++;
//        }
//        writeProcessing("");
//      } catch (IOException ex) {
//        LOGGER.log(Level.SEVERE, name + " has error: {0}", ex);
//        LOGGER.log(Level.SEVERE, null, ex);
//        ImageSupport.writeSkipped(pageCounter, name, ex.toString());
//      } catch (Exception ex) {
//        LOGGER.log(Level.SEVERE, name + " has error: {0}", ex);
//        LOGGER.log(Level.SEVERE, null, ex);
//        ImageSupport.writeSkipped(pageCounter, name, ex.toString());
//      }
//    } catch (JSONException ex) {
//      LOGGER.log(Level.SEVERE, null, ex);
//    }
//  }

  private BufferedImage getImageFromPage(PDFRenderer pdfRenderer,
      PDRectangle mediaBox, int page, String id) throws Exception {
    float width = mediaBox.getWidth();
    float height = mediaBox.getHeight();
    if (width * height > maxPixels) {
      writeSkipped(page, id, String.format("%f x %f", width, height));
      return null;
    } else {
      float ratio = Math.max(getRenderRatio(width), getRenderRatio(height));
      return pdfRenderer.renderImageWithDPI(page, 72 * ratio, ImageType.RGB);
    }
  }

  private float getRenderRatio(float boxDim) {
    return (boxDim <= 1) ? 10f : (float)(maxMedium / boxDim);
  }

  public String thumbnailPdfPage(BufferedImage sourceImage, String id) {

    if (sourceImage == null) {
      LOGGER.log(Level.WARNING, "Cannot read image for page 0 in file {0}", id);
      return "Cannot read image";
    }
    int width = sourceImage.getWidth();
    int height = sourceImage.getHeight();

    if (width * height > maxPixels) {
      writeSkipped(0, id, width + " x " + height);
      return null;
    }

    try {
      int w = opts.getInt("thumbWidth", 100);
      String destDir = ImageSupport.makeDestDir(id);
      new File(destDir).mkdir();
      String outputFile = destDir + id + "_thumb.jpg";
      Thumbnails.of(sourceImage)
              .size(w, w)
              .crop(Positions.CENTER)
              .outputFormat("jpg")
              .toFile(outputFile);

      generated++;
      return outputFile;
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Error in image resizer:", ex);
      return null;
    }
  }

  public void processPage(BufferedImage bim, int pageCounter, String id) throws IOException {

    String outputFile = null;

    int width = bim.getWidth();
    int height = bim.getHeight();

    if (width * height < maxPixels) {
      int w;
      int h;
      if ((width < maxMedium) && (height < maxMedium)) {
        LOGGER.log(Level.WARNING, "Resized image too small at {0} x {1}.",
                new Object[]{ width, height });
        w = width;
        h = height;
      } else {
        if (height > width) {
          double ratio = maxMedium * 1.0 / height;
          w = (int) Math.max(1, Math.round(width * ratio));
          h = maxMedium;
        } else {
          double ratio = maxMedium * 1.0 / width;
          h = (int) Math.max(1, Math.round(height * ratio));
          w = maxMedium;
        }
      }

      String destDir = ImageSupport.makeDestDir(id) + id + File.separator;
      // LOGGER.log(Level.INFO, "Dest dir {0}", destDir);
      new File(destDir).mkdir();
      outputFile = destDir + (pageCounter) + ".jpg";
      File f = new File(outputFile);
//      Thumbnails.of(bim)
//              .size(w, h)
//              .outputFormat("jpg")
//              .toFile(outputFile);
      
       ImageSupport.resizeWithThumbnailator(bim, w, h, f, ImageSupport.getImageType(bim));
      
      //BufferedImage img2 = ImageSupport.scale(bim, w, h);
//            ImageIO.write(img2, "jpg", new File(outputFile));
//            LOGGER.info(outputFile);
//            img2.flush();
      generated++;

    } else {
      writeSkipped(pageCounter, id, width + " x " + height);
    }

  }

  int res = 0;

  private void logRes(COSDictionary d) {
    for (Map.Entry<COSName, COSBase> entry : d.entrySet()) {
      res++;
      COSBase b = entry.getValue();

      System.out.println("");
      System.out.print(entry.getKey().getName());
      System.out.print("\t");
      if (b instanceof COSDictionary) {
        COSDictionary d1 = (COSDictionary) b;
        System.out.print(d1.size());
        logRes(d1);
      } else if (b instanceof COSObject) {

        COSObject d1 = (COSObject) b;
        if (d1.getObject() instanceof COSDictionary) {
          COSDictionary d2 = (COSDictionary) d1.getObject();
          System.out.print(d2.size());
          logRes(d2);
        }
      }
    }
  }

  private String readProcessing() {

    try {
      File file = new File(opts.getString("thumbsDir") + File.separator + "processing.txt");
      if (file.exists()) {
        return FileUtils.readFileToString(file, "UTF-8");
      } else {
        return "";
      }
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return "";
    }
  }

  private void writeProcessing(String name) {
    try {
      File file = new File(opts.getString("thumbsDir") + File.separator + "processing.txt");
      FileUtils.writeStringToFile(file, name, "UTF-8", false);
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  private List<String> readUnprocessable() {

    try {
      File file = new File(opts.getString("thumbsDir") + File.separator + "unprocessables.txt");
      if (file.exists()) {
        return FileUtils.readLines(file, "UTF-8");
      } else {
        return new ArrayList<>();
      }
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return null;
    }
  }

  private void writeUnprocessable(String name) {
    try {
      File file = new File(opts.getString("thumbsDir") + File.separator + "unprocessables.txt");
      FileUtils.writeStringToFile(file, name + System.getProperty("line.separator"), "UTF-8", true);
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  private static void writeSkipped(int page, String id, String size) {
    String d = new Date().toString();
    try {
      if (page == -1) {
        //Image
        LOGGER.log(Level.WARNING, "skipping image {0} with size {1}", new Object[]{id, size});
        File file = new File(Options.getInstance().getString("thumbsDir") + File.separator + "skipped.txt");
        FileUtils.writeStringToFile(file, d + ".- Image in " + id + " Size: " + size + System.getProperty("line.separator"), "UTF-8", true);
      } else {
        //pdf page
        LOGGER.log(Level.WARNING, "skipping page {0} in file {1}. Image size {2}", new Object[]{page, id, size});
        File file = new File(Options.getInstance().getString("thumbsDir") + File.separator + "skipped.txt");
        FileUtils.writeStringToFile(file, d + ".- page " + page + " in " + id + " Image size: " + size + System.getProperty("line.separator"), "UTF-8", true);
      }
    } catch (IOException | JSONException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }
}
