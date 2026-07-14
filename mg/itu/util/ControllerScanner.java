package mg.itu.util;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mg.itu.annotation.Controller;
import mg.itu.annotation.UrlMapping;
import mg.itu.mapping.Mapping;
import mg.itu.mapping.UrlMethod;

/**
 * Scanne un package (donne par son nom, ex: "mg.itu.controller") a la recherche
 * de toutes les classes annotees @Controller, puis de toutes leurs methodes
 * annotees @UrlMapping, pour construire la Map<UrlMethod, Mapping> utilisee
 * par le FrontControllerServlet pour router les requetes.
 *
 * Avant : le scan se faisait via getRealPath(...) (chemin disque), ce qui casse
 * si l'appli est deployee en .war non explode. Ici on utilise le ClassLoader
 * (classpath), qui fonctionne dans tous les cas.
 */
public class ControllerScanner {

    private List<String> listController = new ArrayList<>();
    private Map<UrlMethod, Mapping> urlMappings = new HashMap<>();

    public ControllerScanner(String basePackage) throws Exception {

        String cheminDossier = basePackage.replace('.', '/');

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL ressource = classLoader.getResource(cheminDossier);

        if (ressource == null) {
            throw new IllegalArgumentException("Le package " + basePackage + " n'existe pas (ou est vide).");
        }

        File dossier = new File(ressource.toURI());

        if (!dossier.exists() || !dossier.isDirectory()) {
            throw new IllegalArgumentException("Le package " + basePackage + " n'est pas un dossier valide.");
        }

        File[] fichiers = dossier.listFiles();
        if (fichiers == null) {
            return;
        }

        for (File fichier : fichiers) {

            if (!fichier.isFile() || !fichier.getName().endsWith(".class")) {
                continue;
            }

            String className = basePackage + "." + fichier.getName().substring(0, fichier.getName().length() - 6);

            Class<?> clazz = Class.forName(className);

            if (!clazz.isAnnotationPresent(Controller.class)) {
                continue;
            }

            listController.add(className);

            for (Method m : clazz.getDeclaredMethods()) {

                if (!m.isAnnotationPresent(UrlMapping.class)) {
                    continue;
                }

                UrlMapping annotation = m.getAnnotation(UrlMapping.class);

                String url = annotation.value();
                String httpMethod = annotation.method();

                UrlMethod urlMethod = new UrlMethod(url, httpMethod);

                // Verifier les doublons : 2 routes identiques (meme url + meme methode HTTP) interdites
                if (urlMappings.containsKey(urlMethod)) {
                    throw new Exception("URL deja declaree : " + httpMethod + " " + url);
                }

                Mapping mapping = new Mapping(clazz, m);

                urlMappings.put(urlMethod, mapping);
            }
        }
    }

    public List<String> getListController() {
        return listController;
    }

    public Map<UrlMethod, Mapping> getUrlMappings() {
        return urlMappings;
    }
}
