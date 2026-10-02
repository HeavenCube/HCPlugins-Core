# Instructions — HCPlugins-Core

## Démarrage et lecture ciblée

1. Lire ce fichier et `git status --short`, puis la section utile de [docs/TECHNICAL.md](docs/TECHNICAL.md).
2. [docs/ECOSYSTEM.md](docs/ECOSYSTEM.md) : règles communes et dépendances ;
   [docs/NEW_PLUGIN.md](docs/NEW_PLUGIN.md) : création d'un consommateur ;
   [docs/AI_HANDOFF.md](docs/AI_HANDOFF.md) : routage des lectures et transfert. Lire selon la tâche.
3. Avant une API commune, rechercher ses usages dans les clones voisins ; inspecter contrats et tests ciblés.

## Contrat du Core

- `core-api` contient contrats/helpers partagés ; `core-plugin` est HCCore, runtime propriétaire.
  `core-api` ne dépend jamais de `core-plugin` ni des plugins spécialisés.
- Tous les plugins spécialisés exigent HCCore. Leur API Core est compileOnly via build composite ;
  HCCore seul l'embarque sans relocation. Pas de publication Maven/GitHub Packages.
- Réutiliser Core et extraire les fonctionnalités réellement dupliquées dans Core avant les consommateurs.
  Garder le métier chez son propriétaire ; ne pas centraliser GUI/paquets/données d'un unique plugin.
- Java 25 sans preview, toolchain/release 25 ; conserver Paper déclaré (26.2 actuellement).
  Vérifier signatures réelles et utiliser les APIs publiques Paper modernes et Adventure.
- Records, pattern matching, switch expressions et snapshots immuables lorsque lisibles ; aucune
  micro-optimisation sans mesure. Virtual threads pour I/O indépendantes, jamais pour état Bukkit.
- Pas de NMS/CraftBukkit/réflexion maison ni dépendance lourde sans nécessité et accord explicite.
- HCCore seul possède `/hcplugins`. Registre muté sur thread serveur ; handles closeables/idempotents.
  Retrait automatique des modules au disable du propriétaire et fermeture des services au disable Core.
- Traductions via ServicesManager ; `plugins/HCPlugins/translations.yml`, reload atomique.
  `{duration}` inclut `ms`. Valeurs interpolées comme texte ; préserver la protection MiniMessage.
- Préserver helpers YAML/fichiers/permissions et profils lumineux. Changement de carrier : auditer
  Glowing, ItemFrame et HCPack-CustomAssets ensemble avant modification.
- API compatible en priorité ; rupture : adapter/tester les consommateurs et expliquer l'ordre de déploiement.

## Validation et Git

- Après Java/Gradle : `./gradlew build` ou `.\gradlew.bat build` avec JDK 25, puis consommateurs affectés.
  Contrôler le JAR si packaging modifié ; ne pas masquer une erreur ou désactiver une vérification.
- Docs seules : liens/chemins/faits et `git diff --check` ; pas de build coûteux sans risque concret.
- Gradle/CI réussis ne prouvent pas le comportement en jeu. Indiquer les scénarios non vérifiés.
- Conserver workflow partagé `@main`, version/JAR `AAAA.MM.JJ-bN`, tags numériques et licence HeavenCube.
- Pas de commit/push/reset/rebase/stash/changement de branche/release/déploiement sans autorisation explicite.
  Une autorisation de session compte ; préserver les changements d'autres intervenants.

## Coût de contexte et handoff

- Rechercher avec `rg` dans la zone utile ; regrouper lectures indépendantes, limiter sortie/logs.
  Ne pas relire tous les guides ou scanner build/.gradle. Réutiliser les faits déjà vérifiés.
- Changement minimal cohérent ; pas de refactoring cosmétique ni tests recopiant l'implémentation.
- Pas de sous-agents sans demande/instruction applicable. Clarifier seulement une donnée bloquante.
- Maintenir les guides lorsque le contrat change ; CLAUDE.md/GEMINI.md ne dupliquent pas ces règles.
- Restitution française courte : changement, validation exacte, limite/action restante.
  Pour handoff : objectif, fichiers/commits Core et consommateurs, tests, blocage/prochaine action.
  Aucun secret ni mémoire persistante créée sans demande.
