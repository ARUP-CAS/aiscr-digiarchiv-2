package cz.incad.arup.thumbnailsgenerator;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.util.Date;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.stream.ImageInputStream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.coobird.thumbnailator.ThumbnailParameter;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author alberto
 */
public class ImageSupport {

  public static final Logger LOGGER = Logger.getLogger(ImageSupport.class.getName());

  public static boolean thumbExists(String f) {

    String dest = getDestDir(f) + f + "_thumb.jpg";
    return (new File(dest)).exists();
  }

  public static boolean folderExists(String f) {
    String dest = getDestDir(f);
    return (new File(dest)).exists();
  }

  public static String getDestDir(String f) {
    try {
      Options opts = Options.getInstance();
      String destDir = opts.getString("thumbsDir");
      String filename = f.substring(Math.max(0, f.lastIndexOf("/")), f.lastIndexOf("."));
      int period = 2;
      int levels = 3;
      int l = filename.length();

      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < levels; i++) {
        sb.append(filename.substring(l - (i * period) - period, l - (i * period))).append(File.separator);
      }

      //new File(destDir + sb.toString()).mkdirs();
      return destDir + sb.toString();
    } catch (IOException | JSONException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return null;
    }
  }

  public static String makeDestDir(String f) {

    String destDir = getDestDir(f);

    new File(destDir).mkdirs();

    return destDir;
  }

  public static String thumbnailzeImg(File f, String id, boolean onlyThumbs) {

    String outputFile = getDestDir(id) + id;

    try {
      BufferedImage srcImage = ImageIO.read(f);
      Options opts = Options.getInstance();

      makeDestDir(id);
      int t_width = opts.getInt("thumbWidth", 100);
      int t_height = opts.getInt("thumbHeight", 100);

      // Podle https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/111
      // resizeAndCropWithThumbnailator(srcImage, t_width, t_height, new File(outputFile + "_thumb.jpg"), getImageType(f, srcImage));
      resizeWithThumbnailator(srcImage, t_width, t_height, new File(outputFile + "_thumb.jpg"), getImageType(f, srcImage));

      if (!onlyThumbs) {
        int max = opts.getInt("mediumHeight", 1000);
        resizeWithThumbnailator(srcImage, max, max, new File(outputFile + "_medium.jpg"), getImageType(f, srcImage));
      }

      return outputFile;
    } catch (Exception ex) {

      LOGGER.log(Level.SEVERE, "Error creating thumb {0}, ", outputFile);
      LOGGER.log(Level.SEVERE, null, ex);
      return null;
    }
  }

  public static void writeSkipped(int page, String id, String size) {
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

  public static void resizeAndCropWithThumbnailator(BufferedImage srcImage, int w, int h, File dest, int imageType) {
    LOGGER.log(Level.INFO, "generating {0}", dest.getAbsoluteFile());
    try {
      Thumbnails.of(srcImage)
              .size(w, h)
              .crop(Positions.CENTER)
              .imageType(imageType)
              .outputFormat("jpg")
              .toFile(dest);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Error in image resizer:", ex);
    }
  }

  public static void resizeWithThumbnailator(BufferedImage srcImage, int w, int h, File f, int imageType) {
//    byte[] retval = null;
    try {
//                ByteArrayOutputStream os = new ByteArrayOutputStream();

      Thumbnails.of(srcImage)
              .size(w, h)
              .imageType(imageType)
              .outputFormat("jpg")
              .toFile(f);
//                retval = os.toByteArray();
//                os.close();

    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Error in image resizer:", ex);
    }
//        return retval;
  }

  public static int getImageType(BufferedImage img) {

//    WritableRaster raster = img.getRaster();
//    int elemCount = raster.getNumDataElements();
//    return (elemCount == 1) ? BufferedImage.TYPE_BYTE_GRAY : ThumbnailParameter.ORIGINAL_IMAGE_TYPE;
    return (img.getColorModel().getPixelSize() == 8) ? BufferedImage.TYPE_BYTE_GRAY : ThumbnailParameter.ORIGINAL_IMAGE_TYPE;
  }

  private static int getImageType(File f, BufferedImage img) {

//    WritableRaster raster = img.getRaster();
//    int elemCount = raster.getNumDataElements();
    //System.out.println(elemCount);
//    return (elemCount == 1) ? BufferedImage.TYPE_BYTE_GRAY : ThumbnailParameter.ORIGINAL_IMAGE_TYPE;
//    System.out.println(img);
//    ColorModel cm = img.getColorModel();
//    System.out.println(cm);
//    System.out.println(cm.getColorSpace().getType());
//    return (img.getColorModel().getPixelSize() == 8) ? BufferedImage.TYPE_BYTE_GRAY : ThumbnailParameter.ORIGINAL_IMAGE_TYPE;
    //return (isGray(f, img)) ? BufferedImage.TYPE_BYTE_GRAY : ThumbnailParameter.ORIGINAL_IMAGE_TYPE;
    return (isGray(f, img)) ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_INT_RGB;
  }

  private static boolean isGray(File f, BufferedImage img) {
    if (img.getColorModel().getPixelSize() > 8) {
      return false;
    }
    try (ImageInputStream input = ImageIO.createImageInputStream(new FileInputStream(f))) {
      ImageReader reader = ImageIO.getImageReaders(input).next(); // Assumes PNGImageReader is always there
      reader.setInput(input);

      IIOMetadata metadata = reader.getImageMetadata(0);
//      Node nativeTree = metadata.getAsTree(metadata.getNativeMetadataFormatName());
      Node standardTree = metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);

//      StringWriter writer = new StringWriter();
//      Transformer transformer = TransformerFactory.newInstance().newTransformer();
//      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//
//      transformer.transform(new DOMSource(nativeTree), new StreamResult(writer));
//      String xml = writer.toString();
//      System.out.println(xml);
//      
//      writer = new StringWriter();
//      transformer.transform(new DOMSource(standardTree), new StreamResult(writer));
//      xml = writer.toString();
//      System.out.println(xml);
      NodeList nodes = standardTree.getFirstChild().getChildNodes();
      for (int i = 0; i < nodes.getLength(); i++) {
        Node n = nodes.item(i);
        switch (n.getNodeName()) {
          case "ColorSpaceType":
            if ("GRAY".equals(n.getAttributes().getNamedItem("name").getNodeValue())) {
              // System.out.println("IS GRAY");
              return true;
            }
            break;
          case "Palette":
            // Has Pallete. Should test if all values are grays
            NodeList paletteEntries = n.getChildNodes();
            // System.out.println(paletteEntries.getLength());
            for (int j = 0; j < paletteEntries.getLength(); j++) {
              Node pe = paletteEntries.item(j);
              // System.out.println(pe.getNodeName());
              if ("PaletteEntry".equals(pe.getNodeName())) {
                String red = pe.getAttributes().getNamedItem("red").getNodeValue();
                String green = pe.getAttributes().getNamedItem("green").getNodeValue();
                String blue = pe.getAttributes().getNamedItem("blue").getNodeValue();
                // System.out.println(red + ", " + green + ", " + blue);
                if (!red.equals(blue) || !red.equals(green)) {
                  // System.out.println("NOT GRAY");
                  return false;
                }
              }
            }
            // System.out.println("IS GRAY");
            return true;
          default:
        }
      }
      // System.out.println(standardTree.getFirstChild().getFirstChild().getAttributes().getNamedItem("name").getNodeValue());
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    return false;
  }
}
