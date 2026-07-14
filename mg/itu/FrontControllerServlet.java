package mg.itu;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import mg.itu.mapping.Mapping;
import mg.itu.mapping.UrlMethod;
import mg.itu.util.MappingNotFoundException;
import mg.itu.view.ModelAndView;
import mg.itu.view.ViewResolver;

/**
 * Point d'entree unique (Front Controller) de toutes les requetes de l'appli.
 *
 * Principe :
 * 1) Au demarrage, DemmarageListener a deja scanne les @Controller / @UrlMapping
 *    et depose la Map<UrlMethod, Mapping> dans le ServletContext.
 * 2) init() recupere cette map une seule fois.
 * 3) A chaque requete (doGet/doPost -> processRequest) :
 *    - on construit la cle (url + methode HTTP)
 *    - on cherche le Mapping correspondant
 *    - si absent -> 404 (MappingNotFoundException)
 *    - si present -> on instancie le Controller, on invoque la methode par reflexion
 *      - si elle retourne un ModelAndView -> on resout la vue (ViewResolver),
 *        on pousse les attributs dans la request, et on forward vers la JSP
 *      - sinon -> on affiche simplement le resultat (texte)
 */
public class FrontControllerServlet extends HttpServlet {

    private Map<UrlMethod, Mapping> urlMappings;

    @Override
    public void init() throws ServletException {
        urlMappings = (Map<UrlMethod, Mapping>) getServletContext().getAttribute("urlMapping");

        if (urlMappings == null) {
            throw new ServletException(
                    "Aucun mapping trouve dans le ServletContext. "
                            + "Verifie que DemmarageListener s'est bien execute et que "
                            + "le context-param 'controllerPackage' est defini dans web.xml.");
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        try {
            String uri = request.getRequestURI();
            String contextPath = request.getContextPath();
            uri = uri.substring(contextPath.length());

            String httpMethod = request.getMethod();

            UrlMethod key = new UrlMethod(uri, httpMethod);
            Mapping mapping = urlMappings.get(key);

            if (mapping == null) {
                throw new MappingNotFoundException(
                        "Aucun mapping trouve pour URL = " + uri + " et method = " + httpMethod);
            }

            invoquer(mapping, request, response, out);

        } catch (MappingNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.println("<h2 style='color:red'>ERROR 404</h2>");
            out.println("<pre>" + e.getMessage() + "</pre>");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("<h2 style='color:red'>ERROR SYSTEM</h2>");
            out.println("<pre>" + e.getMessage() + "</pre>");
            e.printStackTrace();
        }
    }

    private void invoquer(Mapping mapping, HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws Exception {

        Object instance = mapping.getClasse().getDeclaredConstructor().newInstance();
        Method methode = mapping.getMethode();

        Object resultat = methode.invoke(instance);

        if (resultat instanceof ModelAndView) {

            ModelAndView mv = (ModelAndView) resultat;

            ViewResolver viewResolver = new ViewResolver();
            viewResolver.setNom_vue(mv.getNom_vue());
            viewResolver.setPrefix_vue(getServletContext().getInitParameter("prefixVue"));
            viewResolver.setExtension_vue(getServletContext().getInitParameter("suffixVue"));

            for (Map.Entry<String, Object> entry : mv.getAttributs().entrySet()) {
                request.setAttribute(entry.getKey(), entry.getValue());
            }

            RequestDispatcher dispatcher = request.getRequestDispatcher(viewResolver.getCheminCompletVue());
            dispatcher.forward(request, response);

        } else {
            // Le controller peut aussi renvoyer un simple texte/objet : on l'affiche tel quel
            out.println("<p>" + (resultat == null ? "null" : resultat.toString()) + "</p>");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
