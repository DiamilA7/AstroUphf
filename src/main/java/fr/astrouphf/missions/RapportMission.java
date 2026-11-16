package fr.astrouphf.missions;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/** Seule classe métier persistée dans H2, conformément au périmètre du projet. */
@Entity
@Table(name = "rapports_mission")
public class RapportMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomMission;

    @Column(nullable = false)
    private LocalDateTime dateCloture;

    @Column(nullable = false)
    private String nomCorpsCeleste;

    @Column(nullable = false)
    private int nbZonesExplorees;

    @Column(nullable = false)
    private int nbEchantillons;

    @Column(nullable = false)
    private int interetScientifiqueTotal;

    @Column(nullable = false)
    private boolean reussite;

    @Column(nullable = false)
    private double budget;

    protected RapportMission() {
        // Constructeur exigé par JPA.
    }

    public RapportMission(String nomMission, LocalDateTime dateCloture,
                          String nomCorpsCeleste, int nbZonesExplorees,
                          int nbEchantillons, int interetScientifiqueTotal,
                          boolean reussite, double budget) {
        this.nomMission = Objects.requireNonNull(nomMission);
        this.dateCloture = Objects.requireNonNull(dateCloture);
        this.nomCorpsCeleste = Objects.requireNonNull(nomCorpsCeleste);
        if (nbZonesExplorees < 0 || nbEchantillons < 0 || interetScientifiqueTotal < 0) {
            throw new IllegalArgumentException("Les compteurs du rapport ne peuvent pas être négatifs.");
        }
        if (!Double.isFinite(budget) || budget < 0) {
            throw new IllegalArgumentException("Le budget doit être positif ou nul.");
        }
        this.nbZonesExplorees = nbZonesExplorees;
        this.nbEchantillons = nbEchantillons;
        this.interetScientifiqueTotal = interetScientifiqueTotal;
        this.reussite = reussite;
        this.budget = budget;
    }

    public Long getId() { return id; }
    public String getNomMission() { return nomMission; }
    public LocalDateTime getDateCloture() { return dateCloture; }
    public String getNomCorpsCeleste() { return nomCorpsCeleste; }
    public int getNbZonesExplorees() { return nbZonesExplorees; }
    public int getNbEchantillons() { return nbEchantillons; }
    public int getInteretScientifiqueTotal() { return interetScientifiqueTotal; }
    public boolean isReussite() { return reussite; }
    public double getBudget() { return budget; }
}
