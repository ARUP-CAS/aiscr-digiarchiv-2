package cz.inovatika.arup.digiarchiv.web;

import cz.inovatika.arup.digiarchiv.web.index.IndexUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;

/**
 *
 * @author alberto
 */
public class InitServlet extends HttpServlet {

    public static final Logger LOGGER = Logger.getLogger(InitServlet.class.getName());

    //Directory where cant override configuration  
    public static String CONFIG_DIR = ".amcr";

    //Default config directory in webapp
    public static String DEFAULT_CONFIG_DIR = "/assets";

    //Default configuration file 
    public static String DEFAULT_CONFIG_FILE = "config.json";

    //Default config directory in webapp
    public static String DEFAULT_I18N_DIR = "/assets/i18n";

    //Directory where cant override configuration  
    public static String TEMP_DIR = ".amcr/tmp";

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    }

    @Override
    public void init() throws ServletException {
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
        // System.setProperty("jdk.httpclient.keepalive.timeout", "600");

        if (getServletContext().getInitParameter("def_config_dir") != null) {
            DEFAULT_CONFIG_DIR = getServletContext().getInitParameter("def_config_dir");
        }

        DEFAULT_CONFIG_FILE = getServletContext().getRealPath(DEFAULT_CONFIG_DIR) + File.separator + DEFAULT_CONFIG_FILE;
        DEFAULT_I18N_DIR = getServletContext().getRealPath(DEFAULT_I18N_DIR);

        if (System.getProperty("amcr_app_dir") != null) {
            CONFIG_DIR = System.getProperty("amcr_app_dir");
        } else if (getServletContext().getInitParameter("app_dir") != null) {
            CONFIG_DIR = getServletContext().getInitParameter("app_dir");
        } else {
            CONFIG_DIR = System.getProperty("user.home") + File.separator + CONFIG_DIR;
        }

        try {
            TEMP_DIR = CONFIG_DIR + File.separator + "tmp";
            File tmp = new File(TEMP_DIR);
            if (!tmp.exists()) {
                Files.createDirectories(Paths.get(TEMP_DIR));
            }
        } catch (IOException ex) {
            Logger.getLogger(InitServlet.class.getName()).log(Level.SEVERE, null, ex);
        }

        // clear index status. In case of restart while running
        writeStatusFile("index", "inited");
        writeStatusFile("update", "inited");
        
        try {
            //Init locales
            I18n.getInstance().getLocale("cs");
            I18n.getInstance().getLocale("en");
            LOGGER.log(Level.FINE, "Locales loaded"); 
        } catch (IOException | JSONException ex) {
            LOGGER.log(Level.SEVERE, "Error loading locales");
            LOGGER.log(Level.SEVERE, null, ex);
        }

        LOGGER.log(Level.INFO, "CONFIG_DIR is -> {0}", CONFIG_DIR);
    }

    private static void writeStatusFile(String type, String status) {
        try {
            File f = new File(InitServlet.CONFIG_DIR + File.separator + type + "_" + "status.txt");
            FileUtils.writeStringToFile(f, status, "UTF-8");
        } catch (IOException ex) {
            Logger.getLogger(InitServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void destroy() {
        IndexUtils.closeClient();
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
