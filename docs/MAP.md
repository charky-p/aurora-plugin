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
- **Containers and signs** come across with their contents. Player data and advancements do not.
- **Memory.** A paste is held in memory whole. `scripts/server.sh` gives the server 6G (`AURORA_MEMORY=8G scripts/server.sh start` for more); with 2G a paste of 15 million blocks ran out and froze the server.
- **Back it up** once it is in: `scripts/server.sh backup`.

## 2. Set Aurora up on it

None of Aurora's setup is part of the build. Mines, spawners, traders, portals and spawns live in Aurora's database, so after the paste they are placed again by command. Do it in this order, one region at a time.

1. **Spawn.** Stand where players should arrive. `/a setspawn main` in the world players join in, which makes it the main spawn, and `/a setspawn` in every other world.
2. **Mines.** For each zone, select the mine with `/a wand` (left and right click two corners), then:
   - `/a mine wandcreate <name>`
   - `/a mine setblocks <name> <block> <%> ...` with the mix below
   - `/a mine settp <name> <x> <y> <z>`, where players stand when it resets
   The price model assumes mines of about 9 x 9 and 5 deep. A mine much smaller makes the zone slower than its time target, as players run out of blocks between resets.
3. **Spawners.** Stand where each mob should spawn, `/a spawner create <name> ~ ~ ~ <mob> [limit] [radius] [delay]`. Hostile zones are better fenced so mobs stay in them.
4. **Traders.** Stand where each should be, facing the way players come from, `/a trader create <trader>`.
5. **Portals and gates.** Select the portal box, `/a portal create <name>`, stand at the arrival point, `/a portal setdest <name>`. Make one for the way back. Lock the Gormak gate with `/a portal settag <name> gormak` and put the Gate Warden beside it.
6. **Tutorial and questline.** By the Cobblestone Quarry, facing the mine, `/a tutorial setstart`. New players begin there. Test it with `/a tutorial start`. The questline after it needs nothing of its own: its trail leads to each Origin Isles zone's trader and then `gate_trader`, so those have to stand in the same world.
7. **Fishing.** One spot a realm: select its water with the wand, `/a fishspot create <name> <spot>`, and place its angler beside it (`pond_angler`, `oasis_angler`, `rootwater_angler`, `cloud_angler`). See [COMMANDS.md](COMMANDS.md).
8. **Warps.** A region's world needs a row in `scripts/data/world/warps.csv` (Elderwood and the Sky World have none until their worlds are built) and its world in `realms.csv`, then `python scripts/generate.py --live` and `/refresh`.
9. **Back up:** `scripts/server.sh backup` (local) or `python scripts/deploy.py backup <server>` (live).

## 3. What each zone needs

Generated from the data: mine block mixes from `scripts/data/world/mines.csv`, mob ids from `scripts/data/mobs/`, trader ids from `scripts/data/shops/traders.csv`. A boss rises at a spawner of its own, summoned by its key. Mine names are free; the test world uses the mine id.

### Origin Isles

| Zone | Mines (`setblocks`) | Spawner mobs | Trader |
|---|---|---|---|
| Spawn | - | - | `starter_trader`, `food_trader`, `fish_merchant` |
| Cobblestone Quarry | `cobblestone`: `cobblestone 60 stone 40` | - | `cobble_trader` |
| Coal Seam | `coal`: `coal_ore 60 coal_block 40` | - | `coal_trader` |
| Copper Vein | `copper`: `copper_ore 60 copper_block 40` | `chicken` | `copper_trader` |
| Iron Deposit | `iron`: `iron_ore 60 iron_block 40` | `sheep` | `iron_trader` |
| Gold Reef | `gold`: `gold_ore 60 gold_block 40` | `cow` | `gold_trader` |

At the end, the portal to Gormak locked to `gormak`, with `gate_trader` beside it.

### Gormak

| Zone | Mines (`setblocks`) | Spawner mobs | Trader |
|---|---|---|---|
| Abandoned City | `carved_brick`: `carved_brick 80 cracked_brick 20` | `hollowed_serf`, `scavenger_husk` | `city_trader` |
| The Dunes | `sunbaked_sand`: `sunbaked_sand 80 cut_sunbaked_sand 20` | `dune_scout`, `sand_stalker` | `dunes_trader` |
| Sun Temple | `raw_gold`: `raw_gold_block 20 gilded_stone 80` | `tomb_guard`, `vizier_mage` | `temple_trader` |
| Royal Tomb | - | `gormak_guardian`, `gormak_eternal` (boss spawner; it summons the minions) | `tomb_trader` |

### Verdant Groves

| Zone | Mines (`setblocks`) | Spawner mobs | Trader |
|---|---|---|---|
| Oak Grove | `oak_log`: `oak_log 60 oak_wood 25 stripped_oak_log 15`, `tin`: `tin_ore 60 tin_rich 40` | `grove_horse` | `oak_trader` |
| Birch Hollow | `birch_log`: `birch_log 60 birch_wood 25 stripped_birch_log 15`, `bronze`: `bronze_ore 60 bronze_rich 40` | `hollow_fox` | `birch_trader` |
| Spruce Ridge | `spruce_log`: `spruce_log 60 spruce_wood 25 stripped_spruce_log 15`, `lead`: `lead_ore 60 lead_rich 40` | `ridge_wolf` | `spruce_trader` |
| Dark Oak Thicket | `dark_oak_log`: `dark_oak_log 60 darkoak_wood 25 stripped_dark_oak_log 15`, `silver`: `silver_ore 60 silver_rich 40` | `thicket_sniffer` | `darkoak_trader` |

At the end, the portal to Elderwood Roots locked to `elderwood`, with `elderwood_gate_trader` beside it.

### Khorvath

| Zone | Mines (`setblocks`) | Spawner mobs | Trader |
|---|---|---|---|
| Howling Canyons | `crimson_sand`: `crimson_sand 80 cut_crimson_sand 20`; `canyon_sand` (shovel): `red_sand 80 sand 20` | `scarab_zealot`, `hollowed_legionnaire` | `canyons_trader` |
| Ancient Ruins | `relic_stone`: `relic_stone 70 sunfired_tile 30` | `scarab_zealot`, `relic_warrior` | `ruins_trader` |
| Catacombs | `ossuary_bone`: `ossuary_bone 70 tomb_brick 30` | `royal_myrmidon`, `pharaoh` | `catacombs_trader` |
| Sanctuary of the Sun | - | `anubite_executioner`, `sphinx`, `khorvath` (boss spawner) | `sanctuary_trader` |

### Elderwood Roots

| Zone | Mines (`setblocks`) | Spawner mobs | Trader |
|---|---|---|---|
| Jungle Floor | `jungle_log`: `jungle_log 60 stripped_jungle_log 40` | `jungle_panda` | `jungle_trader` |
| Bamboo Thicket | `bamboo`: `bamboo_block 60 bamboo_mosaic 40` | `jungle_ocelot` | `bamboo_trader` |
| Amber Hollow | `elderbark`: `elderbark 100`, `amber`: `amber 100` | `wild_bee` | `amber_trader` |
| Treant Grove | - | `tree_zombee`, `treant` | `treehouse_trader` |

### Sunken Marsh

| Zone | Mines (`setblocks`) | Spawner mobs | Trader |
|---|---|---|---|
| Mudflats | `packed_mud`: `packed_mud 60 mud_brick 40`, `mud`: `mud 60 muddy_roots 40` | `bog_frog` | `mudflats_trader` |
| Slime Hollow | `bog_slime`: `slime_block 100`, `mangrove_log`: `mangrove_roots 80 mangrove_wood 20` | `swamp_slime` | `slime_trader` |
| The Fens | `bog_stone`: `mossy_cobble 60 mossy_bricks 40`, `fen_soil`: `fen_soil 100`, `moss`: `bog_moss 100` | `bog_walker` | `fens_trader` |
| Sunken Ruins | `verdigris`: `verdigris 60 verdigris_cut 40`, `mangrove_leaves`: `mangrove_leaves 100`, `mushroom_stem`: `mushroom_stem 100` | `bogged_archer` | `sunken_trader` |
| Glowmire | `ochre_froglight`: `ochre_froglight 100`, `verdant_froglight`: `verdant_froglight 100`, `pearlescent_froglight`: `pearlescent_froglight 100`, `red_mushroom`: `red_mushroom 100`, `brown_mushroom`: `brown_mushroom 100` | `glow_wisp`, `marsh_hydra` (boss spawner) | `glowmire_trader` |

### Emerald Peaks

| Zone | Mines (`setblocks`) | Spawner mobs | Trader |
|---|---|---|---|
| Foothills | `dirt`: `mountain_dirt 80 coarse_mountain_dirt 20`, `pumpkin`: `pumpkin 80 jack_o_lantern 20` | `mountain_pig` | `foothills_trader` |
| Stony Crags | `melon`: `melon 100`, `calcite`: `calcite 80 dripstone 20` | `mountain_goat` | `crags_trader` |
| Frozen Slopes | `snow`: `snow_block 100`, `packed_ice`: `packed_ice 80 blue_ice 20` | `peak_stray` | `slopes_trader` |
| Redstone Grotto | `redstone`: `redstone_ore 80 redstone_block 20` | `frost_spider` | `grotto_trader` |
| Emerald Depths | `emerald`: `emerald_ore 80 deepslate_emerald_ore 20`, `lapis`: `lapis_ore 80 lapis_block 20` | `cave_spider` | `depths_trader` |
| Arachne Cave | - | `cave_skeleton`, `arachne` (boss spawner, she wakes with `arachne_rider` on her back) | `arachne_trader` |

At the end, the portal to Cloud Isles locked to `sky`, with `sky_gate_trader` beside it.

### The Duat

| Zone | Mines (`setblocks`) | Spawner mobs | Trader |
|---|---|---|---|
| Ashen River | `basalt`: `ashen_basalt 60 polished_ashen_basalt 40`, `ash`: `river_ash 100` | `anubis`, `sobek`, `osiris` | `ashen_trader` |
| Hall of Judgement | `scale_gold`: `judgement_brick 60 scale_gold 40` | `thoth`, `maat`, `horus` | `judgement_trader` |
| Solar Forge | `solar_slag`: `forge_magma 70 forge_obsidian 30` | `sekhmet`, `ptah`, `khnum` | `forge_trader` |
| Throne of Night | `nightstone`: `nightstone 100` | `set`, `apep`, `nephthys`, `sol` (boss spawner) | `throne_trader` |

### Elderwood Heart

| Zone | Mines (`setblocks`) | Spawner mobs | Trader |
|---|---|---|---|
| Root Cellar | `heartwood_log`: `heartwood_log 70 root_knot 30` | `sapling_zombee` | `cellar_trader` |
| Vine Halls | `elder_vine`: `vine_stem 60 vine_cap 40` | `elder_zombee`, `canopy_spider` | `vine_trader` |
| Hive Chambers | `wax_comb`: `wax_comb 100`, `beeswax`: `beeswax 100` | `hive_guard` | `hive_trader` |
| Upper Canopy | - | `treehouse_sentry`, `canopy_treant` | `canopy_trader` |
| The Crown | - | `canopy_treant`, `elder_treant` (boss spawner) | `crown_trader` |

### Cloud Isles

| Zone | Mines (`setblocks`) | Spawner mobs | Trader |
|---|---|---|---|
| Cloudbank | `cloudstone`: `cloudstone 100`, `cloud_dust`: `cloud_dust 100` | `sky_sheep` | `cloudbank_trader` |
| Skyshard Spires | `skyshard`: `skyshard 60 sky_quartz 40` | `sky_phantom` | `spires_trader` |
| Storm Front | `storm_copper`: `storm_copper 60 storm_grate 40` | `storm_wraith` | `storm_trader` |
| Sky Harbour | - | `sky_pirate`, `pirate_captain` | `harbour_trader` |

### Sky Kingdom

| Zone | Mines (`setblocks`) | Spawner mobs | Trader |
|---|---|---|---|
| Outer Walls | `royal_quartz`: `royal_quartz 60 royal_pillar 40` | `royal_guard` | `walls_trader` |
| Gilded Gardens | `cherry_log`: `cherry_log 100`, `sunpetal`: `sunpetal 100` | `garden_bee`, `gardener` | `gardens_trader` |
| Royal Treasury | `celestine`: `celestine 100`, `aurium`: `aurium 100` | `treasury_knight`, `crossbowman` | `treasury_trader` |
| Throne Room | - | `throne_guard`, `sky_king` (boss spawner) | `throne_room_trader` |

### Ironbound Quarry

| Zone | Mines (`setblocks`) | Spawner mobs | Trader |
|---|---|---|---|
| Gravel Pits | `gravel`: `gravel 100`, `tungsten`: `tungsten_ore 70 tungsten_rich 30` | `quarry_pillager` | `gravel_trader` |
| Pale Woods | `pale_log`: `pale_log 70 stripped_pale_log 30`, `platinum`: `platinum 100` | `quarry_vindicator` | `pale_trader` |
| Maple Grove | `maple`: `maple_log 70 stripped_maple_log 30`, `clay`: `clay 100`, `titanium`: `titanium 100` | `quarry_pillager`, `quarry_vindicator` | `maple_trader` |
| Diamond Depths | `diamond`: `diamond_ore 70 deepslate_diamond_ore 30` | `quarry_ravager` | `diamond_trader` |
| The Quarry Pit | - | `pillager_captain`, `quarry_warden` | `pit_trader` |
| Golem Rest | - | `overworld_golem` | `golem_trader`, `core_trader`, `essence_trader` |

### The Gauntlet

A world of its own, not part of any region: a lobby with the `gauntlet_forge` trader and a start portal, a PvP area beside it, a duel arena (two spawns facing each other), and one arena with room for a few mob spawns and a care package spot. Keep 1,000 blocks along z from the arena, and along x from the duel arena, empty: every run and every duel pastes its copy there. The steps are under Gauntlet in [COMMANDS.md](COMMANDS.md).

## 4. Check it

- Walk the whole route as a new player: `/a reset <you> all`, then play from spawn.
- `/a mine list`, `/a spawner list`, `/a trader list` and `/a portal list` show everything placed; compare with the tables above.
- Watch `LocalServer/logs/latest.log` for warnings from Aurora.
