package fr.astrouphf.engins;

import fr.astrouphf.exploration.ResultatExploration;
import fr.astrouphf.monde.Zone;

import java.time.LocalDateTime;
import java.util.Random;

public interface Preleveur {
    ResultatExploration prelever(Zone zone, Random random, LocalDateTime date);
}
