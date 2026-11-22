# Aurora Commands

`/aurora` has the aliases `/a` and `/aura`. On its own it opens the player menu for everyone; every subcommand below needs `aurora.admin` (ops have it). In game, `/a help` lists them and `/a info <command>` shows one in detail.

Arguments: `<required>`, `[optional]`. Coordinates take `~` for your own position. Names of mines, spawners and portals are case-insensitive.

**`-w <world>`** can go anywhere in any `/aurora` command to pick the world it works in, e.g. `/a mine create coal 0 60 0 8 64 8 -w aurora_test`. Without it, commands use your own world, or the main world from the console. Everything can be set up from the console this way, as long as coordinates are given instead of `~` or the wand.

## Player commands

| Command | Does |
|---|---|
| `/a`, `/aurora` | Open the Aurora menu: How to Play, backpack, collections, customise, the Essence shop, warps (supporters only: a donor or staff rank), spawn, the Gauntlet and the leaderboard |
| `/backpack`, `/bp` | Open your backpack |
| `/customise`, `/customize`, `/cosmetics` | Choose how you look: rank, suffixes, chat and name colour, join and death message, trail, kill effect, the trim on your armour, and the look of your tools and armour (any material you have bought, wood to gold; Best Owned shows the newest). A look changes only how it shows: a tool digs at its best material's speed. Cosmetics are bought here with Essence |
| `/ranks`, `/rank` | Choose the rank shown by your name, from the ones you have unlocked |
| `/collections`, `/collection` | Every resource you have gathered, its tier and what the next one pays |
| `/leaderboard` | The top ten by level, blocks mined, mob kills, Essence and playtime, as player heads; the arrows switch stats, and your own place is shown |
| `/trash` | A bin: whatever is left in it when you close it is deleted. Tools, the backpack and keys are handed back |
| `/spawn` | Teleport to this world's spawn |
| `/shop` | The Essence shop: Dash, Double Jump (press jump again in mid-air for a normal second jump), mounts, the Keyring and ranks. Mounts have three levels, each faster: left click upgrades one you own, right click gives its saddle again. Click anything else you own to use it again |
| `/dash` | The Dash Feather again, once Dash is bought: right click it to dash the way you face, once a second |
| `/tutorial skip` | Leave the first-join tutorial without its reward. The Skip link in chat runs it |
| `/gauntlet` | Go to the Gauntlet's lobby; there, open its menu: start a run, the forge, milestones, top floors, leave. `/gauntlet boon` reopens a boon waiting to be picked, `/gauntlet leave` goes back to the main spawn with your own inventory |
| `/discord` | A clickable link to the Aurora Discord, https://discord.gg/MefQWSgkBA |
| `/suggest <your idea>` | Send an idea to the staff, up to 256 characters, one a minute. Staff online see it straight away, and it is saved for `/a suggestions` |

**In chat.** `[item]` or `\item` in a message shows the item in your hand, hover it to read it. `[inv]` or `\inv` adds a link anyone can click to look at your inventory as it was when you sent it; the link works for 10 minutes.

How the game progresses is in [PROGRESSION.md](PROGRESSION.md). Selling donor ranks and cosmetics through a store is in [STORE.md](STORE.md).

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

**Loadouts.** Every world whose name starts with `aurora_test` plays with an **experimental** loadout, every other world with the **main** one, and the swap happens by itself as you go between them. A build world keeps whichever one is in play. Everything Aurora keeps for a player is separate: inventory and ender chest, backpack, tool levels, level, XP, kills, blocks mined, Essence, playtime and rank, collections, gear unlocked from mobs, portal access, perks, cosmetics, bought, donor and staff ranks, the tutorial, the questline and the fishing rod. Builder status, the build inventory, suggestions, punishments and everything in the Gauntlet are shared. The first time in a test world starts from nothing with the starter kit; a main loadout that starts from nothing gets the tutorial too, like a first join. `/a loadout 1|2|3|experimental` switches an admin's loadout where they stand and keeps it in every world until `/a loadout auto` or they leave. Slot 1 is the main loadout (`main` works too); slots 2 and 3 are extra main-world loadouts for admins, each with its own progress, starting fresh with the tutorial, never on the leaderboards. `/a testworld [world]` goes to a test world (`aurora_test` without one) on the experimental loadout; the first time, with no experimental loadout yet, it starts as a copy of your main one instead of from nothing, after that the two go their own ways. Admin commands like `/a grant` and `/a reset` act on the loadout in play, so a reset in a test world only wipes the experimental one. Leaderboards only count main loadouts (slot 1), also while their player is in a test world. The first `deploy.py upload --db` with loadouts makes everything players did on the host so far their experimental loadout, and their main one starts fresh on their next join.

## Worlds

Extra void worlds, saved so they load on every startup.

| Command | Does |
|---|---|
| `/a world create <name>` | Create an empty void world, or load an existing folder. Lowercase letters, numbers and `_`. `overworld`, `the_nether` and `the_end` are taken by vanilla |
| `/a world list` | List the loaded worlds, click to teleport |
| `/a world tp <world> [player]` | Teleport to a world's spawn, the one from `/a setspawn` if set |
| `/a world remove <name>` | Stop loading it on startup. Its files are kept |
| `/a world build <world> <on\|off>` | Make it a build world, or a normal one again. See Build worlds below |

## Build worlds

Only builders and admins can go to a build world, by any teleport or portal, and there they build in creative without OP. On the way in their own inventory is put away and they get their build inventory, on the way out it's swapped back, so nothing made in creative reaches the game. Inside one, the game's menus (backpack, shops, trash), traders and ender chests don't open. A builder taken off the list while inside, or anyone not on it when a world becomes a build world, is sent to the main spawn.

| Command | Does |
|---|---|
| `/a builder add <player>` | Let a player go to build worlds and build there. They must have joined the server before |
| `/a builder remove <player>` | Take them off the list |
| `/a builder list` | The builders and the build worlds |

## PvP and safe zones

PvP is on everywhere except in safe zones, and in the Gauntlet's world only inside its PvP zone. A hit is stopped when either player stands in one, so nobody can hit from inside one. Dying to another player costs nothing: the inventory, levels and XP are all kept.

**No farming.** Kill the same player 3 times in a row and you can't hurt them for 5 minutes. Killing anyone else, or being killed by them, starts the count again.

| Command | Does |
|---|---|
| `/a safezone create <name>` | A safe zone from the wand selection |
| `/a safezone delete <name>` | Delete it, PvP is on there again |
| `/a safezone list` | Every safe zone and its corners |

## Floating text

Text that floats where you stand, facing whoever looks at it. It is saved with the world, so it goes wherever the world goes.

| Command | Does |
|---|---|
| `/a text add <text>` | Place it at your eye height. MiniMessage colours, e.g. `<red><b>PvP Zone`, and `\n` for a new line |
| `/a text remove` | Remove the nearest one within 8 blocks |
| `/a text list` | The ones within 8 blocks |

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

**Teleport pads.** A pad is a block players step on to unlock a way back to it. Stand on the block (a diamond block, so players spot it) and `/a pad create <pad>`. Stepping on it opens the access `pad_<pad>` for good, on the loadout in play, with a message. At spawn, select a spot with the wand and `/a pad point <pad>`: that makes the portal `pad_<pad>`, leading to the top of the pad and locked to its access, so it takes only players who have been there and knocks everyone else back with "Step on this teleport pad first". It is an ordinary portal, so the `/a portal` commands work on it.

| Command | What it does |
|---|---|
| `/a pad create <pad>` | The block you stand on becomes the pad. Lowercase letters, numbers and `_` |
| `/a pad point <pad>` | A portal from the wand selection to the pad, for players who unlocked it |
| `/a pad delete <pad>` | Delete the pad. Its portal stays, delete it with `/a portal delete pad_<pad>` |
| `/a pad list` | Every pad, where it is and its access |
| `/a pad tp <pad>` | Teleport onto it |

**Locked portals.** Players get access by right clicking a pass, in their backpack or their hand, which uses it up. Passes are in `scripts/data/resources_special.csv`, the `unlocks` column is the access they give, e.g. `gormak_key` (the Dunesworld Pass) gives `gormak`. Access is kept by the plugin in its own database (the `portal_access` table), not in vanilla scoreboard tags; `/a access` shows, gives and takes it, and `/a reset <player> all` takes it all away.

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

## Fishing

Fishing is a side quest apart from the zones: one spot a realm (`scripts/data/fishing/`), each with its own fish and rod upgrades. Mark a spot's water with the wand (select two corners around it) and `/a fishspot create <name> <spot>`; a spot can have as many areas as it needs, the hook has to land in one. Place the **Fish Merchant** (`fish_merchant`, hands out the rod and buys fish for Essence) at spawn and each spot's angler beside its water: `pond_angler`, `oasis_angler`, `rootwater_angler`, `cloud_angler`.

| Command | What it does |
|---|---|
| `/a fishspot create <name> <spot>` | The wand selection becomes water of that spot (`pond`, `oasis`, `rootwater`, `cloud_pool`) |
| `/a fishspot delete <name>` | Delete that area |
| `/a fishspot list` | Every area, its spot and where it is |

## Tutorial

A new player's first join hands out the starter tools, the backpack and a stack of bread, and starts the tutorial at the spot set with `setstart`, instead of the main spawn. A boss bar at the top shows the step, and a chat line says what to do: mine 10 cobblestone, open the backpack, click a trader, buy an upgrade. Each step ends by itself. Finishing pays 50 Essence and the `NEWCOMER` suffix. A player who leaves mid-way comes back to the start of the step they were on. Players who joined before it existed never get it.

**Origin Isles questline.** When the tutorial ends, finished or skipped, a second boss bar takes over and walks the player through Origin Isles: buy every upgrade at a zone (the bar counts them), then follow a trail of aqua dots to the next zone's trader, which ends when they come within 6 blocks of it or open its upgrades. After the Gold Reef the trail leads to the Dunesworld Gate. Each maxed zone pays 25 Essence; reaching the gate pays 100 and the `ISLANDER` suffix. Players already past the tutorial pick it up on their next join at the first Origin Isles zone they have not maxed; those who maxed them all never get it. The trail needs the trader placed in the player's world. `/a reset <player> all` starts it over after the tutorial; `/a tutorial start` does not, and a finished questline is never paid twice.

| Command | Does |
|---|---|
| `/a tutorial setstart` | New players start the tutorial where you stand |
| `/a tutorial tp` | Go to where it starts |
| `/a tutorial start [player]` | Put a player through it from the first step. `/a reset <player> all` does this too |
| `/a tutorial skip [player]` | Take a player out of it, no reward |

## Gauntlet

A tower mode in a world of its own (`scripts/data/gauntlet/`, see [PROGRESSION.md](PROGRESSION.md)). Walking into its world puts your inventory away and hands out your gauntlet gear; leaving gives it back. Make a void world for it (`/a world create gauntlet`), build a lobby, a PvP area and one arena, then:

1. `/a gauntlet lobby` where players arrive. Its world becomes the Gauntlet's world, never the main world.
2. `/a wand`, select the arena, `/a gauntlet arena`. It is saved as a template (`plugins/Aurora/gauntlet_arena.nbt`); every run pastes its own copy 1,000 blocks further along z, so keep that strip of the world empty. Saving it again replaces the template.
3. Inside the arena you saved: `/a gauntlet spawn` (where players start), `/a gauntlet mobspawn` a few times (where mobs come in), `/a gauntlet package` (where care packages land).
4. Select the PvP area with the wand, `/a gauntlet pvp`. Players can only hurt each other inside it, and never during a run. Players walk in; build it beside the lobby, or link them with a portal (`/a portal create` and `setdest`).
5. In the lobby, `/a trader create gauntlet_forge`: the Gauntlet Forge opens the forge, the same as Forge in the `/gauntlet` menu.
6. `/a gauntlet status` says what is missing.

| Command | Does |
|---|---|
| `/a gauntlet lobby` | Players arrive here; its world becomes the Gauntlet's |
| `/a gauntlet arena` | Save the wand selection as the arena template |
| `/a gauntlet spawn` | Where a run starts, inside the arena |
| `/a gauntlet mobspawn` | Add a mob spawn inside the arena |
| `/a gauntlet clearmobspawns` | Remove every mob spawn |
| `/a gauntlet package` | Where care packages land, inside the arena |
| `/a gauntlet pvp` | The wand selection is the PvP zone |
| `/a gauntlet status` | What is still missing |

To build in the Gauntlet's world, make it a build world (`/a world build`) while you work; it plays as the Gauntlet again once it is not. Dying anywhere in it costs nothing and you come back in the lobby. Main-game gear taken out there (from the Armory) is taken away, and the ender chest does not open.

## Staff and moderation

A staff rank is its permissions: grant it with the usual rank command and they apply at once (and follow loadouts). Junior Mod to Head Mod get `aurora.mod`; the Builder and Dev ranks get `aurora.builder` with Axiom (`axiom.*`) and WorldEdit (`worldedit.*`); Junior Admin and Admin get `aurora.admin`, which holds both. Helpers and Trainees get nothing extra. Ops have everything.

Builders may use the setup commands under `/a`: `mine`, `spawner`, `trader`, `portal`, `pad`, `fishspot`, `wand`, and `world list` / `world tp`. Everything else under `/a` stays with admins.

A muted player cannot chat, `/msg`, `/tell`, `/w`, `/r`, `/me`, `/say` or `/suggest`. A banned one is turned away at the login screen. Every mute, ban and kick is kept, ended or not, and staff online are told about each one.

| Command | Who | What it does |
|---|---|---|
| `/mute <player> [reason]` | mods | Each mute of the same player is longer: 15 minutes, 1 hour, 1 day, 3 days, then 1 week every time after, never longer |
| `/mute <player> <length\|perm> [reason]` | admins | Any length (`30m`, `2h`, `1d`, `1w`) or for good |
| `/unmute <player>` | mods | Lift the mute in force |
| `/kick <player> [reason]` | mods | Kick them off |
| `/history <player>` | mods | Every mute, ban and kick, in force, run out or lifted |
| `/ban <player> [length\|perm] [reason]` | admins | For good without a length |
| `/unban <player>` | admins | Lift the ban |

## Other admin commands

| Command | Does |
|---|---|
| `/a wand` | Wand: left click a block for corner 1, right click for corner 2, right click a trader to remove it |
| `/a setspawn [main] [<x> <y> <z> [yaw]]` | This world's spawn, used by `/spawn`, respawns and `/a world tp`. With `main` it is also the main spawn, where every player lands on join. Until a main spawn is set, players join in `aurora_overworld` |
| `/a heal [player]` | Restore health and hunger, put out fire |
| `/a give <player> <item> [amount]` | Give an item by registry id from the CSVs, e.g. `gold_pick_5`, or a vanilla one like `cooked_beef`. What doesn't fit drops at their feet |
| `/a access <player> [give\|take <access>]` | Where a player may go: on its own it lists what their passes opened, `give`/`take` open or close one by hand, e.g. `/a access Charky32 give sky` |
| `/a reset <player> <tools\|all>` | `tools` resets every tool level and hands back plain starter tools. `all` erases everything the player has and plays the first join again, starter kit, backpack and tutorial included: tools, backpack and inventory, level, XP, kills, blocks mined, Essence, playtime, perks, collections, gear unlocked from mobs, portals, cosmetics and every rank, staff and donor ranks and the second suffix included, and the questline starts over. Gauntlet progress is kept. It cannot be undone |
| `/a tool <player> give <tool>` | Hand over a tool that lives on the player (pick, axe, shovel, hoe, sword, helmet, chestplate, leggings, boots). Its item is drawn from the player's levels in the realm they stand in |
| `/a tool <player> set <tool> <stat> <level> [realm]` | Set what a player's tool has, in their current realm or a named one. The level is the total there, the realms it builds on included: `set pick fortune 10` in the overworld gives Fortune 10, whatever the regions add. The stat is an enchant, or `material` with a name such as `iron` |
| `/a tool <player> info <tool>` | What the tool has where the player stands, and which realms that builds on |
| `/a gear <player> <zone> [start\|end]` | Set every tool and armour piece to what a player has on reaching a zone, or on finishing it with `end`: each zone played before it (one region at a time, as in `before` in upgrades.yml) bought out in full. Every other tool level goes, and the tools are handed over. `start` without one |
| `/a cosmetic <player> <give\|take> <id>` | Give or take a rank, a cosmetic or `suffix_slot_2`. The only way to the donor and staff ranks, the donor-only cosmetics and the second suffix, which comes with a donor package. Tab completes every id |
| `/a grant <player> <essence\|xp\|hours> <amount>` | Give Essence, XP or hours of playtime, for testing cosmetics, levels and playtime ranks. Hours are counted within a minute, which is when a new playtime rank is handed out |
| `/a testworld [world]` | Go to a test world on the experimental loadout. The first time, with none yet, it starts as a copy of your main one. `aurora_test` without a world |
| `/a loadout [1\|2\|3\|experimental\|auto]` | Switch your loadout where you stand: slot 1 (main), 2, 3 or experimental. The one picked is played in every world until `auto` or you leave, instead of the one each world plays with; `/a testworld` drops the pick. On its own it shows which loadout is in play |
| `/a breaking [on\|off]` | Turn off breaking blocks for real: you mine like a player and break nothing outside a mine, until `on` or the server restarts. On its own it switches. Admins break for real by default, to build |
| `/a suggestions [page]` | What players sent with `/suggest`, newest first with who and when. Each one is also in the server log. Click a number to fill in the delete command |
| `/a suggestions delete <id>` | Delete one that has been dealt with |
| `/a help`, `/a info <command>` | Command list, one command in detail |
| `/refresh` | Reload all game data from the YAML, after `python scripts/generate.py --live` |
| `/dump [items\|icons\|all\|clear]` | Shulkers of every registry item or icon, for checking how they look. `clear` empties your own backpack and inventory |
