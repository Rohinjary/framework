package mg.itu;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.util.List;
import java.util.ArrayList;

public class FrontControllerServlet extends HttpServlet {
    private List<String> listController;

    public void init() throws ServletException {
        listController = new ArrayList<>();
        
        try{
            ServletContext context = getServletContext();
            String basePackage = context.getInitParameter("controllerPackage");

            //chemin
            String path = basePackage.replace('.', '/');
            String vraiPath = context.getRealPath("/WEB-INF/classes/" + path);

            //scan fichier
            File dir = new File(vraiPath);

            for(File f : dir.listFiles()){
                if(f.getName().endsWith(".class")){
                    //enlever .class
                    String className = basePackage + "." + f.getName().replace(".class", "");

                    Class<?> clazz = Class.forName(className);

                    if(clazz.isAnnotationPresent(mg.itu.annotation.Controller.class)){
                        listController.add(className);
                    }
                }
            }
            System.out.println("Package = " + basePackage);
            System.out.println("Path = " + vraiPath);



        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println("<h1>Front Controller</h1>");
        out.println("<p>URL recue : " + request.getRequestURL() + "</p>");
        out.println("<h2>Controllers detectes :</h2>");
        out.println(listController);

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
}