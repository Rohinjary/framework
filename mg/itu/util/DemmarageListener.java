package mg.itu.util;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;

/**
 * Sprint 4 : c'est ICI, au demarrage de l'application (contextInitialized),
 * que tout le travail d'initialisation se fait : scan des Controllers,
 * construction de la Map<UrlMethod, Mapping>, puis on stocke le resultat
 * dans le ServletContext pour que le FrontControllerServlet n'ait plus qu'a
 * le relire dans son init() (sans refaire le scan a chaque redeploiement de servlet).
 */
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
