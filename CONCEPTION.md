# Dossier de conception AstroUPHF

## 1. Méthode d'analyse

On part des règles métier, on identifie les objets qui portent un état, puis on sépare les responsabilités. Une classe est créée parce qu'elle représente un concept stable ou parce qu'elle possède une responsabilité cohérente, pas pour multiplier artificiellement les boîtes UML.

### Retour sur nos erreurs et désaccords

- **Décision portée par l'utilisateur : réexploration possible avec confirmation.** J'ai d'abord insisté sur la règle littérale du sujet (une seule exploration par type). Techniquement, ton idée n'est pas mauvaise : elle ajoute une politique de réautorisation et un historique multi-occurrences. Elle s'écarte du sujet, mais elle est conservée comme décision de conception. Le code exige une confirmation par zone et classe d'engin.
- **Décision portée par l'utilisateur : déplacement différent de l'exploration.** Tu as contesté l'idée qu'un simple passage dans une zone suffise à la déclarer explorée. Cette distinction est correcte : le déplacement modifie la position, alors que prélèvement et cartographie créent des résultats scientifiques.
- **Décision portée par l'utilisateur : échec sans mutation.** Tu as précisé qu'un prélèvement raté ne doit pas modifier l'état de la zone et qu'un nouvel essai doit rester possible. Le simulateur n'enregistre donc que les résultats réussis.
- **Décision portée par l'utilisateur : intérêt scientifique évolutif.** Tu as proposé qu'une découverte importante, par exemple de la vie, puisse faire progresser l'intérêt jusqu'à 10/10. Ce comportement dépasse le minimum indiqué dans le sujet, mais il est implémenté.
- **Piste de l'utilisateur : zone dangereuse et compatibilité des véhicules.** L'exemple de zone volcanique a conduit à représenter le danger comme une propriété de la zone et la résistance comme une propriété de l'engin. On conserve cette extension sans créer une sous-classe par danger.
- **Piste non stabilisée : réserver des zones dès la planification.** L'idée a été discutée, mais elle entraînait des règles supplémentaires d'annulation et de libération. Elle n'est pas implémentée dans cette version.
- **Erreur de ma part : `Map<TypeEngin, ResultatExploration>` dans `Zone`.** J'avais proposé une structure incompatible avec ta demande de répétition : une clé ne peut avoir qu'une seule valeur. Elle a été remplacée par un compteur par classe d'engin dans `Zone` et une liste de résultats dans `Mission`.
- **Erreur de ma part : placer `ResultatExploration` dans `monde`.** Le résultat appartient à l'historique d'une mission, pas au modèle géographique. Il est déplacé dans `exploration`.
- **Erreur de conception évitée : un enum unique pour l'état de zone.** `CARTOGRAPHIEE` et `PRELEVEE` peuvent coexister ; les représenter comme des états exclusifs aurait perdu de l'information.
- **Erreur de conception évitée : stocker l'intérêt total d'une mission dans `Zone`.** Le total dépend des zones d'une mission précise ; il est calculé à la clôture et figé dans `RapportMission`.



## Chronologie détaillée du raisonnement

### A. Partir des ressemblances et des différences

Pour les personnes, on a repéré cinq données communes avant de choisir l'abstraction. La vraie question n'était pas « interface ou héritage par principe ? », mais « est-ce que les objets partagent aussi un état ? ». Oui : `id`, `nom`, `prenom`, `genre` et `telephone`. D'où `Personne` abstraite.

Pour les engins, la même analyse conduit à `Engin` abstraite pour l'identité, la position et l'état. En revanche, les actions scientifiques diffèrent. Le rover prélève, le drone cartographie : ces actions deviennent des interfaces séparées.

### B. Ne pas confondre une valeur et un type de comportement

Lune, Mars, Europe et Titan varient surtout par leurs valeurs (nom, atmosphère, carte). Nous n'avons pas de comportement spécialisé suffisamment distinct pour créer une sous-classe par astre. `CorpsCeleste` reste concret.

La carte rectangulaire est représentée par `Zone[][]`. `Position` est une valeur immuable ; `Zone` représente un emplacement avec un terrain, un intérêt et un niveau de danger.

### C. Décomposer les notions d'exploration

Tu as distingué le déplacement, le prélèvement et la cartographie. Cette séparation est gardée dans le code : une méthode de déplacement ne crée aucun résultat ; les interfaces scientifiques produisent un `ResultatExploration`.

On a écarté l'idée d'un état unique de zone, car la cartographie et le prélèvement peuvent coexister. On a aussi corrigé ma proposition de stocker le résultat détaillé d'une exploration dans la zone : chaque mission doit garder son propre historique, car plusieurs missions utilisent les mêmes objets `Zone`.

### D. Définir les responsabilités entre mission et agence

`Mission` possède ses objectifs et ses résultats. `AgenceSpatiale` connaît toutes les ressources et toutes les missions : elle est donc le bon endroit pour vérifier que les personnes et les engins sont libres, que les compétences correspondent et que le plafond de concurrence n'est pas dépassé.

Le simulateur exécute, la mission conserve le résultat, l'agence clôture et met à jour les ressources, puis le repository archive le rapport. Cette chaîne empêche une seule classe de tout faire.

### E. Choisir les collections selon les opérations attendues

- `Map<Integer, Personne>` et `Map<Integer, Engin>` : recherche par identifiant.
- `List<Mission>` : tri chronologique ou par budget.
- `Set<Operateur>`, `Set<Engin>`, `Set<Zone>` : pas de doublons dans les ressources et objectifs d'une mission.
- `EnumSet<Competence>` : ensemble de valeurs d'un enum.
- `Zone[][]` : carte rectangulaire.
- `List<ResultatExploration>` : plusieurs résultats, y compris plusieurs résultats du même type après autorisation.

### F. Faire la différence entre archive et état vivant

Le monde et les ressources restent en mémoire. Le rapport est une photographie de clôture. C'est pourquoi `RapportMission` est la seule entité JPA, et qu'il conserve le nom du corps céleste ainsi que les indicateurs chiffrés au moment de la fin.

## 2. Personnes

### Choix retenu

`Personne` est abstraite. `DirecteurMission`, `Operateur` et `Cadet` en héritent. Les attributs communs sont stockés une seule fois. Les spécialisations ajoutent uniquement leurs données propres.

### Pistes écartées et raisons

- **Interface `Personne` :** elle aurait défini des méthodes mais n'aurait pas mutualisé naturellement les attributs communs. Une classe abstraite est mieux adaptée.
- **Dupliquer les cinq champs dans les trois classes :** cela aurait répété constructeurs, validations et accesseurs, avec risque de divergence.
- **Stocker un budget total modifiable dans `DirecteurMission` :** cela dupliquerait l'information déjà portée par les missions. Le total est donc calculé depuis les missions réussies.
- **Stocker la mission dans `Cadet` :** le cadet suit la mission de son tuteur. Une deuxième référence pouvait devenir incohérente. La mission capture les cadets participants lors du démarrage. La liaison inverse avec le tuteur est effectuée par `AgenceSpatiale` lors de l’enregistrement du cadet, plutôt que de laisser échapper `this` depuis le constructeur.
- **Promouvoir le cadet lui-même :** sa transformation modifie le registre général et les références d'encadrement. Cette opération appartient à `AgenceSpatiale`.

## 3. Engins et capacités

### Choix retenu

`Engin` est abstraite. `Rover` et `Drone` en héritent. Les capacités sont séparées dans `Deplacable`, `Preleveur` et `Cartographe`.

### Pistes écartées et raisons

- **Mettre toutes les opérations dans `Engin` :** cela imposerait à un véhicule futur des opérations qu'il ne sait pas réaliser. Les interfaces expriment des capacités optionnelles.
- **Faire un enum de type d'engin comme clé de l'historique de zone :** l'ajout d'un véhicule nécessiterait de modifier cet enum central. Le code utilise la classe concrète de l'engin comme clé, ce qui permet d'étendre les types sans toucher à `Zone`. Les capacités sont interrogées via les interfaces et les contraintes d'atmosphère via une méthode polymorphe sur `Engin`, pas via des tests codés en dur pour `Rover` et `Drone`.
- **Faire de `Engin` un simple contrat d'interface :** il y a un état et des données partagées à mutualiser, ce qui justifie une classe abstraite.
- **Mettre `deplacer()` directement dans `Engin` :** ton raisonnement est défendable pour les deux véhicules actuels, qui se déplacent tous les deux. Nous gardons néanmoins `Deplacable` pour ne pas obliger un futur engin fixe ou installé en surface à fournir artificiellement cette capacité. C'est un compromis d'extensibilité, pas une règle universelle.

## 4. Monde

### Choix retenu

`CorpsCeleste`, `Carte`, `Zone` et `Position` sont des classes concrètes (Position est un record immuable). `CorpsCeleste` possède une `Carte`, qui contient une grille rectangulaire `Zone[][]`.

### Pistes écartées et raisons

- **Une sous-classe pour chaque planète ou lune :** aucune différence de comportement n'était spécifiée. Des objets `CorpsCeleste` paramétrés sont plus simples.
- **Fusionner `Carte` et `CorpsCeleste` :** leurs responsabilités sont différentes. Le corps décrit le lieu ; la carte organise spatialement ses zones.
- **Carte en `Map<Position, Zone>` :** une carte rectangulaire est naturellement représentée par une matrice, dont les accès par coordonnées sont simples.
- **Un enum unique d'état de zone :** `CARTOGRAPHIEE` et `PRELEVEE` peuvent être vrais simultanément. Les actions sont donc distinctes.

## 5. Exploration : décisions personnelles conservées

Ces choix s'écartent partiellement du texte initial du sujet, mais sont conservés parce qu'ils correspondent à la conception validée pendant l'analyse.

### Réexploration avec confirmation

Le sujet limite l'exploration à une fois par type de véhicule. Le choix retenu permet une répétition après confirmation explicite. `Mission.confirmerReexploration(zone, typeEngin)` autorise une occurrence supplémentaire ; l'autorisation est consommée par l'enregistrement du résultat réussi.

Une tentative sans confirmation est refusée. Une tentative ratée n'augmente pas l'historique.

L'ancienne proposition `Map<TypeEngin, ResultatExploration>` était inadéquate : une clé ne peut contenir qu'une valeur et écrase ou empêche la conservation de répétitions. Le code utilise donc un compteur historique par classe d'engin dans `Zone` et une `List<ResultatExploration>` dans chaque `Mission`.

### Déplacement séparé

Un engin peut traverser une zone sans l'explorer. `deplacer()` ne modifie que sa position. La cartographie et le prélèvement produisent des résultats scientifiques distincts.

### Échec de prélèvement

Un résultat n'est créé qu'après réussite. Un prélèvement raté ne crée ni échantillon, ni événement d'exploration, ni mise à jour de la zone.

### Intérêt scientifique évolutif

L'intérêt de la zone peut augmenter au fil des découvertes, sans dépasser 10. Une découverte de vie le porte à 10. Le rapport conserve le total calculé à la clôture afin que l'archive ne change pas lorsque la zone évolue ensuite.

### Danger

Le niveau de danger appartient à `Zone`. La résistance appartient à `Engin`. La compatibilité est vérifiée avant le démarrage et lors du choix d'un véhicule par le simulateur. On n'a pas créé de sous-classes artificielles de zone dangereuse.

## 6. Missions et gestion

`Mission` détient ses zones objectifs, ses ressources et son historique de résultats. `AgenceSpatiale` valide les règles globales : disponibilité, compétences, atmosphère du corps céleste, danger, plafond simultané et promotion.

L'idée de centraliser toutes les validations dans `Mission` a été écartée, car une mission ne connaît pas nécessairement les autres missions qui consomment les ressources de l'agence.

Le sujet comporte deux valeurs différentes pour le nombre maximal de missions simultanées. La valeur 2 est centralisée dans une constante afin de pouvoir être modifiée à un seul endroit.

## 7. Simulation et concurrence

`SimulateurMission` utilise un `ExecutorService`. Le programme principal n'attend pas le déroulement de la simulation, sauf lorsqu'il demande explicitement le résultat via `Future`.

Le temps par zone, les probabilités et la graine aléatoire sont configurables. Une graine fixe rend les tests reproductibles. La clôture libère les ressources, promeut les cadets qui remplissent les conditions et produit le rapport.

## 8. Persistance

Seul `RapportMission` est annoté `@Entity`. Il contient des valeurs figées et le nom du corps céleste, sans relation JPA vers les objets métier conservés en mémoire. `RapportMissionRepository` isole l'accès H2.

## 9. Évolutivité : portée réelle

- L'héritage mutualise les données communes.
- Les interfaces permettent de composer les capacités des engins.
- Les nouvelles classes d'engins sont identifiées par leur classe Java dans l'historique des zones.
- Les comparateurs sont définis au moment des tris plutôt que d'imposer un ordre unique aux missions.
- Le repository isole la persistance.

Cela ne rend pas chaque extension automatique : une nouvelle capacité doit aussi être comprise par la simulation et les règles d'affectation. L'architecture réduit les modifications nécessaires, elle ne les supprime pas.
