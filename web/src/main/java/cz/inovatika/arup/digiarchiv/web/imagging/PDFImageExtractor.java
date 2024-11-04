/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.imagging;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.cos.COSName;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 *
 * @author alberto
 */
public class PDFImageExtractor {

  public static void main(String[] args) {

    String source = "/home/alberto/.amcr/soubory/1496304376657_MTX201400253.pdf";
    source = "c:\\Users\\alberto.a.hernandez\\Projects\\DigiArchiv\\data\\1496847831585_CTX201202277.pdf";
    try (PDDocument document = PDDocument.load(new File(source))) {
      PDFRenderer pdfRenderer = new PDFRenderer(document);

      List<RenderedImage> images = new ArrayList<>();
      int i = 0;
      for (PDPage page : document.getPages()) {
//        System.out.println("page: " + i++);
//        if(i == 40){
        images.addAll(getImagesFromResources(page.getResources()));
//        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static List<RenderedImage> getImagesFromResources(PDResources resources) throws IOException {
    List<RenderedImage> images = new ArrayList<>();
    int i = 0;
    
    for (COSName xObjectName : resources.getXObjectNames()) {
      PDXObject xObject = resources.getXObject(xObjectName);

      if (xObject instanceof PDFormXObject) {
        System.out.println("je to form"); 
        images.addAll(getImagesFromResources(((PDFormXObject) xObject).getResources()));
      } else if (xObject instanceof PDImageXObject) {
        i++;
        RenderedImage rimg = ((PDImageXObject) xObject).getImage();
                
        System.out.println(((PDImageXObject) xObject).getSuffix()); 
//        ImageIO.write(rimg, "PNG", new File("/home/alberto/Projects/ARUP/test/" + xObjectName.getName()));
        ImageIO.write(rimg, "PNG", new File("c:\\Users\\alberto.a.hernandez\\Projects\\ARUP\\test\\" + xObjectName.getName()));
//        System.out.print(rimg.getPropertyNames()); 
//        System.out.print("\t"); 
//        System.out.println(rimg.getSampleModel().getDataType()); 
//        images.add(rimg);
        
      }
System.out.println("resources: " + i); 
    }

    return images;
  }
}
