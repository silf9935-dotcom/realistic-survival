## 1.0.0 release candidate

- Consolidate the survival needs, medical treatment, faction NPCs, story characters, spawn markers, faction wars, patrol routes and military checkpoints into one release version.
- Add persistent faction territories with neutral, bandit and military starting ownership.
- Allow players to capture unguarded territory and allow bandit or military units to claim territory for their faction.
- Pause capture while both factions contest a territory; announce successful captures to nearby players.
- Add scheduled bandit-versus-military skirmish events at active territory markers.
- Add configuration for territory capture radius, capture duration, event interval and system enablement.
- Update setup commands and configuration documentation for the consolidated 1.0.0 release.

## 0.3.0-beta.1

- Consolidate the alpha survival loop into the first beta candidate for Minecraft Java 1.21.11 / Fabric.
- Add persistent injury severity after hard hits, with escalating weakness and slowness penalties.
- Add improvised wound treatment: use paper while injured to reduce wound severity; treatment is configurable and creative players do not consume paper.
- Add `injuries.enabled` to the generated configuration.
- Add configurable untreated-water contamination: drinking directly from natural water has a 15% chance to cause brief nausea (`survival.unsafe_water`).
- Display current injury severity in the survival action-bar HUD while a wound is active.
- Make untreated-water contamination probability configurable (0–100%).
- Ensure disabling the injury system also disables ongoing injury penalties without deleting saved wounds.
- Add configurable normal and severe-hit damage thresholds for injury generation.
- Add a configurable cooldown between wound treatments to prevent rapid repeated bandaging.
- Register a dedicated craftable bandage item with English and Russian names, item model, and a paper + string recipe; retain paper as an improvised fallback.
- Retain thirst, stamina, fatigue, sanity, body temperature, environmental exposure, quiet rest, sleep recovery and configurable action-bar HUD.
- This is a beta candidate, not a claim that the planned full overhaul is complete; proper graphical HUD, dedicated medical items/recipes, broader world simulation and in-game multiplayer/balance testing remain future work.

## 0.2.0-alpha.16

- Cold exposure now increases fatigue and makes sprinting consume stamina faster when body temperature becomes critically low.
- Added working HUD settings: the overlay can be disabled or switched to a compact layout.
- Added configuration switches for cold-related fatigue and hot-biome thirst.

## 0.2.0-alpha.15

- Critically low stamina now causes a brief movement penalty, and extreme fatigue further wears down sanity.
- Consolidate the survival feedback loop across hydration, temperature, fatigue, stamina and sanity.

## 0.2.0-alpha.14

- Add a low-stamina movement penalty so exhausting yourself has an immediate gameplay consequence.

## 0.2.0-alpha.13

- Hot biomes now increase thirst loss over time, in addition to heat stress from body temperature.

## 0.2.0-alpha.12

- Darkness gradually reduces sanity; well-lit surroundings support sanity recovery when fatigue is not severe.

## 0.2.0-alpha.11

- Quiet resting now also restores sanity gradually, making safe breaks useful beyond stamina and fatigue recovery.

## 0.2.0-alpha.10

- Add a quiet-rest mechanic: crouching while nearly motionless gradually reduces fatigue and restores stamina.
- Dehydration now makes sprinting more expensive and slows stamina recovery, so thirst affects physical performance before critical dehydration.

# Changelog

## 0.2.0-alpha.9

- Body temperature now responds to sustained sprinting as well as environmental exposure.
- Heat stress increases thirst over time.
- Extreme cold and overheating slow stamina recovery, connecting temperature to physical endurance.

## 0.2.0-alpha.8

- Stabilize survival physiology interactions and document the server-authoritative needs loop.

## 0.2.0-alpha.7

- Refine survival status feedback and persist physiological state consistently during active play.

## 0.2.0-alpha.6

- Add persistent body temperature, affected by water, fire, hot biomes, and cold rainy weather.
- Hypothermia and overheating now apply escalating survival penalties; temperature is shown in the action-bar HUD.

## 0.2.0-alpha.5

- Critical sanity loss now causes periodic nausea.
- Extremely low sanity also causes periodic weakness.

## 0.2.0-alpha.4

- Low hunger now increases fatigue over time.
- Low hunger makes sprinting consume stamina faster and slows stamina recovery.
- Sprinting, thirst, sleep recovery, and fatigue penalties remain server-side.

## 0.2.0-alpha.3

- Fix the Minecraft 1.21.11 Mojang-mapped slowness effect field used by the fatigue penalty.
- Keep the existing server-side thirst, stamina, fatigue, sanity, and persistence loop.

## 0.2.0-alpha.2

- Initial playable survival needs loop and action-bar HUD.
- Drinking from water blocks restores thirst.
- Survival values persist in player entity tags.
