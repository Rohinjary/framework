package mg.itu.upload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UploadedFile {

    private final String nom;
    private final String contentType;
    private final byte[] contenu;

    public UploadedFile(String nom, String contentType, byte[] contenu) {
        this.nom = nom;
        this.contentType = contentType;
        this.contenu = contenu;
    }

    public String getNom() { return nom; }
    public String getContentType() { return contentType; }
    public byte[] getContenu() { return contenu; }
    public long getTaille() { return contenu.length; }

    /**
     * Ecrit le fichier dans le dossier donne (cree le dossier si besoin).
     * Retourne le chemin complet du fichier ecrit.
     */
    public String enregistrer(String dossier) throws IOException {
        Path dir = Paths.get(dossier);
        Files.createDirectories(dir);

        // securite : on ne garde que le nom, jamais un chemin (ex: ../../etc/passwd)
        String nomPropre = Paths.get(nom.replace("\\", "/")).getFileName().toString();

        Path cible = dir.resolve(nomPropre);
        Files.write(cible, contenu);
        return cible.toString();
    }
}