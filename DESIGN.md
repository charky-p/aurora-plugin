## Aurora Java Minecraft Server Plugin

#### Ideas list
god tool (name TBD): later, combine the pick, axe (and shovel) lines into one tool. Each tool's
blocks are a rule on its tool component (ItemFactory.makeRightTool), one rule per kind, so the
god tool is just one item with all the rules, and MineListener gives Fortune on any of them

overworld pickaxe, and world pickaxes

talismans

overworld block combination of all overworld blocks

feather=speed, blind=1, sunder?

player scoreboard underneath name, health | mana | pvp on/off

reforge and enchant, replace with shop and permanent upgrades with essence

crates

currencies: coins + essence

grapple for movement early on, movement items category like elytra, dash

mine tierlists?

world ideas: space, farm, void, twilight, candy, wasteland, sakura, volcano, mushroom

mounts, with the ghast saddle

#### Server Design
world 1 (overworld): 5 main regions
1. Origin Isles - plains like sky islands (stone, coal, copper, iron, gold, gormak)
2. Gormak - desert "mini world" (Duneworld)
3. Verdant Groves - forest island, grapple unlockable (oak log, birch log, spruce log, dark oak log, tin, bronze, lead, silver, treant or tree zombee in large treehouse), 2 large island
4. Elderwood Roots - forest "mini world", huge world tree in the middle, this region only deals with around it
5. Sunken Marsh - swamp area with 1 big island, hydra
6. Khorvath - desert "mini world" region 2
7. Emerald Peaks  - large mountain island, elytra unlockable (snow, dirt, pumpkin, melon, redstone, lapis, emerald, arachne)
8. Elderwood Roots pt2 - inside the huge world tree now
9. Ironbound Quarry - detached quarry island, need elytra to go to (pale log, maple, gravel, clay, ashen soil, tungston, platinum, titanium, diamond, pillager, quarry, world boss (overworld golem))
10. Duat - finale of the desert "mini world" in the duat

NEW SYSTEM:
- New system is we split the mini worlds into their own sub worlds, essentially branching out progression
- Buying upgrades is no longer buying a pickaxe item, it is now buying enchants like fortune and efficiency, etc
- Each zone has a max amount for this, and has for each tool
- We can now add more niche mini worlds like candy land or space world, even event worlds like christmas
- Event worlds like christmas, halloween have special fortune that only apply to that world, and also add to the base fortune
- Like Winter Fortune, Spooky Fortune
- Threshold system to cap out the fortune, for example threshold at 50, then event fortune wont increase further than this
- (need a better solution for this)
- Tool materials are now also a one time upgrade or a threshold to unlock at x efficiency (5, 10, 25, 50, 100, 300), no God tool, keep things distinct
- u need to do every mini world and the main world before going to the next main world
- u also need to do the world boss after that prerequisite
- the plan is to release an alpha test with no builds on minefort, then once we have balanced it, release a beta test
- on minehut, with probably no builds or basic builds, then hire builders and launch the server once we have at least
- origin isles, verdant groves and gormak part 1.
- turn the system arhcitecture into a graph/image
- create a formula for generating the prices (IMPORTANT)
- ive decided for upgrades in each zone to be single chain for that tool, so the shop shows every tool to upgrade
- at that zone, and if u click on it, it will upgrade, and once max it will have some item change to show its max


world boss spawns retroactively every 5 minutes, there are also temple guards that drop overworld shards, 
can combine to overworld core, dropped by overworld golem

tin (light_gray_concrete) needs oak log, bronze (brown_concrete) needs birch log, lead (gray_concrete) needs spruce, silver (iron_block) needs dark oak log
lapis needs snow AND pumpkin, emerald needs dirt AND melons
tungston (?), platinum (?) needs gravel, platinum (?) needs pale, titanium (?) needs clay AND maple, diamond needs maple, clay, gravel, pale
dead log be pale log, maple be mangrove log
quarry is mob fight in quarry

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

cobalt for cave world

Overworld:
1. Origin Isles ([Stone], [Coal], [Copper, Chicken], [Iron, Sheep], [Gold, Cow]) -> Duneworld Portal
2. Verdant Groves ([Oak Log, Tin, Horse], [Birch Log, Bronze, Fox], [Spruce Log, Lead, Wolf], [Dark Oak Log, Silver, Sniffer]) -> Elderwood Roots Portal
3. Sunken Marsh - (TODO)
4. Emerald Peaks - (TODO: Snow, Dirt, Redstone, Lapis, Emerald, Pumpkin, Melon, Arachne, Goat, etc) -> Sky Spire
5. Ironbound Quarry - (TODO: tungston, platinum, gravel, pale log, titanium, clay, maple, diamond, pillager, overworld boss) -> Caverns

Duneworld:
1. Gormak
2. Khorvath
3. Sol

Elderwood Roots:
1. Outer region
2. Inside tree levels

Skyworld (TODO):
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

#### OOP Design
See the architecture diagram in `README.md`.

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

Rare drops (a drop under 100% chance in `blocks.csv`, or a `choices` pool under 100%) grow with the cube root of that multiplier instead, and the part after the point is a chance of one more. At L = 20 a common 5-9 becomes 580-1,044 while a rare 1-2 becomes 5-10 and a rare 1 becomes 4-5. Fortune adds up across realms: Origin, Gormak and Verdant Groves each give their own levels and the tool has the sum. The branches specialise so the sum stays sane: Gormak sells the sword, armour and shield with a little pick, Verdant Groves the pick and axe with a little combat, and prices assume both are kept level (`docs/PROGRESSION.md`). The formula lives in `common/loot/Fortune.java` (Java) and `fortune()` in `scripts/price_calculator.py` (prices), keep the two the same.

XP per block: items dropped x the block's `xp` in `scripts/data/world/blocks.csv`, so Fortune raises XP too. Every block of a zone has the same `xp`.

## TODO, switched off until finished

Built, but not registered in `core/Aurora.java`, so not reachable in game:

- The Essence shop (`/shop`): perks, the Keyring among them, and the ranks bought with Essence
- The gauntlet (`/gauntlet`)
- Fishing: the rods and their catch tables

## TODO, from the legacy Skript

Not built yet, see `legacy/skript/` for how each one used to work.

- `/vault` (`command.sk`): personal 6 row chests that keep items between sessions
- `/trash` (`command.sk`): a chest that deletes whatever is left in it when closed
- Arachne region (`mobarachne.sk`): spider boss with a skeleton rider, cobweb traps, invisibility
- Pillager region (`mobpillager.sk`): Ravager and Pillager Captain, arrow volleys, berserk
- Talking NPCs (`npc.sk`): timed dialogue lines
- From `test.sk`: wheat farming with regrowth, Restock bows, a Haste enchant