package fr.astrouphf.gestion;

import fr.astrouphf.personnes.Cadet;
import fr.astrouphf.personnes.Competence;
import fr.astrouphf.personnes.Genre;
import fr.astrouphf.personnes.Operateur;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PromotionCadetTest {

    @Test
    void promotionRemplaceLeCadetParUnOperateurDeMemeIdentifiant() {
        AgenceSpatiale agence = new AgenceSpatiale();
        Operateur tuteur = new Operateur(10, "Tuteur", "T", Genre.AUTRE, "0600000010",
                Set.of(Competence.GEOLOGIE, Competence.ENERGIE));
        Cadet cadet = new Cadet(11, "Cadet", "C", Genre.AUTRE, "0600000011", tuteur);
        agence.ajouterPersonne(tuteur);
        agence.ajouterPersonne(cadet);
        cadet.enregistrerMissionReussie();
        cadet.enregistrerMissionReussie();
        cadet.enregistrerMissionReussie();

        Operateur promu = agence.promouvoirCadet(cadet);

        assertEquals(11, promu.getId());
        assertEquals(tuteur.getCompetences(), promu.getCompetences());
        assertSame(promu, agence.getPersonnes().stream()
                .filter(p -> p.getId() == 11).findFirst().orElseThrow());
        assertFalse(tuteur.getCadetsEncadres().contains(cadet));
    }
}
