# Bloomberto — Project brief

> Internal brief describing what to build, how and in which order.

Bloomberto es un **mod librería** (no añade contenido jugable por sí mismo) que otros mods usan como dependencia para simplificar las partes más tediosas del modding: shaders y post-procesado, animaciones, carga de texturas y recursos, UI, red y utilidades comunes.

---

## 1. Versiones y loaders soportados

| Minecraft | Java | Forge | NeoForge | Fabric | Quilt |
|---|---|---|---|---|---|
| **1.20.1** | 17 | ✅ | ❌ (no se publica: en 1.20.1 se usa Forge) | ✅ | ✅ (usa el jar de Fabric) |
| **1.21.1** | 21 | ✅ | ✅ | ✅ | ✅ (usa el jar de Fabric) |

### Por qué estas dos versiones

- **1.20.1** es la versión "clásica" de Forge, con la mayor cantidad de mods y modpacks.
- **1.21.1** es la versión principal de NeoForge y de los modpacks grandes actuales (por ejemplo All the Mods 10).
- Las versiones intermedias (1.20.2–1.20.6, 1.21.2–1.21.11) **no se soportan** al principio: tienen muy poca base de jugadores con mods y cada una cambia APIs internas.
- **No se sube a 26.x por ahora.** A partir de 1.21.2 Mojang reescribió el sistema de shaders (y en 1.21.5 llegó `RenderPipeline`); 26.1 quitó la ofuscación y pide Java 25; 26.2 añade un backend Vulkan. Portar ahí es casi reescribir el módulo de render. Queda como fase futura (ver §7).

### Quilt
Quilt carga mods de Fabric. **No se crea un módulo Quilt aparte**: el jar de Fabric debe funcionar en Quilt. Solo hay que:
- No depender de nada exclusivo de Fabric Loader que Quilt no implemente.
- Probar el jar de Fabric en Quilt Loader en ambas versiones.
- Declarar Quilt como compatible al publicar.

---

## 2. Estructura del proyecto (multiversión + multiloader)

Herramientas recomendadas (Claude Code debe verificar versiones actuales y compatibilidad antes de fijarlas):

- **Architectury Loom** (soporta Forge, NeoForge y Fabric desde un mismo proyecto).
- **Stonecutter** para compilar el mismo código contra 1.20.1 y 1.21.1 con comentarios condicionales (`//? if >=1.21 {`), en vez de mantener dos ramas.
- **Mappings Mojang oficiales + Parchment** (nombres de parámetros) en ambas versiones.
- **Sin dependencia en tiempo de ejecución de Architectury API**: Bloomberto debe ser autocontenido. Se usa solo la herramienta de build.

```
bloomberto/
├── common/        # La mayor parte del código. Solo Minecraft vanilla.
├── fabric/        # Entrypoints y servicios de Fabric (también sirve para Quilt)
├── forge/         # Entrypoints y servicios de Forge (1.20.1 y 1.21.1)
├── neoforge/      # Entrypoints y servicios de NeoForge (solo 1.21.1)
├── versions/      # Configuración de Stonecutter por versión de Minecraft
├── testmod/       # Mod de ejemplo que usa la API en cada loader
└── docs/          # Documentación de uso de la API
```

Se publica **un solo jar por versión y loader** (5 jars en total):
Forge 1.20.1 · Fabric 1.20.1 · Forge 1.21.1 · NeoForge 1.21.1 · Fabric 1.21.1.

Por dentro, el código se organiza en **módulos lógicos** como paquetes separados (`core`, `render`, `animation`, `ui`), con estas reglas:
- `render`, `animation` y `ui` pueden depender de `core`, nunca al revés; `ui` puede usar `animation`.
- Cada módulo se inicializa de forma **perezosa**: si ningún mod lo usa, no registra nada ni activa mixins innecesarios.
- Las fronteras deben ser lo bastante limpias como para poder partirlo en jars separados en el futuro sin romper la API.

La comunicación `common` → loader se hace con interfaces de servicio cargadas vía `ServiceLoader` (patrón `Services.PLATFORM`), no con reflexión.

### Diferencias entre versiones a tener en cuenta (no exhaustivo, verificar)
- **Identificadores:** en 1.21 el constructor de `ResourceLocation` dejó de ser público → usar un helper propio `Bloomberto.id(ns, path)`.
- **Red:** 1.20.1 usa `FriendlyByteBuf` (Fabric `PacketByteBufs`, Forge `SimpleChannel`); 1.21.1 usa `CustomPacketPayload` + `StreamCodec`. La API pública de Bloomberto debe ocultar esta diferencia.
- **Ítems:** 1.20.5+ sustituyó gran parte del NBT de ítems por **Data Components**. Ofrecer helpers que funcionen en ambas.
- **Render:** ambas versiones usan OpenGL vía `RenderSystem` / `ShaderInstance` / `BufferBuilder`, pero la API de `BufferBuilder`/`Tesselator` cambió en 1.21. Encapsularlo.
- **Forge vs NeoForge en 1.21.1:** comparten origen pero no son compatibles entre sí; eventos y registro difieren.

---

## 3. Principios de diseño

1. **Una sola API pública** igual en todas las versiones y loaders. Las diferencias se resuelven dentro de Bloomberto, nunca en el mod que lo usa.
2. **Capa de render propia.** Aunque en 1.20.1 y 1.21.1 se puede usar OpenGL, todo el acceso a GL pasa por una capa interna (`impl.render`). Así, el futuro port a 26.x (Blaze3D/Vulkan) solo toca esa capa.
3. **API pública estable vs. interna.** Paquete `api` = público y documentado. Paquete `impl`/`internal` = puede cambiar. Versionado semántico.
4. **Separación cliente/servidor estricta.** Nada de clases de cliente cargadas en servidor dedicado.
5. **Opt-in y ligero.** Inicialización perezosa; un módulo no usado no cuesta rendimiento.
6. **Mixins mínimos**, pequeños y documentados (`@Inject`/`@WrapOperation` con MixinExtras antes que `@Overwrite`).
7. **Compatibilidad** con Sodium/Embeddium, Iris/Oculus y OptiFine (1.20.1): detectarlos y degradar con elegancia.
8. **Código y Javadoc en inglés**; documentación de usuario en español e inglés.

---

## 4. Módulos y funcionalidades

La librería se distribuye como **un solo jar**, organizado internamente en módulos (paquetes). Todos dependen de `core`.

Prioridad: **[MVP]** primera versión · **[v2]** segunda iteración.

**Criterio de alcance:** solo entra lo que (a) cuesta mucho repetir en cada mod, (b) cambia entre versiones/loaders, o (c) es el objetivo central (render, animación, texturas). Lo que vanilla ya hace fácil, o lo que debe ser un estándar compartido entre mods, queda fuera (ver §8).

### 4.1 `core` (base de todo)
- [MVP] Helpers de registro (bloques, ítems, entidades, block entities, sonidos, pestañas creativas, efectos, atributos) con una API común para todos los loaders.
- [MVP] Abstracción de eventos comunes (tick, carga de mundo, conexión de jugador, render HUD, render mundo).
- [MVP] **Red**: una sola API para registrar y enviar paquetes (C2S, S2C, a todos, a cercanos), traducida a `FriendlyByteBuf` (1.20.1) o `StreamCodec` (1.21.1).
- [MVP] **Datos por jugador/entidad**: guardar y sincronizar datos propios, sobre Capabilities (Forge), Data Attachments (NeoForge) y una implementación propia en Fabric.
- [MVP] **Configuración**: archivo TOML/JSON5 declarativo, validación, sincronización servidor→cliente opcional y **pantalla de config generada automáticamente**.
- [MVP] Utilidades: entorno (`isClient`, `isModLoaded`, `getLoader()`), scheduler por ticks, matemáticas (interpolación, splines, ruido), color (RGB/HSV/hex).
- [v2] Registro sencillo de **teclas** (keybinds) y **overlays de HUD**.
- [v2] **Tooltips avanzados** (colores, iconos, "mantén Shift para más info").

### 4.2 `render` (cliente)
- [MVP] **Texturas dinámicas** desde `NativeImage`, bytes, archivo o URL (asíncrono, caché en disco, placeholder).
- [MVP] Registro simplificado de shaders propios con uniforms tipados.
- [MVP] **Post-procesado** encadenable: blur, bloom, viñeta, LUT/color, desaturación, pixelado; activable y con intensidad animable.
- [MVP] Render en mundo: cajas, líneas, quads con textura, billboards y texto 3D.
- [v2] Render types personalizados (emisivo/fullbright, aditivo, glow) y framebuffers propios.
- [v2] **Efectos visuales**: builder de partículas, temblor de pantalla y trails (estelas).
- [v2] Hot-reload de shaders y texturas en desarrollo.
- Compatibilidad: detectar Iris/Oculus/OptiFine y desactivar efectos que choquen.

### 4.3 `animation`
- [MVP] **Easing** (todas las curvas estándar) y **tweens** para `float`, `Vector3f`, color y `Quaternionf`.
- [MVP] **Timelines** con keyframes, secuencias y paralelos.
- [v2] **Modelos y animaciones de Blockbench** para entidades y block entities (estilo GeckoLib): carga de geometría + animaciones JSON, controladores por estado y transiciones.
- [v2] Animaciones de cámara (zoom suave, FOV) y de GUI (fade, slide, scale).

### 4.4 `ui` (cliente, v2)
- [v2] Widgets básicos (botón, slider, toggle, campo de texto, lista con scroll).
- [v2] Layout tipo flex, **9-slice** y scissor.
- Objetivo: lo suficiente para construir la pantalla de config y pantallas de mods normales, no un framework completo.

### 4.5 Herramientas de desarrollo (dentro de `core`, solo en desarrollo)
- [MVP] `testmod` con un ejemplo de cada módulo, en cada loader y versión.
- [v2] **Datagen simplificado**: modelos, blockstates, recetas, loot tables, tags e idiomas desde código, con la misma API en todos los loaders.
- [v2] Overlay de debug y comando `/bloomberto reload`.

---

## 5. API deseada (ejemplos orientativos)

Debe ser idéntica en 1.20.1 y 1.21.1, y en todos los loaders:

```java
// Tween
Tween.of(0f, 1f).duration(20).ease(Easing.OUT_CUBIC)
     .onUpdate(v -> alpha = v).onComplete(this::hide).start();

// Post-procesado
PostEffects.register(Bloomberto.id("mymod", "vignette"), VignetteEffect::new);
PostEffects.get(Bloomberto.id("mymod", "vignette")).setIntensity(0.6f).enable();

// Textura dinámica desde URL
DynamicTexture tex = Textures.fromUrl(Bloomberto.id("mymod", "avatar"), "https://...")
     .placeholder(Bloomberto.id("mymod", "textures/gui/loading.png"))
     .cache(true).load();

// Red (oculta FriendlyByteBuf vs StreamCodec)
Network.registerS2C(MyPacket.ID, MyPacket.CODEC, (packet, ctx) -> { ... });
Network.sendToPlayer(player, new MyPacket(...));

// Animación de entidad desde Blockbench
AnimationLibrary.load(Bloomberto.id("mymod", "animations/golem.animation.json"));
controller.play("walk").transition(5);
```

---

## 6. Calidad, pruebas y publicación

- **Tests unitarios (JUnit 5)** para lógica pura (easing, tweens, timelines, matemáticas, color, codecs, config).
- **GameTests** donde sea posible.
- Arrancar el `testmod` en **los 5 targets** (y el jar de Fabric en Quilt) antes de cerrar cada fase. Probar también en servidor dedicado.
- **GitHub Actions**: build de la matriz completa + tests en cada push/PR; jars como artefactos.
- Publicación (más adelante) en Modrinth y CurseForge desde CI, marcando Quilt como compatible en los jars de Fabric.
- Licencia: proponer **MIT** o **LGPL-3.0** (preguntar al autor).
- `CHANGELOG.md` y versionado `MAJOR.MINOR.PATCH+<mc>-<loader>` (ej. `1.0.0+1.21.1-neoforge`).

---

## 7. Hoja de ruta

1. **Fase 0 — Esqueleto:** proyecto con Stonecutter + Architectury Loom que compila los 5 jars y arranca un `testmod` vacío en cada uno. CI funcionando.
2. **Fase 1 — MVP:** todo lo marcado [MVP] en `core`, `render` y `animation`. Primera publicación.
3. **Fase 2:** modelos Blockbench, efectos visuales, módulo `ui`, keybinds/HUD, tooltips, datagen, hot-reload y debug.
4. **Solo si hace falta después:** cualquier sistema nuevo pasa primero por el criterio de alcance de §4.
5. **Fase futura (opcional):** port a Minecraft 26.x (Java 25, sin ofuscación, `RenderPipeline`/Blaze3D compatible con Vulkan). Gracias a la capa de render propia, solo debería afectar a `impl.render` y a los servicios de plataforma.

Trabajar fase por fase: no empezar la siguiente hasta que la anterior compile en toda la matriz, pase los tests y tenga ejemplo en `testmod`.

---

## 8. Fuera de alcance

- Contenido jugable propio (salvo en `testmod`).
- Reemplazar el motor de render (terreno de Sodium/VulkanMod).
- **Huecos de accesorios y energía/fluidos propios.** Deben ser estándares compartidos: si Bloomberto crea los suyos, los mods que lo usen no serán compatibles con el resto. Usar Curios/Trinkets y la API de transferencia de cada loader (Fabric Transfer API, Capabilities de Forge/NeoForge).
- **Integración con JEI/REI/EMI.** Requiere sus APIs de todos modos; cada mod la hace directamente.
- **Generación de mundo, IA de entidades, recetas/máquinas, armaduras/herramientas, multibloques, sonido, libro guía y comandos.** Vanilla ya los resuelve bien (datapacks, Brigadier, builders propios) o son específicos de cada mod. Se pueden reconsiderar más adelante si se repiten en varios mods tuyos.
- Versiones anteriores a 1.20.1 y versiones intermedias entre 1.20.1 y 1.21.1.
- NeoForge para 1.20.1.

---

## 9. Instrucciones directas para Claude Code

1. Lee este README completo antes de escribir código.
2. Empieza por la **Fase 0**. Antes de fijar nada, consulta la documentación oficial actual (Fabric, Forge, NeoForge, Quilt, Architectury Loom, Stonecutter, Parchment) y confirma conmigo: versiones exactas de cada herramienta, nombre/mod id y licencia.
3. **No inventes versiones ni APIs.** Verifica cada clase/método de Minecraft o del loader contra el código fuente (`genSources`) o la documentación de esa versión concreta.
4. Si una funcionalidad no es viable en algún target (por ejemplo, algo que no existe en 1.20.1), avísame y propón una alternativa o un fallback, en vez de romper la API común.
5. Commits pequeños y descriptivos por funcionalidad.
6. Cada módulo nuevo incluye: Javadoc en la API pública, tests si es lógica pura y ejemplo en `testmod`.
