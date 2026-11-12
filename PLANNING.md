## Aurora Java Minecraft Server Plugin

### Features

- Mines
- Scoreboards
- etc TODO

#### System Architecture
Groups: Commands, Listeners, Database, Registry, YAML, Trader, Mining, Spawners, Models, Player Data, Aurora, Managers

Categories: Events = [Commands + Listeners], Data = [Database + Registry + YAML], Objects = [Trader, Mining, Spawners]


Aurora links to Managers, Data and Events

Registry links to YAML

Commands links to Managers

Listeners links to Managers

Managers links to Objects, Database and Registry

feature/purpose directory structure

#### Ideas list
combine axe, pickaxe, shovel, etc progression together, one linear path, since no
need to have more than 1 progression line, armor increases through

god tool (name TBD): later, combine the pick, axe (and shovel) lines into one tool. Each tool's
blocks are a rule on its tool component (ItemFactory.makeRightTool), one rule per kind, so the
god tool is just one item with all the rules, and MineListener gives Fortune on any of them

fortune multiplier, various blast enchantments

bunch of new mines from non mine blocks to be named smthin else

overworld pickaxe, and world pickaxes

blocks have a density factor, which reduces the effectiveness of fortune, as well as efficiency (`-density` to efficiency)

GUI compressor? or maybe just have one upgradable compressor 

mine tps, mine should tp to a set location, or maybe just middle

talismans

overworld block combination of all overworld blocks

feather=speed, blind=1, sunder?

custom shopkeepers with gui

player scoreboard underneath name, health | mana | pvp on/off

reforge and enchant, replace with shop and permanent upgrades with essence

bestiary for mobs

crates

ranks have perks, e.g. 1.1x resoures, coins, xp

currencies: coins + essence

under mob names: level | mob name | health

add borders enum to trader guis

send inventory or item in chat

grapple for movement early on, movement items category like elytra, dash

extra inventory for priority items go when full inventory

rank ups based off playtime + blocks mined, perhaps essence cost or gain essence on rank up
[PLAYER, ROOKIE, APPRENTICE, NOVICE, EXPERT, NOBLE, LORD, DUKE, MASTER, GRANDMASTER]

mine tierlists?

world ideas: space, farm, void, twilight, candy, wasteland, sakura, volcano, mushroom

turn off block drops for specific mines, make fortune go crazy but block density will negate some
fortune levels, efficiency will be gradual at the start, but upgrades
like more essence, 1 blast level, double drops, etc

/menu brings up gui with everything in the game on one screen, block drops, ranks, warps, etc

need boss: yaml in mobs, boss null if not boss, otherwise bossbar configuration

boss keys to force summon with no summon message in global chat

collections rewards: mine 100000 of a block, get stat buff (+ fortune)
collection thing is from low drop rate, if unlocked get essence

transition to backback inventory system, so amounts are variables

#### Server Design
world 1 (overworld): 4 regions
1. Origin Isles - plains like sky islands (stone, coal, copper, iron, gold, gormak)
2. Verdant Groves - forest island, grapple unlockable (oak log, birch log, spruce log, dark oak log, tin, bronze, lead, silver, treant or tree zombee in large treehouse)
3. Emerald Peaks  - large mountain island, elytra unlockable (snow, dirt, pumpkin, melon, redstone, lapis, emerald, arachne)
4. Ironbound Quarry - detached quarry island, need elytra to go to (pale log, maple, gravel, clay, ashen soil, tungston, platinum, titanium, diamond, pillager, quarry, world boss (overworld golem))

world boss spawns retroactively every 5 minutes, there are also temple guards that drop overworld shards, 
can combine to overworld core, dropped by overworld golem

tin (light_gray_concrete) needs oak log, bronze (brown_concrete) needs birch log, lead (gray_concrete) needs spruce, silver (iron_block) needs dark oak log
lapis needs snow AND pumpkin, emerald needs dirt AND melons
tungston (?), platinum (?) needs gravel, platinum (?) needs pale, titanium (?) needs clay AND maple, diamond needs maple, clay, gravel, pale
dead log be pale log, maple be mangrove log
quarry is mob fight in quarry

gormak region:
- abandoned city: scavenger husk, hollowed serf
- dunes: sand stalker (skeleton bowman), dune scout (gormak minion)
  - TODO when building it: obstacles and parkour across the dunes, so it plays differently from the city
- temple: tomb guard, vizier mage
- royal tomb: royal myrmidon, pharaoh, gormak the eternal

rare mob that spawns in any of the gormak regions: mirage, goes invisible every 5 sec for 4 sec, drops a rare item

tomb guard halberd, pharaoh  khopesh
(cracked stone bricks / stone bricks) for abandoned city
sandstone for dunes
raw gold block for temple

cobalt for cave world

drop calculation: `base * (1 + Math.max(0, fortune + 1 - density))`

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

optional features (code, not data):
- locked lifts: a level's portal only works once you own that level's gear, so levels can't be skipped
- elder treant summon key + bossbar (see boss keys / bossbar ideas above), could also give gormak's unused tomb_key a use

#### OOP Design

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

Server Transfer: use `target/Aurora-1.0.jar`, use FileZilla FTP to transfer world files.