# Guide de lecture du code AstroUPHF

Ce document explique les fichiers Java un par un. Le code intégral est dans `src/main/java` et `src/test/java`, et une copie concaténée est dans `TOUS_LES_FICHIERS_JAVA.md`.

## 1. `personnes`

### `Genre.java`
Enum des valeurs autorisées pour le genre. Un enum évite les variantes incohérentes d'une chaîne libre.

### `Competence.java`
Enum des compétences reconnues. `Operateur` utilise ensuite un `EnumSet`, adapté à un ensemble de valeurs d'enum sans doublons.

### `Personne.java`
Classe abstraite parce que directeur, opérateur et cadet partagent un état commun : identifiant, nom, prénom, genre et téléphone. Une interface aurait défini des méthodes, mais n'aurait pas mutualisé ce stockage. Les données d'identité sont immuables ; seul le téléphone est modifiable.

### `DirecteurMission.java`
Hérite de `Personne`. Stocke les missions dirigées dans un `Set` afin d'éviter les doublons. Le budget de carrière est calculé à partir des missions réussies. Le stocker séparément aurait dupliqué une donnée qui peut être recalculée.

### `Operateur.java`
Hérite de `Personne`. Stocke de 1 à 3 compétences, une mission en cours au maximum et les cadets encadrés. `EnumSet` est le bon outil pour les compétences. Les collections retournées sont des copies non modifiables afin d'empêcher un appelant de contourner les règles métier.

### `Cadet.java`
Hérite de `Personne`. Stocke le tuteur et le nombre de missions réussies. Il ne stocke pas de mission en cours : il suit celle du tuteur. La modification de tuteur met à jour les deux côtés de la relation. L'objet ne se transforme pas lui-même en opérateur, car la promotion doit aussi remplacer l'entrée dans le registre central de l'agence.

**Attention à la création d'un cadet :** le tuteur doit déjà être enregistré dans `AgenceSpatiale`. C'est l'agence qui ajoute le cadet à la collection du tuteur. Cela évite de faire sortir `this` du constructeur alors que l'initialisation de l'objet n'est pas terminée.

## 2. `engins`

### `EtatEngin.java`
Enum des trois états : disponible, en mission et en panne.

### `Deplacable.java`
Capacité de déplacement indépendante du type concret d'engin. Tu avais proposé de placer directement `deplacer()` dans `Engin`, ce qui est cohérent pour les deux engins actuels. On conserve l'interface pour qu'un futur engin fixe puisse hériter d'`Engin` sans prétendre se déplacer.

### `Preleveur.java`
Contrat d'un engin qui sait produire un résultat de prélèvement. Le résultat contient l'échantillon.

### `Cartographe.java`
Contrat d'un engin qui sait produire un résultat de cartographie. Cette action ne contient pas d'échantillon.

### `Engin.java`
Classe abstraite qui centralise identifiant, nom, position, état, mission courante et résistance au danger. Les méthodes synchronisées protègent les changements d'état pendant l'exécution concurrente. `competenceRequise()` est polymorphe : chaque nouveau type annonce la compétence nécessaire. `necessiteAtmosphere()` peut être redéfini par un engin qui en a besoin.

### `Rover.java`
Hérite d'`Engin`, implémente `Deplacable` et `Preleveur`. Il se déplace et crée un résultat avec un échantillon de type et de masse aléatoires. La graine du générateur est fournie par le simulateur pour rendre les tests reproductibles.

### `Drone.java`
Hérite d'`Engin`, implémente `Deplacable` et `Cartographe`. Il produit un résultat de cartographie sans échantillon et déclare qu'il nécessite une atmosphère.

**Pourquoi ne pas mettre `prelever()` et `cartographier()` dans `Engin` ?** Cela obligerait chaque futur véhicule à implémenter des opérations qu'il ne sait pas faire. Les interfaces permettent de composer les capacités.

## 3. `monde`

### `Position.java`
Record immuable avec `x` et `y`. Java fournit automatiquement `equals()` et `hashCode()`, utiles pour comparer des coordonnées.

### `TypeTerrain.java`
Enum des terrains connus : plaine, cratère, falaise et glace.

### `NiveauDanger.java`
Enum ordonné des niveaux de danger. Une zone porte son niveau de danger ; un engin porte sa résistance. La compatibilité compare ces niveaux sans créer une sous-classe de zone pour chaque danger.

### `Zone.java`
Classe concrète qui porte position, terrain, intérêt scientifique et danger. L'intérêt peut évoluer jusqu'à 10, et une découverte de vie le porte à 10. La zone conserve un compteur d'explorations réussies par classe concrète d'engin. Ce compteur permet les répétitions confirmées sans écraser les occurrences.

L'ancienne `Map<TypeEngin, ResultatExploration>` était inadaptée : une clé ne peut contenir qu'une seule valeur. La zone garde donc des compteurs ; les détails sont conservés dans la liste des résultats de chaque mission.

### `Carte.java`
Classe concrète qui contient une matrice rectangulaire `Zone[][]`. C'est la structure naturelle d'une carte rectangulaire : accès direct par coordonnées et voisinage simple à ajouter plus tard. Le constructeur vérifie la forme rectangulaire et la cohérence des coordonnées.

### `CorpsCeleste.java`
Classe concrète qui associe un nom, une présence d'atmosphère et une carte. Il n'y a pas de sous-classes `Mars`, `Lune`, etc., car aucune différence de comportement n'est imposée par le sujet.

## 4. `exploration`

### `TypeActionExploration.java`
Enum qui distingue prélèvement et cartographie. Ce ne sont pas des états mutuellement exclusifs de la zone.

### `TypeEchantillon.java`
Enum des catégories d'échantillons : roche, glace et poussière.

### `Echantillon.java`
Record immuable qui stocke type et masse. La masse doit être strictement positive.

### `ResultatExploration.java`
Record immuable qui lie une zone, un engin, une action, une date et éventuellement un échantillon. Il représente un résultat réussi. Un prélèvement exige un échantillon ; une cartographie l'interdit.

Un simple déplacement ne crée pas ce résultat. Une tentative échouée non plus.

## 5. `missions`

### `EtatMission.java`
Enum : planifiée, en cours, terminée.

### `Mission.java`
Classe concrète qui rassemble directeur, opérateurs, engins, corps céleste, zones objectifs et résultats. Elle calcule le taux de réussite sur les zones distinctes : une zone cartographiée et prélevée compte une seule fois dans le seuil de 70 %.

`confirmerReexploration(zone, typeEngin)` est l'appel explicite de confirmation. Cette autorisation est consommée à l'ajout du prochain résultat réussi correspondant. Une deuxième exploration sans cette autorisation est refusée.

La mission conserve une `List<ResultatExploration>` et non une `Map` à clé unique, car elle doit pouvoir conserver plusieurs résultats de même type.

### `RapportMission.java`
Seule entité JPA. Elle fige date de clôture, nom du corps céleste, zones explorées, échantillons, intérêt total, réussite et budget. Elle stocke le nom du corps céleste plutôt qu'une relation JPA vers un objet métier non persisté.

## 6. `gestion`

### `AgenceSpatiale.java`
Registre central des personnes, engins, corps célestes et missions. Elle vérifie les disponibilités, les compétences, l'atmosphère, le danger et le maximum de missions simultanées. Elle choisit la mission planifiée à la date la plus proche.

Elle libère les ressources à la fin, met à jour les cadets ayant participé à une mission réussie et effectue leur promotion au troisième succès. Le cadet est remplacé par un opérateur de même identifiant, doté des compétences de son tuteur au moment de la promotion.

Le maximum simultané est centralisé dans `MAX_MISSIONS_SIMULTANEES`, fixé à 2 pour cette version en raison de la contradiction repérée dans l'énoncé.

## 7. `simulation`

### `ConfigurationSimulation.java`
Record qui centralise durée par zone, probabilités d'exploration, de panne, de découverte de vie, graine aléatoire et nombre de threads. Les probabilités sont validées entre 0 et 1.

### `SimulateurMission.java`
Utilise `ExecutorService` et renvoie un `Future`. Il attend la durée calculée, tente les explorations, choisit aléatoirement un véhicule compatible, crée les résultats réussis, applique les pannes et déclenche la clôture. Les échecs ne mutent pas l'historique. Le pool est fermé dans `close()`.

## 8. `persistance`

### `RapportMissionRepository.java`
Encapsule JPA : enregistrer, retrouver tous les rapports, retrouver les rapports d'un corps céleste. Chaque opération ouvre et ferme son `EntityManager`. La transaction est annulée en cas d'erreur.

## 9. `Main.java`
Construit un exemple de carte, personnes, engins et mission. Il lance la simulation, attend le `Future`, puis affiche le rapport. Les affichages de démonstration restent dans `main`, pas dans les classes métier.

## 10. Tests

- `OperateurCadetTest` : limites de compétences et cohérence du changement de tuteur.
- `ZoneReexplorationTest` : une répétition sans confirmation est refusée ; avec confirmation elle est enregistrée.
- `MissionTest` : le seuil de 70 % est testé exactement.
- `PromotionCadetTest` : remplacement du cadet par un opérateur de même identifiant.
- `EnginTest` : panne, remise en service et compatibilité avec le danger.
- `SimulateurMissionTest` : simulation asynchrone, rapport final et libération des ressources.
