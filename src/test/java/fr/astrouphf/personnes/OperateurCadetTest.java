package fr.astrouphf.personnes;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OperateurCadetTest {

    @Test
    void operateurRefusePlusDeTroisCompetences() {
        assertThrows(IllegalArgumentException.class, () -> new Operateur(
                1, "Nom", "Prenom", Genre.AUTRE, "0600000000",
                Set.of(Competence.PILOTAGE_ROVER, Competence.PILOTAGE_DRONE,
                        Competence.GEOLOGIE, Competence.ENERGIE)));
    }

    @Test
    void changerTuteurMetAJourLesDeuxRelations() {
        Operateur a = new Operateur(1, "A", "A", Genre.AUTRE, "0600000001",
                Set.of(Competence.GEOLOGIE));
        Operateur b = new Operateur(2, "B", "B", Genre.AUTRE, "0600000002",
                Set.of(Competence.ENERGIE));
        Cadet cadet = new Cadet(3, "C", "C", Genre.AUTRE, "0600000003", a);
        a.ajouterCadet(cadet);

        cadet.changerTuteur(b);

        assertSame(b, cadet.getTuteur());
        assertFalse(a.getCadetsEncadres().contains(cadet));
        assertTrue(b.getCadetsEncadres().contains(cadet));
    }
}
