# HCPlugins-Core

Socle commun des plugins Paper de HeavenCube.

Le dépôt contient deux modules :

- `core-api` : contrat public minimal consommé en `compileOnly` par les autres plugins HCPlugins.
- `core-plugin` : plugin serveur `HCCore`, propriétaire de la racine `/hcplugins` et des services communs.

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

Le JAR serveur est généré dans `core-plugin/build/libs/HCCore.jar`.

En local, `gradle.properties` conserve une version SNAPSHOT de développement. Les builds de
release CI remplacent automatiquement cette version avec `-Pversion=<numéro>`.

## Releases automatiques

Chaque build réussi de `main` passe par `HCPlugins-actions` et crée une nouvelle release
GitHub auto-incrémentée :

```text
v1 -> plugin version 1
v2 -> plugin version 2
v3 -> plugin version 3
...
```

Le JAR `HCCore.jar` est joint à la release. Le même numéro est utilisé pour publier
`core-api` dans GitHub Packages avant la création de la release.

Les pull requests ne créent aucune release.

## API Maven

Coordonnées :

```text
fr.noltox.hcplugins:hcplugins-core-api:<version>
```

Exemple côté plugin consommateur :

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/HeavenCube/HCPlugins-Core")
        credentials {
            username = providers.environmentVariable("GITHUB_ACTOR").orNull
            password = providers.environmentVariable("GITHUB_TOKEN").orNull
        }
    }
}

dependencies {
    compileOnly("fr.noltox.hcplugins:hcplugins-core-api:<version>")
}
```

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
- publication `core-api` : même build et même version que la release ;
- création de release GitHub : uniquement après succès des étapes précédentes.

Pendant le bootstrap, les workflows sont référencés via `@main`. Ils devront être épinglés sur
`@v1` quand `HCPlugins-actions` sera stabilisé.
