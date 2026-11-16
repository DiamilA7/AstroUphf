package fr.astrouphf.persistance;

import fr.astrouphf.missions.RapportMission;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.List;
import java.util.Objects;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

/** Accès aux rapports uniquement ; les autres objets métier ne sont pas persistés. */
public class RapportMissionRepository implements AutoCloseable {

    private final EntityManagerFactory factory;

    public RapportMissionRepository() {
        try {
            Files.createDirectories(Path.of("data"));
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de créer le dossier de la base H2.", exception);
        }
        this.factory = Persistence.createEntityManagerFactory("astroPU");
    }

    public void enregistrer(RapportMission rapport) {
        Objects.requireNonNull(rapport);
        EntityManager manager = factory.createEntityManager();
        var transaction = manager.getTransaction();
        try {
            transaction.begin();
            if (rapport.getId() == null) manager.persist(rapport);
            else manager.merge(rapport);
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) transaction.rollback();
            throw exception;
        } finally {
            manager.close();
        }
    }

    public List<RapportMission> trouverTous() {
        EntityManager manager = factory.createEntityManager();
        try {
            return manager.createQuery(
                    "SELECT r FROM RapportMission r ORDER BY r.dateCloture DESC",
                    RapportMission.class).getResultList();
        } finally {
            manager.close();
        }
    }

    public List<RapportMission> trouverParCorpsCeleste(String nomCorpsCeleste) {
        Objects.requireNonNull(nomCorpsCeleste);
        EntityManager manager = factory.createEntityManager();
        try {
            return manager.createQuery(
                    "SELECT r FROM RapportMission r WHERE r.nomCorpsCeleste = :nom ORDER BY r.dateCloture DESC",
                    RapportMission.class)
                    .setParameter("nom", nomCorpsCeleste)
                    .getResultList();
        } finally {
            manager.close();
        }
    }

    @Override
    public void close() {
        if (factory.isOpen()) factory.close();
    }
}
