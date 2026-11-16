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
