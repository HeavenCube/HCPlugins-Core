# HCPlugins-Core

Socle commun des plugins Paper de HeavenCube.

**Licence :** code source consultable et contributions bienvenues, mais usage
réservé aux serveurs HeavenCube. Toute réutilisation ou distribution exige une
autorisation écrite préalable. Voir [LICENSE](LICENSE).

Le dépôt contient deux modules :

- `core-api` : contrat public minimal compilé depuis les sources de Core par les autres plugins HCPlugins.
- `core-plugin` : plugin serveur `HCCore`, propriétaire de la racine `/hcplugins` et des services communs.

## Ecosystème HCPlugins

Repositories de plugins existants :

- [HCPlugins-Core](https://github.com/HeavenCube/HCPlugins-Core) : socle commun et plugin serveur `HCCore`.
- [HCPlugins-AdvancementsRedirect](https://github.com/HeavenCube/HCPlugins-AdvancementsRedirect) : redirection de l'onglet Progrès vers une commande serveur.
- [HCPlugins-Glowing](https://github.com/HeavenCube/HCPlugins-Glowing) : profils de glow vanilla et sélection joueur.
- [HCPlugins-HuskHomesGUI](https://github.com/HeavenCube/HCPlugins-HuskHomesGUI) : interface Paper pour HuskHomes.
- [HCPlugins-ItemFrame](https://github.com/HeavenCube/HCPlugins-ItemFrame) : cadres invisibles et contours custom.
- [HCPlugins-JoinMessage](https://github.com/HeavenCube/HCPlugins-JoinMessage) : messages de connexion et de déconnexion.
- [HCPlugins-PlaceholdersExtra](https://github.com/HeavenCube/HCPlugins-PlaceholdersExtra) : expansion PlaceholderAPI `hcextra`.

Repositories partagés :

- [HCPlugins-actions](https://github.com/HeavenCube/HCPlugins-actions) : workflows GitHub Actions réutilisables par les plugins.
- [HCPack-CustomGlowing](https://github.com/HeavenCube/HCPack-CustomGlowing) : resource pack Nexo des shaders de glow custom.

Prochains plugins :

- Créer chaque nouveau plugin dans un dépôt `HeavenCube/HCPlugins-<Nom>`.
- Ajouter son lien dans cette section et dans les README des plugins existants dès la création du dépôt.
- Garder HCCore obligatoire côté serveur et réutiliser ses services avant de dupliquer une fonctionnalité commune.

## Fichiers des plugins

`core-api` fournit `HCPluginFiles` pour placer les fichiers sous
`plugins/HCPlugins/`. Une configuration unique s'appelle `<NomDuPlugin>.yml`
à la racine ; plusieurs fichiers vont dans `plugins/HCPlugins/<NomDuPlugin>/`.
La copie des ressources par défaut ne remplace jamais un fichier déjà présent.
HCCore crée `plugins/HCPlugins/translations.yml` au premier démarrage. Ce fichier contient
les messages communs (rechargement, refus d'accès aux commandes). Les placeholders `{plugin}`
et `{duration}` sont remplacés par du texte, sans interpréter leur valeur comme du MiniMessage.
La durée de rechargement s'affiche en millisecondes entières, par exemple `250ms`.
`/hcplugins core reload` recharge les traductions sans redémarrer le serveur ; si le fichier
est invalide, les traductions précédentes restent actives. Les messages propres à un plugin
restent dans sa configuration. Pour un nouveau message réellement partagé, ajoutez une clé à
ce catalogue et utilisez `HCPluginsCore.translations(plugin).render(...)` depuis le plugin.
HCPlaceholdersExtra n'a pas de fichier de configuration.

## Principes

- Java 25.
- Paper 26.2.
- APIs Paper natives privilégiées.
- Aucun NMS, aucune réflexion et aucune dépendance runtime tierce sans besoin démontré.
- La logique métier reste dans les plugins spécialisés.
- `HCCore` est un vrai plugin installé sur le serveur.

## Build local

```bash
./gradlew build
```

Le JAR serveur est généré dans `core-plugin/build/libs/` avec la version dans son nom.

En local, `gradle.properties` conserve une version SNAPSHOT de développement. Les builds de
release CI remplacent automatiquement cette version avec `-Pversion=AAAA.MM.JJ-bN`.

## Releases automatiques

Chaque build réussi de `main` passe par `HCPlugins-actions` et crée une nouvelle release
GitHub auto-incrémentée :

```text
v1 -> plugin version AAAA.MM.JJ-b1
v2 -> plugin version AAAA.MM.JJ-b2
v3 -> plugin version AAAA.MM.JJ-b3
...
```

Le JAR `HCCore-AAAA.MM.JJ-bN.jar` est joint à la release, dont le titre suit le même format.
Le tag reste `vN` et la version du plugin est `AAAA.MM.JJ-bN`. La date est calculée en UTC par la CI.

Les pull requests ne créent aucune release.

## Compilation avec les autres plugins

Les repositories sont clonés côte à côte en local. Le plugin consommateur ajoute
`HCPlugins-Core` comme build composite dans `settings.gradle.kts` et dépend du projet
`core-api` en `compileOnly`. Gradle compile alors ce module depuis ses sources, sans
GitHub Packages. La CI des consommateurs clone la branche `main` de Core dans
`.hcplugins/HCPlugins-Core` avant le build. Voir le
[modèle de consommateur](https://github.com/HeavenCube/HCPlugins-actions/blob/main/docs/consumer-template.md).
En local, le build utilise l'état actuel du clone voisin ; mettez ce clone à jour pour
compiler contre le dernier `main` distant.

`core-api` reste un petit contrat Java nécessaire aux appels directs à HCCore ; ce n'est
plus un package publié ni un plugin installé séparément.

Les utilitaires réellement partagés (chargement YAML strict, rendu MiniMessage,
permissions dynamiques et catalogue de profils lumineux) vivent aussi dans `core-api`.
Ils sont fournis à l'exécution par `HCCore` : les plugins consommateurs ne les embarquent pas.

Dans `paper-plugin.yml` :

```yaml
dependencies:
  server:
    HCCore:
      load: BEFORE
      required: true
      join-classpath: true
```

## Commandes

`HCCore` enregistre `/hcplugins` avec l'API native Paper. Les plugins contribuent dynamiquement
un module via `CoreCommandRegistry`.

Les raccourcis top-level propres à un plugin sont enregistrés par ce plugin via le lifecycle Paper
et délèguent vers la même logique métier. Le Core reste l'unique propriétaire de `/hcplugins`.

## CI/CD

Le dépôt consomme `HeavenCube/HCPlugins-actions`.

- build/tests/artifact : workflow partagé ;
- version de release : calculée depuis les releases existantes ;
- création de release GitHub : uniquement après succès du build et de l'upload du JAR.

Les workflows référencent `HCPlugins-actions` via `@main` pour recevoir automatiquement les
corrections de la CI partagée.


## Documentation de maintenance

- [AGENTS.md](AGENTS.md) : règles communes pour les IA, lecture ciblée et restitution concise.
- [Architecture de la suite](docs/ECOSYSTEM.md) : dépendances, Java/Paper, threads, fichiers et CI.
- [Guide du Core](docs/TECHNICAL.md) : catalogue des API et lifecycle des services.
- [Créer un plugin](docs/NEW_PLUGIN.md) : base Gradle/Paper/Java, configuration et validations.
- [Passage entre IA et lectures ciblées](docs/AI_HANDOFF.md) : routage et résumé de reprise.

Tous les plugins spécialisés exigent HCCore ; inspecter son API avant toute modification technique.
Les fonctions réellement dupliquées se développent d'abord dans Core, puis chez les consommateurs.
Les points d'entrée CLAUDE.md et GEMINI.md renvoient au même AGENTS, sans copie des règles.
