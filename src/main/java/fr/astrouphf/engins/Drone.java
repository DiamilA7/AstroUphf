package fr.astrouphf.engins;

import fr.astrouphf.exploration.ResultatExploration;
import fr.astrouphf.exploration.TypeActionExploration;
import fr.astrouphf.monde.NiveauDanger;
import fr.astrouphf.monde.Position;
import fr.astrouphf.monde.Zone;
import fr.astrouphf.personnes.Competence;

import java.time.LocalDateTime;
import java.util.Objects;

public class Drone extends Engin implements Deplacable, Cartographe {

    public Drone(int id, String nom, Position position, NiveauDanger resistanceDanger) {
        super(id, nom, position, resistanceDanger);
    }

    @Override
    public void deplacer(Position destination) {
        changerPosition(destination);
    }

    @Override
    public ResultatExploration cartographier(Zone zone, LocalDateTime date) {
        Objects.requireNonNull(zone);
        return new ResultatExploration(zone, this,
                TypeActionExploration.CARTOGRAPHIE, null, date);
    }

    @Override
    public boolean necessiteAtmosphere() {
        return true;
    }

    @Override
    public Competence competenceRequise() {
        return Competence.PILOTAGE_DRONE;
    }
}
