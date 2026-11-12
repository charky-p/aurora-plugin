# Aurora Commands

`/aurora` has the aliases `/a` and `/aura`. On its own it opens the player menu for everyone; every subcommand below needs `aurora.admin` (ops have it). In game, `/a help` lists them and `/a info <command>` shows one in detail.

Arguments: `<required>`, `[optional]`. Coordinates take `~` for your own position. Names of mines, spawners and portals are case-insensitive.

**`-w <world>`** can go anywhere in any `/aurora` command to pick the world it works in, e.g. `/a mine create coal 0 60 0 8 64 8 -w aurora_test`. Without it, commands use your own world, or the main world from the console. Everything can be set up from the console this way, as long as coordinates are given instead of `~` or the wand.

## Player commands

| Command | Does |
|---|---|
| `/aurora`, `/menu`, `/hub` | Open the Aurora menu |
| `/backpack`, `/bp` | Open your backpack |
| `/spawn` | Teleport to this world's spawn |

## Building a region, in order

1. `/a world create <name>` for a new void world, then `/a world tp <name>` and build.
2. `/a setspawn` where players should arrive in that world.
3. `/a wand`, then left click and right click two corners for each mine and portal.
4. `/a mine wandcreate <name>`, then `/a mine setblocks <name> <block> <%> ...`.
5. `/a spawner create <name> ~ ~ ~ <mob>` for each mob area.
6. `/a trader create <trader>` where each trader should stand, facing you.
7. `/a portal create <name>` from the wand selection, then stand at the arrival point and `/a portal setdest <name>`. Make a second portal for the way back.
8. `scripts/server.sh backup` (local) or `python scripts/deploy.py backup <server>` (live).

## Test world

`python scripts/build_test_world.py --send` builds a playable skeleton of every region in a void world, `aurora_test`, on the running local server, from console commands only. Plain platforms, one square per zone with its mines, spawners and trader, and obsidian gates linking Origin, Gormak and Verdant both ways. Everything it names starts with `t_`. Go there with `/a world tp aurora_test`. Without `--send` it prints the commands instead.

## Worlds

Extra void worlds, saved so they load on every startup.

| Command | Does |
|---|---|
| `/a world create <name>` | Create an empty void world, or load an existing folder. Lowercase letters, numbers and `_` |
| `/a world list` | List the loaded worlds, click to teleport |
| `/a world tp <world> [player]` | Teleport to a world's spawn, the one from `/a setspawn` if set |
| `/a world remove <name>` | Stop loading it on startup. Its files are kept |

## Portals

A box like a mine. Walking in sends you to its destination, which can be in another world. Arriving inside a portal does not trigger it, step out and back in. Nether portal blocks inside a portal do nothing, so they can be the frame.

| Command | Does |
|---|---|
| `/a portal create <name> [<x1> <y1> <z1> <x2> <y2> <z2>]` | Create from the wand selection, or two corners |
| `/a portal setdest <portal> [<world> <x> <y> <z> [yaw] [pitch]]` | Where it leads, defaults to where you stand and face |
| `/a portal delete <portal>` | Delete it |
| `/a portal rename <portal> <new name>` | Rename it |
| `/a portal list` | Every portal and where it leads |
| `/a portal info <portal>` | Its box and destination |
| `/a portal tp <portal> [player]` | Teleport into it |

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

## Other admin commands

| Command | Does |
|---|---|
| `/a wand` | Wand: left click a block for corner 1, right click for corner 2, right click a trader to remove it |
| `/a setspawn [<x> <y> <z> [yaw]]` | This world's spawn, used by `/spawn`, respawns and `/a world tp`. The `aurora_mines` spawn is where every player lands on join |
| `/a heal [player]` | Restore health and hunger, put out fire |
| `/a help`, `/a info <command>` | Command list, one command in detail |
| `/refresh` | Reload all game data from the YAML, after `python scripts/generate.py --live` |
| `/dump [items\|icons\|all]` | Shulkers of every registry item or icon, for checking how they look |
| `/gauntlet` | Gauntlet test mode, unfinished, clears your inventory |
