package mg.itu.util;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mg.itu.annotation.Controller;
import mg.itu.annotation.UrlMapping;
import mg.itu.mapping.Mapping;

public class ControllerScanner {

    private List<String> listController = new ArrayList<>();
    private Map<UrlMethod, Mapping> urlMappings = new HashMap<>();

    public ControllerScanner(String basePackage, String realPath) {

        try {
            File dir = new File(realPath);

            for (File f : dir.listFiles()) {

                if (f.getName().endsWith(".class")) {

                    String className = basePackage + "." + f.getName().replace(".class", "");
                    Class<?> clazz = Class.forName(className);

                    if (clazz.isAnnotationPresent(Controller.class)) {

                        listController.add(className);

                        for (Method m : clazz.getDeclaredMethods()) {

                            if (m.isAnnotationPresent(UrlMapping.class)) {

                                UrlMapping annotation = m.getAnnotation(UrlMapping.class);
                                String url = annotation.value();
                                String httpMethod = annotation.method();

                                UrlMethod urlMethod = new UrlMethod(url, httpMethod);

                                if (urlMappings.containsKey(urlMethod)) {
                                    throw new RuntimeException("URL deja declaree : " + httpMethod + " " + url);
                                }

                                Mapping mapping = new Mapping(clazz, m);
                                urlMappings.put(urlMethod, mapping);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public List<String> getListController() {
        return listController;
    }

    public Map<UrlMethod, Mapping> getUrlMappings() {
        return urlMappings;
    }
}
