package mg.itu;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.util.*;

import mg.itu.mapping.*;
import mg.itu.util.*;
import mg.itu.controller.*;

public class FrontControllerServlet extends HttpServlet {

    private Map<UrlMethod, Mapping> urlMappings;

    
    public void init() throws ServletException {
        try {
            ServletContext context = getServletContext();

            ControllerScanner scanner = new ControllerScanner(
                    context.getInitParameter("controllerPackage"),
                    context.getRealPath("/WEB-INF/classes/mg/itu/controller")
            );

            urlMappings = scanner.getUrlMappings();

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

            // URL propre
            String uri = request.getRequestURI();
            String contextPath = request.getContextPath();

            uri = uri.substring(contextPath.length());

            String httpMethod = request.getMethod();

            UrlMethod key = new UrlMethod(uri, httpMethod);

            Mapping m = urlMappings.get(key);

            // mapping introuvable => exception
            if (m == null) {
                throw new MappingNotFoundException(
                        "Aucune mapping trouvé pour URL = "
                                + uri + " et method = " + httpMethod
                );
            }

            // affichage si OK
            out.println("<h2>URL = " + uri + "</h2>");
            out.println("<p>HTTP Method = " + httpMethod + "</p>");
            out.println("<p>Controller = " + m.getClassName() + "</p>");
            out.println("<p>Methode = " + m.getMethodName() + "</p>");

        } catch (MappingNotFoundException e) {
            out.println("<h2 style='color:red'>ERROR 404</h2>");
            out.println("<pre>" + e.getMessage() + "</pre>");
        } catch (Exception e) {
            out.println("<h2 style='color:red'>ERROR SYSTEM</h2>");
            out.println("<pre>" + e.getMessage() + "</pre>");
        }
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