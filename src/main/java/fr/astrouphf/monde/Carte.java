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
