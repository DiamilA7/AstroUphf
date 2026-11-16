# AstroUPHF

Projet Java 17 / Maven : gestion et simulation de missions d'exploration spatiale.

## Prérequis

- JDK 17 ou supérieur
- Maven 3.9 ou supérieur
- VS Code (facultatif)

## Compiler et tester

```bash
mvn clean test
```

## Lancer la démonstration

```bash
mvn exec:java
```

Les rapports sont stockés dans une base H2 fichier sous `./data/astrouphf`.
Seule la classe `RapportMission` est une entité JPA.

## Documentation de conception

- `GUIDE_CODE.md` : explication classe par classe des 37 fichiers Java (sources principales et tests).
- `CONCEPTION.md` : raisonnement, désaccords, erreurs rencontrées et décisions conservées.
- `TOUS_LES_FICHIERS_JAVA.md` : tous les fichiers Java concaténés pour lecture.

## Structure

- `personnes` : personnes, directeurs, opérateurs, cadets et compétences.
- `engins` : rover, drone, états et interfaces de capacités.
- `monde` : corps célestes, cartes, zones et positions.
- `exploration` : échantillons et résultats scientifiques réussis.
- `missions` : missions et rapports.
- `gestion` : règles globales, planification, affectation et promotion.
- `simulation` : exécution asynchrone et aléatoire.
- `persistance` : repository JPA/H2.

## Diagramme UML

Le diagramme conceptuel est dans `docs/architecture.puml`.
Pour un diagramme généré depuis le vrai code dans VS Code, l'extension recommandée est **LSP UML**. Elle interroge le serveur de langage du projet Java, permet une actualisation lors de l'enregistrement et la navigation des classes vers leur source. L'extension `UML Generator for Java` peut aussi générer des fichiers PlantUML à partir d'un dossier Java.

## Choix métier intégrés

Certains choix gardent volontairement les décisions prises pendant la conception, même lorsqu'elles divergent du texte initial du sujet :

1. **Réexploration autorisée après confirmation explicite.** `Mission.confirmerReexploration(zone, typeEngin)` donne une autorisation consommée par la prochaine exploration réussie de ce type dans cette zone. L'historique de `Zone` compte plusieurs explorations du même type. Une `Map<TypeEngin, ResultatExploration>` avait été envisagée auparavant : elle ne pouvait stocker qu'un résultat par clé, donc elle ne convenait pas à cette décision. Le code utilise un compteur par classe d'engin et une liste de résultats par mission.
2. **Déplacement distinct de l'exploration scientifique.** `deplacer()` modifie la position de l'engin, mais ne crée aucun résultat et ne marque pas une zone comme explorée.
3. **Échec sans modification de l'historique.** Le simulateur ne crée un `ResultatExploration` qu'après une tentative réussie. Un prélèvement raté n'ajoute donc ni échantillon ni exploration.
4. **Cartographie et prélèvement sont indépendants.** Une zone peut conserver des résultats des deux actions. Le taux de réussite d'une mission compte les zones distinctes, pas le nombre d'actions.
5. **Intérêt scientifique évolutif.** `Zone.augmenterInteretScientifique()` augmente la valeur jusqu'à 10 ; `Zone.decouvrirVie()` la porte à 10. Le rapport fige la somme au moment de la clôture.
6. **Danger des zones.** Une zone a un `NiveauDanger`, et chaque engin une résistance. Le démarrage vérifie qu'au moins un engin peut accéder à chaque zone objectif.
7. **Cadet : une seule source de mission.** Le cadet ne stocke pas `missionEnCours`, car il suit celle de son tuteur. La mission capture les cadets participants au démarrage. L’agence relie le cadet à la collection de son tuteur lors de son enregistrement, plutôt que de faire sortir `this` du constructeur avant la fin de l’initialisation.
8. **Deux missions simultanées.** La limite est centralisée à `AgenceSpatiale.MAX_MISSIONS_SIMULTANEES`, actuellement fixée à 2 en raison de la contradiction entre les deux passages du sujet.

## Analyse des pistes abandonnées

- **`Personne` comme interface :** elle n'aurait pas mutualisé le stockage des cinq attributs communs. La classe abstraite `Personne` évite leur duplication.
- **Une sous-classe par planète :** `Mars`, `Lune`, etc. n'ont pas ici de comportements propres. Des instances de `CorpsCeleste` suffisent.
- **Un état unique de zone (`EXPLOREE`, `CARTOGRAPHIEE`, `PRELEVEE`) :** ces valeurs ne sont pas exclusives. Les opérations sont représentées séparément dans les résultats.
- **Confondre déplacement et exploration :** traverser une zone ne produit pas automatiquement une information scientifique.
- **Stocker les résultats historiques des missions directement dans `Zone` :** les zones sont partagées entre missions ; chaque mission doit garder son propre historique, tandis que la zone ne conserve que le compteur global par type d'engin.
- **`Map<TypeEngin, ResultatExploration>` dans `Zone` :** une clé unique empêchait plusieurs explorations du même type. Le code actuel conserve plusieurs occurrences.
- **Faire porter toutes les validations à `Mission` :** les disponibilités et le plafond de missions simultanées concernent toute l'agence. Elles sont vérifiées dans `AgenceSpatiale`.
- **Persister toutes les classes avec JPA :** le sujet ne demande que les rapports. Les autres objets restent en mémoire ; le rapport contient des valeurs figées et le nom du corps céleste.
- **Ajouter une réservation de zone dès la planification :** cette mécanique impliquait annulation, libération et conflits de réservation. Elle n'est pas implémentée dans cette première version car elle n'était pas une exigence stabilisée.

## Limites connues de cette première version

- Le déplacement change directement la position, sans calcul de chemin ni vérification de voisinage.
- Les compétences requises par un nouvel engin sont déclarées dans sa méthode `competenceRequise()`. L'ajout d'un engin nécessite donc une nouvelle classe, mais pas de modifier les classes `Rover` ou `Drone`.
- La simulation associe une action scientifique à chaque zone réussie. Une évolution pourrait autoriser plusieurs actions différentes dans une même zone au cours d'une même mission.
- La promotion remplace le cadet dans le registre central. Les éventuelles références externes conservées par le code appelant restent des références vers l'ancien objet ; l'agence doit donc être la source de vérité après promotion.
- Pour une production plus robuste, il faudrait traiter la reprise après arrêt brutal, les annulations et les transactions entre clôture métier et persistance.
