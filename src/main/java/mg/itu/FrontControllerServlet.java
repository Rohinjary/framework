package mg.itu;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import mg.itu.mapping.Mapping;
import mg.itu.util.ControllerScanner;

public class FrontControllerServlet extends HttpServlet {
        private List<String> listController;
        private Map<String, Mapping> urlMappings;

    public void init() throws ServletException {

        listController = new ArrayList<>();
        urlMappings = new HashMap<>();

        try {

            ServletContext context = getServletContext();
            String basePackage = context.getInitParameter("controllerPackage");

            String path = basePackage.replace('.', '/');
            String realPath = context.getRealPath("/WEB-INF/classes/" + path);

            ControllerScanner scanner =
                new ControllerScanner(basePackage, realPath);

            listController = scanner.getListController();
            urlMappings = scanner.getUrlMappings();

            System.out.println("Controllers = " + listController);
            System.out.println("Mappings = " + urlMappings.keySet());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        String uri = request.getRequestURI().replace(request.getContextPath(), "");

        Mapping m = urlMappings.get(uri);
        // Mapping m = null;

        out.println("<h2>URL = " + uri + "</h2>");

        if (m != null) {
            out.println("<p>Controller = " + m.getClassName() + "</p>");
            out.println("<p>Methode = " + m.getMethodName() + "</p>");
        } else {
            out.println("<p>404 - mapping introuvable</p>");
        }

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
}