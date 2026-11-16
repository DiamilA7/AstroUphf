package fr.astrouphf.monde;

public enum NiveauDanger {
    FAIBLE(0),
    MODERE(1),
    ELEVE(2),
    EXTREME(3);

    private final int niveau;

    NiveauDanger(int niveau) {
        this.niveau = niveau;
    }

    public boolean estAccessibleAvec(NiveauDanger resistance) {
        return resistance != null && resistance.niveau >= niveau;
    }
}
