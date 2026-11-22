# Aurora design ideas

Ideas not built yet. What is built is described in [docs/PROGRESSION.md](docs/PROGRESSION.md) and the CSVs under `scripts/data/`; how to build and run is in the [README](README.md).

## Ideas

- Enchants: feather = speed, blind = 1, sunder?
- Player scoreboard under the name: health | mana | PvP on/off
- Talismans, crates, coins?
- Mirage Finder: an item that tells you which Gormak zone the Mirage is in
- Event worlds (Christmas, Halloween) with a Fortune of their own that only applies there and adds to the base Fortune (Winter Fortune, Spooky Fortune), capped by a threshold: at 50, event Fortune raises it no further
- World themes: space, farm, void, twilight, candy, wasteland, sakura, volcano, mushroom, winter (ice caves, yetis)

## Later regions

**Caverns** (after the Overworld Core, which already gives the `caverns` tag): calcite and amethyst live here, cobalt too.

**Storm Citadel** (a later sky region): a darker castle in the storm clouds.

| Zone | Mine | Mob |
|---|---|---|
| Storm Gate | deepslate bricks, polished blackstone | storm knight |
| Lightning Forge | storm copper, copper bulb | forge golem |
| Citadel Keep | tinted glass, obsidian | crossbowman |
| The Eye | none | Storm Tyrant (boss) |

For any new region: only use mobs that melee, shoot arrows or throw potions (witches; leave their `attack_cooldown` empty). Creepers, slimes and evokers aren't handled by the combat system.

## When building

- Gormak's Dunes: obstacles and parkour across the dunes, so it plays differently from the Abandoned City.

## From the legacy Skript

Not built yet, see `legacy/skript/` for how each one used to work.

- `/vault` (`command.sk`): personal 6 row chests that keep items between sessions
- Arachne's other abilities (`mobarachne.sk`): cobweb traps, invisibility. She already wakes with her skeleton rider, in Emerald Peaks
- The Pillager Captain's abilities (`mobpillager.sk`): arrow volleys, berserk. The Captain and the Quarry Ravager are in the Ironbound Quarry
- Talking NPCs (`npc.sk`): timed dialogue lines
- From `test.sk`: wheat farming with regrowth, Restock bows, a Haste enchant
