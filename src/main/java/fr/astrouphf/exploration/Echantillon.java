package fr.astrouphf.exploration;

import java.util.Objects;

public record Echantillon(TypeEchantillon type, double masse) {
    public Echantillon {
        Objects.requireNonNull(type, "Le type d'échantillon est obligatoire.");
        if (!Double.isFinite(masse) || masse <= 0) {
            throw new IllegalArgumentException("La masse doit être strictement positive.");
        }
    }
}
