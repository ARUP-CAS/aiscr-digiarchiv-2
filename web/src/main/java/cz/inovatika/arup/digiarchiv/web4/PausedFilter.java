package cz.inovatika.arup.digiarchiv.web4;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alber
 */
@WebFilter(filterName = "PausedFilter", urlPatterns = {"/*"})
public class PausedFilter implements Filter {
    
    public static final Logger LOGGER = Logger.getLogger(PausedFilter.class.getName());

    // The filter configuration object we are associated with.  If
    // this value is null, this filter instance is not currently
    // configured.
    private FilterConfig filterConfig = null;
    
    public PausedFilter() {
    }
    
    
    /**
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @param chain The filter chain we are processing
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {
        
        Throwable problem = null;
        try {
            
            HttpServletRequest req =(HttpServletRequest) request;
            
            String path = req.getPathInfo();
            if (req.getRequestURI().contains("config")) {
                Options.resetInstance();
            }
            
            boolean paused = Options.getInstance().getBoolean("isPaused", false);
            String msg = Options.getInstance().getString("pausedMsg", "Work in progress");
            
            if (path != null && path.contains("/oai")) {
                paused = Options.getInstance().getJSONObject("OAI").optBoolean("isPaused", false);
                msg = Options.getInstance().getJSONObject("OAI").optString("pausedMsg", "Work in progress");
            }
            
            if (paused) {
                HttpServletResponse resp =(HttpServletResponse) response;
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE); 
                resp.setContentType("text/html;charset=UTF-8");
                PrintWriter writer = resp.getWriter();
                writer.print("<html><head><meta charset=\"utf-8\"></head><body>");
                writer.print(msg);
                writer.print("</body></html>");
                LOGGER.log(Level.INFO, "System is paused. {0}", req.getRequestURI()); 
            } else {
                chain.doFilter(request, response);
            }
            
        } catch (Throwable t) {
            // If an exception is thrown somewhere down the filter chain,
            // we still want to execute our after processing, and then
            // rethrow the problem after that.
            problem = t;
        }

        // If there was a problem, we want to rethrow it if it is
        // a known type, otherwise log it.
        if (problem != null) {
            if (problem instanceof ServletException) {
                throw (ServletException) problem;
            }
            if (problem instanceof IOException) {
                throw (IOException) problem;
            }
            sendProcessingError(problem, response);
        }
    }

    /**
     * Return the filter configuration object for this filter.
     */
    public FilterConfig getFilterConfig() {
        return (this.filterConfig);
    }

    /**
     * Set the filter configuration object for this filter.
     *
     * @param filterConfig The filter configuration object
     */
    public void setFilterConfig(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    /**
     * Init method for this filter
     */
    @Override
    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        if (filterConfig != null) {
        }
    }

    /**
     * Return a String representation of this object.
     */
    @Override
    public String toString() {
        if (filterConfig == null) {
            return ("PausedFilter()");
        }
        StringBuffer sb = new StringBuffer("PausedFilter(");
        sb.append(filterConfig);
        sb.append(")");
        return (sb.toString());
    }
    
    private void sendProcessingError(Throwable t, ServletResponse response) {
        LOGGER.log(Level.SEVERE, "Unhandled exception while processing request.", t);
        try {
            if (response instanceof HttpServletResponse) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The resource did not process correctly");
            } else {
                response.setContentType("text/plain");
                PrintWriter pw = response.getWriter();
                pw.print("The resource did not process correctly");
                pw.flush();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to send generic error response.", ex);
        }
    }
    
}
