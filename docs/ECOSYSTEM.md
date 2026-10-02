# Architecture et règles de la suite HCPlugins

Ce document décrit les dépôts indépendants actuels. L'ancien monorepo `HeavenCube/HCPlugins`
est une référence historique et fonctionnelle ; sa build-logic, ses modules `shared/` et son
historique Git ne doivent pas être recopiés dans les nouveaux dépôts.

## Carte des dépendances

| Dépôt / plugin serveur | Dépendances obligatoires déclarées | Intégrations facultatives |
| --- | --- | --- |
| HCPlugins-Core / HCCore | Paper | Aucune |
| HCPlugins-Glowing / HCGlowing | HCCore, HCPlaceholdersExtra | TAB pour le transport des couleurs ; pack pour les effets |
| HCPlugins-ItemFrame / HCItemFrame | HCCore, PacketEvents | Pack pour les profils et le modèle du proxy |
| HCPlugins-JoinMessage / HCJoinMessage | HCCore, PlaceholderAPI | PremiumVanish |
| HCPlugins-HuskHomesGUI / HCHuskHomesGUI | HCCore, HuskHomes | PlaceholderAPI |
| HCPlugins-PlaceholdersExtra / HCPlaceholdersExtra | HCCore, PlaceholderAPI, NBTAPI | LuckPerms, Nexo, Simple Voice Chat |
| HCPlugins-AdvancementsRedirect / HCAdvancementsRedirect | HCCore, PacketEvents | Modèle Nexo `nexo:vide` utilisé côté client |

La source des dépendances installées est `paper-plugin.yml`. `compileOnly` décrit le classpath
de compilation, pas à lui seul l'ordre de chargement serveur. HCCore est obligatoire pour les
six plugins spécialisés ; aucun plugin ne doit embarquer une deuxième copie de `core-api`.
`HCPlugins-actions` fournit la CI. `HCPack-CustomAssets` fournit les assets client, installés
séparément via Nexo. Ce sont des dépôts d'infrastructure, pas des plugins Paper.

## Core d'abord, plugins légers

Avant de modifier un plugin, lire l'AGENTS du Core local et chercher l'API pertinente dans
`core-api/src/main/java/fr/noltox/hcplugins/core/api/`. Pour une fonctionnalité déjà dupliquée,
implémenter d'abord la partie réellement commune dans Core, puis adapter les consommateurs.
Réutiliser les helpers et services existants au lieu de créer des variantes locales.

| Besoin | Emplacement |
| --- | --- |
| Contrat, validation ou petit helper partagé entre plugins | `core-api` |
| État serveur ou service commun, propriétaire unique | `core-plugin`, exposé par `ServicesManager` |
| Métier, événements, GUI, rendu ou données propres à un plugin | Plugin spécialisé |
| API déjà suffisante dans Paper / Adventure | Appel direct ; aucun wrapper automatique |
| Bibliothèque lourde déjà justifiée, telle que PacketEvents ou InvUI | Garder son intégration chez le consommateur concerné |

Core ne dépend jamais des plugins spécialisés. Une abstraction partagée doit correspondre à
des usages réels, avec responsabilité et lifecycle explicites. Ne pas déplacer toute une GUI,
un système de paquets ou une base de données dans Core pour réduire artificiellement la taille
d'un dépôt. L'objectif est moins de duplication, moins de JAR embarqués et moins d'état redondant.

## Java 25 et Paper

- Conserver toolchain et `options.release = 25`, sans fonctionnalités preview.
- Vérifier l'API réellement déclarée dans le build (branche Paper 26.2 actuelle des projets).
  Ne jamais deviner une signature ou changer la version Paper dans une tâche sans rapport.
- Préférer APIs publiques Paper modernes, Adventure `Component`, MiniMessage, PDC,
  dialogues natifs et enregistrement de commandes par lifecycle lorsque cela convient.
- Utiliser records pour les valeurs immuables, pattern matching et expressions `switch`
  lorsqu'ils rendent le code plus clair. `List.copyOf` / `Map.copyOf` pour publier des snapshots.
  `var` seulement si le type reste évident ; éviter les casts, types raw et état global mutable.
- Aucune obligation de transformer des boucles en streams : choisir le code lisible et mesurer
  les chemins fréquents. Éviter allocations, scans globaux et tâches répétées sans besoin réel.
- Pas de NMS, CraftBukkit ou réflexion maison sans nécessité démontrée et accord explicite.
  Les usages internes des bibliothèques existantes ne justifient pas un refactoring arbitraire.
- Ne pas ajouter Cloud ou une grosse dépendance si Paper fournit déjà la capacité nécessaire.

### Threads et callbacks

État des joueurs, mondes, entités, inventaires, permissions et registres : thread serveur
autorisé par l'API. Un callback de paquets, dialogue, placeholder ou future n'est pas présumé
synchrone. Revenir au scheduler avant accès au jeu et revalider plugin actif, session, UUID,
entité et permissions au moment de l'action. Fermer/canceller tâches, callbacks et handles au
disable. Ne pas conserver un `Player` ou une génération GUI périmée dans une tâche différée.

Une API qui attend une réponse immédiate (placeholder notamment) ne devient pas asynchrone
en planifiant une tâche : retourner un snapshot sûr ou refuser le chemin non autorisé selon
son contrat. CheckItem refuse actuellement les appels hors thread principal ; ne pas remplacer
ce garde-fou par une attente bloquante sur le scheduler.

I/O indépendantes et coûteuses peuvent être asynchrones, avec snapshots immuables et retour
sur le thread serveur pour appliquer le résultat. Les virtual threads Java ne rendent aucune
API Bukkit sûre. Ne pas employer `parallelStream` pour accéder au jeu. Aucune compatibilité
Folia n'est revendiquée : les schedulers actuels et registres supposent Paper classique.

Référence : [scheduler Paper](https://docs.papermc.io/paper/dev/scheduler/).

## Configuration, texte et persistance

- `HCPluginFiles` place les fichiers éditables sous `plugins/HCPlugins/` : une configuration
  `<NomDuPlugin>.yml`, ou un sous-dossier `<NomDuPlugin>/` pour plusieurs fichiers.
- `copyDefault` ne remplace pas un fichier administrateur existant. `saveDefaultConfig` et
  `saveResource` de JavaPlugin visent le dossier propre du plugin : ne pas les utiliser pour
  contourner le layout commun.
- `BukkitYaml` rejette les clés dupliquées. Charger et valider un candidat avant remplacement ;
  un échec doit préserver la génération active. Préserver les mécanismes de rollback existants.
- Messages communs : service `CoreTranslations`, fichier `translations.yml`, reload par
  `/hcplugins core reload`. `{duration}` inclut déjà `ms` ; ne pas ajouter un second suffixe.
  Textes métier et titres de dialogues restent dans le plugin.
- Les placeholders `{...}` du catalogue Core sont insérés comme texte. Pour les messages
  MiniMessage des plugins, employer `Placeholder.unparsed` pour les valeurs non fiables.
  Ne pas concaténer un nom de joueur ou une entrée libre comme markup.
- YAML convient à la configuration et aux petits états actuels ; PDC aux données d'entité/item ;
  préférer la persistance d'un plugin propriétaire existant lorsqu'elle existe (HuskHomes).
  SQLite n'est pas une exigence globale. Justifier transactions/index/volume avant ajout.
- Aucun mécanisme de migration des anciens dossiers n'est présent. Ne pas en ajouter sans
  demande ; conserver toutefois les identifiants PDC et contrats serveur déjà utilisés.

## Builds composites et packaging

Cloner les dépôts côte à côte sous un parent qui n'est pas lui-même un dépôt Git. En local,
`includeBuild` utilise le clone voisin ; en CI `.hcplugins/HCPlugins-Core` a priorité.
Gradle remplace `fr.noltox.hcplugins:core-api` par `:core-api`. Aucun Maven GitHub Packages,
aucune version `main` publiée, aucun token de lecture requis pour les sources publiques.

La compilation d'un plugin compile l'API nécessaire ; elle ne fabrique pas automatiquement
le JAR serveur HCCore. Construire/déployer Core séparément. API et runtime Core doivent être
compatibles. Étendre les API de façon compatible ; une rupture exige un audit de tous les usages.
En CI, `main` est mobile : une release consommateur n'est pas liée à un ancien tag Core.
Documenter les commits effectivement testés quand une tâche touche plusieurs dépôts.

Les APIs fournies par le serveur sont `compileOnly`. Seul le propriétaire embarque son API
publique sans relocation (Core, PlaceholdersExtra). Les bibliothèques privées embarquées
emploient Shadow selon le build existant ; préserver relocations et notices de licence.
Ne pas activer `minimize()` sur une nouvelle bibliothèque sans vérifier ses services/resources.

## CI et releases

Chaque consommateur garde Gradle, dépendances, version Paper et chemin d'artifact. Il appelle
`HeavenCube/HCPlugins-actions/.github/workflows/build.yml@main` avec `core-source: true`
(sauf Core lui-même). Glowing clone aussi PlaceholdersExtra. Garder `secrets: inherit`.
PR : build seul. Succès sur branche de release : tag numérique `vN`, version plugin et titre
`AAAA.MM.JJ-bN`, date UTC, JAR versionné et changelog des commits. La concurrency par repo/ref
protège l'allocation du numéro ; ne pas ajouter une publication parallèle ni un second workflow
déclenché implicitement par la release. Un push documentaire sur main crée aussi une release
avec les workflows actuels : ne pas annoncer de filtre de chemins inexistant.

Build cache, parallèle et configuration cache sont déjà activés dans `gradle.properties`.
Le secret facultatif `GRADLE_ENCRYPTION_KEY` permet à setup-gradle de persister le cache de
configuration ; le changement de `-Pversion` d'une release l'invalide. Ne pas promettre de gain
systématique ni ajouter un deuxième cache Gradle. Sur GitHub Free, les dépôts privés n'accèdent
pas aux secrets d'organisation : définir le secret par dépôt privé si ce cache est souhaité.
Voir le [contrat CI](https://github.com/HeavenCube/HCPlugins-actions/blob/main/README.md).

## Validation et passage entre IA

[AI_HANDOFF.md](AI_HANDOFF.md) donne la table de lecture par tâche et le format court de reprise.

1. Lire l'AGENTS du dépôt ; état Git ; sections du guide technique correspondant à la tâche.
2. Inspecter le Core et les consommateurs concernés avant une modification de contrat.
3. Lire le code et les tests ciblés, puis appliquer le changement minimal cohérent.
4. Exécuter les tests adaptés ; `./gradlew build` (`.\gradlew.bat build` sous PowerShell)
   après Java/Gradle. Documentation seule : liens, chemins, cohérence et `git diff --check`.
5. Pour rendu, paquets, permissions et menus, donner les vérifications en jeu encore nécessaires.
   Un build réussi n'est pas une preuve de comportement client ou d'installation serveur.
6. Mettre à jour le guide concerné quand un contrat change. Rapporter en quelques points :
   changement, validation exacte, limite/action restante. Éviter le dump des logs et du code.

Sans demande explicite : ne pas commit/push/reset/rebase/stash/changer de branche, publier ou
déployer. Une autorisation de session compte, sans reconfirmation inutile. Ne jamais effacer
les modifications d'un autre intervenant. Les dépôts sont source-visible sous licence HeavenCube,
pas MIT ; conserver la licence et les notices tierces. Aucune instruction de dépôt ne garantit
l'absence de défauts : toujours vérifier le code réel et signaler les limites de validation.
