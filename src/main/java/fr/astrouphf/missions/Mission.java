package fr.astrouphf.missions;

import fr.astrouphf.engins.Cartographe;
import fr.astrouphf.engins.Engin;
import fr.astrouphf.engins.Preleveur;
import fr.astrouphf.exploration.ResultatExploration;
import fr.astrouphf.exploration.TypeActionExploration;
import fr.astrouphf.monde.CorpsCeleste;
import fr.astrouphf.monde.Zone;
import fr.astrouphf.personnes.Cadet;
import fr.astrouphf.personnes.DirecteurMission;
import fr.astrouphf.personnes.Operateur;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Une mission regroupe les ressources, les zones visées et son historique scientifique. */
public class Mission {

    private final String nom;
    private final LocalDateTime dateLancement;
    private final double budget;
    private volatile EtatMission etat = EtatMission.PLANIFIEE;
    private final CorpsCeleste corpsCeleste;
    private final DirecteurMission directeur;
    private final Set<Operateur> operateurs;
    private final Set<Engin> engins;
    private final Set<Zone> zonesObjectifs;
    private final List<ResultatExploration> resultats = new ArrayList<>();
    private final Set<Cadet> cadetsParticipants = new HashSet<>();
    private final Set<ZoneEnginKey> confirmationsReexploration = new HashSet<>();
    private volatile RapportMission rapport;

    public Mission(String nom, LocalDateTime dateLancement, double budget,
                   CorpsCeleste corpsCeleste, DirecteurMission directeur,
                   Set<Operateur> operateurs, Set<Engin> engins,
                   Set<Zone> zonesObjectifs) {
        if (nom == null || nom.isBlank()) throw new IllegalArgumentException("Le nom de mission est obligatoire.");
        if (!Double.isFinite(budget) || budget < 0) throw new IllegalArgumentException("Budget invalide.");
        this.nom = nom.trim();
        this.dateLancement = Objects.requireNonNull(dateLancement);
        this.budget = budget;
        this.corpsCeleste = Objects.requireNonNull(corpsCeleste);
        this.directeur = Objects.requireNonNull(directeur);
        this.operateurs = copieNonVide(operateurs, "Une mission doit avoir au moins un opérateur.");
        this.engins = copieNonVide(engins, "Une mission doit avoir au moins un engin.");
        this.zonesObjectifs = copieNonVide(zonesObjectifs, "Une mission doit avoir au moins une zone objectif.");
        if (this.zonesObjectifs.stream().anyMatch(zone -> !corpsCeleste.getCarte().contient(zone))) {
            throw new IllegalArgumentException("Toutes les zones objectifs doivent appartenir à la carte du corps céleste.");
        }
    }

    private static <T> Set<T> copieNonVide(Set<T> source, String message) {
        if (source == null || source.isEmpty() || source.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(message);
        }
        return new HashSet<>(source);
    }

    public String getNom() { return nom; }
    public LocalDateTime getDateLancement() { return dateLancement; }
    public double getBudget() { return budget; }
    public EtatMission getEtat() { return etat; }
    public CorpsCeleste getCorpsCeleste() { return corpsCeleste; }
    public DirecteurMission getDirecteur() { return directeur; }

    public synchronized Set<Operateur> getOperateurs() {
        return Collections.unmodifiableSet(new HashSet<>(operateurs));
    }

    public synchronized Set<Engin> getEngins() {
        return Collections.unmodifiableSet(new HashSet<>(engins));
    }

    public synchronized Set<Zone> getZonesObjectifs() {
        return Collections.unmodifiableSet(new HashSet<>(zonesObjectifs));
    }

    public synchronized List<ResultatExploration> getResultats() {
        return List.copyOf(resultats);
    }

    public synchronized Set<Cadet> getCadetsParticipants() {
        return Collections.unmodifiableSet(new HashSet<>(cadetsParticipants));
    }

    public RapportMission getRapport() { return rapport; }

    /** L'appel explicite à cette méthode constitue la confirmation d'une réexploration. */
    public synchronized void confirmerReexploration(Zone zone, Class<? extends Engin> typeEngin) {
        Objects.requireNonNull(zone);
        Objects.requireNonNull(typeEngin);
        if (etat == EtatMission.TERMINEE) throw new IllegalStateException("La mission est terminée.");
        if (!zonesObjectifs.contains(zone)) throw new IllegalArgumentException("Cette zone n'est pas un objectif de la mission.");
        if (!zone.aDejaEteExploreePar(typeEngin)) {
            throw new IllegalArgumentException("Cette zone n'a pas encore été explorée par ce type d'engin.");
        }
        confirmationsReexploration.add(new ZoneEnginKey(zone, typeEngin));
    }

    public synchronized boolean reexplorationAutorisee(Zone zone, Class<? extends Engin> typeEngin) {
        return confirmationsReexploration.contains(new ZoneEnginKey(zone, typeEngin));
    }

    public synchronized void demarrer(Set<Cadet> cadetsAffectes) {
        if (etat != EtatMission.PLANIFIEE) throw new IllegalStateException("La mission n'est pas planifiée.");
        if (cadetsAffectes != null) cadetsParticipants.addAll(cadetsAffectes);
        etat = EtatMission.EN_COURS;
    }

    /** Ajoute uniquement un résultat réussi ; les tentatives ratées ne passent jamais ici. */
    public synchronized void enregistrerResultat(ResultatExploration resultat) {
        Objects.requireNonNull(resultat);
        if (etat != EtatMission.EN_COURS) throw new IllegalStateException("La mission doit être en cours.");
        if (!zonesObjectifs.contains(resultat.zone())) throw new IllegalArgumentException("La zone n'est pas un objectif de cette mission.");
        if (!engins.contains(resultat.engin())) throw new IllegalArgumentException("L'engin ne participe pas à cette mission.");

        boolean actionCompatible = switch (resultat.action()) {
            case PRELEVEMENT -> resultat.engin() instanceof Preleveur && resultat.echantillon() != null;
            case CARTOGRAPHIE -> resultat.engin() instanceof Cartographe && resultat.echantillon() == null;
        };
        if (!actionCompatible) throw new IllegalArgumentException("Action incompatible avec les capacités de l'engin.");

        Class<? extends Engin> type = resultat.engin().getClass();
        boolean dejaExploree = resultat.zone().aDejaEteExploreePar(type);
        ZoneEnginKey cle = new ZoneEnginKey(resultat.zone(), type);
        if (dejaExploree && !confirmationsReexploration.remove(cle)) {
            throw new IllegalStateException("Réexploration refusée : confirmation explicite nécessaire.");
        }

        resultat.zone().enregistrerExploration(type);
        resultats.add(resultat);
    }

    public synchronized Set<Zone> getZonesExplorees() {
        Set<Zone> zones = new HashSet<>();
        for (ResultatExploration resultat : resultats) zones.add(resultat.zone());
        return Collections.unmodifiableSet(zones);
    }

    public synchronized double calculerTauxReussite() {
        return 100.0 * getZonesExplorees().size() / zonesObjectifs.size();
    }

    public synchronized boolean estReussie() {
        return etat == EtatMission.TERMINEE && rapport != null && rapport.isReussite();
    }

    public synchronized RapportMission terminer() {
        if (etat != EtatMission.EN_COURS) throw new IllegalStateException("Seule une mission en cours peut être terminée.");
        etat = EtatMission.TERMINEE;
        Set<Zone> zonesExplorees = getZonesExplorees();
        int nombreEchantillons = (int) resultats.stream().filter(r -> r.echantillon() != null).count();
        int interetTotal = zonesExplorees.stream().mapToInt(Zone::getInteretScientifique).sum();
        boolean reussite = calculerTauxReussite() >= 70.0;
        rapport = new RapportMission(nom, LocalDateTime.now(), corpsCeleste.getNom(),
                zonesExplorees.size(), nombreEchantillons, interetTotal, reussite, budget);
        return rapport;
    }

    private record ZoneEnginKey(Zone zone, Class<? extends Engin> typeEngin) { }
}
