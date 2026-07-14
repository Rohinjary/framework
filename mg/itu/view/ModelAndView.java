package mg.itu.view;

import java.util.HashMap;
import java.util.Map;

/**
 * Objet que le Controller renvoie pour dire au FrontController :
 * "affiche telle vue (JSP), avec ces donnees dedans".
 *
 * nom_vue    = le nom logique de la vue (ex: "index" -> deviendra /WEB-INF/vues/index.jsp)
 * attributs  = les donnees a injecter dans la requete (request.setAttribute) avant le forward,
 *              pour que la JSP puisse les afficher avec ${nomAttribut}.
 */
public class ModelAndView {

    private String nom_vue;
    private Map<String, Object> attributs;

    public ModelAndView(String nom_vue) {
        this.nom_vue = nom_vue;
        this.attributs = new HashMap<>();
    }

    public String getNom_vue() {
        return nom_vue;
    }

    public void setNom_vue(String nom_vue) {
        this.nom_vue = nom_vue;
    }

    public Map<String, Object> getAttributs() {
        return attributs;
    }

    public void setAttributs(Map<String, Object> attributs) {
        this.attributs = attributs;
    }

    public Object getAttribut(String nom_attribut) {
        if (this.attributs == null) {
            return null;
        }
        if (nom_attribut == null || nom_attribut.isEmpty()) {
            throw new IllegalArgumentException("Le nom de l'attribut ne peut pas etre null ou vide.");
        }
        return this.attributs.get(nom_attribut);
    }

    public void addAttribut(String nom_attribut, Object valeur) {
        if (nom_attribut == null || nom_attribut.isEmpty()) {
            throw new IllegalArgumentException("Le nom de l'attribut ne peut pas etre null ou vide.");
        }
        this.attributs.put(nom_attribut, valeur);
    }
}
