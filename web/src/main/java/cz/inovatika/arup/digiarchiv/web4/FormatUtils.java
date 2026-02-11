package cz.inovatika.arup.digiarchiv.web4;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author alberto
 */
public class FormatUtils {

  public static String formatInterval(long millis) {
    return String.format("%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis)
            - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
            TimeUnit.MILLISECONDS.toSeconds(millis)
            - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
  }
}
