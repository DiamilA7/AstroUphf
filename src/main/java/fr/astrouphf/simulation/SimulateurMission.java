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
