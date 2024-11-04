package cz.inovatika.arup.digiarchiv.web.imagging;

import cz.inovatika.arup.digiarchiv.web.Options;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import org.json.JSONException;

/**
 *
 * @author alberto
 */
public class ImageSupport {

    public static final Logger LOGGER = Logger.getLogger(ImageSupport.class.getName());
    public static int generated;

    public static void initCount() {
        ImageSupport.generated = 0;
    }

    
    public static boolean thumbExists(String f) {
        String dest = getDestDir(f) + f + ".jpg";
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
//            if (!f.contains(".")) {
//                return null;
//            }
//            String filename = f.substring(Math.max(0, f.lastIndexOf("/")), f.lastIndexOf("."));
            String filename = f.substring(f.lastIndexOf("/") + 1);
            int period = 2;
            int levels = 3;
            int l = filename.length();

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < levels; i++) {
                sb.append(filename.substring(l - (i * period) - period, l - (i * period))).append(File.separator);
            }

            //new File(destDir + sb.toString()).mkdirs();
            return destDir + sb.toString();
        } catch (JSONException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static String makeDestDir(String f) {

        String destDir = getDestDir(f);

        new File(destDir).mkdirs();

        return destDir;
    }

    public static void addWatermark(BufferedImage sourceImage, BufferedImage mark, float alpha) {
        //try {
        Graphics2D g2d = (Graphics2D) sourceImage.getGraphics();

        // initializes necessary graphic properties
        AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
        g2d.setComposite(alphaChannel);
//        g2d.setColor(Color.BLUE);
// 
        // calculates the coordinates where the String is painted

//        int cols = sourceImage.getWidth() / (mark.getWidth() + 20);
//        int rows = (sourceImage.getHeight() / (mark.getHeight() + 20 ));
//        for(int i =0; i<cols; i++){
//          for(int j = 0; j<rows; j++){
//            g2d.drawImage(mark, (mark.getWidth() + 20)*i, (mark.getHeight()+20)*j, null);
//          }
//        }
        int centerX = (sourceImage.getWidth() - (int) mark.getWidth()) / 2;
        int centerY = (sourceImage.getHeight() - (int) mark.getHeight()) / 2;
        g2d.drawImage(mark, centerX, centerY, null);

        //ImageIO.write(sourceImage, "png", destImageFile);
        g2d.dispose();

//    } catch (IOException ex) {
//        System.err.println(ex);
//    }
    }

    public static void addTextWatermark(BufferedImage sourceImage) {
        //try {
        Graphics2D g2d = (Graphics2D) sourceImage.getGraphics();

        // initializes necessary graphic properties
        AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f);
        g2d.setComposite(alphaChannel);
        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("Arial", Font.BOLD, 64));
        FontMetrics fontMetrics = g2d.getFontMetrics();
        Rectangle2D rect = fontMetrics.getStringBounds("AMCR TEXTEXTEXT", g2d);

        // calculates the coordinate where the String is painted
        int centerX = (sourceImage.getWidth() - (int) rect.getWidth()) / 2;
        int centerY = sourceImage.getHeight() / 2;

        // paints the textual watermark
        g2d.drawString("AMCR TEXTEXTEXT", centerX, centerY);

        //ImageIO.write(sourceImage, "png", destImageFile);
        g2d.dispose();

        System.out.println("The tex watermark is added to the image.");

//    } catch (IOException ex) {
//        System.err.println(ex);
//    }
    }

}
