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
