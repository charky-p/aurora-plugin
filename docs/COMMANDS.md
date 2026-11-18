# Aurora Commands

`/aurora` has the aliases `/a` and `/aura`. On its own it opens the player menu for everyone; every subcommand below needs `aurora.admin` (ops have it). In game, `/a help` lists them and `/a info <command>` shows one in detail.

Arguments: `<required>`, `[optional]`. Coordinates take `~` for your own position. Names of mines, spawners and portals are case-insensitive.

**`-w <world>`** can go anywhere in any `/aurora` command to pick the world it works in, e.g. `/a mine create coal 0 60 0 8 64 8 -w aurora_test`. Without it, commands use your own world, or the main world from the console. Everything can be set up from the console this way, as long as coordinates are given instead of `~` or the wand.

## Player commands

| Command | Does |
|---|---|
| `/a`, `/aurora` | Open the Aurora menu |
| `/backpack`, `/bp` | Open your backpack |
| `/customise`, `/customize`, `/cosmetics` | Choose how you look: rank, suffixes, chat and name colour, join and death message, trail, kill effect and the trim on your armour. Cosmetics are bought here with Essence |
| `/ranks`, `/rank` | Choose the rank shown by your name, from the ones you have unlocked |
| `/collections`, `/collection` | Every resource you have gathered, its tier and what the next one pays |
| `/leaderboard` | The ten highest levels on the server |
| `/spawn` | Teleport to this world's spawn |
| `/shop` | The Essence shop: Dash, mounts, the God Tool, the Keyring and ranks, each bought once. Click one you own to use it again |
| `/dash` | Turn Dash on or off, once bought: shift twice quickly to dash the way you face, once a second |
| `/tutorial skip` | Leave the first-join tutorial without its reward. The Skip link in chat runs it |

**In chat.** `[item]` or `\item` in a message shows the item in your hand, hover it to read it. `[inv]` or `\inv` adds a link anyone can click to look at your inventory as it was when you sent it; the link works for 10 minutes.

The gauntlet and fishing are switched off until they are finished, see [PROGRESSION.md](PROGRESSION.md). Selling donor ranks and cosmetics through a store is in [STORE.md](STORE.md).

## Building a region, in order

1. `/a world create <name>` for a new void world, then `/a world tp <name>` and build. To bring a build in from another server, see [MAP.md](MAP.md), which also lists what every zone needs.
2. `/a setspawn` where players should arrive in that world, or `/a setspawn main` for the world players join in.
3. `/a wand`, then left click and right click two corners for each mine and portal.
4. `/a mine wandcreate <name>`, then `/a mine setblocks <name> <block> <%> ...`.
5. `/a spawner create <name> ~ ~ ~ <mob>` for each mob area.
6. `/a trader create <trader>` where each trader should stand, facing you.
7. `/a portal create <name>` from the wand selection, then stand at the arrival point and `/a portal setdest <name>`. Make a second portal for the way back.
8. In the first region, stand by the Cobblestone Quarry and `/a tutorial setstart` for where new players begin the tutorial.
9. `scripts/server.sh backup` (local) or `python scripts/deploy.py backup <server>` (live).

## Test world

`python scripts/build_test_world.py --send` builds a playable skeleton of every region on the running local server, from console commands only, read from `scripts/data` (`regions.csv`, `zones.csv`, `mines.csv`, `traders.csv`). Regions are strips of zones in four void worlds with keep inventory on: `aurora_test` (Origin Isles, Verdant Groves, Sunken Marsh, Emerald Peaks, Ironbound Quarry), `aurora_test_gormak` (Gormak, Khorvath, the Duat), `aurora_test_elderwood` (Elderwood Roots and Heart) and `aurora_test_sky` (Cloud Isles, Sky Kingdom). Khorvath stands on a 10 block cliff (grapple), Elderwood Heart on a 20 block one (elytra), and the Duat and the Ironbound Quarry start 40 blocks of void further on (elytra). Dying respawns you at the world's spawn. Plain platforms, one square per zone with its 9x9x5 mines (they refill the moment they are mined out, and every minute), spawners and a trader beside the mine, fences with carpet around zones with hostile mobs (a gate on the walking line) and a fenced safe platform on their left where the trader stands, a walled arena for each summoned boss, and obsidian gates linking the worlds both ways, locked to their pass with its gatekeeper beside them. Everything it names starts with `t_`. Go there with `/a world tp aurora_test`.

Without `--send` it prints the commands instead. `--regions gormak,verdant` builds only those, and `--teardown --send` deletes everything it built so it can be built again.

## Worlds

Extra void worlds, saved so they load on every startup.

| Command | Does |
|---|---|
| `/a world create <name>` | Create an empty void world, or load an existing folder. Lowercase letters, numbers and `_`. `overworld`, `the_nether` and `the_end` are taken by vanilla |
| `/a world list` | List the loaded worlds, click to teleport |
| `/a world tp <world> [player]` | Teleport to a world's spawn, the one from `/a setspawn` if set |
| `/a world remove <name>` | Stop loading it on startup. Its files are kept |

## Portals

A box like a mine. Walking in sends you to its destination, which can be in another world. Arriving inside a portal does not trigger it, step out and back in. Nether portal blocks inside a portal do nothing, so they can be the frame.

| Command | Does |
|---|---|
| `/a portal create <name> [<x1> <y1> <z1> <x2> <y2> <z2>]` | Create from the wand selection, or two corners |
| `/a portal setdest <portal> [<world> <x> <y> <z> [yaw] [pitch]]` | Where it leads, defaults to where you stand and face |
| `/a portal settag <portal> [access]` | Lock it to players with that access, players without it are thrown back. None unlocks it |
| `/a portal delete <portal>` | Delete it |
| `/a portal rename <portal> <new name>` | Rename it |
| `/a portal list` | Every portal and where it leads |
| `/a portal info <portal>` | Its box and destination |
| `/a portal tp <portal> [player]` | Teleport into it |

**Locked portals.** Players get access by right clicking a pass, in their backpack or their hand, which uses it up. Passes are in `scripts/data/resources_special.csv`, the `unlocks` column is the access they give, e.g. `gormak_key` (the Gormak Pass) gives `gormak`. Access is kept by the plugin in its own database (the `portal_access` table), not in vanilla scoreboard tags; `/a access` shows, gives and takes it, and `/a reset <player> all` takes it all away.

**Summoned bosses.** A key with a `summons` column, e.g. `tomb_key` summons `gormak_eternal`, wakes that mob at its spawner when right clicked within 24 blocks of it. Spawners of a summoned mob never spawn it by themselves, so place the boss's spawner with `/a spawner create` as usual, where it should rise. Its limit is how many can be awake at once.

## Mines

| Command | Does |
|---|---|
| `/a mine wandcreate <name> [threshold] [delay]` | Create from the wand selection |
| `/a mine create <name> <x1> <y1> <z1> <x2> <y2> <z2> [threshold] [delay]` | Create from two corners |
| `/a mine setblocks <mine> <block> <%> [<block> <%>]...` | Blocks from `blocks.csv` and their share, e.g. `oak_log 70 oak_wood 30` |
| `/a mine setdelay <mine> <delay>` | Reset timer in ticks, 1200 is a minute |
| `/a mine setthreshold <mine> <threshold>` | Also reset once this % is mined, above 100 for never |
| `/a mine settp <mine> <x> <y> <z>` | Where players go on reset and `/a mine tp` |
| `/a mine redefine <mine> [<x1> <y1> <z1> <x2> <y2> <z2>]` | Move it, from coordinates or the wand selection |
| `/a mine rename <mine> <new name>` | Rename it |
| `/a mine reset <mine>`, `/a mine resetall` | Reset now |
| `/a mine list [page]`, `/a mine info <mine>` | List, details |
| `/a mine tp <mine> [player]` | Teleport to it |
| `/a mine delete <mine>` | Delete it |

Defaults: threshold 101 (timer only), delay 1200. Mines can't overlap.

## Spawners

| Command | Does |
|---|---|
| `/a spawner create <spawner> <x> <y> <z> <mob> [limit] [radius] [delay]` | Create, mob ids from the `mobs_*.csv` files. Defaults: limit 3, radius 5, delay 200 ticks |
| `/a spawner setmob <spawner> <mob>` | Change its mob |
| `/a spawner setlimit <spawner> <limit>` | Most alive at once |
| `/a spawner setradius <spawner> <radius>` | How far from it mobs spawn |
| `/a spawner setdelay <spawner> <delay>` | Ticks between spawns, 20 is a second |
| `/a spawner redefine <spawner> [<x> <y> <z>]` | Move it, defaults to where you stand |
| `/a spawner toggle <spawner>` | Turn it off or on |
| `/a spawner rename <old> <new>` | Rename it |
| `/a spawner list [page]`, `/a spawner info <spawner>` | List, details |
| `/a spawner tp <spawner> [player]` | Teleport to it |
| `/a spawner delete <spawner>` | Delete it |

Spawners only spawn with a player within 64 blocks.

## Traders

Saved as numbered spots. A trader that is killed or unloaded respawns at its spot, and a new server spawns them all from the database.

| Command | Does |
|---|---|
| `/a trader create <trader> [<x> <y> <z>]` | Place one, trader ids from `traders.csv`. Defaults to where you stand, facing you |
| `/a trader delete <spot>` | Remove it for good, `3` or `#3` |
| `/a trader list [page]` | Every spot, click to teleport |
| `/a trader tp <spot> [player]` | Teleport in front of it |

Right clicking a trader with the wand also removes it and its spot.

## Tutorial

A new player's first join starts the tutorial at the spot set with `setstart`, instead of the main spawn. A boss bar at the top shows the step, and a chat line says what to do: mine 10 cobblestone, open the backpack, click a trader, buy an upgrade. Each step ends by itself. Finishing pays 50 Essence and the `NEWCOMER` suffix. A player who leaves mid-way comes back to the start of the step they were on. Players who joined before it existed never get it.

| Command | Does |
|---|---|
| `/a tutorial setstart` | New players start the tutorial where you stand |
| `/a tutorial tp` | Go to where it starts |
| `/a tutorial start [player]` | Put a player through it from the first step. `/a reset <player> all` does this too |
| `/a tutorial skip [player]` | Take a player out of it, no reward |

## Other admin commands

| Command | Does |
|---|---|
| `/a wand` | Wand: left click a block for corner 1, right click for corner 2, right click a trader to remove it |
| `/a setspawn [main] [<x> <y> <z> [yaw]]` | This world's spawn, used by `/spawn`, respawns and `/a world tp`. With `main` it is also the main spawn, where every player lands on join. Until a main spawn is set, players join in `aurora_mines` |
| `/a heal [player]` | Restore health and hunger, put out fire |
| `/a give <player> <item> [amount]` | Give an item by registry id from the CSVs, e.g. `gold_pick_5`, or a vanilla one like `cooked_beef`. What doesn't fit drops at their feet |
| `/a access <player> [give\|take <access>]` | Where a player may go: on its own it lists what their passes opened, `give`/`take` open or close one by hand, e.g. `/a access Charky32 give sky` |
| `/a reset <player> <tools\|all>` | `tools` resets every tool level and hands back plain starter tools. `all` erases everything the player has and plays the first join again, starter kit, backpack and tutorial included: tools, backpack and inventory, level, XP, kills, blocks mined, Essence, playtime, perks, collections, gear unlocked from mobs, portals, cosmetics and every rank, staff and donor ranks and the second suffix included. It cannot be undone |
| `/a tool <player> give <tool>` | Hand over a tool that lives on the player (pick, axe, shovel, sword, helmet, chestplate, leggings, boots). Its item is drawn from the player's levels in the realm they stand in |
| `/a tool <player> set <tool> <stat> <level> [realm]` | Set one level of a player's tool, in their current realm or a named one. The stat is an enchant, or `material` with a name such as `iron` |
| `/a tool <player> info <tool>` | What the tool has where the player stands, and which realms that builds on |
| `/a cosmetic <player> <give\|take> <id>` | Give or take a rank, a cosmetic or `suffix_slot_2`. The only way to the donor and staff ranks, the donor-only cosmetics and the second suffix, which comes with a donor package. Tab completes every id |
| `/a grant <player> <essence\|xp\|hours> <amount>` | Give Essence, XP or hours of playtime, for testing cosmetics, levels and playtime ranks. Hours are counted within a minute, which is when a new playtime rank is handed out |
| `/a help`, `/a info <command>` | Command list, one command in detail |
| `/refresh` | Reload all game data from the YAML, after `python scripts/generate.py --live` |
| `/dump [items\|icons\|all\|clear]` | Shulkers of every registry item or icon, for checking how they look. `clear` empties your own backpack and inventory |
