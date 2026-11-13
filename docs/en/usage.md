# Using Bloomberto

Bloomberto is a library mod: other mods depend on it. The API is the same on Fabric (and Quilt), Forge and NeoForge, for
Minecraft 1.20.1 and 1.21.1. Public classes live in `com.dougg865.bloomberto.api`; anything in `impl` can change.

> Status: version 0.1.0, not published yet. Items marked *(client)* only exist on the physical client: never touch them from
> code that runs on a dedicated server. Rendering features compile on every target but have not been seen on screen yet.

## Add it to your mod

Until it is published, build Bloomberto (`./gradlew build`) and use the jar for your loader and version from
`versions/<mc>-<loader>/build/libs/`. In your own Gradle project add it as a local dependency
(`modImplementation files("libs/bloomberto-0.1.0+1.21.1-fabric.jar")` on Fabric/Loom) and declare the dependency in your mod
metadata (`"depends": {"bloomberto": "*"}` for Fabric, a `[[dependencies.yourmod]]` block for Forge/NeoForge).

Use `Bloomberto.id("namespace", "path")` instead of `new ResourceLocation(...)`: the constructor is private in 1.21.

## Events

```java
BloombertoEvents.SERVER_TICK.register(server -> { /* end of every server tick */ });
BloombertoEvents.LEVEL_LOAD.register(level -> { });
BloombertoEvents.PLAYER_JOIN.register(player -> { });
BloombertoEvents.PLAYER_LEAVE.register(player -> { });

ClientEvents.CLIENT_TICK.register(() -> { });                  // (client)
ClientEvents.HUD_RENDER.register((graphics, partialTick) -> { }); // (client)
ClientEvents.WORLD_RENDER.register(ctx -> { });                // (client) after the world, before hand and HUD
ClientEvents.DISCONNECT.register(() -> { });                   // (client)
```

## Scheduler and animation

```java
Schedulers.server().runLater(40, () -> ...);            // after 2 seconds, on the server thread
Schedulers.client().runRepeating(0, 20, () -> ...);     // every second, on the client thread

Tween.of(0f, 1f).duration(20).ease(Easing.OUT_CUBIC)
     .onUpdate(v -> alpha = v).onComplete(this::hide).start();   // float, Vector3f, Quaternionf and Color
Timeline.sequence(tweenA, Timeline.pause(10), tweenB).start();   // or Timeline.parallel(...)
KeyframeTrack.create().at(0, 0f).at(10, 1f, Easing.OUT_BACK).at(30, 0f).onUpdate(v -> scale = v).start();
```

Tweens started with `start()` are ticked by Bloomberto on the client. On the server give them your own
`TweenManager` (`tween.start(manager)`) and call `manager.tick()` from `SERVER_TICK`.

## Networking

```java
record Ping(String text) {
    static final PacketType<Ping> TYPE = PacketType.of(Bloomberto.id("mymod", "ping"), Ping.class,
        PacketCodec.of((p, buf) -> buf.writeUtf(p.text()), buf -> new Ping(buf.readUtf())));
}

// in your mod initializer, on both sides, while the mod loads:
Network.registerS2C(Ping.TYPE, (packet, ctx) -> { /* client game thread */ });
Network.registerC2S(Pong.TYPE, (packet, ctx) -> { /* server game thread; ctx.player() is the sender */ });

Network.sendToPlayer(player, new Ping("hi"));
Network.sendToAll(server, new Ping("hi"));
Network.sendToNear(level, pos, 32, new Ping("hi"));
Network.sendToServer(new Pong("hi"));      // (client)
```

## Config

```java
ConfigBuilder b = ConfigBuilder.create("mymod");
b.push("general");
ConfigValue<Boolean> enabled = b.defineBoolean("enabled", true, "Turns the feature on");
ConfigValue<Integer> radius = b.defineInt("radius", 8, 1, 64, "Search radius").synced(); // server value wins on clients
b.pop();
Config config = b.build();                 // loads or creates config/mymod.toml, adds a "config" button on Forge/NeoForge

if (enabled.get()) { ... radius.get() ... }
radius.set(10); config.save();
```

Types: boolean, int and double with ranges, string, enum, list of strings (or your own `ValueType`). Invalid or out-of-range
values are corrected and the file is rewritten; an unreadable file is kept as `.bak`. `ConfigScreen.create(parent, config)`
*(client)* opens a generated settings screen. On Fabric, show it through Mod Menu.

## Data on players and entities

```java
public static final DataKey<Integer> MANA = DataKey.ofInt(Bloomberto.id("mymod", "mana"), 0).synced();

EntityData.set(player, MANA, 5);           // server only
int mana = EntityData.get(player, MANA);   // server, and client for synced keys of the local player
```

Data is saved with the world and tied to the entity's UUID, so players keep it across death and dimension changes.
Call `EntityData.clear(entity)` when an entity is gone for good. Only players are synced to clients.

## Registering content

```java
private static final Registrar REG = Registrar.of("mymod");
public static final Registered<Block> RUBY_BLOCK = REG.block("ruby_block", () -> new Block(BlockBehaviour.Properties.of()));
public static final Registered<Item> RUBY_BLOCK_ITEM = REG.blockItem("ruby_block", RUBY_BLOCK);
public static final Registered<Item> RUBY = REG.item("ruby", () -> new Item(new Item.Properties()));
// also: entityType, blockEntityType, sound, mobEffect, attribute, creativeTab, register(registryKey, name, factory)

REG.register();   // once, from your mod constructor / initializer
```

`RUBY.get()` works after registration. Entity attributes, renderers and models are still registered with each loader's
own events.

## Rendering *(client)*

**Dynamic textures**
```java
TextureHandle avatar = Textures.fromUrl(Bloomberto.id("mymod", "avatar"), "https://example.com/a.png")
        .placeholder(Bloomberto.id("mymod", "textures/gui/loading.png")).cache(true).load();
// draw with avatar.location(); it is the placeholder until the image is ready. avatar.close() frees it.
```
Also `fromFile`, `fromBytes` and `fromImage(NativeImage)`. PNG, JPEG, GIF and BMP; downloads are size-limited and cached
in `bloomberto/cache/textures`.

**Shaders with typed uniforms**
```java
static final ShaderProgram GLOW = Shaders.create(Bloomberto.id("mymod", "glow"), DefaultVertexFormat.POSITION_COLOR);
GLOW.setFloat("Intensity", 0.5f).setColor("Tint", Color.fromHex("#ff8800"));
if (GLOW.use()) { /* draw */ }
```
Files go in `assets/<ns>/shaders/core/glow.json` plus `.vsh` and `.fsh`.

**Post-processing**
```java
PostEffects.get(Bloomberto.id("vignette")).setIntensity(0.6f).enable();
PostEffects.get(Bloomberto.id("blur")).fadeIn(1f, 20, Easing.OUT_CUBIC);
PostEffects.register(Bloomberto.id("mymod", "bloom"), () -> new BuiltInEffects.Bloom().strength(2f)).enable();
```
Built in: `blur`, `bloom`, `vignette`, `desaturate`, `pixelate`, `color_grade`. Effects run in registration order and are
skipped while Iris, Oculus or OptiFine render a shader pack. Write your own by extending `PostEffect` and describing the
passes in `define(ChainSpec)`.

**Drawing in the world**
```java
ClientEvents.WORLD_RENDER.register(ctx -> WorldDraw.of(ctx)
    .box(new AABB(pos), Color.fromHex("#ff0000"))
    .line(a, b, Color.WHITE)
    .text(Component.literal("Here"), pos.getCenter().add(0, 1, 0), 0.025f, Color.WHITE)
    .throughWalls(true).billboard(texture, pos.getCenter(), 1f, Color.WHITE));
```

## Example

`src/testmod` is a complete example mod using each feature above; install `bloomberto-testmod-*.jar` next to Bloomberto.
