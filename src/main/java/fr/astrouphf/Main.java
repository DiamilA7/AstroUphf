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
