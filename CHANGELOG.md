# Changelog

All notable changes to this project are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/); versions follow
`MAJOR.MINOR.PATCH+<mc>-<loader>` (e.g. `1.0.0+1.21.1-neoforge`).

## [Unreleased]

## [0.2.0] - API level 2

### Fixed (code review)
- Image "decompression bombs": `Textures` checked an image's size only after decoding it, so a small file claiming a huge picture could exhaust memory. The size is now read from the header first (`ImageDecoder`), and decoding no longer writes ImageIO temp files.
- A download whose server stops sending data could block a texture worker forever; the whole body now has a time budget (three times the request timeout).
- A texture whose loading hit an `Error` (e.g. out of memory) stayed `LOADING` forever; it now ends as `FAILED`.
- Broken or hostile model / animation files (wrong JSON types, absurd nesting) could crash the game from the renderer; every parser now reports them as `ParseException` and `ModelLibrary` logs and skips them. Molang expressions are limited to 4096 characters and 64 levels of nesting; TOML arrays to 32 levels.
- `AnimatedBlockEntityRenderer` ticked only the first block entity of its type each game tick, so with two or more of them the others did not animate. Controllers now tick once per game tick each (`AnimationController.tickAt`).
- A scheduled task, tween or event listener that throws no longer stops or delays the others; a failing tween is removed; repeated listener failures are logged at 1, 2, 4, 8, ... occurrences instead of every frame; `LinkageError`s (such as `NoClassDefFoundError`) in a listener are isolated too.
- `WorldDraw` restores the depth test even if drawing throws.
- `DataGenerator` refuses to write outside its output folder.
- `Shaders.create` with an id that already exists returns the same program instead of leaking a new one; two packet types with the same id are rejected.

### Fixed (found by the in-game self-test)
- `Tooltips` did nothing on Fabric and Quilt: the tooltip callback was never registered there (it worked on Forge and NeoForge).
- In a Forge 1.21.1 singleplayer game started by our test launcher, `LEVEL_LOAD` never fired for the integrated server's levels
  (Forge's level-load and server lifecycle events did not arrive, tick and player events did). Bloomberto now also announces missed
  levels on the server's first tick, once per level.

### Added (testing)
- In-game self-test in the testmod (`scripts/selftest.sh server|client <targets>`, or `-Dbloomberto.selfTest=server|client|all`):
  28 automatic checks of every feature on a real game. CI runs it on every push. Fuzz tests for every parser.
- `PostEffects.hasFailed(effect)` and `AnimationController.time()`.

### Fixed (second review)
- Restarting a tween or keyframe track that is still playing no longer makes it advance twice per tick; it rewinds instead.
- If a post effect fails while drawing, the main framebuffer is bound again, so the hand and HUD are not drawn into the effect's buffer.
- A failing resource-reload callback no longer aborts the whole F3+T reload.
- The config screen's slider no longer divides by zero when an option's minimum equals its maximum.
- Config keys and sections are limited to letters, digits, `_` and `-`, so a name can never produce a broken TOML file.
- Translations (English and Spanish) for the F8 debug key, the config screen and the "Hold [Shift]" tooltip, which showed raw keys or fixed English text; the testmod's keys and blocks have names too.

### Added (code review)
- `Textures.Request.allowLocalAddresses(false)`: refuses URLs (and redirects) that resolve to this computer or the local network, for URLs that come from players or servers. Redirects are now followed by hand (at most 5, never https to http).
- The texture download cache prunes itself once per session: files older than 30 days go, and the oldest are deleted while the folder is over 256 MiB.
- Entity data of destroyed (killed or discarded) non-player entities is deleted automatically; chunk unloads and dimension changes keep it.
- Client and server compare their Bloomberto API level on join; a difference is logged and shown in chat (nothing is refused).
- `WorldDraw.box` draws its 12 edges in one batch instead of 12.
- Animated models no longer rebuild their geometry every frame: `ModelGeometry.compile(model)` prepares each model once (kept until the model object is no longer used, e.g. after F3+T), and `GeoRenderer` transforms its corners straight into the vertex buffer without creating objects. `ModelGeometry.build` still returns the same quads as before.
- `BloombertoEvents.SERVER_STOPPED` on every loader, and `Scheduler.clear()`, so world-specific tasks can be cancelled when a singleplayer world closes.

### Fixed
- `CameraZoom` also magnified the first-person hand: the Fabric mixin and the Forge/NeoForge `ComputeFov` listener changed every `getFov` call, including the hand's fixed one. It now only changes the world's field of view (`useFovSetting` / `usedConfiguredFov()`), so items drawn in the hand keep their size and position while zoomed.

### Added
- `BloombertoApi` (`level()`, `has(n)`, `require(modName, n)`): an API level that goes up with every public API change, so a dependent mod can refuse to start with a clear message when an older jar is installed. Documented in the usage guides, with the stability policy for 0.x.

_API level 1 is the first public build (0.1.0). Everything listed below under 0.2.0 was added after it; jars named 0.1.0 from development builds may contain any part of it._

### Added
- `BbModelParser` and `.bbmodel` support in `ModelLibrary`: Blockbench projects are read directly (cubes, groups, UV, bone animations, Molang, embedded textures). The testmod totem now loads from `models/totem.bbmodel`; a test checks it against `geo/totem.geo.json`. Not compared with exports of real Blockbench files.
- Molang in animations: keyframe values can be expressions (`api.animation.molang.Molang`: arithmetic, comparisons, `?:`, `math.*`, `query.anim_time`/`life_time`, `variable.*`, assignments and `return`), compiled once and evaluated every frame; `AnimationController.variable(name, value)` feeds `variable.*`. Verified in a NeoForge client with the testmod totem (`animation.totem.sway`).
- `CameraZoom`: a real field-of-view zoom with easing that also works with shader packs (Fabric mixin on `GameRenderer.getFov`; `ViewportEvent.ComputeFov` on Forge/NeoForge). Testmod key Z.
- `CameraShake`: shakes the real camera (yaw/pitch), so it keeps working while a shader pack is active. Fabric uses a small mixin on `Camera`; Forge and NeoForge use `ViewportEvent.ComputeCameraAngles`. The testmod's J key uses it.

### Fixed
- Blur and Bloom did nothing on Minecraft 1.20.1: they used vanilla's `box_blur` program, which only exists in 1.21. Bloomberto now ships its own `bloomberto_blur` program for every version.

### Added
- The F8 debug overlay is also enabled when the testmod is installed (it needed `-Dbloomberto.debugOverlay=true` before).
- `ShaderProgram.drawQuad` and `impl.render.Immediate`, which hide the 1.20.1 / 1.21 `BufferBuilder` difference; the `testmod` now has examples of a URL texture, a custom core shader, a Blockbench block entity and a key (Y) that cycles the post effects.

### Fixed
- World drawing on Forge 1.21: the view matrix Forge provides drew nothing; the bridge now derives it from the camera there (Fabric and NeoForge keep using their own, which a camera-derived matrix broke on Fabric).
- Custom core shaders: the program JSON must be in the `minecraft` namespace (`assets/minecraft/shaders/core/<namespace>_<name>.json`); `ShaderProgram` now derives that name.
- Blockbench block entities need a block with `.noOcclusion()`, otherwise the model is lit as if inside a wall (documented).
- Client crash on startup (`getResourceManager()` was still null when the client entrypoint ran): reload listeners are now registered once the resource manager exists.
- Post-processing programs and shaders must live in the `minecraft` namespace in 1.21 (`assets/minecraft/shaders/program/bloomberto_*`); the effects failed to build before.
- World drawing on 1.21: the camera view matrix is now set explicitly (`WorldRenderBridge`), and 3D text is turned to face the camera.

### Added
- `animation` v2: Blockbench support: `GeoParser` (Bedrock geometry 1.12.0 / 1.8.0), `AnimationParser` (linear, Catmull-Rom and step keyframes, loop/once/hold), `AnimationController` (`play(...).transition(n).then(...)`), `ModelGeometry` (pure geometry with GeckoLib conventions), `ModelLibrary` (reloads with F3+T), `GeoRenderer`, `AnimatedEntityRenderer` and `AnimatedBlockEntityRenderer`. Parsing, sampling, blending and geometry are unit tested; drawing on screen is not verified, and the GeckoLib sign conventions are reproduced from memory so mirrored or inverted rotations are possible. No Molang.
- `ui` module: `FlexLayout` (row/column, grow, justify, align, min/max), `NineSlice` geometry, `ScrollModel`, `SliderModel`, `Transition` (fade/slide/scale) as pure tested logic; client widgets `ThemedButton`, `ThemedToggle`, `ThemedSlider`, `ScrollList` and helpers `UiGraphics.nineSlice/scissor/transition` with a default dark theme (generated textures). Widgets compile on all targets but have not been seen on screen. Text fields and a full widget framework are out of scope (use vanilla `EditBox`).
- `datagen`: `DataGenerator` writes item/block models, block states, language files, recipes (shaped, shapeless, cooking), block loot tables and tags in the format of the running version (1.20.1 plural folders and `item` results, 1.21.1 singular folders and `id` results); `Datagen.register` + `/bloomberto datagen <modid>`. Verified on a real Forge 1.21.1 server: the generated data pack loaded 3 new recipes without errors.
- `render` v2: `BuiltInEffects.Shake` (screen shake) and `Zoom` (screen-space zoom) as post effects, `Particles` builder with ring/sphere/line/spiral patterns (client and server), `Trail` ribbons drawn with `WorldDraw.trail`, `RenderTypes` (additive, emissive, glow) and `OffscreenTarget` framebuffers. Geometry and fade logic are unit tested; drawing is not yet seen on screen. The zoom is a screen-space scale, not a real FOV change.
- `core` v2: `Keybinds.register`, `HudOverlays` (ordered, toggleable), `Tooltips` (with `shiftInfo`), `/bloomberto reload` and `/bloomberto info` commands, and a debug overlay (F8) in development. The command is verified on a real Forge 1.21.1 server; keybinds, HUD and tooltips compile on all targets but have not been used on a client.
- `render` post-processing: `PostEffects`/`PostEffect`/`ChainSpec` with built-in `Blur`, `Bloom`, `Vignette`, `Desaturate`, `Pixelate` and `ColorGrade` effects (GLSL programs in `assets/bloomberto/shaders`), animated intensity (`fadeIn/fadeOut/fadeTo`), chained in registration order after the world is drawn; skipped while Iris/Oculus/OptiFine run a shader pack. Chains are built in code because 1.20.1 has no public way to set uniforms on a JSON chain. Image LUT grading is not included yet. Not yet seen on screen.
- `render` textures: `Textures.fromUrl/fromFile/fromBytes/fromImage` with background decode (PNG/JPEG/GIF/BMP), disk cache, size limits and a placeholder via `TextureHandle`. `render` shaders: `Shaders.create` / `ShaderProgram` with typed uniforms, rebuilt after resource reloads. Logic is unit tested; GPU upload not yet seen on screen.
- `render` world: `ClientEvents.WORLD_RENDER` (with `WorldRenderContext`) on every loader and `WorldDraw` for lines, boxes, filled boxes, quads, textured quads, billboards and 3D text, built on `impl.render.Vtx` that hides the 1.20.1 / 1.21.1 vertex API difference. Compiles on all targets; not yet seen on screen.
- `core` registry: `Registrar` declares items, blocks, block items, entity/block-entity types, sounds, mob effects, attributes and creative tabs (or any registry) once, and registers them through Fabric registries or the Forge/NeoForge `RegisterEvent`. Call `register()` from the mod constructor.
- `core` data: `EntityData`/`DataKey`/`DataSerializer` attach saved data to players and entities by UUID (stored in a world `SavedData`, same on every loader, survives death), with `synced()` keys sent to the owning player. Uses one mechanism for all loaders instead of Capabilities / Data Attachments.
- `core` config screen: `ConfigScreen.create(parent, config)` generates toggles, sliders, cycle buttons and text boxes from a `Config` (paginated, saved on close); `ConfigBuilder.build()` adds its button to the Forge and NeoForge mod lists. Fabric needs Mod Menu (use `ConfigScreen.create` from its API). Compiles on all targets; not yet seen on screen.
- `core` config: `ConfigBuilder`/`Config`/`ConfigValue` with typed options (boolean, int/double ranges, string, enum, string list), TOML file with comments, validation (clamping, defaults, backup of unreadable files), `synced()` options sent server-to-client on join and cleared on disconnect. The auto-generated config screen is still to do.
- `core` network: `Network.registerS2C/registerC2S`, `sendToPlayer/sendToAll/sendToNear/sendToServer`, `PacketType`/`PacketCodec`/`PacketContext`; one API over Fabric networking, Forge channels (single envelope channel) and NeoForge/Fabric 1.21 custom payloads.
- `core` events: `BloombertoEvents` (server tick, level load, player join/leave) and client-only `ClientEvents` (client tick, HUD render) wired on every loader; shared `Schedulers` and `TweenManager` are now ticked automatically. World-render event comes with the render module.
- `animation`: `Easing` (all standard curves), `Tween` (float, Vector3f, Quaternionf, Color), `TweenManager`, `Timeline` (sequence/parallel) and `KeyframeTrack`, with tests.
- `core`/`api`: `Color` (RGB/HSV/hex), `Interpolation`, `Spline`, `Noise` and a tick `Scheduler`, with JUnit 5 tests.
- `testmod` now has an example of every MVP feature (config, registry, data, network, scheduler, animation, world drawing, post effect).
- `testmod`: example mod packaged as a separate jar per target (`bloomberto-testmod-*.jar`).
- Phase 0 skeleton: Stonecutter + Architectury Loom build producing 5 jars (Fabric/Forge 1.20.1, Fabric/Forge/NeoForge 1.21.1), `Bloomberto.id`, `Services.PLATFORM`.
- Repository base: README, license (MIT), `.gitignore`, project brief in `docs/`.
