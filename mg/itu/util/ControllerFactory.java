package mg.itu.util;

import java.util.HashMap;
import java.util.Map;

public class ControllerFactory {

    // unique instanc
    private static ControllerFactory instance;

    //  une classe -> son unique objet
    private final Map<Class<?>, Object> controleurs = new HashMap<>();

    private ControllerFactory() {
    }

    public static synchronized ControllerFactory getInstance() {
        if (instance == null) {
            instance = new ControllerFactory();
        }
        return instance;
    }

    /** Renvoie l'unique objet du controller, le cree au premier appel. */
    public synchronized Object getControleur(Class<?> classe) throws Exception {
        Object objet = controleurs.get(classe);
        if (objet == null) {
            objet = classe.getDeclaredConstructor().newInstance();
            controleurs.put(classe, objet);
        }
        return objet;
    }
}