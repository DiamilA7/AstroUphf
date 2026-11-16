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
