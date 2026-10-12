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
import mg.itu.annotation.ResponseBody;
import mg.itu.json.JsonUtil;

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
                        "Aucune mapping trouvé pour URL = " + uri + " et method = " + httpMethod
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

        Object[] arguments = creerArguments(methode, springContext, request);
        Object resultat = methode.invoke(instance, arguments);

        // truc de json
        if (methode.isAnnotationPresent(ResponseBody.class)) {
            response.setContentType("application/json;charset=UTF-8");
            out.print(JsonUtil.toJson(resultat));
            return;
        }

        if (resultat instanceof ModelAndView) {

            ModelAndView mv = (ModelAndView) resultat;
            
            if (mv.getNom_vue().startsWith("redirect:")) {
                String url = mv.getNom_vue().substring("redirect:".length());
                response.sendRedirect(request.getContextPath() + url);
                return;
            }

            ViewResolver viewResolver = new ViewResolver();
            viewResolver.setNom_vue(mv.getNom_vue());
            viewResolver.setPrefix_vue(getServletContext().getInitParameter("prefixVue"));
            viewResolver.setExtension_vue(getServletContext().getInitParameter("suffixVue"));

            for (Map.Entry<String, Object> entry : mv.getAttributs().entrySet()) {
                request.setAttribute(entry.getKey(), entry.getValue());
            }

            RequestDispatcher dispatcher = request.getRequestDispatcher(viewResolver.getCheminCompletVue());
            dispatcher.forward(request, response);

        }
    }

private Object[] creerArguments(Method methode, Object springContext, HttpServletRequest request)
        throws Exception {

    Parameter[] parametres = methode.getParameters();
    Object[] arguments = new Object[parametres.length];

    for (int i = 0; i < parametres.length; i++) {
        Parameter p = parametres[i];
        Class<?> type = p.getType();

        if (springContext != null && type.isAssignableFrom(springContext.getClass())) {
            // cas 1 : le contexte Spring
            arguments[i] = springContext;

        } else if (estTypeSimple(type)) {
            // cas 2 : String, int, double, boolean...
            arguments[i] = convertir(request.getParameter(p.getName()), type);

        } else {
            // cas 3 : un objet (Emp...) rempli depuis les champs du formulaire
            arguments[i] = construireObjet(type, p.getName(), request);
        }
    }
    return arguments;
}

private boolean estTypeSimple(Class<?> t) {
    return t == String.class
            || t.isPrimitive()
            || t == Integer.class || t == Long.class
            || t == Double.class || t == Float.class
            || t == Boolean.class;
}

/** Transforme le texte du formulaire dans le type attendu par la methode */
private Object convertir(String valeur, Class<?> type) {

    if (type == String.class) {
        return valeur;
    }

    // champ absent ou vide : null pour les objets, valeur par defaut pour les primitifs
    if (valeur == null || valeur.trim().isEmpty()) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0f;
        if (type == boolean.class) return false;
        return null;
    }

    valeur = valeur.trim();

    if (type == int.class || type == Integer.class) return Integer.parseInt(valeur);
    if (type == long.class || type == Long.class) return Long.parseLong(valeur);
    if (type == double.class || type == Double.class) return Double.parseDouble(valeur);
    if (type == float.class || type == Float.class) return Float.parseFloat(valeur);
    if (type == boolean.class || type == Boolean.class) {
        return valeur.equalsIgnoreCase("true") || valeur.equals("on") || valeur.equals("1");
    }
    return null;
}


    private Object construireObjet(Class<?> classe, String nomParam, HttpServletRequest request)
            throws Exception {

        Object objet = classe.getDeclaredConstructor().newInstance();

        for (Method m : classe.getMethods()) {

            if (!m.getName().startsWith("set") || m.getName().length() <= 3
                    || m.getParameterCount() != 1) {
                continue;
            }

            Class<?> typeChamp = m.getParameterTypes()[0];
            if (!estTypeSimple(typeChamp)) {
                continue;
            }

            // setNom -> "nom"
            String champ = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);

            String valeur = request.getParameter(nomParam + "." + champ);   // ex: emp.nom
            if (valeur == null) {
                valeur = request.getParameter(champ);                       // ex: nom
            }
            if (valeur == null) {
                continue;   // pas envoye par le formulaire : on laisse la valeur par defaut
            }

            m.invoke(objet, convertir(valeur, typeChamp));
        }
        return objet;
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
