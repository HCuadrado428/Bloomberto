# Changelog

All notable changes to this project are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/); versions follow
`MAJOR.MINOR.PATCH+<mc>-<loader>` (e.g. `1.0.0+1.21.1-neoforge`).

## [Unreleased]

### Added
- `animation`: `Easing` (all standard curves), `Tween` (float, Vector3f, Quaternionf, Color), `TweenManager`, `Timeline` (sequence/parallel) and `KeyframeTrack`, with tests.
- `core`/`api`: `Color` (RGB/HSV/hex), `Interpolation`, `Spline`, `Noise` and a tick `Scheduler`, with JUnit 5 tests.
- `testmod`: example mod packaged as a separate jar per target (`bloomberto-testmod-*.jar`).
- Phase 0 skeleton: Stonecutter + Architectury Loom build producing 5 jars (Fabric/Forge 1.20.1, Fabric/Forge/NeoForge 1.21.1), `Bloomberto.id`, `Services.PLATFORM`.
- Repository base: README, license (MIT), `.gitignore`, project brief in `docs/`.
