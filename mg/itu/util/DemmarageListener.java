package mg.itu.util;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class DemmarageListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();

        try {
            String basePackage = servletContext.getInitParameter("controllerPackage");

            ControllerScanner scanner = new ControllerScanner(basePackage);

            servletContext.setAttribute("listeControllers", scanner.getListController());
            servletContext.setAttribute("urlMapping", scanner.getUrlMappings());

            servletContext.log("## Application demarree : " + scanner.getUrlMappings().size() + " route(s) trouvee(s) ##");

        } catch (Exception e) {
            System.err.println("================================");
            System.err.println("Erreur lors de l'initialisation du framework : " + e.getMessage());
            System.err.println("================================");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        sce.getServletContext().log("## Arret de l'application ##");
    }
}
