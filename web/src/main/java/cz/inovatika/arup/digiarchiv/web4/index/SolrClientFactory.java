package cz.inovatika.arup.digiarchiv.web4.index;

import cz.inovatika.arup.digiarchiv.web4.Options;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;

/**
 *
 * @author alber
 */
public class SolrClientFactory {

    public static final Logger LOGGER = Logger.getLogger(SolrClientFactory.class.getName());
    // volatile for thread-safety
    private static volatile SolrClient solrClient; 
    private static volatile SolrClient solrClientSearch; 

    private SolrClientFactory() {
        // Private constructor to prevent instantiation
    }

    public static SolrClient getSolrClient() {
        if (solrClient == null) {
            synchronized (SolrClientFactory.class) {  
                if (solrClient == null) {
                    // Replace with your Solr URL and appropriate client implementation
                    String solrUrl = Options.getInstance().getString("solrhost");
                    solrClient = new Http2SolrClient.Builder(solrUrl).build();
                    // For a Solr Cloud setup, you would use CloudSolrClient
                    // String zkHost = "localhost:2181/solr";
                    // solrClient = new CloudSolrClient.Builder().withZkHost(zkHost).build();
                }
            }
        }
        return solrClient;
    }

    public static SolrClient getSolrClientSearch() {
        if (solrClientSearch == null) {
            synchronized (SolrClientFactory.class) {  
                if (solrClientSearch == null) {
                    // Replace with your Solr URL and appropriate client implementation
                    String solrUrl = Options.getInstance().getString("solrhost");
                    solrClientSearch = new Http2SolrClient.Builder(solrUrl).build();
                    // For a Solr Cloud setup, you would use CloudSolrClient
                    // String zkHost = "localhost:2181/solr";
                    // solrClient = new CloudSolrClient.Builder().withZkHost(zkHost).build();
                }
            }
        }
        return solrClientSearch;
    }

    public static void resetSolrClient() {
        LOGGER.log(Level.INFO, "Reseting SolrClient");
        if (solrClient != null) {
            try {
                solrClient.close();
            } catch (IOException ex) {
                Logger.getLogger(SolrClientFactory.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        solrClient = null;
    }

    public static void resetSolrClientSearch() {
        LOGGER.log(Level.INFO, "Reseting solrClientSearch");
        if (solrClientSearch != null) {
            try {
                solrClientSearch.close();
            } catch (IOException ex) {
                Logger.getLogger(SolrClientFactory.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        solrClientSearch = null;
    }

    private static class InstanceHolder {

        private static final SolrClientFactory INSTANCE = new SolrClientFactory();
    }

    public static SolrClientFactory getInstance() {
        return InstanceHolder.INSTANCE;
    }

}
