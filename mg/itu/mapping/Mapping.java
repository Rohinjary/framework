package mg.itu.mapping;

import java.lang.reflect.Method;

/**
 * Contient tout ce qu'il faut pour executer une route trouvee :
 * - la Class du Controller
 * - la Method (java.lang.reflect) a invoquer dessus
 *
 * Avant, on stockait juste des String (nom de classe / nom de methode), ce qui obligeait
 * a refaire un Class.forName(...) + getMethod(...) a CHAQUE requete. Ici on garde
 * directement les objets Class/Method recuperes une seule fois au demarrage (scan),
 * comme ca l'invocation a chaque requete est immediate.
 */
public class Mapping {

    private Class<?> classe;
    private Method methode;

    public Mapping(Class<?> classe, Method methode) {
        this.classe = classe;
        this.methode = methode;
    }

    public Class<?> getClasse() {
        return classe;
    }

    public void setClasse(Class<?> classe) {
        this.classe = classe;
    }

    public Method getMethode() {
        return methode;
    }

    public void setMethode(Method methode) {
        this.methode = methode;
    }
}
