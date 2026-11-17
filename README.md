# Bloomberto

**Bloomberto** is a utility library mod for Minecraft. It adds no playable content: other mods
use it as a dependency to skip the most tedious parts of modding — shaders and post-processing,
animation, texture loading, UI, networking and common helpers — with **one public API that is
identical across versions and loaders**.

**Bloomberto** es un mod librería para Minecraft. No añade contenido jugable: otros mods lo usan
como dependencia para simplificar shaders y post-procesado, animación, texturas, UI, red y
utilidades comunes, con **una única API pública idéntica en todas las versiones y loaders**.

> Status / Estado: **beta.** Every target passes the in-game self-test (13 server + 15 client checks) and rendering has been checked on screen, also with Iris shader packs. / **Beta.** Todos los targets pasan el self-test dentro del juego (13 comprobaciones de servidor + 15 de cliente) y el render se ha comprobado en pantalla, también con shader packs de Iris.

## Supported targets

| Minecraft | Java | Forge | NeoForge | Fabric | Quilt |
|---|---|---|---|---|---|
| 1.20.1 | 17 | yes | no | yes | via Fabric jar (verified on 1.20.1) |
| 1.21.1 | 21 | yes | yes | yes | via Fabric jar |
| 1.21.11 | 21 | yes | yes | yes | via Fabric jar |

One jar per version and loader (8 in total).

## Modules

- `core` — registration, events, networking, per-entity data, config, utilities.
- `render` — dynamic textures, custom shaders, chainable post-processing, in-world rendering, camera shake and FOV zoom; everything also works with Iris shader packs.
- `animation` — easing, tweens, timelines, Blockbench models and animations (`.geo.json`, `.animation.json` or `.bbmodel`) with Molang expressions.
- `ui` — basic widgets and layout (v2).

## Documentation

- [Using Bloomberto (English)](docs/en/usage.md) · [Usar Bloomberto (español)](docs/es/uso.md)
- [Building and testing](docs/BUILD.md)
- [Project brief / roadmap](docs/PROJECT_BRIEF.md)
- [Changelog](CHANGELOG.md)

## AI disclosure

AI disclosure: most of this library's code, tests, documentation and default assets (UI textures, example models and shaders) were produced with Claude (Anthropic's AI assistant) from my project brief. I directed the design, tested it in real clients (Fabric with Iris and Sodium), reported the bugs I found, and reviewed the results. Every feature is covered by unit tests, CI builds and in-game checks listed in docs/BUILD.md.

## License

[MIT](LICENSE) © DougG865
