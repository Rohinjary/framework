package mg.itu.mapping;

import java.util.Objects;

/**
 * Represente la cle unique d'une route : une URL + une methode HTTP (GET/POST).
 * On la met en cle de Map pour pouvoir retrouver rapidement le controller
 * associe a une requete, et pour interdire 2 mappings identiques (meme url + meme methode).
 */
public class UrlMethod {

    private String url;
    private String method;

    public UrlMethod(String url, String method) {
        this.url = url;
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlMethod urlMethod = (UrlMethod) o;
        return Objects.equals(url, urlMethod.url) && Objects.equals(method, urlMethod.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, method);
    }
}
