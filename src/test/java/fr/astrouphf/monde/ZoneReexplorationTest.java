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
