package fr.astrouphf.gestion;

import fr.astrouphf.engins.Engin;
import fr.astrouphf.engins.Cartographe;
import fr.astrouphf.engins.Preleveur;
import fr.astrouphf.engins.EtatEngin;
import fr.astrouphf.exploration.Echantillon;
import fr.astrouphf.exploration.TypeEchantillon;
import fr.astrouphf.missions.EtatMission;
import fr.astrouphf.missions.Mission;
import fr.astrouphf.missions.RapportMission;
import fr.astrouphf.monde.CorpsCeleste;
import fr.astrouphf.monde.Zone;
import fr.astrouphf.personnes.Cadet;
import fr.astrouphf.personnes.Competence;
import fr.astrouphf.personnes.DirecteurMission;
import fr.astrouphf.personnes.Operateur;
import fr.astrouphf.personnes.Personne;
import fr.astrouphf.simulation.SimulateurMission;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Gestionnaire central : règles globales, affectations et promotion des cadets. */
public class AgenceSpatiale {

    public static final int MAX_MISSIONS_SIMULTANEES = 2;

    private final Map<Integer, Personne> personnes = new ConcurrentHashMap<>();
    private final Map<Integer, Engin> engins = new ConcurrentHashMap<>();
    private final Map<String, CorpsCeleste> corpsCelestes = new ConcurrentHashMap<>();
    private final List<Mission> missions = new ArrayList<>();
    private final Consumer<RapportMission> persisteurRapport;

    public AgenceSpatiale() {
        this(rapport -> { });
    }

    public AgenceSpatiale(Consumer<RapportMission> persisteurRapport) {
        this.persisteurRapport = Objects.requireNonNull(persisteurRapport);
    }

    public synchronized void ajouterPersonne(Personne personne) {
        Objects.requireNonNull(personne);
        if (personnes.containsKey(personne.getId())) {
            throw new IllegalArgumentException("Identifiant de personne déjà utilisé : " + personne.getId());
        }
        if (personne instanceof Cadet cadet
                && personnes.get(cadet.getTuteur().getId()) != cadet.getTuteur()) {
            throw new IllegalArgumentException("Le tuteur doit être enregistré avant son cadet.");
        }
        personnes.put(personne.getId(), personne);
        if (personne instanceof Cadet cadet) {
            cadet.getTuteur().ajouterCadet(cadet);
        }
    }

    public synchronized void ajouterEngin(Engin engin) {
        Objects.requireNonNull(engin);
        if (engins.putIfAbsent(engin.getId(), engin) != null) {
            throw new IllegalArgumentException("Identifiant d'engin déjà utilisé : " + engin.getId());
        }
    }

    public synchronized void ajouterCorpsCeleste(CorpsCeleste corps) {
        Objects.requireNonNull(corps);
        if (corpsCelestes.putIfAbsent(corps.getNom(), corps) != null) {
            throw new IllegalArgumentException("Corps céleste déjà enregistré : " + corps.getNom());
        }
    }

    public synchronized void planifierMission(Mission mission) {
        Objects.requireNonNull(mission);
        if (mission.getEtat() != EtatMission.PLANIFIEE) {
            throw new IllegalArgumentException("Seule une mission planifiée peut être ajoutée.");
        }
        if (missions.contains(mission)) throw new IllegalArgumentException("Mission déjà planifiée.");
        verifierPersonneEnregistree(mission.getDirecteur());
        if (corpsCelestes.get(mission.getCorpsCeleste().getNom()) != mission.getCorpsCeleste()) {
            throw new IllegalArgumentException("Le corps céleste n'est pas enregistré dans l'agence.");
        }
        mission.getOperateurs().forEach(this::verifierPersonneEnregistree);
        mission.getEngins().forEach(engin -> {
            if (engins.get(engin.getId()) != engin) {
                throw new IllegalArgumentException("Un engin de la mission n'est pas enregistré dans l'agence.");
            }
        });
        mission.getDirecteur().ajouterMission(mission);
        missions.add(mission);
    }

    private void verifierPersonneEnregistree(Personne personne) {
        if (personnes.get(personne.getId()) != personne) {
            throw new IllegalArgumentException("Une personne de la mission n'est pas enregistrée dans l'agence.");
        }
    }

    public synchronized Optional<Future<?>> lancerMissionSuivante(SimulateurMission simulateur) {
        Objects.requireNonNull(simulateur);
        long enCours = missions.stream().filter(m -> m.getEtat() == EtatMission.EN_COURS).count();
        if (enCours >= MAX_MISSIONS_SIMULTANEES) return Optional.empty();

        Mission suivante = missions.stream()
                .filter(m -> m.getEtat() == EtatMission.PLANIFIEE)
                .min(Comparator.comparing(Mission::getDateLancement))
                .orElse(null);
        if (suivante == null) return Optional.empty();

        verifierConditionsDemarrage(suivante);
        Set<Cadet> cadets = new HashSet<>();
        suivante.getOperateurs().forEach(op -> cadets.addAll(op.getCadetsEncadres()));

        // Les vérifications sont terminées avant de modifier le moindre état.
        suivante.getOperateurs().forEach(op -> op.affecterMission(suivante));
        suivante.getEngins().forEach(engin -> engin.affecterMission(suivante));
        suivante.demarrer(cadets);

        try {
            return Optional.of(simulateur.lancerSimulation(suivante, this::finaliserMission));
        } catch (RuntimeException exception) {
            suivante.getOperateurs().forEach(Operateur::libererMission);
            suivante.getEngins().forEach(Engin::libererMission);
            throw exception;
        }
    }

    public synchronized void verifierConditionsDemarrage(Mission mission) {
        if (mission.getDirecteur() == null) throw new IllegalStateException("La mission doit avoir un directeur.");
        if (mission.getOperateurs().isEmpty()) throw new IllegalStateException("La mission doit avoir un opérateur.");
        if (mission.getEngins().isEmpty()) throw new IllegalStateException("La mission doit avoir un engin.");
        if (mission.getEtat() != EtatMission.PLANIFIEE) throw new IllegalStateException("La mission n'est pas planifiée.");

        for (Engin engin : mission.getEngins()) {
            if (!engin.estDisponible()) throw new IllegalStateException("Engin indisponible : " + engin.getNom());
            boolean operateurCompetent = mission.getOperateurs().stream()
                    .anyMatch(op -> op.possedeCompetence(engin.competenceRequise()));
            if (!operateurCompetent) {
                throw new IllegalStateException("Aucun opérateur ne possède la compétence " + engin.competenceRequise());
            }
            if (engin.necessiteAtmosphere() && !mission.getCorpsCeleste().possedeAtmosphere()) {
                throw new IllegalStateException("Cet engin nécessite une atmosphère.");
            }
        }

        for (Operateur operateur : mission.getOperateurs()) {
            if (!operateur.estDisponible()) {
                throw new IllegalStateException("Opérateur déjà engagé : " + operateur.getPrenom() + " " + operateur.getNom());
            }
        }

        for (Zone zone : mission.getZonesObjectifs()) {
            boolean accessible = mission.getEngins().stream().anyMatch(engin ->
                    engin.peutAcceder(zone)
                    && (engin instanceof Preleveur || engin instanceof Cartographe)
                    && (!engin.necessiteAtmosphere() || mission.getCorpsCeleste().possedeAtmosphere()));
            if (!accessible) {
                throw new IllegalStateException("Aucun engin compatible avec le danger de la zone " + zone.getPosition());
            }
        }
    }

    private synchronized void finaliserMission(Mission mission) {
        mission.getOperateurs().forEach(Operateur::libererMission);
        mission.getEngins().forEach(Engin::libererMission);

        if (mission.estReussie()) {
            for (Cadet cadet : mission.getCadetsParticipants()) {
                cadet.enregistrerMissionReussie();
                if (cadet.peutEtrePromu()) promouvoirCadet(cadet);
            }
        }
        persisteurRapport.accept(mission.getRapport());
    }

    /** Remplace le cadet dans la collection centrale par un opérateur de même identifiant. */
    public synchronized Operateur promouvoirCadet(Cadet cadet) {
        Objects.requireNonNull(cadet);
        if (!cadet.peutEtrePromu()) throw new IllegalStateException("Le cadet n'a pas encore trois missions réussies.");
        if (personnes.get(cadet.getId()) != cadet) throw new IllegalArgumentException("Ce cadet n'est pas enregistré dans l'agence.");
        if (!cadet.getTuteur().estDisponible()) throw new IllegalStateException("Le tuteur doit être libre pour la promotion.");

        Operateur nouvelOperateur = new Operateur(
                cadet.getId(), cadet.getNom(), cadet.getPrenom(), cadet.getGenre(),
                cadet.getTelephone(), cadet.getTuteur().getCompetences());
        cadet.getTuteur().retirerCadet(cadet);
        personnes.put(cadet.getId(), nouvelOperateur);
        return nouvelOperateur;
    }

    public synchronized void changerTuteur(Cadet cadet, Operateur nouveauTuteur) {
        Objects.requireNonNull(cadet);
        Objects.requireNonNull(nouveauTuteur);
        if (personnes.get(cadet.getId()) != cadet || personnes.get(nouveauTuteur.getId()) != nouveauTuteur) {
            throw new IllegalArgumentException("Le cadet et le nouveau tuteur doivent être enregistrés dans l'agence.");
        }
        if (!nouveauTuteur.estDisponible()) throw new IllegalStateException("Le nouveau tuteur est déjà engagé.");
        cadet.changerTuteur(nouveauTuteur);
    }

    public synchronized List<Mission> getMissionsPlanifiees() {
        return missions.stream().filter(m -> m.getEtat() == EtatMission.PLANIFIEE)
                .sorted(Comparator.comparing(Mission::getDateLancement)).toList();
    }

    public synchronized List<Mission> getMissionsTermineesParBudget() {
        return missions.stream().filter(m -> m.getEtat() == EtatMission.TERMINEE)
                .sorted(Comparator.comparingDouble(Mission::getBudget).reversed()).toList();
    }

    public Map<EtatEngin, List<Engin>> getEnginsParEtat() {
        Map<EtatEngin, List<Engin>> resultat = new EnumMap<>(EtatEngin.class);
        for (EtatEngin etat : EtatEngin.values()) {
            resultat.put(etat, engins.values().stream().filter(e -> e.getEtat() == etat).toList());
        }
        return resultat;
    }

    public List<Operateur> getOperateursDisponibles() {
        return personnes.values().stream().filter(Operateur.class::isInstance)
                .map(Operateur.class::cast).filter(Operateur::estDisponible).toList();
    }

    public List<Operateur> getOperateursOccupes() {
        return personnes.values().stream().filter(Operateur.class::isInstance)
                .map(Operateur.class::cast).filter(op -> !op.estDisponible()).toList();
    }

    public List<Zone> getZonesExploreesParInteret(CorpsCeleste corps) {
        if (corpsCelestes.get(corps.getNom()) != corps) throw new IllegalArgumentException("Corps céleste inconnu.");
        return corps.getCarte().getZonesExplorees().stream()
                .sorted(Comparator.comparingInt(Zone::getInteretScientifique).reversed()
                        .thenComparingInt(z -> z.getPosition().y())
                        .thenComparingInt(z -> z.getPosition().x()))
                .toList();
    }

    public Map<TypeEchantillon, List<Echantillon>> getEchantillonsParType(Mission mission) {
        Map<TypeEchantillon, List<Echantillon>> resultat = new EnumMap<>(TypeEchantillon.class);
        for (TypeEchantillon type : TypeEchantillon.values()) resultat.put(type, new ArrayList<>());
        mission.getResultats().stream().map(r -> r.echantillon()).filter(Objects::nonNull)
                .forEach(e -> resultat.get(e.type()).add(e));
        resultat.replaceAll((type, liste) -> List.copyOf(liste));
        return resultat;
    }

    public synchronized List<Mission> getMissions() {
        return List.copyOf(missions);
    }

    public List<Personne> getPersonnes() {
        return personnes.values().stream().sorted(Comparator.comparingInt(Personne::getId)).toList();
    }

    public List<Engin> getEngins() {
        return engins.values().stream().sorted(Comparator.comparingInt(Engin::getId)).toList();
    }

    public List<CorpsCeleste> getCorpsCelestes() {
        return corpsCelestes.values().stream().sorted(Comparator.comparing(CorpsCeleste::getNom)).toList();
    }
}
