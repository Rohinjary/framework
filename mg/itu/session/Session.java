package mg.itu.session;

import java.util.HashMap;
import java.util.Map;


public class Session {

    private final Map<String, Object> donnees = new HashMap<>();

    public void ajouter(String cle, Object valeur) {
        donnees.put(cle, valeur);
    }

    public Object get(String cle) {
        return donnees.get(cle);
    }

    public void supprimer(String cle) {
        donnees.remove(cle);
    }

    public boolean contient(String cle) {
        return donnees.containsKey(cle);
    }

    public Map<String, Object> getDonnees() {
        return donnees;
    }
}