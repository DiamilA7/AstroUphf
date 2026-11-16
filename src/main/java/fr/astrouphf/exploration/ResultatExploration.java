package fr.astrouphf.exploration;

import fr.astrouphf.engins.Engin;
import fr.astrouphf.monde.Zone;

import java.time.LocalDateTime;
import java.util.Objects;

/** Résultat scientifique réussi. Les échecs de tentative ne créent pas cet objet. */
public record ResultatExploration(
        Zone zone,
        Engin engin,
        TypeActionExploration action,
        Echantillon echantillon,
        LocalDateTime date) {

    public ResultatExploration {
        Objects.requireNonNull(zone);
        Objects.requireNonNull(engin);
        Objects.requireNonNull(action);
        Objects.requireNonNull(date);
        if (action == TypeActionExploration.PRELEVEMENT && echantillon == null) {
            throw new IllegalArgumentException("Un prélèvement réussi doit produire un échantillon.");
        }
        if (action == TypeActionExploration.CARTOGRAPHIE && echantillon != null) {
            throw new IllegalArgumentException("Une cartographie ne produit pas d'échantillon.");
        }
    }
}
