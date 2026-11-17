# Moving a Map In and Setting It Up

How to bring a build from another server (Minehut, a creative server, a singleplayer world) into the local server, and turn it into a playable region. The command reference is [COMMANDS.md](COMMANDS.md).

## 1. Bring the build over

### Option A: download the whole world

If the host's panel can download the world (Minehut: look in the dashboard for a world download or backup), do that. It keeps everything: the build, chests, signs, entities and biomes.

1. Stop the local server: `scripts/server.sh stop`.
2. Unzip the world into `LocalServer/` as its own folder, e.g. `LocalServer/aurora_origin`. Delete `uid.dat` inside it if there is one, or the server refuses to load a copy of a world it already knows.
3. Start the server and run `/a world create aurora_origin`. With the folder already there, this loads it instead of making a void world, and from then on it loads on every startup.

### Option B: copy it with WorldEdit

Use this when the host gives no world download. The host needs WorldEdit (or FastAsyncWorldEdit) and a file manager; the local server already has WorldEdit.

**On the host:**

1. Stand at an anchor point you can find again: a corner block or the centre of spawn, at a known Y. The paste lands relative to where you stood when you copied, so note your coordinates (F3).
2. `//wand`, then left click one corner and right click the opposite one. Cover the whole build from its lowest block to its highest. Or stand at each corner and use `//pos1` and `//pos2`.
3. `//copy -e -b`. `-e` brings entities (item frames, armour stands, paintings), `-b` brings biomes.
4. `//schem save aurora_origin`
5. Download `plugins/WorldEdit/schematics/aurora_origin.schem` with the host's file manager.

**Locally:**

1. Put the file in `LocalServer/plugins/WorldEdit/schematics/`.
2. `/a world create aurora_origin`, then `/a world tp aurora_origin`. Names are lowercase letters, numbers and `_`. `overworld`, `the_nether` and `the_end` are taken by vanilla, so use something like `aurora_overworld`.
3. Go to the same coordinates as your anchor on the host, so the build keeps the same coordinates in both places.
4. `//schem load aurora_origin`, then `//paste -e -b`. `//undo` takes it back if it lands wrong.

**Things to know:**

- **Big builds.** A very large selection can lag the host or hit its limits. Copy it in parts: one schematic per island or zone, each with its own anchor written down.
- **Versions.** A build from an older Minecraft version pastes fine, but check doors, stairs, redstone and anything with a facing.
- **Containers and signs** come across with their contents. Player data, advancements and scoreboard tags do not.
- **Memory.** A paste is held in memory whole. `scripts/server.sh` gives the server 6G (`AURORA_MEMORY=8G scripts/server.sh start` for more); with 2G a paste of 15 million blocks ran out and froze the server.
- **Back it up** once it is in: `scripts/server.sh backup`.

## 2. Set Aurora up on it

None of Aurora's setup is part of the build. Mines, spawners, traders, portals and spawns live in Aurora's database, so after the paste they are placed again by command. Do it in this order, one region at a time.

1. **Spawn.** Stand where players should arrive. `/a setspawn main` in the world players join in, which makes it the main spawn, and `/a setspawn` in every other world.
2. **Mines.** For each zone, select the mine with `/a wand` (left and right click two corners), then:
   - `/a mine wandcreate <name>`
   - `/a mine setblocks <name> <block> <%> ...` with the mix below
   - `/a mine settp <name> <x> <y> <z>`, where players stand when it resets
   The test world's mines are 9 x 9 and 5 deep. A mine much smaller makes the zone slower than its time target, as players run out of blocks between resets.
3. **Spawners.** Stand where each mob should spawn, `/a spawner create <name> ~ ~ ~ <mob> [limit] [radius] [delay]`. Hostile zones are better fenced so mobs stay in them.
4. **Traders.** Stand where each should be, facing the way players come from, `/a trader create <trader>`.
5. **Portals and gates.** Select the portal box, `/a portal create <name>`, stand at the arrival point, `/a portal setdest <name>`. Make one for the way back. Lock the Gormak gate with `/a portal settag <name> gormak` and put the Gate Warden beside it.
6. **Warps.** The world column of `scripts/data/world/warps.csv` still names the test worlds. Change it to the new world, then `python scripts/generate.py --live` and `/refresh`.
7. **Back up:** `scripts/server.sh backup` (local) or `python scripts/deploy.py backup <server>` (live).

The test world names everything `t_...`. Use plain names on the real map, so `--teardown` of the test world never touches it.

## 3. What each zone needs

Mine names are free; the block ids are from `scripts/data/world/blocks.csv`, mob ids from `scripts/data/mobs/`, trader ids from `scripts/data/shops/traders.csv`.

### Origin Isles

| Zone | Mine blocks | Mobs | Trader |
|---|---|---|---|
| Spawn | - | - | `starter_trader`, `food_trader` |
| Cobblestone Quarry | `cobblestone 60 stone 40` | - | `cobble_trader` |
| Coal Seam | `coal_ore 60 coal_block 40` | - | `coal_trader` |
| Copper Vein | `copper_ore 60 copper_block 40` | `chicken` | `copper_trader` |
| Iron Deposit | `iron_ore 60 iron_block 40` | `sheep` | `iron_trader` |
| Gold Reef | `gold_ore 60 gold_block 40` | `cow` | `gold_trader`, and `gate_trader` at the Gormak gate |

### Gormak

| Zone | Mine blocks | Mobs | Trader |
|---|---|---|---|
| Abandoned City | `carved_brick 90 cracked_brick 10` | `hollowed_serf`, `scavenger_husk` | `city_trader` |
| The Dunes | `sunbaked_sand 80 cut_sunbaked_sand 20` | `dune_scout`, `sand_stalker` | `dunes_trader` |
| Howling Canyons | `crimson_sand 80 cut_crimson_sand 20` | `scarab_zealot`, `hollowed_legionnaire` | `canyons_trader` |
| Sun Temple | `raw_gold_block 20 gilded_stone 80` | `tomb_guard`, `vizier_mage` | `temple_trader` |
| Royal Tomb | - | `royal_myrmidon`, `pharaoh`, and a spawner for `gormak_eternal` where the boss should rise | `tomb_trader` |

### Verdant Groves

Each zone has two mines, the wood and the ore.

| Zone | Mine blocks | Mobs | Trader |
|---|---|---|---|
| Oak Grove | `oak_log 60 oak_wood 25 stripped_oak_log 15`, `tin_ore 60 tin_rich 40` | `grove_horse` | `oak_trader` |
| Birch Hollow | `birch_log 60 birch_wood 25 stripped_birch_log 15`, `bronze_ore 60 bronze_rich 40` | `hollow_fox` | `birch_trader` |
| Spruce Ridge | `spruce_log 60 spruce_wood 25 stripped_spruce_log 15`, `lead_ore 60 lead_rich 40` | `ridge_wolf` | `spruce_trader` |
| Dark Oak Thicket | `dark_oak_log 60 darkoak_wood 25 stripped_dark_oak_log 15`, `silver_ore 60 silver_rich 40` | `thicket_sniffer` | `darkoak_trader` |
| Great Treehouse | - | `tree_zombee`, `treant` | `treehouse_trader` |

If a zone's mines or mobs change in `zones.csv`, this table goes with it.

## 4. Check it

- Walk the whole route as a new player: `/a reset <you> all`, then play from spawn.
- `/a mine list`, `/a spawner list`, `/a trader list` and `/a portal list` show everything placed; compare with the tables above.
- Watch `LocalServer/logs/latest.log` for warnings from Aurora.
