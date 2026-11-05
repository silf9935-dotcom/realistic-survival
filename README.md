# Realistic Survival

A Fabric survival-simulation mod for Minecraft Java 1.21.11.

## Version 1.0.0 release candidate

This release consolidates the survival, medical, faction NPC, patrol, checkpoint, story-character and territory systems into one build.

### Survival systems
- Persistent thirst, stamina, fatigue, sanity and body temperature.
- Drink from water sources by crouching with an empty hand; untreated water has a configurable contamination risk.
- Sprinting, hunger, dehydration, fatigue and temperature affect stamina and movement.
- Hot and cold environments affect body temperature; heat increases thirst and severe cold increases fatigue.
- Sleeping and quiet crouching restore selected survival values.
- Darkness and extreme fatigue reduce sanity; safe light and rest support recovery.
- Survival status action-bar HUD, with compact and disabled modes.

### Medical system
- Hard hits can cause persistent injuries with escalating weakness/slowness penalties.
- Craftable bandages (paper + string) reduce injury severity; paper remains supported as an improvised fallback.
- Injury thresholds, treatment cooldown and penalties are configurable.

### Faction NPCs

The NPC scope is intentionally limited to exactly three groups:
- **Bandits:** pillagers with light leather protection and ranged crossbow combat.
- **Military:** vindicators with iron weapons and full iron armor for close combat.
- **Story characters:** individually marked villagers with persistent dialogue and a short branching quest. They cannot be recruited.

Place invisible spawn markers:
```mcfunction
/summon minecraft:armor_stand ~ ~ ~ {Tags:["rs_spawn_bandits"],Invisible:1b,Marker:1b,NoGravity:1b,Invulnerable:1b}
/summon minecraft:armor_stand ~ ~ ~ {Tags:["rs_spawn_military"],Invisible:1b,Marker:1b,NoGravity:1b,Invulnerable:1b}
```
When a player comes within 96 blocks, each marker dispatches groups on the configured interval. Groups spawn on nearby solid ground and obey local faction population caps. Markers do not force-load chunks.

### Patrols and checkpoints

Place up to four invisible armor stands per faction and tag them in travel order. Example:
```mcfunction
/summon minecraft:armor_stand ~ ~ ~ {Tags:["rs_patrol_bandits_0"],Invisible:1b,Marker:1b,NoGravity:1b,Invulnerable:1b}
/summon minecraft:armor_stand ~10 ~ ~ {Tags:["rs_patrol_bandits_1"],Invisible:1b,Marker:1b,NoGravity:1b,Invulnerable:1b}
/summon minecraft:armor_stand ~10 ~ ~10 {Tags:["rs_patrol_bandits_2"],Invisible:1b,Marker:1b,NoGravity:1b,Invulnerable:1b}
/summon minecraft:armor_stand ~ ~ ~10 {Tags:["rs_patrol_bandits_3"],Invisible:1b,Marker:1b,NoGravity:1b,Invulnerable:1b}
```
For military routes, replace `rs_patrol_bandits_` with `rs_patrol_military_`. Routes loop through the existing waypoint indices. Combat interrupts patrol movement until the mob has no living target.

Military checkpoints dispatch guards and warn approaching players:
```mcfunction
/summon minecraft:armor_stand ~ ~ ~ {Tags:["rs_checkpoint_military"],Invisible:1b,Marker:1b,NoGravity:1b,Invulnerable:1b}
```
Nearby military guards engage survival players within the configured guard radius.

### Territories and faction events

Create a territory marker. Choose its initial owner by adding one start tag:
```mcfunction
/summon minecraft:armor_stand ~ ~ ~ {Tags:["rs_territory","rs_territory_start_neutral"],Invisible:1b,Marker:1b,NoGravity:1b,Invulnerable:1b}
```
Use `rs_territory_start_bandits` or `rs_territory_start_military` for faction-owned territory. The marker stores its owner and capture progress in entity tags, so control persists after saving and reloading the world.

- A player can capture a territory by remaining inside its capture radius while no faction force is present.
- Bandit or military units can capture territory for their faction.
- If both factions are present, the territory is contested and capture progress pauses.
- A successful capture is announced to nearby players.
- Each territory periodically triggers a local bandit-versus-military skirmish event. Event units use the existing faction combat behavior.

### Story characters

Create a story character:
```mcfunction
/summon minecraft:villager ~ ~ ~ {Tags:["rs_story_npc"],CustomName:'{"text":"Старый связной"}',CustomNameVisible:1b}
```
Interact to begin the quest. Give paper to choose the intelligence route (receive a compass), or an emerald to choose the supply route (receive bread). Both routes lead to a task to deliver 5 iron ingots, rewarded with a golden apple and 3 emeralds. Dialogue, route choice and quest stage are saved on the character.

Recruitable survivors, survivor jobs, trust/trading, camp supplies and companion systems are intentionally not part of this NPC design.

### Configuration

The file `config/realistic-survival.properties` is created on first launch. Restart the world/server after changing settings.

| Setting | Default | Purpose |
|---|---:|---|
| `hud.enabled` | `true` | Show or hide the action-bar HUD |
| `hud.compact` | `false` | Show a compact HUD |
| `survival.cold_fatigue` | `true` | Severe cold increases fatigue |
| `survival.hot_thirst` | `true` | Hot biomes increase thirst loss |
| `survival.unsafe_water` | `true` | Untreated natural water can cause nausea |
| `survival.unsafe_water_chance` | `15` | Water contamination chance, percent |
| `injuries.enabled` | `true` | Enable injury penalties and treatment |
| `injuries.damage_threshold` | `4.0` | Damage required to cause an injury |
| `injuries.severe_hit_threshold` | `8.0` | Damage required for a severe injury |
| `injuries.treatment_cooldown_seconds` | `10` | Delay between bandage uses |
| `world.npc.faction_wars` | `true` | Enable bandit/military hostility |
| `world.npc.faction_war_scan_ticks` | `40` | Faction scan interval |
| `world.npc.faction_detection_radius` | `24` | Faction detection distance |
| `world.npc.spawn_interval_seconds` | `300` | Spawn marker dispatch interval |
| `world.npc.spawn_radius` | `8` | Spawn distance around a marker |
| `world.npc.population_radius` / `population_cap` | `64` / `6` | Local faction population cap |
| `world.npc.group_min` / `group_max` | `2` / `3` | Spawn group size |
| `world.npc.patrols_enabled` | `true` | Enable waypoint patrols |
| `world.npc.patrol_speed` | `0.85` | Patrol movement speed |
| `world.npc.checkpoints_enabled` | `true` | Enable military checkpoints |
| `world.npc.checkpoint_alert_radius` / `guard_radius` | `12` / `20` | Checkpoint warning and guard distances |
| `world.npc.territories_enabled` | `true` | Enable territory control and skirmishes |
| `world.npc.territory_capture_radius` | `12` | Territory capture radius |
| `world.npc.territory_capture_seconds` | `30` | Time required to capture |
| `world.npc.territory_event_interval_seconds` | `600` | Time between territory skirmishes |

### Release notes and testing

This 1.0.0 build consolidates the implemented systems into one release line. CI compilation and packaging verify that the mod builds; they do not replace hands-on testing of mob navigation, capture balance, multiplayer behavior and world-save persistence. Custom NPC models and a graphical HUD are not included in this release.
