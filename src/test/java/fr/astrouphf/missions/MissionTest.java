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
