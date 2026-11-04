# Realistic Survival

A Fabric survival-simulation mod for Minecraft Java 1.21.11.

## Development build: 0.3.0-beta.2 candidate

### Implemented survival systems
- Persistent thirst, stamina, fatigue, sanity and body temperature.
- Drink from water sources by crouching with an empty hand; untreated water has a configurable contamination risk.
- Sprinting, hunger, dehydration, fatigue and temperature affect stamina and movement.
- Hot and cold environments affect body temperature; heat increases thirst and severe cold increases fatigue.
- Sleeping and quiet crouching restore selected survival values.
- Darkness and extreme fatigue reduce sanity; safe light and rest support recovery.
- Survival status action-bar HUD, with compact and disabled modes.
- Hard hits can cause persistent injuries with escalating weakness/slowness penalties.
- Craftable bandages (paper + string) reduce injury severity; paper remains supported as an improvised fallback.

### NPC design

The NPC scope is intentionally limited to exactly three groups:

- **Bandits:** vanilla pillagers, identified and named as bandits.
- **Military:** vanilla vindicators, identified and named as military personnel.
- **Story characters:** individually marked villagers with persistent dialogue progress. They are not recruitable companions.

Story characters can be created with a vanilla summon command, for example:

```mcfunction
/summon minecraft:villager ~ ~ ~ {Tags:["rs_story_npc"],CustomName:'{"text":"Старый связной"}',CustomNameVisible:1b}
```

Interact with a marked story character to advance through their introductory dialogue. Dialogue progress is saved on that character.

Recruitable survivors, survivor jobs, trust/trading, camp supplies, companion combat and survivor quest chains are no longer registered or part of the NPC design.

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

### NPC roadmap

- Bandits and military currently use vanilla hostile illager entity types; custom models, equipment sets, patrol logic and faction relationships are future work.
- Story characters currently have a small persistent dialogue prototype; branching quests, unique characters and story chapters are future work.
