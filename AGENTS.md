# HCPlugins-Core

## Scope

This repository contains the shared HeavenCube Paper core only:
- `core-api`: public compile-time contract for HCPlugins.
- `core-plugin`: HCCore server runtime.

Do not move business logic from specialized plugins into Core.

## Java / Paper

- Java 25 only; no preview features.
- Target the current Paper 26.2 API declared by this repository.
- Prefer supported Paper APIs over Bukkit legacy APIs when useful.
- No NMS, CraftBukkit internals or reflection.
- Paper game state stays on an allowed server thread.
- Virtual threads are only for suitable independent blocking I/O, never Bukkit/Paper state.

## Dependencies

- Keep the runtime dependency surface minimal.
- Server-provided APIs are `compileOnly`.
- Do not add an abstraction when Paper already provides a reasonable supported API.
- `core-api` must never depend on `core-plugin`.

## Public API

- Keep `core-api` minimal, stable and implementation-agnostic.
- Public types belong under `fr.noltox.hcplugins.core.api.*`.
- API changes potentially affect every HCPlugins repository.
- HCCore is exposed through Paper ServicesManager; consumers use `HCPluginsCore.require(plugin)`.
- Registration handles are idempotent and closeable.

## Commands

- HCCore alone owns `/hcplugins`.
- Modules register through `CoreCommandRegistry`.
- Use Paper's native command API; no command framework without explicit approval.
- Plugin-specific top-level shortcuts belong to each consuming plugin and delegate to the same logic.
- Registration/unregistration happens on the primary server thread.
- Core removes registrations automatically when an owner plugin disables.

## Build / CI / releases

- Keep Gradle simple; shared CI logic belongs in `HeavenCube/HCPlugins-actions`.
- `core-api` publishes `fr.noltox.hcplugins:hcplugins-core-api`.
- `core-plugin` produces `HCCore.jar`.
- The plugin JAR embeds the API without relocating it for joined-classpath consumers.
- Successful `main` CI builds create numeric releases `v1`, `v2`, ... through HCPlugins-actions.
- The generated numeric release version is passed to Gradle as `-Pversion=<n>` and is also used for the Maven API publication.
- Do not manually hardcode a release version into Gradle files for CI.
- Pull requests must build without creating releases or publishing packages.
- During bootstrap shared workflows may use `@main`; once stable, pin them to `@v1`.
- Run `./gradlew build` before finalizing Java/Gradle changes.
- Do not commit, push, rebase, reset, stash or force-update refs unless explicitly requested.

## Style

- Prefer small immutable types, explicit ownership and simple control flow.
- Avoid speculative abstractions and generic helper dumping grounds.
- Tests protect contracts, lifecycle, validation and regressions.
