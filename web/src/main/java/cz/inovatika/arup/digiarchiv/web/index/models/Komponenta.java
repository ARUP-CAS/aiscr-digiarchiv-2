/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.arup.digiarchiv.web.index.models;

import com.alibaba.fastjson.JSON;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.csv.CSVRecord;
import org.apache.solr.client.solrj.beans.Field;

/**
 *
 * @author alberto
 */
public class Komponenta {

  static final Logger LOGGER = Logger.getLogger(Komponenta.class.getName());

  @Field
  public String ident_cely;

  @Field
  public String parent;

  @Field
  public String obdobi;

  @Field
  public String jistota;

  @Field
  public String presna_datace;

  @Field
  public String areal;

  @Field
  public int aktivita_sidlistni;

  @Field
  public int aktivita_pohrebni;

  @Field
  public int aktivita_vyrobni;

  @Field
  public int aktivita_tezebni;

  @Field
  public int aktivita_kultovni;

  @Field
  public int aktivita_komunikace;

  @Field
  public int aktivita_deponovani;

  @Field
  public int aktivita_intruze;

  @Field
  public int aktivita_boj;

  @Field
  public int aktivita_jina;

  @Field
  public String poznamka;

  @Field
  public String nalez;

  public static Komponenta fromCSV(Map<String, Integer> header, CSVRecord record) {

    Komponenta ret = new Komponenta();
    Class aClass = Komponenta.class;
    java.lang.reflect.Field[] fields = aClass.getDeclaredFields();

    for (java.lang.reflect.Field field : fields) {
      if (header.containsKey(field.getName())) {
        try {
          field.set(ret, record.get(header.get(field.getName())));
        } catch (IllegalArgumentException | IllegalAccessException ex) {
          LOGGER.log(Level.SEVERE, null, ex);
        }
      }
    }
    return ret;
  }
  
  @Override
  public String toString() {
    return JSON.toJSONString(this);
  }

}
