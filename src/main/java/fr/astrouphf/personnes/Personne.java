package fr.astrouphf.personnes;

import java.util.Objects;

/** Informations communes à tous les membres d'une mission spatiale. */
public abstract class Personne {

    private final int id;
    private final String nom;
    private final String prenom;
    private final Genre genre;
    private String telephone;

    protected Personne(int id, String nom, String prenom,
                       Genre genre, String telephone) {
        if (id <= 0) {
            throw new IllegalArgumentException("L'identifiant doit être positif.");
        }
        this.id = id;
        this.nom = texteObligatoire(nom, "nom");
        this.prenom = texteObligatoire(prenom, "prénom");
        this.genre = Objects.requireNonNull(genre, "Le genre est obligatoire.");
        this.telephone = texteObligatoire(telephone, "téléphone");
    }

    private static String texteObligatoire(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("Le champ " + champ + " est obligatoire.");
        }
        return valeur.trim();
    }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public Genre getGenre() { return genre; }
    public String getTelephone() { return telephone; }

    public void setTelephone(String telephone) {
        this.telephone = texteObligatoire(telephone, "téléphone");
    }
}
