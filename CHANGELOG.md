# Changelog

All notable changes to this project are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/); versions follow
`MAJOR.MINOR.PATCH+<mc>-<loader>` (e.g. `1.0.0+1.21.1-neoforge`).

## [Unreleased]

### Added
- `render` post-processing: `PostEffects`/`PostEffect`/`ChainSpec` with built-in `Blur`, `Bloom`, `Vignette`, `Desaturate`, `Pixelate` and `ColorGrade` effects (GLSL programs in `assets/bloomberto/shaders`), animated intensity (`fadeIn/fadeOut/fadeTo`), chained in registration order after the world is drawn; skipped while Iris/Oculus/OptiFine run a shader pack. Chains are built in code because 1.20.1 has no public way to set uniforms on a JSON chain. Image LUT grading is not included yet. Not yet seen on screen.
- `render` textures: `Textures.fromUrl/fromFile/fromBytes/fromImage` with background decode (PNG/JPEG/GIF/BMP), disk cache, size limits and a placeholder via `TextureHandle`. `render` shaders: `Shaders.create` / `ShaderProgram` with typed uniforms, rebuilt after resource reloads. Logic is unit tested; GPU upload not yet seen on screen.
- `render` world: `ClientEvents.WORLD_RENDER` (with `WorldRenderContext`) on every loader and `WorldDraw` for lines, boxes, filled boxes, quads, textured quads, billboards and 3D text, built on `impl.render.Vtx` that hides the 1.20.1 / 1.21.1 vertex API difference. Compiles on all targets; not yet seen on screen.
- `core` registry: `Registrar` declares items, blocks, block items, entity/block-entity types, sounds, mob effects, attributes and creative tabs (or any registry) once, and registers them through Fabric registries or the Forge/NeoForge `RegisterEvent`. Call `register()` from the mod constructor.
- `core` data: `EntityData`/`DataKey`/`DataSerializer` attach saved data to players and entities by UUID (stored in a world `SavedData`, same on every loader, survives death), with `synced()` keys sent to the owning player. Uses one mechanism for all loaders instead of Capabilities / Data Attachments.
- `core` config: `ConfigBuilder`/`Config`/`ConfigValue` with typed options (boolean, int/double ranges, string, enum, string list), TOML file with comments, validation (clamping, defaults, backup of unreadable files), `synced()` options sent server-to-client on join and cleared on disconnect. The auto-generated config screen is still to do.
- `core` network: `Network.registerS2C/registerC2S`, `sendToPlayer/sendToAll/sendToNear/sendToServer`, `PacketType`/`PacketCodec`/`PacketContext`; one API over Fabric networking, Forge channels (single envelope channel) and NeoForge/Fabric 1.21 custom payloads.
- `core` events: `BloombertoEvents` (server tick, level load, player join/leave) and client-only `ClientEvents` (client tick, HUD render) wired on every loader; shared `Schedulers` and `TweenManager` are now ticked automatically. World-render event comes with the render module.
- `animation`: `Easing` (all standard curves), `Tween` (float, Vector3f, Quaternionf, Color), `TweenManager`, `Timeline` (sequence/parallel) and `KeyframeTrack`, with tests.
- `core`/`api`: `Color` (RGB/HSV/hex), `Interpolation`, `Spline`, `Noise` and a tick `Scheduler`, with JUnit 5 tests.
- `testmod`: example mod packaged as a separate jar per target (`bloomberto-testmod-*.jar`).
- Phase 0 skeleton: Stonecutter + Architectury Loom build producing 5 jars (Fabric/Forge 1.20.1, Fabric/Forge/NeoForge 1.21.1), `Bloomberto.id`, `Services.PLATFORM`.
- Repository base: README, license (MIT), `.gitignore`, project brief in `docs/`.
