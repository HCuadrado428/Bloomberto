# Building Bloomberto

Requirements: JDK 21 (the 1.20.1 jars are compiled with `--release 17`), network access to the Maven
repositories listed in `settings.gradle.kts`.

```
./gradlew build                   # builds the 5 jars
./gradlew :1.21.1-fabric:build    # one target
```

Jars are written to `versions/<mc>-<loader>/build/libs/`.

## Toolchain versions (verified against the official Maven repositories)

| Piece | Version |
|---|---|
| Gradle | 9.6.1 (Stonecutter 0.9.x requires Gradle 9) |
| Architectury Loom | 1.11-SNAPSHOT |
| Stonecutter | 0.9.8 (0.10 needs Java 25, so it is not used) |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.92.12+1.20.1 / 0.116.18+1.21.1 |
| Forge | 1.20.1-47.4.29 / 1.21.1-52.1.21 |
| NeoForge | 21.1.266 |
| Parchment | 2023.09.03 (1.20.1) / 2024.11.17 (1.21.1) |

## Layout

Instead of separate `common/`, `fabric/`, `forge/` Gradle modules, Stonecutter builds one project per
`<mc>-<loader>` target from a single source tree:

- `src/main/java` — shared code. Version differences use Stonecutter comments (`//? if >=1.21 {`).
- `src/main/java/.../platform/<loader>/` — loader-specific code; only the active loader's package is compiled.
- `loaders/<loader>/resources/` — loader metadata (`fabric.mod.json`, `mods.toml`, `neoforge.mods.toml`)
  and the `ServiceLoader` registration for `PlatformHelper`.
- `versions/<mc>-<loader>/gradle.properties` — dependency versions for that target.

## Smoke test

`scripts/smoke-test.sh [target...]` starts a dedicated server per target and checks that Bloomberto logs
`Initializing Bloomberto on <LOADER>`.

| Target | Status |
|---|---|
| 1.20.1-fabric | OK |
| 1.20.1-forge | OK |
| 1.21.1-fabric | OK |
| 1.21.1-neoforge | OK |
| 1.21.1-forge | **Known issue**: the jar builds, but Architectury Loom's dev `runServer` fails to start (module resolution errors in Forge's bootstrap, also with Loom 1.13). Needs verification with a real Forge 1.21.1 server/client. |
