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

#### Ideas list
combine axe, pickaxe, shovel, etc progression together, one linear path, since no
need to have more than 1 progression line, armor increases through

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

cobalt for cave world
#### OOP Design

#### Run Instructions
Download Paper 1.21

Add a start.bat file, with:

Add a new directory LocalServer

Run Maven build, which puts plugin in LocalServer, so make sure dir is created

`cd LocalServer` then run `./start.bat`

if `start.bat` fails, go to `eula.txt` and change `false` to `true`

rerun `start.bat`, join `127.0.0.1` on minecraft in server list

to stop server, first write `stop`, then `CTRL + C` after server starts to reboot

Note: Turn off named entity console alerts in `spigot.yml`, `log-named-deaths: false`