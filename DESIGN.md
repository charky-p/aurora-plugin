## Aurora Java Minecraft Server Plugin

#### Ideas list
feather=speed, blind=1, sunder?

player scoreboard underneath name, health | mana | pvp on/off

talismans, crates, coins?

world ideas: space, farm, void, twilight, candy, wasteland, sakura, volcano, mushroom, winter (ice caves, yetis)

Storm Citadel (a later sky region): a darker castle in the storm clouds. Storm Gate (deepslate bricks, polished blackstone | storm knight), Lightning Forge (storm copper, copper bulb | forge golem), Citadel Keep (tinted glass, obsidian | crossbowman), The Eye (Storm Tyrant, boss)

Caverns (after the Overworld Core): calcite and amethyst live here, cobalt too

#### Server Design
Built so far (stages, zones and mobs in `scripts/data`, the ladder in `docs/PROGRESSION.md`): Origin Isles; Duneworld: Gormak, Khorvath, the Duat; Overworld: Verdant Groves, Sunken Marsh, Emerald Peaks, Ironbound Quarry and Golem's Rest; Elderwood Roots and Heart; Cloud Isles and the Sky Kingdom. The notes below are the ideas they came from.

world 1 (overworld): 5 main regions
1. Origin Isles - plains like sky islands (stone, coal, copper, iron, gold, gormak)
2. Gormak - desert "mini world" (Duneworld)
3. Verdant Groves - forest island, grapple unlockable (oak log, birch log, spruce log, dark oak log, tin, bronze, lead, silver, treant or tree zombee in large treehouse), 2 large island
4. Elderwood Roots - forest "mini world", huge world tree in the middle, this region only deals with around it
5. Sunken Marsh - swamp area with 1 big island: Mudflats, Slime Hollow, the Fens, Sunken Ruins and Glowmire (froglights, glow wisps), no boss
6. Khorvath - desert "mini world" region 2
7. Emerald Peaks  - large mountain island, elytra unlockable (snow, dirt, pumpkin, melon, redstone, lapis, emerald, arachne)
8. Elderwood Roots pt2 - inside the huge world tree now
9. Ironbound Quarry - detached quarry island, need elytra to go to (pale log, maple, gravel, clay, ashen soil, tungston, platinum, titanium, diamond, pillager, quarry, world boss (overworld golem))
10. Duat - finale of the desert "mini world" in the duat

- Event worlds like christmas, halloween have special fortune that only apply to that world, and also add to the base fortune
- Like Winter Fortune, Spooky Fortune
- Threshold system to cap out the fortune, for example threshold at 50, then event fortune wont increase further than this

world boss spawns retroactively every 5 minutes, there are also temple guards that drop overworld shards, 
can combine to overworld core, dropped by overworld golem

gormak region:
- abandoned city: scavenger husk, hollowed serf
- dunes: sand stalker (skeleton bowman), dune scout (gormak minion), has an oasis
  - TODO when building it: obstacles and parkour across the dunes, so it plays differently from the city
- temple: tomb guard, vizier mage
- royal tomb: royal myrmidon, pharaoh, gormak the eternal

rare mob that spawns in any of the gormak regions: mirage, goes invisible every 5 sec for 4 sec, drops a rare item
can spawn in any of the zones, mirage finder is an item that tells u which zone

tomb guard halberd, pharaoh  khopesh
(cracked stone bricks / stone bricks) for abandoned city
sandstone for dunes
raw gold block for temple
Scarab Zealot uses a Khopesh, Hollowed Legionnaire uses a Halberd (Spear).

Overworld:
1. Origin Isles ([Stone], [Coal], [Copper, Chicken], [Iron, Sheep], [Gold, Cow]) -> Duneworld Portal
2. Verdant Groves ([Oak Log, Tin, Horse], [Birch Log, Bronze, Fox], [Spruce Log, Lead, Wolf], [Dark Oak Log, Silver, Sniffer]) -> Elderwood Roots Portal
3. Sunken Marsh ([Packed Mud, Mud, Bog Frog], [Bog Slime, Mangrove Log, Swamp Slime], [Bog Stone, Fen Soil, Moss, Bog Walker], [Verdigris, Mangrove Leaves, Bogged Archer], [Ochre, Verdant and Pearlescent Froglight, Glow Wisp])
4. Emerald Peaks - (Snow, Dirt, Redstone, Lapis, Emerald, Pumpkin, Melon, Arachne, Goat, etc) -> Sky Spire
5. Ironbound Quarry - (tungston, platinum, gravel, pale log, titanium, clay, maple, diamond, pillager, overworld boss) -> Caverns

Duneworld:
1. Gormak
2. Khorvath
3. Sol

Elderwood Roots:
1. Outer region
2. Inside tree levels

Skyworld:
1. clouds
2. kingdom

Scale [Stone: 50%, Coal: 75%, Copper: 95%, Iron: 135%, Gold: 155%]
Add trims as cosmetics


#### The Great Canopy (region after Verdant Groves)
jungle region around one huge treehouse tree, reached by portal from the Dark Oak Thicket.
you climb it: each level up the tree is a zone, levels linked by portals (vine lifts / ladders)

| Level | Zone          | Mine                                   | Mobs                                        |
|-------|---------------|----------------------------------------|---------------------------------------------|
| 0     | Jungle Floor  | jungle log, jungle wood, stripped      | panda, ocelot (passive, like the groves)    |
| 1     | The Roots     | mud, rooted dirt, moss block           | sapling zombee (small weak tree zombee)     |
| 2     | Vine Halls    | mossy cobblestone, mossy stone bricks  | tree zombee, canopy spider                  |
| 3     | The Treehouse | bamboo block, bamboo mosaic            | tree zombee, treehouse sentry (archer)      |
| 4     | Upper Canopy  | azalea leaves, flowering azalea leaves | treant                                      |
| 5     | The Crown     | none                                   | elder treant (boss)                         |

- main mobs are a ladder up the tree: sapling zombee -> tree zombee -> treant -> elder treant
- gated twice per level: block density (more fortune each level) and mob strength
- gear like gormak: picks from the level's mine, swords and armour from its mobs, shields carry on with deflection
- avoid melon, pale oak, mangrove (emerald peaks / ironbound quarry use them)
- avoid mobs that don't melee or shoot arrows (witch, creeper, slime, evoker), the combat system only handles those
- once built, the tree zombee, treant and great treehouse gear move out of verdant groves, verdant ends at the dark oak thicket

Gormak - Fallen King
1. Abandoned City
- Scavenger Husk
- Hollowed Serf
2. Dunes
- Sand Stalker
- Dune Scout
- Mirage
3. Temple
- Tomb Guard
- Vizier Mage
4. Royal Tomb
- Gormak Minion
- Gormak Guardian
- Gormak The Eternal: Summons Gormak Ghoul

Khorvath - Avatar Of The Sun (WIP)
1. Canyons
- Hollowed Legionnaire
- Nomad Deserter
2. Ancient Ruins
- Scarab Zealot
- Relic Warrior
3. Catacombs
- Royal Myrmidon
- Pharaoh
4. Sanctuary
- Sphinx / Soul Weaver
- Anubite Executioner
- Khorvath

Sol, The Undying Sun
Location: Duat Realm
12 Gods? 12 mini zone
1. Ashen River
- Anubis
- Ferrymen, etc
2. Hall of Judgement
- Anubite exec?
- Acolytes
- Justice theme
3. Obsidian? Forge
- Solar Golems
- Eclipse Covenants
4. Throne, Darkness, Pits of Hell?

#### Run Instructions
Setup: put the Paper 26.3 jar at `LocalServer/versions/26.3/`, run the server once,
set `eula=true` in `LocalServer/eula.txt`.

From Git Bash, `scripts/server.sh` handles the rest (it has the Maven and JDK paths):
- `scripts/server.sh build` builds and copies the jar and YAML into LocalServer
- `scripts/server.sh start` / `stop` / `restart` / `status`
- `scripts/server.sh cmd "<command>"` runs a console command

`LocalServer/start.bat` still works for running the server in a window.
Join `127.0.0.1` from Minecraft.

Note: Turn off named entity console alerts in `spigot.yml`, `log-named-deaths: false`


#### Notes

Player health is real, mob health is pseudo, run `/data get entity @s Health`

Reload CSV Data: run `generate.py --live`

Server Transfer: `python scripts/deploy.py upload` sends the jar and YAML to the host, `--all` adds the database and worlds, see the README.

Drop Calculation (Fortune): with `L = fortune - density`, a block drops nothing if `L < 0`, otherwise

`drops = round(base * (1 + 0.75 * L + 0.25 * L^2))`, never less than 1

so the multiplier is 1, 2, 3.5, 5.5, 8, 11 ... for L = 0, 1, 2, 3, 4, 5. A block needs Fortune of at least its density to drop anything.

Rare drops (a drop under 100% chance in `blocks.csv`, or a `choices` pool under 100%) grow with the cube root of that multiplier instead, and the part after the point is a chance of one more. At L = 20 a common 5-9 becomes 580-1,044 while a rare 1-2 becomes 5-10 and a rare 1 becomes 4-5.

Looting is the same for mobs: drops are multiplied by the curve with L = the Looting level (mobs have no density), rare drops by its cube root, gear and keys never (`common/loot/Looting.java`, `mob_income` in `scripts/balance/price_zones.py`). At Looting 8 a common 1-3 becomes 23-69 and a rare 1-2 becomes 3-6. Fortune adds up across realms: Origin, Gormak and Verdant Groves each give their own levels and the tool has the sum. The branches specialise so the sum stays sane: Gormak sells the sword, armour and shield with a little pick, Verdant Groves the pick and axe with a little combat, and prices assume both are kept level (`docs/PROGRESSION.md`). The formula lives in `common/loot/Fortune.java` (Java) and `fortune()` in `scripts/price_calculator.py` (prices), keep the two the same.

XP per block: items dropped x the block's `xp` in `scripts/data/world/blocks.csv`, so Fortune raises XP too. Every block of a zone has the same `xp`.

## TODO, switched off until finished

Built, but not registered in `core/Aurora.java`, so not reachable in game:

- The gauntlet (`/gauntlet`)
- Fishing: the rods and their catch tables

## TODO, from the legacy Skript

Not built yet, see `legacy/skript/` for how each one used to work.

- `/vault` (`command.sk`): personal 6 row chests that keep items between sessions
- Arachne's abilities (`mobarachne.sk`): a skeleton rider, cobweb traps, invisibility. Arachne herself is in Emerald Peaks
- The Pillager Captain's abilities (`mobpillager.sk`): arrow volleys, berserk. The Captain and the Quarry Ravager are in the Ironbound Quarry
- Talking NPCs (`npc.sk`): timed dialogue lines
- From `test.sk`: wheat farming with regrowth, Restock bows, a Haste enchant