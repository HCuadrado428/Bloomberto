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

`scripts/smoke-test.sh [target...]` starts a dedicated server per target (with `-Dbloomberto.debugEvents=true`) and checks that Bloomberto logs
`Initializing Bloomberto on <LOADER>` and fires `level_load` and `server_tick`. Client events (client tick, HUD) are compiled on all targets but not yet run, since there is no client in this environment.

| Target | Status |
|---|---|
| 1.20.1-fabric | OK |
| 1.20.1-forge | OK |
| 1.21.1-fabric | OK |
| 1.21.1-neoforge | OK |
| 1.21.1-forge | Jar verified on a real Forge 1.21.1-52.1.21 server (installer). Loom's dev `runServer` fails for this target (module resolution errors in Forge's bootstrap, also with Loom 1.13), so `smoke-test.sh` reports FAIL for it; this is a Loom dev-run limitation, not a problem of the jar. |

## Testmod

`src/testmod` holds an example mod (`bloomberto_testmod`) that depends on Bloomberto. Each target also
produces `bloomberto-testmod-<version>.jar`; install both jars in `mods/` to try it. Add one example per
feature here as modules are implemented.

## In-game self-test

The testmod checks every feature by itself on a real game and logs `[selftest] PASS <check>` or `[selftest] FAIL <check>: <why>`:

```
scripts/selftest.sh server 1.20.1-fabric 1.20.1-forge 1.21.1-fabric 1.21.1-neoforge   # dedicated servers (13 checks)
scripts/selftest.sh client 1.20.1-fabric 1.20.1-forge 1.21.1-fabric 1.21.1-neoforge   # singleplayer: 13 server + 15 client checks
```

The server side covers registries, events (including `SERVER_STOPPED`), packet codecs (round trip and truncated packets), config
files (clamping, broken file with backup), datagen, the Blockbench files packed in the jar, Molang, particles, entity data on a live
pig (and its removal when the pig is gone), the scheduler with a failing task in between, and tweens. The client side covers
translations, key mappings, models from resource packs, textures (a good, a corrupt and an oversized image), the custom shader,
every post effect, camera shake and zoom, two animated block entities, world and HUD render events, the UI and config screens,
item tooltips, networking with synced entity data, and an F3+T reload. With `-Dbloomberto.selfTest=server|client|all` it also runs
in any other launcher; the game closes when done (`-Dbloomberto.selfTest.exit=false` keeps it open). CI runs both sides on every
push. With Iris 1.7.6, Sodium 0.5.13 and a shader pack on a real Fabric 1.20.1 client (official Fabric loader profile, our launcher
script) all 28 checks also pass, including every post effect drawn over the shaded image. Last run by hand: all 28 checks passed on Fabric 1.20.1 and 1.21.1, Forge 1.20.1 and 1.21.1 (Forge's own installer and a
small launcher script, since Loom cannot run it), NeoForge 1.21.1 and Quilt 1.20.1, plus the 13 server checks on a dedicated
server of each. `src/test/.../fuzz/FuzzTest.java` throws thousands of corrupted model, animation, Blockbench, TOML, Molang and image
inputs at the parsers; bad input must only ever produce the documented checked exception.

## Quilt

The Fabric jar runs on Quilt Loader (no separate build). Verified with Quilt Loader 0.30.2 and Quilted Fabric API 7.7.0+0.92.2-1.20.1 on Minecraft 1.20.1: a dedicated server (init, events, networking, registries) and a client (HUD, custom shader, camera shake through the mixin). Install the Quilt launcher profile with `quilt-installer install client|server <mc>` and put Quilted Fabric API, `bloomberto` and `bloomberto-testmod` in `mods/`.

## Network status

Packet registration is verified on all 5 targets (dedicated server). Sending and receiving packets over a real
connection is **not yet verified**: it needs a client, which this environment does not have. With
`-Dbloomberto.debugEvents=true` Bloomberto registers a ping/pong pair that logs on join, so the round trip can be
checked by joining a server with a client (`[debugEvents] client received ping` then `server received 'pong:...'`).

## Client smoke test without a monitor

Rendering code can be checked on a machine with no GPU using a virtual display and Mesa's software OpenGL
(`xvfb`, `libgl1-mesa-dri`, `imagemagick`):

```bash
LIBGL_ALWAYS_SOFTWARE=1 MESA_GL_VERSION_OVERRIDE=4.5 MESA_GLSL_VERSION_OVERRIDE=450 \
  xvfb-run -a -s "-screen 0 1280x720x24" \
  ./gradlew :1.21.1-fabric:runClient --args="--quickPlaySingleplayer <world> --username Dev"
```

Put a world in `versions/<target>/run/saves/<world>` (set `allowCommands:1b` in its `level.dat` to use chat commands) and the
`bloomberto-testmod` jar in `versions/<target>/run/mods`. `import -window root shot.png` (with the `DISPLAY` and
`XAUTHORITY` of the Xvfb process) takes a screenshot, and `scripts/xinput.py` sends keys, text and clicks
(`python3 scripts/xinput.py key u`, `type "/tp @s ~ ~ ~"`). Verified this way on 1.21.1 Fabric: world drawing (box, line, 3D text, billboard, trail), all six post effects
(blur, bloom, pixelate, color grade, desaturate, zoom) plus vignette, a texture downloaded from a URL, a custom core shader with
a uniform, an animated Blockbench model on a block entity, HUD overlays, keybinds, the UI screen, networking and entity data.
On NeoForge 1.21.1 (client): startup, networking, world drawing, URL texture, custom shader, a post effect and the UI screen.
On Forge 1.21.1 (client, installed with Forge's own installer and launched by a small script, since Loom's `runClient` fails for
it): startup, networking, entity data, the HUD layer, UI screen, keybinds, post effects, URL texture, custom shader and world
drawing. On 1.20.1 Fabric: startup and world drawing; on Forge 1.20.1 (client, Bloomberto alone): startup, networking and keybinds.
On 1.20.1 Fabric the post effects were also checked (blur, bloom, pixelate, vignette) with `-PautoEffect=<name>` (turns an effect on without a keyboard). On Forge 1.20.1 the testmod (put `bloomberto-testmod-<version>-dev.jar`, not the remapped one, in `run/mods`) was also run: HUD, world drawing, custom shader and the camera shake. Camera shake and zoom were also checked on Fabric 1.21.1 (mixins); the zoom leaves the first-person hand at its normal size (seen on Fabric 1.20.1), and the shake on Fabric 1.20.1 and NeoForge 1.21.1, and by the author on Fabric 1.20.1 with Iris. Forge 1.21.1 (manual install) was checked for the camera shake and the zoom. A real two-player session (Quilt 1.20.1 dedicated server, two clients that joined by `--quickPlayMultiplayer`) showed both players receiving the testmod greeting and the server receiving both replies. OptiFine detection is covered by a unit test that stands in for `net.optifine.Config`; OptiFine itself is not redistributable and was not run. Not run: Fabric/Forge/NeoForge servers with real players on 1.21.1.

Known quirk: under this launcher Forge 1.21.1 sometimes crashes ~1 minute after joining, when a bat dies in lava
("Can not retrieve LootModifierManager until resources have loaded once", inside Forge); setting the `doMobLoot` game rule
to false avoids it. It is not in Bloomberto code.

## AI disclosure

AI disclosure: most of this library's code, tests, documentation and default assets (UI textures, example models and shaders) were produced with Claude (Anthropic's AI assistant) from my project brief. I directed the design, tested it in real clients (Fabric with Iris and Sodium), reported the bugs I found, and reviewed the results. Every feature is covered by unit tests, CI builds and in-game checks listed in docs/BUILD.md.
