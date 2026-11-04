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
- Companions track a persistent trust score: give them bread to increase trust.
- At the configured trust threshold, trade emeralds with a companion for a bandage; the barter price is configurable.
- Recruited survivors remember their owner through world saves, follow their owner, and can be told to stay or follow again by sneak-interacting.
- Assign survivor specializations by giving an owned companion an iron sword (guard), iron pickaxe (scavenger), or shears (medic); the role is saved on the villager.
- Assign one of three survivor faction identities with a white, red, or blue banner (North, Ash, Free). These are persistent identities, not yet a full diplomacy/war simulation.
- Give an owned survivor a campfire or soul campfire item to mark a persistent camp anchor at their position. Give a chest or barrel to add 8 stock units; bread adds food units up to a cap of 64. A scavenger assigned with an iron pickaxe can collect nearby dropped food around the camp and add it to the reserve. When the owner is within 8 blocks of the marked camp and has 17 or less hunger, one stock unit is automatically consumed every in-game minute to restore 2 hunger points.
- Guard-role survivors defend a wider area around their owner. Medic-role survivors periodically grant brief regeneration when the owner is nearby and below 65% health.
- Added a persistent three-stage supply quest: 4 iron ingots, 8 rotten flesh, then 8 cooked beef. Rewards are emeralds, bread, and a golden apple.
- Interact with an owned companion using an empty hand to see its role, faction, trust and a simple day/night status line.
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
- `npcs.starting_trust=10` — trust when a survivor is recruited (0–100).
- `npcs.bread_trust=10` — trust gained for each bread given to a companion (1–50).
- `npcs.trade_min_trust=20` — trust required before a companion trades (0–100).
- `npcs.bandage_price=8` — emerald cost for one bandage (1–64).
- `npcs.role_item_consumption=false` — consume the iron tool used to assign a survivor role.

### NPC roadmap and known limitations

- The current NPC feature is a first prototype built on vanilla villagers, not a new custom NPC entity.
- Companions now have a first defensive combat behavior: nearby vanilla hostile monsters that threaten the owner or approach the companion are selected as targets using vanilla villager navigation/attack behavior. This is a prototype, not a trained soldier system; weapon use, tactical commands and robust combat animation are not implemented.
- Survivor role labels, faction identities, a status dialogue and a three-stage supply quest are implemented as an initial progression layer. They provide basic guard/medic support, persistent camp markers and a stock reserve that feeds a nearby hungry owner, but not collection of non-food loot, a full dialogue tree, faction diplomacy/war, generated settlements, physical storage containers, or autonomous camp construction.
- True work/home schedules and a full story quest campaign remain in development. The day/night line is informational; it is not yet a complete schedule simulation.
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
