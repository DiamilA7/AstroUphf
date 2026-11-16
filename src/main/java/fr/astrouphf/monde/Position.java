package fr.astrouphf.monde;

/** Coordonnée immuable dans une carte : x = colonne, y = ligne. */
public record Position(int x, int y) {
    public Position {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Les coordonnées ne peuvent pas être négatives.");
        }
    }
}
