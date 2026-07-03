package mg.itu.util;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class DemmarageListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // sce.getServletContext().log("## Lancement de l'application ##");
        
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        sce.getServletContext().log("## Arrêt de l'application ##");
    }
}
