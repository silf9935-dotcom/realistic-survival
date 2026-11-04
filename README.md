# Realistic Survival

A Fabric survival-simulation mod for Minecraft Java 1.21.11.

## Beta candidate: 0.3.0-beta.1

This first beta candidate consolidates the alpha survival loop and introduces a basic injury/treatment system.

### Implemented systems

- Persistent thirst, stamina, fatigue, sanity and body temperature.
- Drink from water sources by crouching with an empty hand; untreated water has a configurable 15% contamination risk that can cause brief nausea.
- Sprinting, hunger, dehydration, fatigue and temperature affect stamina and movement.
- Hot and cold environments affect body temperature; heat increases thirst and severe cold increases fatigue.
- Sleeping and quiet crouching restore selected survival values.
- Darkness and extreme fatigue reduce sanity; safe light and rest support recovery.
- Survival status action-bar HUD, with compact and disabled modes; active injury severity is also displayed.
- Hard hits can cause persistent injuries with escalating weakness/slowness penalties.
- Paper can be used as an improvised bandage to reduce injury severity.
- Player survival and injury state are stored on player entity tags and persist through relogs/world saves.

### Configuration

The file `config/realistic-survival.properties` is created on first launch. Restart the world/server after changing settings.

- `hud.enabled=true` — show or hide the action-bar HUD.
- `hud.compact=false` — show only thirst, stamina and fatigue.
- `survival.cold_fatigue=true` — severe cold increases fatigue.
- `survival.hot_thirst=true` — hot biomes add thirst loss.
- `survival.unsafe_water=true` — untreated natural water can cause brief nausea.
- `survival.unsafe_water_chance=15` — contamination chance from 0 to 100 percent.
- `injuries.enabled=true` — enable injury penalties and paper bandages.
- `injuries.damage_threshold=4.0` — minimum damage from one hit required to cause an injury.
- `injuries.severe_hit_threshold=8.0` — damage from one hit required to add two injury severity levels instead of one. Both thresholds are clamped to safe ranges; the severe threshold cannot be lower than the normal threshold.

The existing `world.fire.*` values are reserved configuration defaults; a full fire simulation is not implemented.

### Known limitations before a full release

- The HUD is action-bar based, not a dedicated graphical overlay.
- Paper is a temporary improvised treatment, not a dedicated craftable medical item.
- No complete fracture, bleeding, infection, disease or medical-inventory simulation yet.
- No NPC ecology or broad world-event simulation yet.
- Gameplay balance and multiplayer behavior still need hands-on testing in Minecraft 1.21.11.

A successful CI build proves compilation and packaging only. It does not replace in-game testing.

## Development

- Minecraft Java 1.21.11
- Java 21
- Fabric Loader 0.18.x
- Fabric API 0.141.x
- Fabric Loom 1.14.x

Build with the Gradle version configured in `.github/workflows/build.yml`:

```bash
gradle build
```

The remapped mod JAR is placed in `build/libs/`.
