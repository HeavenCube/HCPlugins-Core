# Créer un plugin HCPlugins

## 1. Définir le besoin et le propriétaire

Lire [AGENTS.md](../AGENTS.md), [ECOSYSTEM.md](ECOSYSTEM.md) et la capacité Core utile dans
[TECHNICAL.md](TECHNICAL.md). Définir métier, commandes, permissions, données, threads et intégrations.
Chercher Core/Paper avant d'écrire une variante ; extraire d'abord la partie réellement commune.
Nom GitHub `HeavenCube/HCPlugins-<Nom>`, nom serveur explicite (exemple HCTemplate), package
`fr.noltox.hcplugins.<domaine>`. Base Git neuve sans historique monorepo ; création/push seulement
avec autorisation utilisateur.

## 2. Layout et builds composites

```text
parent/                              # parent non Git
├── HCPlugins-Core/
└── HCPlugins-Template/
    ├── AGENTS.md, CLAUDE.md, GEMINI.md, README.md, LICENSE
    ├── docs/TECHNICAL.md
    ├── settings.gradle.kts, build.gradle.kts, gradle.properties
    ├── gradlew, gradlew.bat, gradle/wrapper/*
    ├── .github/workflows/build.yml, .github/dependabot.yml
    └── src/main/{java,resources}/
```

Copier uniquement le wrapper vérifié d'un plugin actif, avec son checksum. Exclure .git,
.gradle, build, .hcplugins, modules shared et build-logic historiques. Les extraits sont une base
à adapter, pas un plugin métier prêt pour production.

`settings.gradle.kts` :

```kotlin
pluginManagement { repositories { gradlePluginPortal(); mavenCentral() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}
rootProject.name = "HCPlugins-Template"
val coreBuild = file(".hcplugins/HCPlugins-Core").takeIf { it.isDirectory }
    ?: file("../HCPlugins-Core")
require(coreBuild.resolve("settings.gradle.kts").isFile) {
    "Clone HCPlugins-Core next to this repository before building."
}
includeBuild(coreBuild) {
    dependencySubstitution {
        substitute(module("fr.noltox.hcplugins:core-api")).using(project(":core-api"))
    }
}
```

`build.gradle.kts` :

```kotlin
plugins { java }
group = "fr.noltox.hcplugins"
version = providers.gradleProperty("version").get()
java { toolchain.languageVersion = JavaLanguageVersion.of(25) }
dependencies {
    compileOnly("fr.noltox.hcplugins:core-api")
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
}
tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
}
tasks.processResources {
    val pluginVersion = project.version.toString()
    inputs.property("version", pluginVersion)
    filesMatching("paper-plugin.yml") { expand("version" to pluginVersion) }
}
tasks.jar { archiveFileName.set("HCTemplate-${project.version}.jar") }
```

Paper reproduit la branche des consommateurs actuels, pas une promesse de version toujours à jour.
Vérifier les artefacts avant une mise à jour. Pour JUnit, reprendre les versions vérifiées des builds
actifs et useJUnitPlatform. Shadow est utile pour bibliothèques embarquées, jamais pour Core.

`gradle.properties` :

```properties
version=0.1.0-SNAPSHOT
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configuration-cache=true
```

`.gitignore` : .gradle/, build/, .hcplugins/ et fichiers IDE locaux. Le build consommateur compile
l'API Core ; construire Core séparément pour obtenir son JAR serveur.

## 3. Dépendance Paper et permissions

`src/main/resources/paper-plugin.yml` :

```yaml
name: HCTemplate
version: "${version}"
main: fr.noltox.hcplugins.template.HCTemplate
api-version: "26.2"
description: Description concrète du plugin.
author: Noltox
dependencies:
  server:
    HCCore:
      load: BEFORE
      required: true
      join-classpath: true
```

Ajouter les autres plugins serveur selon leur rôle réel, avec compileOnly dans Gradle.
Une dépendance facultative a required false et un chemin sûr en son absence : pas de chargement
de ses types avant contrôle. Ne pas embarquer une API serveur pour cacher une déclaration erronée.
Déclarer explicitement les permissions d'action/défauts et tester opérateur, joueur et console.

## 4. Java et commande canonique

Classe minimale, sans métier :

```java
package fr.noltox.hcplugins.template;

import fr.noltox.hcplugins.core.api.HCPluginsCore;
import fr.noltox.hcplugins.core.api.command.CoreCommandRegistration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;

public final class HCTemplate extends JavaPlugin {
    private CoreCommandRegistration registration;

    @Override
    public void onEnable() {
        var translations = HCPluginsCore.translations(this);
        registration = HCPluginsCore.require(this).register(
                this, "template", "Description concrète", List.of(),
                (source, args) -> {
                    if (!source.getSender().isOp()) {
                        source.getSender().sendMessage(translations.operatorOnly());
                        return;
                    }
                    // Déléguer à une commande/service métier validé.
                });
    }

    @Override
    public void onDisable() {
        if (registration != null) registration.close();
    }
}
```

Ne pas enregistrer /hcplugins dans le plugin. Module unique/minuscule et description explicite ;
ajouter CoreCommand.suggest et syntaxe précise si arguments. Un raccourci indépendant justifié
s'enregistre au lifecycle Paper avec la même logique métier et les mêmes permissions.
Définir la possession/fermeture de listeners, services, tasks, callbacks et données temporaires.
Les callbacks tiers ne sont pas présumés synchrones : scheduler + contexte/permission revalidés.

## 5. Configuration, reload et données

Une configuration unique utilise HCPluginFiles.singleConfiguration(this) et copyDefault ;
plusieurs fichiers utilisent pluginDirectory(this). Types importés de core.api.config :

```java
var path = HCPluginFiles.singleConfiguration(this);
HCPluginFiles.copyDefault(this, "config.yml", path);
var candidate = BukkitYaml.load(path);
// Valider bornes, IDs, références et templates, puis construire un snapshot.
```

Ne jamais remplacer l'état actif avant validation. En cas d'échec, logger la cause et conserver
la génération précédente. Pour le résultat de reload, le Core fournit le nom et la durée :

```java
long started = System.nanoTime();
// Reload effectif et validé ; signaler le succès uniquement après application.
sender.sendMessage(HCPluginsCore.translations(this)
        .reloadSuccess(this, System.nanoTime() - started));
```

Échec : reloadFailure(this). Messages communs dans translations.yml ; métier local.
Ne pas ajouter ms à duration. Valeurs utilisateur MiniMessage : Placeholder.unparsed.
Choisir PDC pour entité/item, YAML pour config/petit état, backend du plugin propriétaire si existant.
SQLite exige un besoin de volume/transactions explicite, pas un ajout automatique à chaque plugin.

## 6. CI, Dependabot et licence

`.github/workflows/build.yml` :

```yaml
name: Build
on:
  push:
    branches: [main]
  pull_request:
  workflow_dispatch:
permissions:
  contents: write
jobs:
  build:
    uses: HeavenCube/HCPlugins-actions/.github/workflows/build.yml@main
    with:
      project-name: HCTemplate
      artifact-path: build/libs/HCTemplate-*.jar
      core-source: true
    secrets: inherit
```

Réutiliser la mécanique release : vN, version datée, JAR et changelog après succès ; PR build seul.
Conserver @main pour le workflow partagé. GRADLE_ENCRYPTION_KEY facultative ; sources publiques
sans token. Concurrency et cache sont chez HCPlugins-actions, ne pas créer des variantes locales.

Copier Dependabot depuis un plugin actif pour Gradle + GitHub Actions, en adaptant seulement les
répertoires de projets réels. Copier [la licence HeavenCube](../LICENSE), pas MIT ; conserver
notices tierces. Voir le [modèle CI](https://github.com/HeavenCube/HCPlugins-actions/blob/main/docs/consumer-template.md).

## 7. Guides IA et contrôle final

AGENTS court adapté depuis un plugin actif : nom, module, dépendances, invariants et validations.
CLAUDE.md/GEMINI.md importent ou référencent AGENTS sans duplication. README : rôle/prérequis,
installation et lien vers docs/TECHNICAL.md. Guide technique : carte du code, flux, permissions,
threads, persistance, reload/disable, packaging, tests/scénarios et limites confirmées.

Avant livraison :

- Builds Core + plugin avec JDK 25 ; version correcte, API Core absente du JAR consommateur.
- Démarrage/disable dans l'ordre, permissions/console, arguments et suggestions.
- Defaults dans layout partagé, reload invalide conservant l'ancien état.
- APIs facultatives absentes/présentes et scénario joueur/GUI/paquet/pack concerné.
- Aucun secret, identifiant en conflit, API serveur embarquée ou artefact historique.
- Liens/instructions accessibles, rapport bref avec validations exactes et prochaines actions.

Risques principaux : API/runtime Core désynchronisés, thread erroné, reload destructeur,
persistance doublonnée, dépendance facultative chargée trop tôt, rendu annoncé sur la seule base
de Gradle. Corriger dans le propriétaire concerné et documenter les limites plutôt que les masquer.
