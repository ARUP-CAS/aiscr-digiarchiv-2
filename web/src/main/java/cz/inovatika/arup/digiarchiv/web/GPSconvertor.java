/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web;

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

public class GPSconvertor {

  /**
   * Konverze dat z JTKS do WGS EPSG:5514 / https://epsg.io/2065
   *
   * @param data
   */
  public static String convertGeojson(String data) {
    System.out.println(data);
    StringBuilder output = new StringBuilder();
    // String p = "\\s(-{0,1}(\\d{0,}\\.{0,1}\\d{0,}),-{0,1}(\\d{0,}\\.{0,1}\\d{0,}))\\s"; // [-735280.35,-1034312.02]
    String p = "(?<=\\()[^()]+?(?=\\))"; // [-735280.35,-1034312.02]
    Pattern pattern = Pattern.compile(p);
    Matcher matcher = pattern.matcher(data);
    while (matcher.find()) {
      // System.out.println("group " + matcher.group());

      // Coordinates coordinates = jtsk_to_wgs(matcher.group(1), matcher.group(2));
      String[] css = matcher.group().split(",");
      StringBuilder o = new StringBuilder();
      for (String cs : css) {
      // System.out.println(cs);
        String[] xy = cs.split(" ");
        Coordinates coordinates = jtsk_to_wgs(xy[0], xy[1]);
        o.append(",").append(coordinates.toSimpleString());
      }
      
      matcher.appendReplacement(output, o.substring(1));
      //output.append(coordinates.getGeoJsonString());
//        data.replaceAll(matcher.group( ), "[" + coordinates.getElasticSuitableString() + "]");
    }
    matcher.appendTail(output);
    System.out.println(output.toString());

    return output.toString();

  }
  

  public static Coordinates jtsk_to_wgs(String X, String Y) {
    return jtsk_to_wgs(Math.abs(Double.parseDouble(X)), Math.abs(Double.parseDouble(Y)), 245);
  }

  public static Coordinates jtsk_to_wgs(double X, double Y, double H) {
    if (Y > X) {
      double t = X;
      X = Y;
      Y = t;
    }
    // var coord = {wgs84_latitude:"", wgs84_longitude:"", lat: 0, lon: 0, vyska: 0};

    Coordinates coord = new Coordinates();
    /* Přepočet vstupích údajů - vychazi z nejakeho skriptu, ktery jsem nasel na Internetu - nejsem autorem prepoctu. */

 /*Vypocet zemepisnych souradnic z rovinnych souradnic*/
    double a = 6377397.15508;
    double e = 0.081696831215303;
    double n = 0.97992470462083;
    double konst_u_ro = 12310230.12797036;
    double sinUQ = 0.863499969506341;
    double cosUQ = 0.504348889819882;
    double sinVQ = 0.420215144586493;
    double cosVQ = 0.907424504992097;
    double alfa = 1.000597498371542;
    double k = 1.003419163966575;
    double ro = Math.sqrt(X * X + Y * Y);
    double epsilon = 2 * Math.atan(Y / (ro + X));
    double D = epsilon / n;
    double S = 2 * Math.atan(Math.exp(1 / n * Math.log(konst_u_ro / ro))) - Math.PI / 2;
    double sinS = Math.sin(S);
    double cosS = Math.cos(S);
    double sinU = sinUQ * sinS - cosUQ * cosS * Math.cos(D);
    double cosU = Math.sqrt(1 - sinU * sinU);
    double sinDV = Math.sin(D) * cosS / cosU;
    double cosDV = Math.sqrt(1 - sinDV * sinDV);
    double sinV = sinVQ * cosDV - cosVQ * sinDV;
    double cosV = cosVQ * cosDV + sinVQ * sinDV;
    double Ljtsk = 2 * Math.atan(sinV / (1 + cosV)) / alfa;
    double t = Math.exp(2 / alfa * Math.log((1 + sinU) / cosU / k));
    double pom = (t - 1) / (t + 1);
    double sinB;
    do {
      sinB = pom;
      pom = t * Math.exp(e * Math.log((1 + e * sinB) / (1 - e * sinB)));
      pom = (pom - 1) / (pom + 1);
    } while (Math.abs(pom - sinB) > 1e-15);

    double Bjtsk = Math.atan(pom / Math.sqrt(1 - pom * pom));


    /* Pravoúhlé souřadnice ve S-JTSK */
    a = 6377397.15508;
    double f_1 = 299.152812853;
    double e2 = 1 - (1 - 1 / f_1) * (1 - 1 / f_1);
    ro = a / Math.sqrt(1 - e2 * Math.sin(Bjtsk) * Math.sin(Bjtsk));
    double x = (ro + H) * Math.cos(Bjtsk) * Math.cos(Ljtsk);
    double y = (ro + H) * Math.cos(Bjtsk) * Math.sin(Ljtsk);
    double z = ((1 - e2) * ro + H) * Math.sin(Bjtsk);

    /* Pravoúhlé souřadnice v WGS-84*/
    double dx = 570.69;
    double dy = 85.69;
    double dz = 462.84;
    double wz = -5.2611 / 3600 * Math.PI / 180;
    double wy = -1.58676 / 3600 * Math.PI / 180;
    double wx = -4.99821 / 3600 * Math.PI / 180;
    double m = 3.543e-6;
    double xn = dx + (1 + m) * (x + wz * y - wy * z);
    double yn = dy + (1 + m) * (-wz * x + y + wx * z);
    double zn = dz + (1 + m) * (wy * x - wx * y + z);

    /* Geodetické souřadnice v systému WGS-84*/
    a = 6378137.0;
    f_1 = 298.257223563;
    double a_b = f_1 / (f_1 - 1);
    double p = Math.sqrt(xn * xn + yn * yn);
    e2 = 1 - (1 - 1 / f_1) * (1 - 1 / f_1);
    double theta = Math.atan(zn * a_b / p);
    double st = Math.sin(theta);
    double ct = Math.cos(theta);
    t = (zn + e2 * a_b * a * st * st * st) / (p - e2 * a * ct * ct * ct);
    double B = Math.atan(t);
    double L = 2 * Math.atan(yn / (p + xn));
    H = Math.sqrt(1 + t * t) * (p - a / Math.sqrt(1 + (1 - e2) * t * t));

    /* Formát výstupních hodnot */
    B = B / Math.PI * 180;

    coord.lat = B;
    String latitude = "N";
    if (B < 0) {
      B = -B;
      latitude = "S";
    }
    ;

    double st_sirky = Math.floor(B);
    B = (B - st_sirky) * 60;
    double min_sirky = Math.floor(B);
    B = (B - min_sirky) * 60;
    double vt_sirky = Math.round(B * 1000) / 1000;
    latitude = st_sirky + "°" + min_sirky + "'" + vt_sirky + latitude;
    coord.wgs84_latitude = latitude;

    L = L / Math.PI * 180;
    coord.lon = L;
    String longitude = "E";
    if (L < 0) {
      L = -L;
      longitude = "W";
    }
    ;

    double st_delky = Math.floor(L);
    L = (L - st_delky) * 60;
    double min_delky = Math.floor(L);
    L = (L - min_delky) * 60;
    double vt_delky = Math.round(L * 1000) / 1000;
    longitude = st_delky + "°" + min_delky + "'" + vt_delky + longitude;
    coord.wgs84_longitude = longitude;

    coord.vyska = Math.round((H) * 100) / 100;

    return coord;
  }
}

class Coordinates {

  public String wgs84_latitude;
  public String wgs84_longitude;

  public double lat = 0;
  public double lon = 0;
  public double vyska = 0;

  public String getElasticSuitableString() {
    return lat + ", " + lon;
  }

  public String getGeoJsonString() {
    return String.format(Locale.ENGLISH, "[%.3f,%.3f]", lon, lat); // musi byt Locale.ENGLISH, jinak dela desetinou carku
//        return "[" + Math.roulat + "," + lon + "]";
  }

  public String toSimpleString() {
    return String.format(Locale.ENGLISH, "%.6f %.6f", lon, lat); // musi byt Locale.ENGLISH, jinak dela desetinou carku
    // return String.format(Locale.ENGLISH, "%.5f,%.5f", wgs84_longitude, wgs84_latitude); // musi byt Locale.ENGLISH, jinak dela desetinou carku
  }

  @Override
  public String toString() {
    return wgs84_latitude + "  ; " + wgs84_longitude
            + "\n  " + lat + " ; " + lon;

  }

}
