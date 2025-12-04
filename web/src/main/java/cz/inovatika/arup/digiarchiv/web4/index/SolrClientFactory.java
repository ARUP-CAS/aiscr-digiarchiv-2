package cz.inovatika.arup.digiarchiv.web4.index;

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

    private static volatile SolrClient solrClient; // volatile for thread-safety

    private SolrClientFactory() {
        // Private constructor to prevent instantiation
    }

    public static SolrClient getSolrClient() {
        if (solrClient == null) {
            synchronized (SolrClientFactory.class) {  
                if (solrClient == null) {
                    // Replace with your Solr URL and appropriate client implementation
                    String solrUrl = "http://localhost:8983/solr";
                    solrClient = new Http2SolrClient.Builder(solrUrl).build();
                    // For a Solr Cloud setup, you would use CloudSolrClient
                    // String zkHost = "localhost:2181/solr";
                    // solrClient = new CloudSolrClient.Builder().withZkHost(zkHost).build();
                }
            }
        }
        return solrClient;
    }

    public static void resetSolrClient() {
        if (solrClient != null) {
            try {
                solrClient.close();
            } catch (IOException ex) {
                Logger.getLogger(SolrClientFactory.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        solrClient = null;
    }

    private static class InstanceHolder {

        private static final SolrClientFactory INSTANCE = new SolrClientFactory();
    }

    public static SolrClientFactory getInstance() {
        return InstanceHolder.INSTANCE;
    }

}
