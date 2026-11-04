# Realistic Survival

Fabric mod for Minecraft Java 1.21.11. The alpha focuses on a server-authoritative survival simulation with persistent player vitals.

## Development target
- Minecraft Java 1.21.11
- Java 21
- Fabric Loader 0.18.x
- Fabric API 0.141.x
- Fabric Loom 1.14.x

## Current alpha: 0.2.0-alpha.16

Implemented in the current prototype:
- Persistent thirst, stamina, fatigue, sanity and body temperature.
- Drinking from water blocks while crouching with an empty hand.
- Sprinting, hunger, dehydration, temperature and fatigue affect stamina and movement.
- Heat and cold exposure affect body temperature; heat increases thirst and severe cold increases fatigue.
- Sleeping and quiet crouching rest restore survival values.
- Darkness and severe fatigue wear down sanity; safe light and rest support recovery.
- Action-bar status HUD with configurable compact and disabled modes.
- Server-side state persistence through player entity tags.

## Configuration

The file `config/realistic-survival.properties` is created on first launch. Restart the world/server after changing settings.

- `hud.enabled=true` — show or hide the survival HUD.
- `hud.compact=false` — show only thirst, stamina and fatigue when enabled.
- `survival.cold_fatigue=true` — let severe cold increase fatigue.
- `survival.hot_thirst=true` — enable additional thirst loss in hot biomes.

The `world.fire.*` values are currently reserved defaults; a configurable fire simulation is not implemented yet.

## Alpha completion scope

The project is still an early alpha, not a finished overhaul. Remaining major work includes a proper graphical HUD, configurable medical/injury gameplay, usable inventory/medical items, broader world simulation, multiplayer testing and balance. A successful compile verifies the build, not in-game behavior; release readiness requires a real Minecraft test pass.

## Build

Use Java 21 and the Gradle version configured by the CI workflow:

```bash
gradle build
```

The remapped mod JAR is placed in `build/libs/`.
