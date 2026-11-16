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
