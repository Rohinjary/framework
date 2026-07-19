package mg.itu;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.util.*;

import mg.itu.mapping.*;
import mg.itu.util.*;
import mg.itu.view.*;

public class FrontControllerServlet extends HttpServlet {

    private Map<UrlMethod, Mapping> urlMappings;
    private Object springContext;

    public void init() throws ServletException {
        try {
            ServletContext context = getServletContext();

            ControllerScanner scanner = new ControllerScanner(
                    context.getInitParameter("controllerPackage"),
                    context.getRealPath("/WEB-INF/classes/mg/itu/controller")
            );

            urlMappings = scanner.getUrlMappings();

            springContext = context.getAttribute("springContext");

            System.out.println("Mappings chargés = " + urlMappings.keySet());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        try {

            String uri = request.getRequestURI();
            String contextPath = request.getContextPath();

            uri = uri.substring(contextPath.length());

            String httpMethod = request.getMethod();

            UrlMethod key = new UrlMethod(uri, httpMethod);

            Mapping m = urlMappings.get(key);

            if (m == null) {
                throw new MappingNotFoundException(
                        "Aucune mapping trouvé pour URL = "
                                + uri + " et method = " + httpMethod
                );
            }

            invoquer(m, request, response, out);

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

    private void invoquer(Mapping mapping, HttpServletRequest request,
                           HttpServletResponse response, PrintWriter out) throws Exception {

        Object instance = mapping.getClasse().getDeclaredConstructor().newInstance();
        Method methode = mapping.getMethode();

       
        Object[] arguments = creerArguments(methode, springContext);

        Object resultat = methode.invoke(instance, arguments);

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

        } else if (resultat == null) {
            out.println("<p>Methode executee (pas de retour ModelAndView).</p>");
        } else {
            out.println("<p>" + resultat + "</p>");
        }
    }

    private Object[] creerArguments(Method methode, Object springContext) {
        Parameter[] parametres = methode.getParameters();
        Object[] arguments = new Object[parametres.length];

        for (int i = 0; i < parametres.length; i++) {
            Parameter p = parametres[i];
            if (springContext != null && p.getType().isAssignableFrom(springContext.getClass())) {
                arguments[i] = springContext;
            }
        }
        return arguments;
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res);
    }


    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res);
    }
}
