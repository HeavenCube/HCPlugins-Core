# Guide technique — HCCore

## Navigation

- [AGENTS.md](../AGENTS.md) : instructions courtes au démarrage.
- [ECOSYSTEM.md](ECOSYSTEM.md) : dépendances, règles communes Java/Paper, threads et CI.
- [NEW_PLUGIN.md](NEW_PLUGIN.md) : création pas à pas d'un plugin indépendant.
- [AI_HANDOFF.md](AI_HANDOFF.md) : routage des lectures et résumé entre outils IA.
- Ce guide : implémentation actuelle et choix à préserver lors d'une modification du Core.

## Deux modules, un seul plugin serveur

`core-api` expose `fr.noltox.hcplugins.core.api.*`. Il compile contre Paper, sans dépendre du
runtime Core. Les consommateurs le compilent depuis un clone source en `compileOnly`.
`core-plugin` embarque cette API sans relocation dans `HCCore-<version>.jar` et l'installe
sur le serveur. Les autres plugins ont HCCore requis, `load: BEFORE`, `join-classpath: true`.
Le JAR compile-time `core-api` ne se met pas dans `plugins/`.

## Catalogue des capacités partagées

Les chemins ci-dessous sont relatifs à `core-api/src/main/java/fr/noltox/hcplugins/core/api/`.

| Type / chemin | Contrat | Consommateurs à vérifier |
| --- | --- | --- |
| `HCPluginsCore.java` | `require(plugin)` = registre commandes ; `translations(plugin)` = service texte ; absence = erreur explicite | Tous les appels de lookup |
| `command/CoreCommandRegistry.java` | Inscription par propriétaire, module, description et permissions d'accès | Commandes de chaque plugin |
| `command/CoreCommand.java` | Exécution avec CommandSourceStack et arguments après module ; suggestions facultatives | Parseurs et contrôles d'actions |
| `command/CoreCommandRegistration.java` | Handle closeable/idempotent | Disable et réinscription |
| `message/CoreTranslations.java` | Rendu par clé/valeurs texte, résultats reload et refus communs | Catalogue, loader runtime et tests |
| `message/MiniMessages.java` | Parse standard/strict et resolvers Adventure | Templates et placeholders |
| `config/HCPluginFiles.java` | Layout commun et copie des defaults sans écrasement | Nom serveur et fichiers existants |
| `config/BukkitYaml.java` | YAML strict sur doublons, overload avec defaults embarqués | Configuration candidate et validation locale |
| `permission/DynamicPermissionRegistry.java` | Ownership, respect des permissions externes, thread serveur | Synchronisation/retrait/rollback |
| `glow/GlowProfiles.java` | Cinq IDs de profils, carriers RGB/legacy et types d'effet | Pack, Glowing, ItemFrame |

Core ne gère pas toutes les configurations métier, ni le moteur de glowing. Il ne publie pas
d'API Maven et ne met pas à jour automatiquement son JAR serveur quand un plugin est compilé.

## Lifecycle serveur

1. [HCCore](../core-plugin/src/main/java/fr/noltox/hcplugins/core/HCCore.java) charge les traductions.
2. Il crée le registre, publie CoreCommandRegistry/CoreTranslations avec ServicesManager, puis le module core.
3. Le lifecycle Paper enregistre `/hcplugins` via
   [HCPluginsRootCommand](../core-plugin/src/main/java/fr/noltox/hcplugins/core/command/HCPluginsRootCommand.java).
4. Les plugins se chargent après Core et inscrivent leurs modules. La racine filtre la visibilité
   puis délègue exécution et suggestions ; les contrôles métier restent chez chaque plugin.
5. Sur PluginDisableEvent, Core retire les inscriptions du propriétaire. Sur son propre disable,
   il retire ses services puis ferme le registre.

[CoreCommandRegistryImpl](../core-plugin/src/main/java/fr/noltox/hcplugins/core/command/CoreCommandRegistryImpl.java)
valide module minuscule `[a-z][a-z0-9-]*`, description/permissions ; rejette doublons et mutations
hors thread principal. Le nom `hcplugins` est réservé. Une liste de permissions vide rend le
module visible à tous ; sinon une seule permission de la liste suffit (OU). Cela contrôle
l'accès au module, pas chaque action. Un nom de module n'est pas une permission. Les checks opérateur/action
ne doivent pas disparaître lors d'une extraction. Les raccourcis `/glow` restent chez le plugin.

## Traductions

Source : [translations.yml](../core-plugin/src/main/resources/translations.yml).
Fichier serveur : `plugins/HCPlugins/translations.yml`.
[YamlCoreTranslations](../core-plugin/src/main/java/fr/noltox/hcplugins/core/message/YamlCoreTranslations.java)
valide un candidat avant remplacement du snapshot. Les clés embarquées servent de défauts ;
les clés supplémentaires texte sont accessibles par render. Les clés communes restent valides.

`reload.success` accepte `{plugin}`/`{duration}`, `reload.failure` accepte `{plugin}` ; les refus
de commandes n'ont pas de placeholders. Duration est un entier en millisecondes avec suffixe ms.
Les valeurs passent par Placeholder.unparsed : un nom contenant `<red>` reste du texte.
Clé inconnue ou valeur manquante = erreur explicite.

`/hcplugins core reload` recharge uniquement les traductions, pas toutes les configurations.
Une erreur conserve le snapshot précédent. Les anciens textes locaux sont ignorés par les
consommateurs migrés ; aucun nettoyage automatique des fichiers administrateur n'est effectué.

## Ajouter une fonctionnalité commune

1. Relever les usages et différences, données manipulées et propriétaire légitime.
2. Réutiliser l'API existante ; sinon ajouter un contrat minimal dans core-api.
3. Service stateful : implémentation dans core-plugin, inscription et fermeture explicites.
   Helper stateless réellement partagé : peut rester directement dans core-api.
4. Définir threads, erreurs, valeurs absentes, lifecycle et compatibilité avant les adaptations.
5. Ajouter les tests du contrat/rollback/ownership, puis migrer les consommateurs.
6. Construire Core et les plugins affectés avec les mêmes sources ; actualiser leurs guides.
7. Déployer Core compatible avant les consommateurs ; distinguer JAR/CI d'un déploiement réel.

La centralisation doit supprimer une duplication utile sans dépendance circulaire. Les APIs
tierces et règles métier d'un unique plugin ne deviennent pas automatiquement des services Core.

## Validation

`./gradlew build` / `.\gradlew.bat build` avec JDK 25. Les tests des deux modules couvrent layout,
YAML, profils, registre et traductions. Si packaging évolue : vérifier dans le JAR l'API non
relocalisée, paper-plugin.yml/translations.yml, et l'absence de Paper embarqué.
Les builds des consommateurs ne sont pas lancés automatiquement par celui du Core.

Tests à ouvrir selon le contrat : `HCPluginFilesTest`, `BukkitYamlTest`, `GlowProfilesTest`
dans core-api ; `CoreCommandRegistryImplTest` et `YamlCoreTranslationsTest` dans core-plugin.

En jeu : démarrage dans l'ordre, liste/modules/suggestions, autorisations, reload texte valide
et invalide, disable d'un propriétaire et absence d'inscription orpheline. Le test unitaire
seul ne prouve pas ce comportement. Pour la CI, voir ECOSYSTEM et HCPlugins-actions.
