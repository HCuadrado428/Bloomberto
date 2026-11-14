# Usar Bloomberto

Bloomberto es un mod librería: otros mods dependen de él. La API es la misma en Fabric (y Quilt), Forge y NeoForge, para
Minecraft 1.20.1 y 1.21.1. Las clases públicas están en `com.dougg865.bloomberto.api`; lo que hay en `impl` puede cambiar.

> Estado: versión 0.1.0, todavía sin publicar. Lo marcado *(cliente)* solo existe en el cliente físico: no lo toques desde
> código que corra en un servidor dedicado. Las funciones de render compilan en todos los targets, pero aún no se han visto
> en pantalla.

## Añadirlo a tu mod

Mientras no esté publicado, compila Bloomberto (`./gradlew build`) y usa el jar de tu loader y versión de
`versions/<mc>-<loader>/build/libs/`. En tu proyecto Gradle añádelo como dependencia local
(`modImplementation files("libs/bloomberto-0.1.0+1.21.1-fabric.jar")` en Fabric/Loom) y declara la dependencia en los
metadatos de tu mod (`"depends": {"bloomberto": "*"}` en Fabric, un bloque `[[dependencies.tumod]]` en Forge/NeoForge).

Usa `Bloomberto.id("namespace", "ruta")` en vez de `new ResourceLocation(...)`: en 1.21 el constructor es privado.

## Eventos

```java
BloombertoEvents.SERVER_TICK.register(server -> { /* final de cada tick del servidor */ });
BloombertoEvents.LEVEL_LOAD.register(level -> { });
BloombertoEvents.PLAYER_JOIN.register(player -> { });
BloombertoEvents.PLAYER_LEAVE.register(player -> { });

ClientEvents.CLIENT_TICK.register(() -> { });                  // (cliente)
ClientEvents.HUD_RENDER.register((graphics, partialTick) -> { }); // (cliente)
ClientEvents.WORLD_RENDER.register(ctx -> { });                // (cliente) tras el mundo, antes de la mano y el HUD
ClientEvents.DISCONNECT.register(() -> { });                   // (cliente)
```

## Scheduler y animación

```java
Schedulers.server().runLater(40, () -> ...);            // a los 2 segundos, en el hilo del servidor
Schedulers.client().runRepeating(0, 20, () -> ...);     // cada segundo, en el hilo del cliente

Tween.of(0f, 1f).duration(20).ease(Easing.OUT_CUBIC)
     .onUpdate(v -> alpha = v).onComplete(this::hide).start();   // float, Vector3f, Quaternionf y Color
Timeline.sequence(tweenA, Timeline.pause(10), tweenB).start();   // o Timeline.parallel(...)
KeyframeTrack.create().at(0, 0f).at(10, 1f, Easing.OUT_BACK).at(30, 0f).onUpdate(v -> escala = v).start();
```

Los tweens iniciados con `start()` los avanza Bloomberto en el cliente. En el servidor dales tu propio `TweenManager`
(`tween.start(manager)`) y llama a `manager.tick()` desde `SERVER_TICK`.

## Red

```java
record Ping(String text) {
    static final PacketType<Ping> TYPE = PacketType.of(Bloomberto.id("mymod", "ping"), Ping.class,
        PacketCodec.of((p, buf) -> buf.writeUtf(p.text()), buf -> new Ping(buf.readUtf())));
}

// en el inicializador de tu mod, en ambos lados, mientras el mod carga:
Network.registerS2C(Ping.TYPE, (packet, ctx) -> { /* hilo del juego del cliente */ });
Network.registerC2S(Pong.TYPE, (packet, ctx) -> { /* hilo del servidor; ctx.player() es quien lo envió */ });

Network.sendToPlayer(player, new Ping("hola"));
Network.sendToAll(server, new Ping("hola"));
Network.sendToNear(level, pos, 32, new Ping("hola"));
Network.sendToServer(new Pong("hola"));      // (cliente)
```

## Configuración

```java
ConfigBuilder b = ConfigBuilder.create("mymod");
b.push("general");
ConfigValue<Boolean> enabled = b.defineBoolean("enabled", true, "Activa la función");
ConfigValue<Integer> radius = b.defineInt("radius", 8, 1, 64, "Radio de búsqueda").synced(); // en clientes gana el valor del servidor
b.pop();
Config config = b.build();                 // carga o crea config/mymod.toml y añade un botón "config" en Forge/NeoForge

if (enabled.get()) { ... radius.get() ... }
radius.set(10); config.save();
```

Tipos: booleano, entero y decimal con rango, texto, enum, lista de textos (o tu propio `ValueType`). Los valores inválidos o
fuera de rango se corrigen y el archivo se reescribe; un archivo ilegible se guarda como `.bak`.
`ConfigScreen.create(parent, config)` *(cliente)* abre una pantalla de ajustes generada. En Fabric, muéstrala con Mod Menu.

## Datos en jugadores y entidades

```java
public static final DataKey<Integer> MANA = DataKey.ofInt(Bloomberto.id("mymod", "mana"), 0).synced();

EntityData.set(player, MANA, 5);           // solo servidor
int mana = EntityData.get(player, MANA);   // servidor, y cliente para claves synced del jugador local
```

Los datos se guardan con el mundo y van ligados al UUID de la entidad, así que el jugador los conserva al morir y al cambiar
de dimensión. Llama a `EntityData.clear(entity)` cuando una entidad ya no vuelva. Solo se sincronizan los jugadores.

## Registrar contenido

```java
private static final Registrar REG = Registrar.of("mymod");
public static final Registered<Block> RUBY_BLOCK = REG.block("ruby_block", () -> new Block(BlockBehaviour.Properties.of()));
public static final Registered<Item> RUBY_BLOCK_ITEM = REG.blockItem("ruby_block", RUBY_BLOCK);
public static final Registered<Item> RUBY = REG.item("ruby", () -> new Item(new Item.Properties()));
// también: entityType, blockEntityType, sound, mobEffect, attribute, creativeTab, register(registryKey, nombre, factory)

REG.register();   // una vez, desde el constructor / inicializador de tu mod
```

`RUBY.get()` funciona tras el registro. Los atributos de entidades, renderers y modelos se siguen registrando con los
eventos propios de cada loader.

## Render *(cliente)*

**Texturas dinámicas**
```java
TextureHandle avatar = Textures.fromUrl(Bloomberto.id("mymod", "avatar"), "https://example.com/a.png")
        .placeholder(Bloomberto.id("mymod", "textures/gui/loading.png")).cache(true).load();
// dibuja con avatar.location(); es el placeholder hasta que la imagen esté lista. avatar.close() la libera.
```
También `fromFile`, `fromBytes` y `fromImage(NativeImage)`. PNG, JPEG, GIF y BMP; las descargas tienen límite de tamaño y se
guardan en `bloomberto/cache/textures`.

**Shaders con uniforms tipados**
```java
static final ShaderProgram GLOW = Shaders.create(Bloomberto.id("mymod", "glow"), DefaultVertexFormat.POSITION_COLOR);
GLOW.setFloat("Intensity", 0.5f).setColor("Tint", Color.fromHex("#ff8800"));
if (GLOW.use()) { /* dibujar */ }
```
Los archivos van en `assets/minecraft/shaders/core/mymod_glow.json` (vanilla solo lo busca ahí) más los `.vsh` y `.fsh` que nombra.

**Post-procesado**
```java
PostEffects.get(Bloomberto.id("vignette")).setIntensity(0.6f).enable();
PostEffects.get(Bloomberto.id("blur")).fadeIn(1f, 20, Easing.OUT_CUBIC);
PostEffects.register(Bloomberto.id("mymod", "bloom"), () -> new BuiltInEffects.Bloom().strength(2f)).enable();
```
Incluidos: `blur`, `bloom`, `vignette`, `desaturate`, `pixelate`, `color_grade`. Los efectos se ejecutan en orden de registro y se
omiten mientras Iris, Oculus u OptiFine rendericen un shader pack. Para uno propio, extiende `PostEffect` y describe los
pases en `define(ChainSpec)`.

**Dibujar en el mundo**
```java
ClientEvents.WORLD_RENDER.register(ctx -> WorldDraw.of(ctx)
    .box(new AABB(pos), Color.fromHex("#ff0000"))
    .line(a, b, Color.WHITE)
    .text(Component.literal("Aquí"), pos.getCenter().add(0, 1, 0), 0.025f, Color.WHITE)
    .throughWalls(true).billboard(textura, pos.getCenter(), 1f, Color.WHITE));
```

## Más funciones de cliente *(cliente)*

```java
Keybinds.register("mymod", "open_menu", GLFW.GLFW_KEY_M, "general", () -> abrirMenu());   // llámalo mientras el mod carga
HudOverlays.register(Bloomberto.id("mymod", "mana"), (graphics, partialTick) -> graphics.drawString(font, "Maná", 4, 4, 0xFFFFFF));
Tooltips.shiftInfo(stack -> stack.is(MyItems.RUBY.get()), List.of(Component.literal("Se encuentra bajo tierra.")));
```

**Efectos visuales**
```java
((BuiltInEffects.Shake) PostEffects.get(Bloomberto.id("shake"))).trigger(1f, 20);        // temblor de impacto
PostEffects.get(Bloomberto.id("zoom")).fadeIn(0.5f, 10, Easing.OUT_CUBIC);               // zoom en pantalla
Particles.of(ParticleTypes.FLAME).ring(pos, 1.5, 24).speed(0.05).spawn(level);            // nivel de cliente o de servidor
Trail trail = new Trail(30, 30); trail.add(pos);  /* cada tick */  WorldDraw.of(ctx).trail(trail, 0.4f, cabeza, cola);
RenderTypes.additive(texture); RenderTypes.emissive(texture); RenderTypes.glow(texture);   // render types con brillo
new OffscreenTarget(256, 256);                                                             // framebuffer propio
```
El zoom escala la imagen; no cambia el campo de visión.

**UI**
```java
List<Rect> celdas = FlexLayout.row().gap(6).padding(8).add(FlexLayout.Item.fixed(20, 20)).add(FlexLayout.Item.grow(1, 0, 20)).compute(area);
UiGraphics.nineSlice(graphics, texture, new NineSlice(16, 16, 3), rect);   // las esquinas no se deforman
UiGraphics.scissor(graphics, rect, () -> dibujar());                         // recorte
Transition t = Transition.create().fade().slide(0, 12).scale(0.9f).duration(10); t.show();
UiGraphics.transition(graphics, t, pivotRect, () -> dibujar());
// widgets con skin: ThemedButton, ThemedToggle, ThemedSlider, ScrollList (incluye un tema oscuro por defecto)
```

**Modelos y animaciones de Blockbench (estilo GeckoLib)**
```java
GeoModel model = ModelLibrary.model(Bloomberto.id("mymod", "geo/golem.geo.json"));
AnimationController controller = ModelLibrary.animations(Bloomberto.id("mymod", "animations/golem.animation.json")).controller();
controller.play("animation.golem.walk").transition(5);                      // mezcla durante 5 ticks
controller.play("animation.golem.attack").transition(2).then("animation.golem.idle");
// entidad: implementa Animatable y registra  new AnimatedEntityRenderer<>(ctx, geoId, textureId)
// block entity: implementa Animatable y registra  new AnimatedBlockEntityRenderer<>(geoId, textureId)
```
Lee geometría Bedrock (`.geo.json`, formatos 1.12.0 y 1.8.0) y archivos de animación con keyframes lineales, Catmull-Rom y de
paso, y los tres modos de bucle. Las expresiones Molang aún no están soportadas. Los archivos se recargan con F3+T.

## Herramientas de desarrollo

- `/bloomberto reload` vuelve a leer todos los archivos de configuración; `/bloomberto info` muestra versiones y valores.
- `/bloomberto datagen <modid>` ejecuta tu generador de datos registrado (`Datagen.register("mymod", gen -> ...)`):
  modelos, block states, idiomas, recetas, loot tables y tags, en el formato de la versión de Minecraft en ejecución.
- Un overlay de depuración (F8) muestra tweens, tareas, texturas y efectos activos; está activo en desarrollo, con el testmod instalado o con `-Dbloomberto.debugOverlay=true`.

## Ejemplo

`src/testmod` es un mod de ejemplo completo que usa cada función de arriba; instala `bloomberto-testmod-*.jar` junto a
Bloomberto.
