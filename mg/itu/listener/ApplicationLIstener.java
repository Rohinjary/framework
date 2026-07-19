package mg.itu.listener;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;


@WebListener
public class ApplicationListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();

        try {
            Class<?> utils = Class.forName(
                    "org.springframework.web.context.support.WebApplicationContextUtils");

            Object springContext = utils
                    .getMethod("getWebApplicationContext", ServletContext.class)
                    .invoke(null, servletContext);

            servletContext.setAttribute("springContext", springContext);

            servletContext.log("## Spring detecte et recupere avec succes ##");

        } catch (Exception e) {
            servletContext.log("## Spring non detecte (appli sans base de donnees ?) : " + e.getMessage() + " ##");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        sce.getServletContext().log("## Arret de l'application ##");
    }
}
