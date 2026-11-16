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
