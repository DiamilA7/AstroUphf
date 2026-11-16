# Tous les fichiers Java AstroUPHF

Les fichiers ci-dessous sont également présents séparément dans `src/main/java` et `src/test/java`.


## `src/main/java/fr/astrouphf/Main.java`

```java
package fr.astrouphf;

import fr.astrouphf.engins.Drone;
import fr.astrouphf.engins.Engin;
import fr.astrouphf.engins.Rover;
import fr.astrouphf.gestion.AgenceSpatiale;
import fr.astrouphf.missions.Mission;
import fr.astrouphf.monde.Carte;
import fr.astrouphf.monde.CorpsCeleste;
import fr.astrouphf.monde.NiveauDanger;
import fr.astrouphf.monde.Position;
import fr.astrouphf.monde.TypeTerrain;
import fr.astrouphf.monde.Zone;
import fr.astrouphf.personnes.Competence;
import fr.astrouphf.personnes.DirecteurMission;
import fr.astrouphf.personnes.Genre;
import fr.astrouphf.personnes.Operateur;
import fr.astrouphf.persistance.RapportMissionRepository;
import fr.astrouphf.simulation.ConfigurationSimulation;
import fr.astrouphf.simulation.SimulateurMission;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;

/** Démonstration exécutable : les affichages sont volontairement limités au main. */
public class Main {

    public static void main(String[] args) throws Exception {
        Zone[][] grille = new Zone[2][3];
        TypeTerrain[] terrains = TypeTerrain.values();
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                grille[y][x] = new Zone(new Position(x, y), terrains[(x + y) % terrains.length],
                        3 + x + y, NiveauDanger.values()[Math.min(2, x)]);
            }
        }
        Carte carte = new Carte(grille);
        CorpsCeleste mars = new CorpsCeleste("Mars", true, carte);

        DirecteurMission directeur = new DirecteurMission(
                1, "Martin", "Alice", Genre.FEMININ, "+33100000001");
        Operateur operateur = new Operateur(
                2, "Durand", "Sam", Genre.AUTRE, "+33100000002",
                Set.of(Competence.PILOTAGE_ROVER, Competence.PILOTAGE_DRONE, Competence.GEOLOGIE));

        Engin rover = new Rover(101, "Rover-Alpha", new Position(0, 0), NiveauDanger.EXTREME);
        Engin drone = new Drone(102, "Drone-Beta", new Position(0, 0), NiveauDanger.ELEVE);

        Set<Zone> objectifs = new HashSet<>(carte.getZones());
        Mission mission = new Mission("Exploration Vallis", LocalDateTime.now().plusSeconds(1),
                2_500_000, mars, directeur, Set.of(operateur), Set.of(rover, drone), objectifs);

        try (RapportMissionRepository repository = new RapportMissionRepository();
             SimulateurMission simulateur = new SimulateurMission(
                     new ConfigurationSimulation(80, 0.85, 0.10, 0.05, 2026L, 2))) {

            AgenceSpatiale agence = new AgenceSpatiale(repository::enregistrer);
            agence.ajouterPersonne(directeur);
            agence.ajouterPersonne(operateur);
            agence.ajouterEngin(rover);
            agence.ajouterEngin(drone);
            agence.ajouterCorpsCeleste(mars);
            agence.planifierMission(mission);

            Optional<Future<?>> future = agence.lancerMissionSuivante(simulateur);
            if (future.isPresent()) future.get().get();

            System.out.println("Mission : " + mission.getNom());
            System.out.println("Zones explorées : " + mission.getRapport().getNbZonesExplorees());
            System.out.println("Échantillons : " + mission.getRapport().getNbEchantillons());
            System.out.println("Réussite : " + mission.getRapport().isReussite());
            System.out.println("Rapports archivés : " + repository.trouverTous().size());
        }
    }
}
```


## `src/main/java/fr/astrouphf/engins/Cartographe.java`

```java
package fr.astrouphf.engins;

import fr.astrouphf.exploration.ResultatExploration;
import fr.astrouphf.monde.Zone;

import java.time.LocalDateTime;

public interface Cartographe {
    ResultatExploration cartographier(Zone zone, LocalDateTime date);
}
```


## `src/main/java/fr/astrouphf/engins/Deplacable.java`

```java
package fr.astrouphf.engins;

import fr.astrouphf.monde.Position;

public interface Deplacable {
    void deplacer(Position destination);
}
```


## `src/main/java/fr/astrouphf/engins/Drone.java`

```java
package fr.astrouphf.engins;

import fr.astrouphf.exploration.ResultatExploration;
import fr.astrouphf.exploration.TypeActionExploration;
import fr.astrouphf.monde.NiveauDanger;
import fr.astrouphf.monde.Position;
import fr.astrouphf.monde.Zone;
import fr.astrouphf.personnes.Competence;

import java.time.LocalDateTime;
import java.util.Objects;

public class Drone extends Engin implements Deplacable, Cartographe {

    public Drone(int id, String nom, Position position, NiveauDanger resistanceDanger) {
        super(id, nom, position, resistanceDanger);
    }

    @Override
    public void deplacer(Position destination) {
        changerPosition(destination);
    }

    @Override
    public ResultatExploration cartographier(Zone zone, LocalDateTime date) {
        Objects.requireNonNull(zone);
        return new ResultatExploration(zone, this,
                TypeActionExploration.CARTOGRAPHIE, null, date);
    }

    @Override
    public boolean necessiteAtmosphere() {
        return true;
    }

    @Override
    public Competence competenceRequise() {
        return Competence.PILOTAGE_DRONE;
    }
}
```


## `src/main/java/fr/astrouphf/engins/Engin.java`

```java
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
```


## `src/main/java/fr/astrouphf/engins/EtatEngin.java`

```java
package fr.astrouphf.engins;

public enum EtatEngin {
    DISPONIBLE,
    EN_MISSION,
    EN_PANNE
}
```


## `src/main/java/fr/astrouphf/engins/Preleveur.java`

```java
package fr.astrouphf.engins;

import fr.astrouphf.exploration.ResultatExploration;
import fr.astrouphf.monde.Zone;

import java.time.LocalDateTime;
import java.util.Random;

public interface Preleveur {
    ResultatExploration prelever(Zone zone, Random random, LocalDateTime date);
}
```


## `src/main/java/fr/astrouphf/engins/Rover.java`

```java
package fr.astrouphf.engins;

import fr.astrouphf.exploration.Echantillon;
import fr.astrouphf.exploration.ResultatExploration;
import fr.astrouphf.exploration.TypeActionExploration;
import fr.astrouphf.exploration.TypeEchantillon;
import fr.astrouphf.monde.NiveauDanger;
import fr.astrouphf.monde.Position;
import fr.astrouphf.monde.Zone;
import fr.astrouphf.personnes.Competence;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Random;

public class Rover extends Engin implements Deplacable, Preleveur {

    public Rover(int id, String nom, Position position, NiveauDanger resistanceDanger) {
        super(id, nom, position, resistanceDanger);
    }

    @Override
    public void deplacer(Position destination) {
        changerPosition(destination);
    }

    @Override
    public ResultatExploration prelever(Zone zone, Random random, LocalDateTime date) {
        Objects.requireNonNull(zone);
        Objects.requireNonNull(random);
        TypeEchantillon[] types = TypeEchantillon.values();
        TypeEchantillon type = types[random.nextInt(types.length)];
        double masse = 1.0 + random.nextDouble() * 99.0;
        Echantillon echantillon = new Echantillon(type, masse);
        return new ResultatExploration(zone, this,
                TypeActionExploration.PRELEVEMENT, echantillon, date);
    }

    @Override
    public Competence competenceRequise() {
        return Competence.PILOTAGE_ROVER;
    }
}
```


## `src/main/java/fr/astrouphf/exploration/Echantillon.java`

```java
package fr.astrouphf.exploration;

import java.util.Objects;

public record Echantillon(TypeEchantillon type, double masse) {
    public Echantillon {
        Objects.requireNonNull(type, "Le type d'échantillon est obligatoire.");
        if (!Double.isFinite(masse) || masse <= 0) {
            throw new IllegalArgumentException("La masse doit être strictement positive.");
        }
    }
}
```


## `src/main/java/fr/astrouphf/exploration/ResultatExploration.java`

```java
package fr.astrouphf.exploration;

import fr.astrouphf.engins.Engin;
import fr.astrouphf.monde.Zone;

import java.time.LocalDateTime;
import java.util.Objects;

/** Résultat scientifique réussi. Les échecs de tentative ne créent pas cet objet. */
public record ResultatExploration(
        Zone zone,
        Engin engin,
        TypeActionExploration action,
        Echantillon echantillon,
        LocalDateTime date) {

    public ResultatExploration {
        Objects.requireNonNull(zone);
        Objects.requireNonNull(engin);
        Objects.requireNonNull(action);
        Objects.requireNonNull(date);
        if (action == TypeActionExploration.PRELEVEMENT && echantillon == null) {
            throw new IllegalArgumentException("Un prélèvement réussi doit produire un échantillon.");
        }
        if (action == TypeActionExploration.CARTOGRAPHIE && echantillon != null) {
            throw new IllegalArgumentException("Une cartographie ne produit pas d'échantillon.");
        }
    }
}
```


## `src/main/java/fr/astrouphf/exploration/TypeActionExploration.java`

```java
package fr.astrouphf.exploration;

public enum TypeActionExploration {
    PRELEVEMENT,
    CARTOGRAPHIE
}
```


## `src/main/java/fr/astrouphf/exploration/TypeEchantillon.java`

```java
package fr.astrouphf.exploration;

public enum TypeEchantillon {
    ROCHE,
    GLACE,
    POUSSIERE
}
```


## `src/main/java/fr/astrouphf/gestion/AgenceSpatiale.java`

```java
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
```


## `src/main/java/fr/astrouphf/missions/EtatMission.java`

```java
package fr.astrouphf.missions;

public enum EtatMission {
    PLANIFIEE,
    EN_COURS,
    TERMINEE
}
```


## `src/main/java/fr/astrouphf/missions/Mission.java`

```java
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
```


## `src/main/java/fr/astrouphf/missions/RapportMission.java`

```java
package fr.astrouphf.missions;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/** Seule classe métier persistée dans H2, conformément au périmètre du projet. */
@Entity
@Table(name = "rapports_mission")
public class RapportMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomMission;

    @Column(nullable = false)
    private LocalDateTime dateCloture;

    @Column(nullable = false)
    private String nomCorpsCeleste;

    @Column(nullable = false)
    private int nbZonesExplorees;

    @Column(nullable = false)
    private int nbEchantillons;

    @Column(nullable = false)
    private int interetScientifiqueTotal;

    @Column(nullable = false)
    private boolean reussite;

    @Column(nullable = false)
    private double budget;

    protected RapportMission() {
        // Constructeur exigé par JPA.
    }

    public RapportMission(String nomMission, LocalDateTime dateCloture,
                          String nomCorpsCeleste, int nbZonesExplorees,
                          int nbEchantillons, int interetScientifiqueTotal,
                          boolean reussite, double budget) {
        this.nomMission = Objects.requireNonNull(nomMission);
        this.dateCloture = Objects.requireNonNull(dateCloture);
        this.nomCorpsCeleste = Objects.requireNonNull(nomCorpsCeleste);
        if (nbZonesExplorees < 0 || nbEchantillons < 0 || interetScientifiqueTotal < 0) {
            throw new IllegalArgumentException("Les compteurs du rapport ne peuvent pas être négatifs.");
        }
        if (!Double.isFinite(budget) || budget < 0) {
            throw new IllegalArgumentException("Le budget doit être positif ou nul.");
        }
        this.nbZonesExplorees = nbZonesExplorees;
        this.nbEchantillons = nbEchantillons;
        this.interetScientifiqueTotal = interetScientifiqueTotal;
        this.reussite = reussite;
        this.budget = budget;
    }

    public Long getId() { return id; }
    public String getNomMission() { return nomMission; }
    public LocalDateTime getDateCloture() { return dateCloture; }
    public String getNomCorpsCeleste() { return nomCorpsCeleste; }
    public int getNbZonesExplorees() { return nbZonesExplorees; }
    public int getNbEchantillons() { return nbEchantillons; }
    public int getInteretScientifiqueTotal() { return interetScientifiqueTotal; }
    public boolean isReussite() { return reussite; }
    public double getBudget() { return budget; }
}
```


## `src/main/java/fr/astrouphf/monde/Carte.java`

```java
package fr.astrouphf.monde;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Grille rectangulaire de zones. Les objets Zone ne sont pas copiés. */
public class Carte {

    private final int largeur;
    private final int hauteur;
    private final Zone[][] zones;

    public Carte(Zone[][] zones) {
        Objects.requireNonNull(zones, "La matrice de zones est obligatoire.");
        if (zones.length == 0 || zones[0] == null || zones[0].length == 0) {
            throw new IllegalArgumentException("La carte doit être non vide.");
        }
        this.hauteur = zones.length;
        this.largeur = zones[0].length;
        this.zones = new Zone[hauteur][largeur];

        for (int y = 0; y < hauteur; y++) {
            if (zones[y] == null || zones[y].length != largeur) {
                throw new IllegalArgumentException("La carte doit être rectangulaire.");
            }
            for (int x = 0; x < largeur; x++) {
                Zone zone = Objects.requireNonNull(zones[y][x], "Une zone ne peut pas être nulle.");
                if (!zone.getPosition().equals(new Position(x, y))) {
                    throw new IllegalArgumentException("La position de la zone ne correspond pas à sa case.");
                }
                this.zones[y][x] = zone;
            }
        }
    }

    public int getLargeur() { return largeur; }
    public int getHauteur() { return hauteur; }

    public Zone getZone(Position position) {
        Objects.requireNonNull(position);
        if (position.x() >= largeur || position.y() >= hauteur) {
            throw new IndexOutOfBoundsException("Position hors de la carte : " + position);
        }
        return zones[position.y()][position.x()];
    }

    public List<Zone> getZones() {
        List<Zone> resultat = new ArrayList<>(largeur * hauteur);
        for (Zone[] ligne : zones) {
            Collections.addAll(resultat, ligne);
        }
        return Collections.unmodifiableList(resultat);
    }

    public List<Zone> getZonesExplorees() {
        return getZones().stream().filter(Zone::estExploree).toList();
    }

    public boolean contient(Zone zone) {
        if (zone == null) return false;
        Position p = zone.getPosition();
        return p.x() < largeur && p.y() < hauteur && getZone(p) == zone;
    }
}
```


## `src/main/java/fr/astrouphf/monde/CorpsCeleste.java`

```java
package fr.astrouphf.monde;

import java.util.Objects;

/** Un corps céleste concret possède une atmosphère éventuelle et une carte. */
public class CorpsCeleste {

    private final String nom;
    private final boolean atmosphere;
    private final Carte carte;

    public CorpsCeleste(String nom, boolean atmosphere, Carte carte) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom du corps céleste est obligatoire.");
        }
        this.nom = nom.trim();
        this.atmosphere = atmosphere;
        this.carte = Objects.requireNonNull(carte);
    }

    public String getNom() { return nom; }
    public boolean possedeAtmosphere() { return atmosphere; }
    public Carte getCarte() { return carte; }

    /** Pourcentage de zones ayant au moins une exploration réussie, tous engins confondus. */
    public double tauxExploration() {
        long explorees = carte.getZones().stream().filter(Zone::estExploree).count();
        return 100.0 * explorees / carte.getZones().size();
    }
}
```


## `src/main/java/fr/astrouphf/monde/NiveauDanger.java`

```java
package fr.astrouphf.monde;

public enum NiveauDanger {
    FAIBLE(0),
    MODERE(1),
    ELEVE(2),
    EXTREME(3);

    private final int niveau;

    NiveauDanger(int niveau) {
        this.niveau = niveau;
    }

    public boolean estAccessibleAvec(NiveauDanger resistance) {
        return resistance != null && resistance.niveau >= niveau;
    }
}
```


## `src/main/java/fr/astrouphf/monde/Position.java`

```java
package fr.astrouphf.monde;

/** Coordonnée immuable dans une carte : x = colonne, y = ligne. */
public record Position(int x, int y) {
    public Position {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Les coordonnées ne peuvent pas être négatives.");
        }
    }
}
```


## `src/main/java/fr/astrouphf/monde/TypeTerrain.java`

```java
package fr.astrouphf.monde;

public enum TypeTerrain {
    PLAINE,
    CRATERE,
    FALAISE,
    GLACE
}
```


## `src/main/java/fr/astrouphf/monde/Zone.java`

```java
package fr.astrouphf.monde;

import fr.astrouphf.engins.Engin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Zone de terrain. L'historique compte les explorations réussies par classe d'engin.
 * Le choix de Class<? extends Engin> permet d'ajouter un nouveau véhicule sans
 * ajouter une constante à un enum central.
 */
public class Zone {

    private final Position position;
    private final TypeTerrain typeTerrain;
    private int interetScientifique;
    private final NiveauDanger niveauDanger;
    private final Map<Class<? extends Engin>, Integer> explorationsReussies = new HashMap<>();

    public Zone(Position position, TypeTerrain typeTerrain,
                int interetScientifique, NiveauDanger niveauDanger) {
        this.position = Objects.requireNonNull(position);
        this.typeTerrain = Objects.requireNonNull(typeTerrain);
        this.niveauDanger = Objects.requireNonNull(niveauDanger);
        verifierInteret(interetScientifique);
        this.interetScientifique = interetScientifique;
    }

    public Zone(Position position, TypeTerrain typeTerrain, int interetScientifique) {
        this(position, typeTerrain, interetScientifique, NiveauDanger.FAIBLE);
    }

    private static void verifierInteret(int valeur) {
        if (valeur < 0 || valeur > 10) {
            throw new IllegalArgumentException("L'intérêt scientifique doit être compris entre 0 et 10.");
        }
    }

    public Position getPosition() { return position; }
    public TypeTerrain getTypeTerrain() { return typeTerrain; }
    public synchronized int getInteretScientifique() { return interetScientifique; }
    public NiveauDanger getNiveauDanger() { return niveauDanger; }

    /** Une découverte peut augmenter l'intérêt, sans dépasser 10. */
    public synchronized void augmenterInteretScientifique(int augmentation) {
        if (augmentation < 0) {
            throw new IllegalArgumentException("L'augmentation doit être positive ou nulle.");
        }
        interetScientifique = Math.min(10, interetScientifique + augmentation);
    }

    /** Exemple de découverte majeure : la présence de vie porte l'intérêt à 10/10. */
    public synchronized void decouvrirVie() {
        interetScientifique = 10;
    }

    public synchronized boolean aDejaEteExploreePar(Class<? extends Engin> typeEngin) {
        return explorationsReussies.getOrDefault(Objects.requireNonNull(typeEngin), 0) > 0;
    }

    public synchronized int getNombreExplorationsPar(Class<? extends Engin> typeEngin) {
        return explorationsReussies.getOrDefault(Objects.requireNonNull(typeEngin), 0);
    }

    /** Appelée uniquement après une exploration réussie et validée par une mission. */
    public synchronized void enregistrerExploration(Class<? extends Engin> typeEngin) {
        Objects.requireNonNull(typeEngin);
        explorationsReussies.merge(typeEngin, 1, Integer::sum);
    }

    public synchronized boolean estExploree() {
        return explorationsReussies.values().stream().anyMatch(nombre -> nombre > 0);
    }

    public synchronized Map<Class<? extends Engin>, Integer> getHistoriqueExplorations() {
        return Collections.unmodifiableMap(new HashMap<>(explorationsReussies));
    }
}
```


## `src/main/java/fr/astrouphf/persistance/RapportMissionRepository.java`

```java
package fr.astrouphf.persistance;

import fr.astrouphf.missions.RapportMission;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.List;
import java.util.Objects;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

/** Accès aux rapports uniquement ; les autres objets métier ne sont pas persistés. */
public class RapportMissionRepository implements AutoCloseable {

    private final EntityManagerFactory factory;

    public RapportMissionRepository() {
        try {
            Files.createDirectories(Path.of("data"));
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de créer le dossier de la base H2.", exception);
        }
        this.factory = Persistence.createEntityManagerFactory("astroPU");
    }

    public void enregistrer(RapportMission rapport) {
        Objects.requireNonNull(rapport);
        EntityManager manager = factory.createEntityManager();
        var transaction = manager.getTransaction();
        try {
            transaction.begin();
            if (rapport.getId() == null) manager.persist(rapport);
            else manager.merge(rapport);
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) transaction.rollback();
            throw exception;
        } finally {
            manager.close();
        }
    }

    public List<RapportMission> trouverTous() {
        EntityManager manager = factory.createEntityManager();
        try {
            return manager.createQuery(
                    "SELECT r FROM RapportMission r ORDER BY r.dateCloture DESC",
                    RapportMission.class).getResultList();
        } finally {
            manager.close();
        }
    }

    public List<RapportMission> trouverParCorpsCeleste(String nomCorpsCeleste) {
        Objects.requireNonNull(nomCorpsCeleste);
        EntityManager manager = factory.createEntityManager();
        try {
            return manager.createQuery(
                    "SELECT r FROM RapportMission r WHERE r.nomCorpsCeleste = :nom ORDER BY r.dateCloture DESC",
                    RapportMission.class)
                    .setParameter("nom", nomCorpsCeleste)
                    .getResultList();
        } finally {
            manager.close();
        }
    }

    @Override
    public void close() {
        if (factory.isOpen()) factory.close();
    }
}
```


## `src/main/java/fr/astrouphf/personnes/Cadet.java`

```java
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
```


## `src/main/java/fr/astrouphf/personnes/Competence.java`

```java
package fr.astrouphf.personnes;

public enum Competence {
    PILOTAGE_ROVER,
    PILOTAGE_DRONE,
    GEOLOGIE,
    TELECOMMUNICATIONS,
    ENERGIE
}
```


## `src/main/java/fr/astrouphf/personnes/DirecteurMission.java`

```java
package fr.astrouphf.personnes;

import fr.astrouphf.missions.Mission;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Un directeur peut diriger plusieurs missions, y compris simultanément. */
public class DirecteurMission extends Personne {

    private final Set<Mission> missionsDirigees = new HashSet<>();

    public DirecteurMission(int id, String nom, String prenom,
                            Genre genre, String telephone) {
        super(id, nom, prenom, genre, telephone);
    }

    public synchronized void ajouterMission(Mission mission) {
        if (mission == null) {
            throw new IllegalArgumentException("La mission ne peut pas être nulle.");
        }
        missionsDirigees.add(mission);
    }

    public synchronized Set<Mission> getMissionsDirigees() {
        return Collections.unmodifiableSet(new HashSet<>(missionsDirigees));
    }

    /** Total calculé depuis les missions : aucune copie du budget n'est conservée. */
    public synchronized double calculerBudgetMissionsReussies() {
        return missionsDirigees.stream()
                .filter(Mission::estReussie)
                .mapToDouble(Mission::getBudget)
                .sum();
    }
}
```


## `src/main/java/fr/astrouphf/personnes/Genre.java`

```java
package fr.astrouphf.personnes;

public enum Genre {
    MASCULIN,
    FEMININ,
    AUTRE
}
```


## `src/main/java/fr/astrouphf/personnes/Operateur.java`

```java
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
```


## `src/main/java/fr/astrouphf/personnes/Personne.java`

```java
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
```


## `src/main/java/fr/astrouphf/simulation/ConfigurationSimulation.java`

```java
package fr.astrouphf.simulation;

public record ConfigurationSimulation(
        long tempsParZoneMillis,
        double probabiliteExploration,
        double probabilitePanne,
        double probabiliteDecouverteVie,
        long graineAleatoire,
        int nombreThreads) {

    public ConfigurationSimulation {
        if (tempsParZoneMillis < 0) throw new IllegalArgumentException("Le temps par zone ne peut pas être négatif.");
        verifierProbabilite(probabiliteExploration, "exploration");
        verifierProbabilite(probabilitePanne, "panne");
        verifierProbabilite(probabiliteDecouverteVie, "découverte de vie");
        if (nombreThreads < 1) throw new IllegalArgumentException("Il faut au moins un thread.");
    }

    private static void verifierProbabilite(double valeur, String nom) {
        if (!Double.isFinite(valeur) || valeur < 0 || valeur > 1) {
            throw new IllegalArgumentException("La probabilité de " + nom + " doit être comprise entre 0 et 1.");
        }
    }

    public static ConfigurationSimulation parDefaut() {
        return new ConfigurationSimulation(200, 0.80, 0.10, 0.02, 42L, 2);
    }
}
```


## `src/main/java/fr/astrouphf/simulation/SimulateurMission.java`

```java
package fr.astrouphf.simulation;

import fr.astrouphf.engins.Cartographe;
import fr.astrouphf.engins.Deplacable;
import fr.astrouphf.engins.Engin;
import fr.astrouphf.engins.Preleveur;
import fr.astrouphf.exploration.ResultatExploration;
import fr.astrouphf.missions.Mission;
import fr.astrouphf.monde.Zone;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Exécute les missions en parallèle sans bloquer le programme principal. */
public class SimulateurMission implements AutoCloseable {

    private final ConfigurationSimulation configuration;
    private final ExecutorService executor;

    public SimulateurMission(ConfigurationSimulation configuration) {
        this.configuration = Objects.requireNonNull(configuration);
        this.executor = Executors.newFixedThreadPool(configuration.nombreThreads());
    }

    public Future<?> lancerSimulation(Mission mission, Consumer<Mission> callbackFin) {
        Objects.requireNonNull(mission);
        Objects.requireNonNull(callbackFin);
        return executor.submit(() -> {
            RuntimeException erreur = null;
            try {
                simuler(mission);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                erreur = new IllegalStateException("Simulation interrompue.", exception);
            } catch (RuntimeException exception) {
                erreur = exception;
            } finally {
                if (mission.getEtat() == fr.astrouphf.missions.EtatMission.EN_COURS) {
                    mission.terminer();
                }
                callbackFin.accept(mission);
            }
            if (erreur != null) throw erreur;
        });
    }

    private void simuler(Mission mission) throws InterruptedException {
        long nombreZones = mission.getZonesObjectifs().size();
        long nombreEngins = mission.getEngins().size();
        long duree = (nombreZones * configuration.tempsParZoneMillis()) / Math.max(1, nombreEngins);
        if (duree > 0) Thread.sleep(duree);

        long graineMission = configuration.graineAleatoire() + mission.getNom().hashCode();
        Random random = new Random(graineMission);

        for (Zone zone : mission.getZonesObjectifs()) {
            if (random.nextDouble() > configuration.probabiliteExploration()) {
                continue; // Échec de tentative : aucun résultat ni état d'exploration enregistré.
            }

            List<Engin> candidats = trouverEnginsCompatibles(mission, zone);
            candidats.removeIf(engin -> zone.aDejaEteExploreePar(engin.getClass())
                    && !mission.reexplorationAutorisee(zone, engin.getClass()));
            if (candidats.isEmpty()) continue;
            Engin engin = candidats.get(random.nextInt(candidats.size()));

            if (zone.aDejaEteExploreePar(engin.getClass())
                    && !mission.reexplorationAutorisee(zone, engin.getClass())) {
                continue; // La réexploration nécessite une confirmation avant le lancement.
            }

            if (engin instanceof Deplacable deplacable) {
                deplacable.deplacer(zone.getPosition());
            }

            LocalDateTime maintenant = LocalDateTime.now();
            ResultatExploration resultat;
            if (engin instanceof Preleveur preleveur) {
                resultat = preleveur.prelever(zone, random, maintenant);
            } else if (engin instanceof Cartographe cartographe) {
                resultat = cartographe.cartographier(zone, maintenant);
            } else {
                continue;
            }

            mission.enregistrerResultat(resultat);

            if (random.nextDouble() < configuration.probabiliteDecouverteVie()) {
                zone.decouvrirVie();
            }
        }

        for (Engin engin : mission.getEngins()) {
            if (random.nextDouble() < configuration.probabilitePanne()) {
                engin.mettreEnPanne();
            }
        }
    }

    private List<Engin> trouverEnginsCompatibles(Mission mission, Zone zone) {
        List<Engin> candidats = new ArrayList<>();
        for (Engin engin : mission.getEngins()) {
            if (!engin.peutAcceder(zone)) continue;
            if (engin instanceof Preleveur) candidats.add(engin);
            else if (engin instanceof Cartographe
                    && (!engin.necessiteAtmosphere() || mission.getCorpsCeleste().possedeAtmosphere())) {
                candidats.add(engin);
            }
        }
        return candidats;
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Le pool de simulation ne s'est pas arrêté correctement.");
                }
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```


## `src/test/java/fr/astrouphf/engins/EnginTest.java`

```java
package fr.astrouphf.engins;

import fr.astrouphf.monde.NiveauDanger;
import fr.astrouphf.monde.Position;
import fr.astrouphf.monde.TypeTerrain;
import fr.astrouphf.monde.Zone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnginTest {

    @Test
    void panneBloqueLeDeplacementJusquaLaRemiseEnService() {
        Rover rover = new Rover(1, "R1", new Position(0, 0), NiveauDanger.ELEVE);
        rover.mettreEnPanne();
        assertThrows(IllegalStateException.class,
                () -> rover.deplacer(new Position(1, 0)));
        rover.remettreEnService();
        rover.deplacer(new Position(1, 0));
        assertEquals(new Position(1, 0), rover.getPosition());
    }

    @Test
    void resistanceAuDangerEstVerifiee() {
        Rover rover = new Rover(1, "R1", new Position(0, 0), NiveauDanger.MODERE);
        Zone dangereuse = new Zone(new Position(0, 0), TypeTerrain.FALAISE, 8, NiveauDanger.EXTREME);
        assertFalse(rover.peutAcceder(dangereuse));
    }
}
```


## `src/test/java/fr/astrouphf/gestion/PromotionCadetTest.java`

```java
package fr.astrouphf.gestion;

import fr.astrouphf.personnes.Cadet;
import fr.astrouphf.personnes.Competence;
import fr.astrouphf.personnes.Genre;
import fr.astrouphf.personnes.Operateur;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PromotionCadetTest {

    @Test
    void promotionRemplaceLeCadetParUnOperateurDeMemeIdentifiant() {
        AgenceSpatiale agence = new AgenceSpatiale();
        Operateur tuteur = new Operateur(10, "Tuteur", "T", Genre.AUTRE, "0600000010",
                Set.of(Competence.GEOLOGIE, Competence.ENERGIE));
        Cadet cadet = new Cadet(11, "Cadet", "C", Genre.AUTRE, "0600000011", tuteur);
        agence.ajouterPersonne(tuteur);
        agence.ajouterPersonne(cadet);
        cadet.enregistrerMissionReussie();
        cadet.enregistrerMissionReussie();
        cadet.enregistrerMissionReussie();

        Operateur promu = agence.promouvoirCadet(cadet);

        assertEquals(11, promu.getId());
        assertEquals(tuteur.getCompetences(), promu.getCompetences());
        assertSame(promu, agence.getPersonnes().stream()
                .filter(p -> p.getId() == 11).findFirst().orElseThrow());
        assertFalse(tuteur.getCadetsEncadres().contains(cadet));
    }
}
```


## `src/test/java/fr/astrouphf/missions/MissionTest.java`

```java
package fr.astrouphf.missions;

import fr.astrouphf.engins.Rover;
import fr.astrouphf.exploration.ResultatExploration;
import fr.astrouphf.monde.Carte;
import fr.astrouphf.monde.CorpsCeleste;
import fr.astrouphf.monde.NiveauDanger;
import fr.astrouphf.monde.Position;
import fr.astrouphf.monde.TypeTerrain;
import fr.astrouphf.monde.Zone;
import fr.astrouphf.personnes.Competence;
import fr.astrouphf.personnes.DirecteurMission;
import fr.astrouphf.personnes.Genre;
import fr.astrouphf.personnes.Operateur;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MissionTest {

    @Test
    void soixanteDixPourCentDesZonesSuffisentPourReussir() {
        Zone[][] grille = new Zone[2][5];
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 5; x++) {
                grille[y][x] = new Zone(new Position(x, y), TypeTerrain.PLAINE, 4,
                        NiveauDanger.FAIBLE);
            }
        }
        Carte carte = new Carte(grille);
        CorpsCeleste corps = new CorpsCeleste("Mars", true, carte);
        DirecteurMission directeur = new DirecteurMission(1, "D", "D", Genre.AUTRE, "0600000001");
        Operateur operateur = new Operateur(2, "O", "O", Genre.AUTRE, "0600000002",
                Set.of(Competence.PILOTAGE_ROVER));
        Rover rover = new Rover(3, "R", new Position(0, 0), NiveauDanger.EXTREME);
        Mission mission = new Mission("M", LocalDateTime.now(), 100, corps, directeur,
                Set.of(operateur), Set.of(rover), new HashSet<>(carte.getZones()));
        mission.demarrer(Set.of());

        for (int i = 0; i < 7; i++) {
            Zone zone = carte.getZone(new Position(i % 5, i / 5));
            ResultatExploration resultat = rover.prelever(zone, new Random(i), LocalDateTime.now());
            mission.enregistrerResultat(resultat);
        }

        RapportMission rapport = mission.terminer();
        assertTrue(rapport.isReussite());
        assertEquals(7, rapport.getNbZonesExplorees());
        assertEquals(70.0, mission.calculerTauxReussite());
    }
}
```


## `src/test/java/fr/astrouphf/monde/ZoneReexplorationTest.java`

```java
package fr.astrouphf.monde;

import fr.astrouphf.engins.Rover;
import fr.astrouphf.exploration.ResultatExploration;
import fr.astrouphf.missions.Mission;
import fr.astrouphf.personnes.Competence;
import fr.astrouphf.personnes.DirecteurMission;
import fr.astrouphf.personnes.Genre;
import fr.astrouphf.personnes.Operateur;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ZoneReexplorationTest {

    @Test
    void decouverteScientifiquePeutFaireMonterInteretJusquaDix() {
        Zone zone = new Zone(new Position(0, 0), TypeTerrain.PLAINE, 6);
        zone.augmenterInteretScientifique(2);
        assertEquals(8, zone.getInteretScientifique());
        zone.decouvrirVie();
        assertEquals(10, zone.getInteretScientifique());
        zone.augmenterInteretScientifique(5);
        assertEquals(10, zone.getInteretScientifique());
    }

    @Test
    void uneReexplorationDemandeUneConfirmationExplicite() {
        Zone zone = new Zone(new Position(0, 0), TypeTerrain.PLAINE, 5);
        Carte carte = new Carte(new Zone[][]{{zone}});
        CorpsCeleste mars = new CorpsCeleste("Mars", true, carte);
        DirecteurMission directeur = new DirecteurMission(1, "D", "D", Genre.AUTRE, "0600000001");
        Operateur operateur = new Operateur(2, "O", "O", Genre.AUTRE, "0600000002",
                Set.of(Competence.PILOTAGE_ROVER));
        Rover rover = new Rover(3, "R", new Position(0, 0), NiveauDanger.EXTREME);
        Mission mission = new Mission("M", LocalDateTime.now(), 100, mars, directeur,
                Set.of(operateur), Set.of(rover), Set.of(zone));
        mission.demarrer(Set.of());

        ResultatExploration premier = rover.prelever(zone, new Random(1), LocalDateTime.now());
        mission.enregistrerResultat(premier);
        ResultatExploration second = rover.prelever(zone, new Random(2), LocalDateTime.now());

        assertThrows(IllegalStateException.class, () -> mission.enregistrerResultat(second));
        mission.confirmerReexploration(zone, Rover.class);
        mission.enregistrerResultat(second);
        assertEquals(2, zone.getNombreExplorationsPar(Rover.class));
        assertEquals(2, mission.getResultats().size());
    }
}
```


## `src/test/java/fr/astrouphf/personnes/OperateurCadetTest.java`

```java
package fr.astrouphf.personnes;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OperateurCadetTest {

    @Test
    void operateurRefusePlusDeTroisCompetences() {
        assertThrows(IllegalArgumentException.class, () -> new Operateur(
                1, "Nom", "Prenom", Genre.AUTRE, "0600000000",
                Set.of(Competence.PILOTAGE_ROVER, Competence.PILOTAGE_DRONE,
                        Competence.GEOLOGIE, Competence.ENERGIE)));
    }

    @Test
    void changerTuteurMetAJourLesDeuxRelations() {
        Operateur a = new Operateur(1, "A", "A", Genre.AUTRE, "0600000001",
                Set.of(Competence.GEOLOGIE));
        Operateur b = new Operateur(2, "B", "B", Genre.AUTRE, "0600000002",
                Set.of(Competence.ENERGIE));
        Cadet cadet = new Cadet(3, "C", "C", Genre.AUTRE, "0600000003", a);
        a.ajouterCadet(cadet);

        cadet.changerTuteur(b);

        assertSame(b, cadet.getTuteur());
        assertFalse(a.getCadetsEncadres().contains(cadet));
        assertTrue(b.getCadetsEncadres().contains(cadet));
    }
}
```


## `src/test/java/fr/astrouphf/simulation/SimulateurMissionTest.java`

```java
package fr.astrouphf.simulation;

import fr.astrouphf.engins.Rover;
import fr.astrouphf.gestion.AgenceSpatiale;
import fr.astrouphf.missions.EtatMission;
import fr.astrouphf.missions.Mission;
import fr.astrouphf.monde.Carte;
import fr.astrouphf.monde.CorpsCeleste;
import fr.astrouphf.monde.NiveauDanger;
import fr.astrouphf.monde.Position;
import fr.astrouphf.monde.TypeTerrain;
import fr.astrouphf.monde.Zone;
import fr.astrouphf.personnes.Competence;
import fr.astrouphf.personnes.DirecteurMission;
import fr.astrouphf.personnes.Genre;
import fr.astrouphf.personnes.Operateur;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SimulateurMissionTest {

    @Test
    void simulationAsynchroneClotureLaMissionEtLibereLesRessources() throws Exception {
        Zone zone = new Zone(new Position(0, 0), TypeTerrain.PLAINE, 5);
        CorpsCeleste corps = new CorpsCeleste("Mars", true, new Carte(new Zone[][]{{zone}}));
        DirecteurMission directeur = new DirecteurMission(1, "D", "D", Genre.AUTRE, "0600000001");
        Operateur operateur = new Operateur(2, "O", "O", Genre.AUTRE, "0600000002",
                Set.of(Competence.PILOTAGE_ROVER));
        Rover rover = new Rover(3, "R", new Position(0, 0), NiveauDanger.EXTREME);
        Mission mission = new Mission("Simulation test", LocalDateTime.now(), 100,
                corps, directeur, Set.of(operateur), Set.of(rover), Set.of(zone));

        AgenceSpatiale agence = new AgenceSpatiale();
        agence.ajouterPersonne(directeur);
        agence.ajouterPersonne(operateur);
        agence.ajouterEngin(rover);
        agence.ajouterCorpsCeleste(corps);
        agence.planifierMission(mission);

        ConfigurationSimulation configuration = new ConfigurationSimulation(
                0, 1.0, 0.0, 0.0, 1234L, 1);
        try (SimulateurMission simulateur = new SimulateurMission(configuration)) {
            agence.lancerMissionSuivante(simulateur).orElseThrow().get();
        }

        assertEquals(EtatMission.TERMINEE, mission.getEtat());
        assertNotNull(mission.getRapport());
        assertTrue(mission.getRapport().isReussite());
        assertTrue(operateur.estDisponible());
        assertTrue(rover.estDisponible());
        assertTrue(zone.estExploree());
    }
}
```
