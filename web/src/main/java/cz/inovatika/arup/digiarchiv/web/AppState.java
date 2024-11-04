
package cz.inovatika.arup.digiarchiv.web;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
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
        int interval = Options.getInstance().getInt("requestInterval", 60);
        return !ipsRunning.containsKey(ip) && 
               (!ipsTimes.containsKey(ip) || ipsTimes.get(ip).isBefore(Instant.now().minus(interval, ChronoUnit.SECONDS)));
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
