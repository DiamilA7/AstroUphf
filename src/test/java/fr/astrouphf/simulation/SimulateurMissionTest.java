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
