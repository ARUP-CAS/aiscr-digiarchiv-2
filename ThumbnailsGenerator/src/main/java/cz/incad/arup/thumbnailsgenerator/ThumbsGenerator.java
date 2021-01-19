/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.incad.arup.thumbnailsgenerator;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class ThumbsGenerator {

  public static void main(String[] args) {

//    try {
//      //"sun.java2d.cmm.kcms.CMM"
//      Class.forName("sun.java2d.cmm.kcms.CMM");
//      Class.forName("sun.java2d.cmm.kcms.KcmsServiceProvider");
//    } catch (ClassNotFoundException ex) {
//      Logger.getLogger(ThumbsGenerator.class.getName()).log(Level.SEVERE, null, ex);
//    }
    System.setProperty("java.awt.headless", "true");
    System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
    System.setProperty("org.apache.pdfbox.rendering.UsePureJavaCMYKConversion", "true");
    ImageIO.scanForPlugins();
    boolean overwrite = false;
    boolean onlyThumbs = false;

    if (args.length > 0) {
      String action = args[0];
      Logger.getLogger(ThumbsGenerator.class.getName()).log(Level.INFO, "action: {0}", action);
      switch (action) {
        case "-ot": {
          //Only thumbs
          overwrite = true;
          onlyThumbs = true;
          break;
        }
        case "-o": {
          overwrite = true;
          break;
        }
        case "-pdf": {
          String file = args[1];
          PDFThumbsGenerator pg = new PDFThumbsGenerator(true);
          pg.processFile(new File(file), true, false);
          return;
        }
        case "-f": {
          String file = args[1];
          File f = new File(file);
          ImageSupport.thumbnailzeImg(f, f.getName(), false);
          return;
        }
        case "-id": {
          String id = args[1];
          Indexer indexer = new Indexer(true);
          indexer.createThumb(id, false, true, false);
          return;
        }
        case "-fq": {
          String fq = args[1];
          Indexer indexer = new Indexer(true);
          try {
            indexer.createThumbs(false, false, fq);
          } catch (Exception ex) {
            Logger.getLogger(ThumbsGenerator.class.getName()).log(Level.SEVERE, null, ex);
          }
          return;
        }
        case "-used": {
          try {

            Indexer indexer = new Indexer(false);
            JSONObject jo = indexer.createForUsed(overwrite, onlyThumbs);
            System.out.println(jo.toString(2));
          } catch (Exception ex) {
            Logger.getLogger(ThumbsGenerator.class.getName()).log(Level.SEVERE, null, ex);
          }
          return;
        }
      }
    }
    Indexer indexer = new Indexer(false);
    try {
      JSONObject jo = indexer.createThumbs(overwrite, onlyThumbs);
      System.out.println(jo.toString(2));
    } catch (Exception ex) {
      Logger.getLogger(ThumbsGenerator.class.getName()).log(Level.SEVERE, null, ex);
    }

  }

}
