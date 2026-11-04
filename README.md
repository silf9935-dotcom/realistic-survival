 # Realistic Survival

Fabric mod for Minecraft Java 1.21.11 focused on deep survival simulation, world persistence, believable NPCs, exploration and psychological horror.

## Development target
- Minecraft 1.21.11
- Java 21
- Fabric Loader 0.18.x
- Fabric API 0.141.x
- Fabric Loom 1.14.x

## Alpha.15

The survival loop includes persistent thirst, stamina, fatigue, sanity and body temperature. Temperature responds to environmental exposure and sprinting. Heat stress increases thirst, while temperature extremes and dehydration reduce physical endurance. Crouching while nearly motionless allows the player to rest, gradually reducing fatigue and restoring stamina and sanity. Darkness wears down sanity while well-lit areas support recovery. Hot biomes increase thirst, and critically low stamina slows movement. These systems are server-authoritative and shown in the action bar.

## Current prototype
This is an early development foundation. It loads a configuration file with the agreed fire-system defaults and contains a first server-side survival-vitals model. It is **not** yet a feature-complete gameplay mod. The HUD, medical interface, inventory extensions and gameplay simulation are planned implementation milestones.

## Build
Use Java 21 and Gradle 8.14.3 or compatible:

```bash
gradle build
```

The remapped mod JAR will be placed in `build/libs/`.
