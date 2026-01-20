package cz.inovatika.arup.digiarchiv.web4.imagging;

import cz.inovatika.arup.digiarchiv.web4.Options;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.cos.COSName;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.json.JSONException;

public class PDFAcceptor {

  public static final Logger LOGGER = Logger.getLogger(PDFAcceptor.class.getName());


  public static boolean acceptPage(PDPage page) {
    try {
      int maxPixels = Options.getInstance().getInt("maxPixels", 2000*2000);
      @SuppressWarnings("rawtypes")
              PDResources resources = page.getResources();
      
      for (COSName cosName : resources.getXObjectNames()) {
        PDXObject xobject = resources.getXObject(cosName);
        if (xobject instanceof PDImageXObject) {
          
          PDImageXObject image = (PDImageXObject) xobject;
          int width = image.getWidth();
          int height = image.getHeight();
          int pixels = width * height;
          if (pixels > maxPixels) {
            LOGGER.warning("skipping file");
            return false;
          }
        }
      }
      
      return true;
    } catch (Exception ex) {
      LOGGER.warning("cannot process page");
      LOGGER.log(Level.SEVERE, null, ex);
      return false;
    }
  }

  public static boolean acceptFile(PDDocument doc) throws IOException {
    int maxPixels = Options.getInstance().getInt("maxPixels", 2000*2000);
    @SuppressWarnings("rawtypes")
    PDPageTree pages = doc.getDocumentCatalog().getPages();
    @SuppressWarnings("rawtypes")
    Iterator iter = pages.iterator();
    while (iter.hasNext()) {
      PDPage page = (PDPage) iter.next();
      PDResources resources = page.getResources();

      for (COSName cosName : resources.getXObjectNames()) {
        PDXObject xobject = resources.getXObject(cosName);
        if (xobject instanceof PDImageXObject) {

          PDImageXObject image = (PDImageXObject) xobject;
          int width = image.getWidth();
          int height = image.getHeight();
          int pixels = width * height;
          if (pixels > maxPixels) {
            LOGGER.warning("skipping file");
            return false;
          }
        }
      }
    }
    return true;
  }

}
