package fr.astrouphf.personnes;

import fr.astrouphf.missions.Mission;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Un directeur peut diriger plusieurs missions, y compris simultanément. */
public class DirecteurMission extends Personne {

    private final Set<Mission> missionsDirigees = new HashSet<>();

    public DirecteurMission(int id, String nom, String prenom,
                            Genre genre, String telephone) {
        super(id, nom, prenom, genre, telephone);
    }

    public synchronized void ajouterMission(Mission mission) {
        if (mission == null) {
            throw new IllegalArgumentException("La mission ne peut pas être nulle.");
        }
        missionsDirigees.add(mission);
    }

    public synchronized Set<Mission> getMissionsDirigees() {
        return Collections.unmodifiableSet(new HashSet<>(missionsDirigees));
    }

    /** Total calculé depuis les missions : aucune copie du budget n'est conservée. */
    public synchronized double calculerBudgetMissionsReussies() {
        return missionsDirigees.stream()
                .filter(Mission::estReussie)
                .mapToDouble(Mission::getBudget)
                .sum();
    }
}
