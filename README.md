# Realistic Survival

A Fabric survival-simulation mod for Minecraft Java 1.21.11.

## Development build: 0.3.0-beta.2 candidate

This development candidate extends the survival and injury loop with the first recruitable survivor companion prototype.

### Implemented systems

- Persistent thirst, stamina, fatigue, sanity and body temperature.
- Drink from water sources by crouching with an empty hand; untreated water has a configurable contamination risk.
- Sprinting, hunger, dehydration, fatigue and temperature affect stamina and movement.
- Hot and cold environments affect body temperature; heat increases thirst and severe cold increases fatigue.
- Sleeping and quiet crouching restore selected survival values.
- Darkness and extreme fatigue reduce sanity; safe light and rest support recovery.
- Survival status action-bar HUD, with compact and disabled modes; active injury severity is also displayed.
- Hard hits can cause persistent injuries with escalating weakness/slowness penalties.
- Craftable bandages (paper + string) reduce injury severity; paper remains supported as an improvised fallback.
- Sneak-interact with a villager while holding 5 emeralds to recruit them as a survivor companion.
- Recruited survivors remember their owner through world saves, follow their owner, and can be told to stay or follow again by sneak-interacting.
- Player survival and injury state are stored on player entity tags and persist through relogs/world saves; recruited survivor ownership is stored on the villager.

### Configuration

The file `config/realistic-survival.properties` is created on first launch. Restart the world/server after changing settings.

- `hud.enabled=true` — show or hide the action-bar HUD.
- `hud.compact=false` — show only thirst, stamina and fatigue.
- `survival.cold_fatigue=true` — severe cold increases fatigue.
- `survival.hot_thirst=true` — hot biomes add thirst loss.
- `survival.unsafe_water=true` — untreated natural water can cause brief nausea.
- `survival.unsafe_water_chance=15` — contamination chance from 0 to 100 percent.
- `injuries.enabled=true` — enable injury penalties and treatment.
- `injuries.damage_threshold=4.0` — minimum damage from one hit required to cause an injury.
- `injuries.severe_hit_threshold=8.0` — damage from one hit required to add two injury severity levels instead of one.
- `injuries.treatment_cooldown_seconds=10` — delay between bandage uses (0–300 seconds).
- `npcs.survivors_enabled=true` — enable or disable the survivor companion prototype.

### NPC roadmap and known limitations

- The current NPC feature is a first prototype built on vanilla villagers, not a new custom NPC entity.
- Companions currently follow or stay; advanced combat commands, jobs, needs, schedules, dialogue, trading, quests, reputation, factions and settlements are not implemented yet.
- The HUD is action-bar based, not a dedicated graphical overlay.
- The current bandage uses a placeholder vanilla paper texture; advanced medicine, bleeding, fractures and infection are not implemented.
- Gameplay balance, multiplayer behavior and NPC pathfinding still need hands-on testing in Minecraft 1.21.11.

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
