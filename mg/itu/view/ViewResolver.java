package mg.itu.view;

/**
 * Construit le chemin physique complet d'une vue a partir de :
 * prefix_vue (ex: "/WEB-INF/vues/") + nom_vue (ex: "index") + extension_vue (ex: ".jsp")
 * => "/WEB-INF/vues/index.jsp"
 *
 * Le prefixe et l'extension viennent des <context-param> du web.xml
 * (prefixVue / suffixVue), pour ne jamais coder en dur le chemin des vues
 * dans le framework.
 */
public class ViewResolver {

    private String prefix_vue;
    private String nom_vue;
    private String extension_vue;

    public String getPrefix_vue() {
        return prefix_vue;
    }

    public void setPrefix_vue(String prefix_vue) {
        this.prefix_vue = prefix_vue;
    }

    public String getExtension_vue() {
        return extension_vue;
    }

    public void setExtension_vue(String extension_vue) {
        this.extension_vue = extension_vue;
    }

    public String getNom_vue() {
        return nom_vue;
    }

    public void setNom_vue(String nom_vue) {
        this.nom_vue = nom_vue;
    }

    public String getCheminCompletVue() {
        return prefix_vue + nom_vue + extension_vue;
    }
}
