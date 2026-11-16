package fr.astrouphf.personnes;

import java.util.Objects;

/** Un cadet suit la mission de son tuteur ; il ne stocke donc pas sa propre mission. */
public class Cadet extends Personne {

    private Operateur tuteur;
    private int missionsReussies;

    public Cadet(int id, String nom, String prenom, Genre genre,
                 String telephone, Operateur tuteur) {
        super(id, nom, prenom, genre, telephone);
        this.tuteur = Objects.requireNonNull(tuteur, "Un cadet doit avoir un tuteur.");
        this.missionsReussies = 0;
    }

    public synchronized Operateur getTuteur() { return tuteur; }
    public synchronized int getMissionsReussies() { return missionsReussies; }

    public synchronized void changerTuteur(Operateur nouveauTuteur) {
        Objects.requireNonNull(nouveauTuteur, "Le nouveau tuteur est obligatoire.");
        if (nouveauTuteur == tuteur) {
            return;
        }
        tuteur.retirerCadet(this);
        tuteur = nouveauTuteur;
        nouveauTuteur.ajouterCadet(this);
    }

    public synchronized void enregistrerMissionReussie() {
        missionsReussies++;
    }

    public synchronized boolean peutEtrePromu() {
        return missionsReussies >= 3;
    }
}
