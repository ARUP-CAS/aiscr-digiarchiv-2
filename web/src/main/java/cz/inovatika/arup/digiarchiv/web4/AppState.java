
package cz.inovatika.arup.digiarchiv.web4;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author alber
 */
public class AppState {
    
    static Map<String, Boolean>  ipsRunning = new HashMap<>();
    static Map<String, Instant>  ipsTimes = new HashMap<>();
    
    public static synchronized boolean canGetFile(String ip, String id) {
        if (id.contains("thumb")) {
            return true;
        }
        int interval = Options.getInstance().getInt("requestInterval", 5000);
        return !ipsRunning.containsKey(ip) && 
               (!ipsTimes.containsKey(ip) || ipsTimes.get(ip).isBefore(Instant.now().minus(interval, ChronoUnit.MILLIS)));
    } 
    
    public static synchronized long canGetFileInterval(String ip, String id) {
        if (id.contains("thumb")) {
            return 0;
        }
        if(ipsRunning.containsKey(ip)) {
            return -1; // "Downloading file in progress. Try later.";
        }
        int interval = Options.getInstance().getInt("requestInterval", 5000);
        if (ipsTimes.containsKey(ip)) {
            return Duration.between(Instant.now(), ipsTimes.get(ip).plus(interval, ChronoUnit.MILLIS)).toSeconds();
        }
        return 0;
    }
    
    public static synchronized void writeGetFileStarted(String ip, String id) {
        if (id.contains("thumb")) {
            return;
        }
        ipsRunning.put(ip, true);
    } 
    
    public static synchronized void writeGetFileFinished(String ip, String id, boolean success) {
        if (id.contains("thumb")) {
            return;
        }
        ipsRunning.remove(ip);
        if (success) {
            ipsTimes.put(ip, Instant.now());
        }
        
    }
}
