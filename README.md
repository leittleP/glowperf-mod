# GlowPerf

A rendering and performance mod for **Minecraft Java Edition 1.20.1**, built from a single shared codebase and shipped as separate jars for **Forge**, **NeoForge**, **Fabric** and **Quilt**.

GlowPerf adds item glow with bloom, distance-based entity culling, a spawn guard, live performance diagnostics, a fully interactive minimap with waypoints, and an in-game command manual.

## Downloads

Pick **one** jar that matches your mod loader.

| Loader | File | Requirements |
| --- | --- | --- |
| Forge | `glowperf-2.1.0-mc1.20.1-forge.jar` | Forge 47.2.20+ |
| NeoForge | `glowperf-2.1.0-mc1.20.1-neoforge.jar` | NeoForge 47.1.106+ |
| Fabric | `glowperf-2.1.0-mc1.20.1-fabric.jar` | Fabric Loader 0.16.5+, Fabric API 0.92.2+ |
| Quilt | `glowperf-2.1.0-mc1.20.1-quilt.jar` | Quilt Loader 0.26.0+, QFAPI 7.6.0+ |

All builds target Java 17 and Minecraft 1.20.1 only.

Latest release: <https://github.com/leittleP/glowperf-mod/releases/latest>

## Installation

1. Install the mod loader for Minecraft 1.20.1.
2. Fabric and Quilt users must also install Fabric API / QFAPI.
3. Drop the matching GlowPerf jar into `.minecraft/mods`.
4. Launch the game. GlowPerf has **no key bindings** — every feature is reachable from commands, on-screen buttons, or `Esc`.

## Features

- **Item glow with bloom** — enabled by default; an automatic tier reacts to frame time.
- **Performance presets** — `off`, `mild`, `moderate`, `severe`, `t matter`, `auto`; the panel shows both the selected preset and the tier actually in effect.
- **Entity culling** — distant living entities and players stop rendering beyond a configurable distance.
- **Spawn guard** — caps newly spawned mobs to protect tick time.
- **Idle AI freeze** — distant idle mobs stop ticking AI until a player comes close.
- **Diagnostics** — frame-time and P95 sampling, long-frame snapshots, heap watch, mod conflict scan.
- **Minimap** — drag, zoom, click-to-teleport, teleport by coordinates or online player, dimension / biome / chunk readout, colour-coded markers for dropped items, spawners, hostile and passive mobs, and an automatic cave mode below Y=0.
- **Waypoints** — add from the map with right-click or from commands, persisted across sessions.
- **HUD layout editor** — drag every HUD element, `✕` to cancel, `✓` to save.
- **Starter manual** — a 12-page in-game book is handed out on first join; older copies are removed automatically.

## Commands

| Command | Description |
| --- | --- |
| `/light and shadow open` | Enable glow rendering |
| `/light and shadow off` | Disable glow rendering |
| `/light and shadow auto` | Let frame time drive glow on/off |
| `/light and shadow fps on\|off` | Toggle the FPS overlay |
| `/light and shadow settings` | Open the glow settings screen |
| `/performance optimization off\|mild\|moderate\|severe\|auto` | Set the performance preset |
| `/performance optimization t matter` | Maximum performance preset |
| `/open the map` | Open the full-screen map |
| `/close the map` | Close the map |
| `/minimap on\|off\|toggle` | Toggle the always-on minimap |
| `/wp add [name]` | Create a waypoint at your position |
| `/wp list` | List stored waypoints |
| `/wp tp <name>` | Teleport to a waypoint |
| `/wp del <name>` | Delete a waypoint |
| `/gp scan` | Run a one-shot performance health check |
| `/gp top` | Show the heaviest entities or chunks |
| `/gp slow` | Show recent long frames |
| `/gp posedit` | Open the HUD layout editor |
| `/gp help` | Command reference |
| `/perf` | Show current status |
| `/perf help` | Full command reference |
| `/perf settings` | Open the performance panel |
| `/perf cull\|player\|spawn\|noai\|mem\|probe\|panel on\|off` | Toggle individual optimisations |
| `/perf distance\|cap\|budget\|longFrame <value>` | Tune thresholds |
| `/perf allow\|disallow <type>` | Exempt an entity type from culling |
| `/perf save\|reload\|reset` | Manage the configuration |

## Configuration

- **Forge / NeoForge:** `config/glowperf-client.toml` and `config/glowperf-common.toml`
- **Fabric / Quilt:** `config/glowperf-client.json` and `config/glowperf-common.json`

Every option is also editable in game through `/light and shadow settings` or `/perf settings`.

## Building from source

```bash
git clone https://github.com/leittleP/glowperf-mod.git
cd glowperf-mod
./gradlew build -Ploaders=forge,neoforge,fabric,quilt
```

Requires JDK 17. Build a single loader with `-Ploaders=fabric`, for example.

| Module | Output |
| --- | --- |
| `common` | Shared, loader-independent logic |
| `forge` | `glowperf-2.1.0-mc1.20.1-forge.jar` |
| `neoforge` | `glowperf-2.1.0-mc1.20.1-neoforge.jar` |
| `fabric` | `glowperf-2.1.0-mc1.20.1-fabric.jar` |
| `quilt` | `glowperf-2.1.0-mc1.20.1-quilt.jar` |

## Notes

- Every release jar is verified for ZIP integrity, loader metadata, entry points, Mixin configuration and Java 17 bytecode before publishing.
- In-game text ships with Chinese (`zh_cn`) and English (`en_us`) language files; the client follows its own language setting.
- Not every feature has been play-tested in a live client yet. If something fails to load or render, attach `latest.log` in an issue.

## License

MIT
