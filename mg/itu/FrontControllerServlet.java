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
import mg.itu.upload.UploadedFile;

import java.util.concurrent.ConcurrentHashMap;
import mg.itu.session.Session;

import mg.itu.annotation.Auth;
public class FrontControllerServlet extends HttpServlet {

    private Map<UrlMethod, Mapping> urlMappings;
    private Object springContext;
    private final Map<Class<?>, Object> controleurs = new ConcurrentHashMap<>();
    private String cleAuthentification;  
    private String cleRole;  

    public void init() throws ServletException {
        try {
            ServletContext context = getServletContext();

            ControllerScanner scanner = new ControllerScanner(
                    context.getInitParameter("controllerPackage"),
                    context.getRealPath("/WEB-INF/classes/mg/itu/controller")
            );

            urlMappings = scanner.getUrlMappings();
            springContext = context.getAttribute("springContext");
            cleAuthentification = context.getInitParameter("variable-authentification-check");
            cleRole = context.getInitParameter("variable-role-check");

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

        } catch (AuthentificationException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.println("<h2 style='color:red'>ERROR 401</h2>");
            out.println("<pre>" + e.getMessage() + "</pre>");
        } catch (AutorisationException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.println("<h2 style='color:red'>ERROR 403</h2>");
            out.println("<pre>" + e.getMessage() + "</pre>");
        }catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("<h2 style='color:red'>ERROR SYSTEM</h2>");
            out.println("<pre>" + e.getMessage() + "</pre>");
            e.printStackTrace();
        }
    }

    private void invoquer(Mapping mapping, HttpServletRequest request,
                           HttpServletResponse response, PrintWriter out) throws Exception {

        Object instance = getControleur(mapping.getClasse());
        Method methode = mapping.getMethode();

            Auth auth = methode.getAnnotation(Auth.class);
            if (auth != null) {
                verifierAuthentification(request, auth);
            }

        Object[] arguments = creerArguments(methode, springContext, request);
        Object resultat = methode.invoke(instance, arguments);

        // Recopie Session developpeur dans HttpSession

        for (Object arg : arguments) {
            if (arg instanceof Session) {
                sauvegarderSession((Session) arg, request);
            }
        }

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

    // upload file + formulaire maka parametre sy comparaison
    private Object[] creerArguments(Method methode, Object springContext, HttpServletRequest request)
            throws Exception {

        Parameter[] parametres = methode.getParameters();
        Object[] arguments = new Object[parametres.length];

        for (int i = 0; i < parametres.length; i++) {
            Parameter p = parametres[i];
            Class<?> type = p.getType();

            if (springContext != null && type.isAssignableFrom(springContext.getClass())) {
                arguments[i] = springContext;

            } else if (type == UploadedFile.class) {
                //  fichier envoye par le formulaire upload
                arguments[i] = lireFichier(request, p.getName());

            } else if (type == Session.class) {
                arguments[i] = chargerSession(request);

            } else if (estTypeSimple(type)) {
                arguments[i] = convertir(request.getParameter(p.getName()), type);

            } else {
                arguments[i] = construireObjet(type, p.getName(), request);
            }
        }
        return arguments;
    }

    private UploadedFile lireFichier(HttpServletRequest request, String champ) throws Exception {

        // pas un formulaire multipart : pas de fichier
        String ct = request.getContentType();
        if (ct == null || !ct.toLowerCase().startsWith("multipart/")) {
            return null;
        }

        Part part = request.getPart(champ);   // "champ" = name="..." de l'input file
        if (part == null || part.getSize() == 0) {
            return null;   // aucun fichier choisi
        }

        String nom = part.getSubmittedFileName();
        if (nom == null || nom.isEmpty()) {
            return null;
        }

        byte[] contenu;
        try (InputStream in = part.getInputStream()) {
            contenu = in.readAllBytes();
        }

        return new UploadedFile(nom, part.getContentType(), contenu);
    }

    // formulaire simple objet
    private boolean estTypeSimple(Class<?> t) {
        return t == String.class
                || t.isPrimitive()
                || t == Integer.class || t == Long.class
                || t == Double.class || t == Float.class
                || t == Boolean.class;
    }

    /** Transforme le text form par methode azo */
    private Object convertir(String valeur, Class<?> type) {

        if (type == String.class) {
            return valeur;
        }

        // champ absent ou vide
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
                continue;
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

    private void verifierAuthentification(HttpServletRequest request, Auth auth) {

        if (cleAuthentification == null || cleAuthentification.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Methode @Auth utilisee mais le parametre 'variable-authentification-check' "
                            + "est absent ou vide dans web.xml");
        }

        HttpSession httpSession = request.getSession(false);

        // 1)  l'authentification
        if (httpSession == null || httpSession.getAttribute(cleAuthentification) == null) {
            throw new AuthentificationException(
                    "Authentification requise : connectez-vous avant d'acceder a cette page");
        }

        // 2) le role (si @Auth(role=...))
        String[] rolesAutorises = auth.role();
        if (rolesAutorises.length == 0) {
            return;   // @Auth simple : etre connecte suffit
        }

        if (cleRole == null || cleRole.trim().isEmpty()) {
            throw new IllegalStateException(
                    "@Auth(role=...) utilise mais le parametre 'variable-role-check' "
                            + "est absent ou vide dans web.xml");
        }

        Object roleUtilisateur = httpSession.getAttribute(cleRole);

        if (roleUtilisateur != null) {
            for (String role : rolesAutorises) {
                if (role.equals(roleUtilisateur.toString())) {
                    return;   // role accepte
                }
            }
        }

        throw new AutorisationException(
                "Acces refuse : role requis = " + Arrays.toString(rolesAutorises)
                        + ", votre role = " + roleUtilisateur);
    }
    /** Un seul objet par classe de controller, cree au premier usage. */
    private Object getControleur(Class<?> classe) throws Exception {
        Object instance = controleurs.get(classe);
        if (instance == null) {
            synchronized (this) {
                instance = controleurs.get(classe);
                if (instance == null) {
                    instance = classe.getDeclaredConstructor().newInstance();
                    controleurs.put(classe, instance);
                }
            }
        }
        return instance;
    }

    /** HttpSession -> Session (copie) */
    private Session chargerSession(HttpServletRequest request) {
        HttpSession httpSession = request.getSession();   // la cree si elle n'existe pas
        Session session = new Session();

        Enumeration<String> noms = httpSession.getAttributeNames();
        while (noms.hasMoreElements()) {
            String nom = noms.nextElement();
            session.ajouter(nom, httpSession.getAttribute(nom));
        }
        return session;
    }

    /** Session -> HttpSession (copie, suppressions comprises). */
    private void sauvegarderSession(Session session, HttpServletRequest request) {
        HttpSession httpSession = request.getSession();

        // retirer les cles que le controller a supprimees
        List<String> aSupprimer = new ArrayList<>();
        Enumeration<String> noms = httpSession.getAttributeNames();
        while (noms.hasMoreElements()) {
            String nom = noms.nextElement();
            if (!session.contient(nom)) {
                aSupprimer.add(nom);
            }
        }
        for (String nom : aSupprimer) {
            httpSession.removeAttribute(nom);
        }

        // ecrire les cles ajoutees ou modifiees
        for (Map.Entry<String, Object> e : session.getDonnees().entrySet()) {
            httpSession.setAttribute(e.getKey(), e.getValue());
        }
    }
}