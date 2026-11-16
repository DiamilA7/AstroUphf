package fr.astrouphf.engins;

import fr.astrouphf.missions.Mission;
import fr.astrouphf.monde.NiveauDanger;
import fr.astrouphf.monde.Position;
import fr.astrouphf.monde.Zone;
import fr.astrouphf.personnes.Competence;

import java.util.Objects;

/** Etat et identité communs à tous les véhicules. */
public abstract class Engin {

    private final int id;
    private final String nom;
    private Position position;
    private EtatEngin etat;
    private Mission missionEnCours;
    private final NiveauDanger resistanceDanger;

    protected Engin(int id, String nom, Position position, NiveauDanger resistanceDanger) {
        if (id <= 0) throw new IllegalArgumentException("L'identifiant doit être positif.");
        if (nom == null || nom.isBlank()) throw new IllegalArgumentException("Le nom est obligatoire.");
        this.id = id;
        this.nom = nom.trim();
        this.position = Objects.requireNonNull(position);
        this.resistanceDanger = Objects.requireNonNull(resistanceDanger);
        this.etat = EtatEngin.DISPONIBLE;
    }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public synchronized Position getPosition() { return position; }
    public synchronized EtatEngin getEtat() { return etat; }
    public NiveauDanger getResistanceDanger() { return resistanceDanger; }
    public synchronized Mission getMissionEnCours() { return missionEnCours; }

    protected synchronized void changerPosition(Position destination) {
        if (etat == EtatEngin.EN_PANNE) {
            throw new IllegalStateException("Un engin en panne ne peut pas se déplacer.");
        }
        position = Objects.requireNonNull(destination);
    }

    public synchronized boolean estDisponible() {
        return etat == EtatEngin.DISPONIBLE && missionEnCours == null;
    }

    public synchronized void affecterMission(Mission mission) {
        Objects.requireNonNull(mission);
        if (!estDisponible()) {
            throw new IllegalStateException("L'engin " + nom + " n'est pas disponible.");
        }
        missionEnCours = mission;
        etat = EtatEngin.EN_MISSION;
    }

    public synchronized void libererMission() {
        missionEnCours = null;
        if (etat != EtatEngin.EN_PANNE) {
            etat = EtatEngin.DISPONIBLE;
        }
    }

    public synchronized void mettreEnPanne() {
        etat = EtatEngin.EN_PANNE;
    }

    public synchronized void remettreEnService() {
        if (missionEnCours != null) {
            throw new IllegalStateException("Impossible de réparer un engin encore affecté à une mission.");
        }
        if (etat != EtatEngin.EN_PANNE) {
            throw new IllegalStateException("L'engin n'est pas en panne.");
        }
        etat = EtatEngin.DISPONIBLE;
    }

    public boolean peutAcceder(Zone zone) {
        return zone != null && zone.getNiveauDanger().estAccessibleAvec(resistanceDanger);
    }

    /** Un véhicule peut préciser une contrainte environnementale supplémentaire. */
    public boolean necessiteAtmosphere() {
        return false;
    }

    public abstract Competence competenceRequise();
}
