# Bloomberto

**Bloomberto** is a utility library mod for Minecraft. It adds no playable content: other mods
use it as a dependency to skip the most tedious parts of modding — shaders and post-processing,
animation, texture loading, UI, networking and common helpers — with **one public API that is
identical across versions and loaders**.

**Bloomberto** es un mod librería para Minecraft. No añade contenido jugable: otros mods lo usan
como dependencia para simplificar shaders y post-procesado, animación, texturas, UI, red y
utilidades comunes, con **una única API pública idéntica en todas las versiones y loaders**.

> Status / Estado: **Phase 0 — skeleton (in progress)**. Nothing is published yet.

## Supported targets

| Minecraft | Java | Forge | NeoForge | Fabric | Quilt |
|---|---|---|---|---|---|
| 1.20.1 | 17 | yes | no | yes | via Fabric jar |
| 1.21.1 | 21 | yes | yes | yes | via Fabric jar |

One jar per version and loader (5 in total).

## Modules

- `core` — registration, events, networking, per-entity data, config, utilities.
- `render` — dynamic textures, custom shaders, chainable post-processing, in-world rendering.
- `animation` — easing, tweens, timelines (Blockbench models in v2).
- `ui` — basic widgets and layout (v2).

## Documentation

- [Project brief / roadmap](docs/PROJECT_BRIEF.md)
- [Changelog](CHANGELOG.md)

## License

[MIT](LICENSE) © DougG865
