package fr.astrouphf.engins;

import fr.astrouphf.exploration.Echantillon;
import fr.astrouphf.exploration.ResultatExploration;
import fr.astrouphf.exploration.TypeActionExploration;
import fr.astrouphf.exploration.TypeEchantillon;
import fr.astrouphf.monde.NiveauDanger;
import fr.astrouphf.monde.Position;
import fr.astrouphf.monde.Zone;
import fr.astrouphf.personnes.Competence;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Random;

public class Rover extends Engin implements Deplacable, Preleveur {

    public Rover(int id, String nom, Position position, NiveauDanger resistanceDanger) {
        super(id, nom, position, resistanceDanger);
    }

    @Override
    public void deplacer(Position destination) {
        changerPosition(destination);
    }

    @Override
    public ResultatExploration prelever(Zone zone, Random random, LocalDateTime date) {
        Objects.requireNonNull(zone);
        Objects.requireNonNull(random);
        TypeEchantillon[] types = TypeEchantillon.values();
        TypeEchantillon type = types[random.nextInt(types.length)];
        double masse = 1.0 + random.nextDouble() * 99.0;
        Echantillon echantillon = new Echantillon(type, masse);
        return new ResultatExploration(zone, this,
                TypeActionExploration.PRELEVEMENT, echantillon, date);
    }

    @Override
    public Competence competenceRequise() {
        return Competence.PILOTAGE_ROVER;
    }
}
