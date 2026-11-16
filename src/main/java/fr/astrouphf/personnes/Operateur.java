package fr.astrouphf.personnes;

import fr.astrouphf.missions.Mission;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/** Un opérateur a de 1 à 3 compétences et participe à une mission à la fois. */
public class Operateur extends Personne {

    private final EnumSet<Competence> competences;
    private Mission missionEnCours;
    private final Set<Cadet> cadetsEncadres = new HashSet<>();

    public Operateur(int id, String nom, String prenom, Genre genre,
                     String telephone, Set<Competence> competences) {
        super(id, nom, prenom, genre, telephone);
        if (competences == null || competences.isEmpty() || competences.size() > 3) {
            throw new IllegalArgumentException(
                    "Un opérateur doit posséder entre 1 et 3 compétences.");
        }
        this.competences = EnumSet.copyOf(competences);
    }

    public Set<Competence> getCompetences() {
        return Collections.unmodifiableSet(EnumSet.copyOf(competences));
    }

    public boolean possedeCompetence(Competence competence) {
        return competences.contains(competence);
    }

    public synchronized boolean estDisponible() {
        return missionEnCours == null;
    }

    public synchronized Mission getMissionEnCours() { return missionEnCours; }

    public synchronized void affecterMission(Mission mission) {
        if (mission == null) {
            throw new IllegalArgumentException("La mission ne peut pas être nulle.");
        }
        if (!estDisponible()) {
            throw new IllegalStateException("L'opérateur participe déjà à une mission.");
        }
        missionEnCours = mission;
    }

    public synchronized void libererMission() {
        missionEnCours = null;
    }

    public synchronized void ajouterCadet(Cadet cadet) {
        if (cadet == null) {
            throw new IllegalArgumentException("Le cadet ne peut pas être nul.");
        }
        cadetsEncadres.add(cadet);
    }

    public synchronized void retirerCadet(Cadet cadet) {
        cadetsEncadres.remove(cadet);
    }

    public synchronized Set<Cadet> getCadetsEncadres() {
        return Collections.unmodifiableSet(new HashSet<>(cadetsEncadres));
    }
}
