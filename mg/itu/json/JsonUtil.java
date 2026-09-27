package mg.itu.json;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

public class JsonUtil {


    public static String toJson(Object obj) {
        StringBuilder sb = new StringBuilder();
        ecrireValeur(obj, sb);
        return sb.toString();
    }

    private static void ecrireValeur(Object obj, StringBuilder sb) {

        if (obj == null) {
            sb.append("null");

        } else if (obj instanceof String || obj instanceof Character) {
            sb.append('"').append(echapper(obj.toString())).append('"');

        } else if (obj instanceof Number || obj instanceof Boolean) {
            sb.append(obj.toString());

        } else if (obj instanceof Enum) {
            sb.append('"').append(((Enum<?>) obj).name()).append('"');

        } else if (obj instanceof Map<?, ?>) {
            ecrireMap((Map<?, ?>) obj, sb);

        } else if (obj instanceof Collection<?>) {
            ecrireTableau((Collection<?>) obj, sb);

        } else if (obj.getClass().isArray()) {
            ecrireTableauReflexion(obj, sb);

        } else {
            ecrireObjet(obj, sb);
        }
    }

    private static void ecrireMap(Map<?, ?> map, StringBuilder sb) {
        sb.append('{');
        boolean premier = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!premier) sb.append(',');
            premier = false;
            sb.append('"').append(echapper(String.valueOf(entry.getKey()))).append("\":");
            ecrireValeur(entry.getValue(), sb);
        }
        sb.append('}');
    }

    private static void ecrireTableau(Collection<?> collection, StringBuilder sb) {
        sb.append('[');
        boolean premier = true;
        for (Object o : collection) {
            if (!premier) sb.append(',');
            premier = false;
            ecrireValeur(o, sb);
        }
        sb.append(']');
    }

    private static void ecrireTableauReflexion(Object tableau, StringBuilder sb) {
        int longueur = java.lang.reflect.Array.getLength(tableau);
        sb.append('[');
        for (int i = 0; i < longueur; i++) {
            if (i > 0) sb.append(',');
            ecrireValeur(java.lang.reflect.Array.get(tableau, i), sb);
        }
        sb.append(']');
    }

    private static void ecrireObjet(Object obj, StringBuilder sb) {
        sb.append('{');
        boolean premier = true;

        for (Method m : obj.getClass().getMethods()) {

            if (m.getParameterCount() != 0 || m.getDeclaringClass() == Object.class) {
                continue;
            }

            String nomProp = nomPropriete(m);
            if (nomProp == null) {
                continue;
            }

            try {
                Object valeur = m.invoke(obj);
                if (!premier) sb.append(',');
                premier = false;
                sb.append('"').append(nomProp).append("\":");
                ecrireValeur(valeur, sb);
            } catch (Exception ignored) {
            }
        }

        sb.append('}');
    }

    private static String nomPropriete(Method m) {
        String nom = m.getName();

        if (nom.startsWith("get") && nom.length() > 3 && !nom.equals("getClass")) {
            return Character.toLowerCase(nom.charAt(3)) + nom.substring(4);
        }

        if (nom.startsWith("is") && nom.length() > 2
                && (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class)) {
            return Character.toLowerCase(nom.charAt(2)) + nom.substring(3);
        }

        return null;
    }

    private static String echapper(String s) {
        StringBuilder r = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  r.append("\\\""); break;
                case '\\': r.append("\\\\"); break;
                case '\n': r.append("\\n");  break;
                case '\r': r.append("\\r");  break;
                case '\t': r.append("\\t");  break;
                default:   r.append(c);
            }
        }
        return r.toString();
    }
}