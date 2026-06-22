package mg.itu.util;

import java.io.*;
import java.util.*;
import mg.itu.mapping.Mapping;

public class ControllerScanner {

    private List<String> listController = new ArrayList<>();
    private Map<String, Mapping> urlMappings = new HashMap<>();

    public ControllerScanner(String basePackage, String realPath) {

        try {
            File dir = new File(realPath);

            for (File f : dir.listFiles()) {

                if (f.getName().endsWith(".class")) {

                    String className =
                        basePackage + "." + f.getName().replace(".class", "");

                    Class<?> clazz = Class.forName(className);

                    // controller
                    if (clazz.isAnnotationPresent(mg.itu.annotation.Controller.class)) {
                        listController.add(className);

                        // scanner methods
                        for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {

                            if (m.isAnnotationPresent(mg.itu.annotation.UrlMapping.class)) {

                                String url = m.getAnnotation(
                                    mg.itu.annotation.UrlMapping.class
                                ).value();

                                urlMappings.put(
                                    url,
                                    new Mapping(className, m.getName())
                                );
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getListController() {
        return listController;
    }

    public Map<String, Mapping> getUrlMappings() {
        return urlMappings;
    }
}