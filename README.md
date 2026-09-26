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

## Build

```bash
./gradlew build
```

Le JAR serveur est généré dans `core-plugin/build/libs/HCCore.jar`.

## API Maven

Coordonnées : `fr.noltox.hcplugins:hcplugins-core-api:<version>`.

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

`HCCore` enregistre `/hcplugins` avec l'API native Paper. Les plugins contribuent dynamiquement un module via `CoreCommandRegistry`.

Les raccourcis top-level propres à un plugin sont enregistrés par ce plugin via le lifecycle Paper et doivent déléguer vers la même logique métier. Le Core reste l'unique propriétaire de `/hcplugins`.

## CI/CD

Les workflows restent volontairement petits afin d'être remplacés ensuite par un toolkit partagé `HeavenCube/actions`, sur le modèle de `GroupeZ-dev/actions`.
