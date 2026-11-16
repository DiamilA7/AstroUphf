package fr.astrouphf.engins;

import fr.astrouphf.exploration.ResultatExploration;
import fr.astrouphf.monde.Zone;

import java.time.LocalDateTime;

public interface Cartographe {
    ResultatExploration cartographier(Zone zone, LocalDateTime date);
}
