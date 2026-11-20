# Aurora progression

How a player moves through the game as it is built today, and the rules behind it. The numbers come from the CSVs under `scripts/data/`; when they change, the table in section 3 is the part to regenerate (`python scripts/balance/price_zones.py`).

Ideas for later regions are in [DESIGN.md](../DESIGN.md), not here.

## 1. The loop

1. **Mine.** Blocks in a zone's mine drop its resource straight into the backpack.
2. **Upgrade.** The zone's trader sells upgrade steps for the player's tools, paid in that zone's resources. Tools are never replaced: the pick, axe, sword, shield and four armour pieces are levels on the player, and the items are redrawn from them.
3. **Fight.** From the Copper Vein on, some steps cost what the zone's mobs drop.
4. **Move on.** When a zone's steps are bought the next zone's blocks and mobs are in reach.

A zone is one mine (or two), one or two mobs, one trader and a list of steps in `upgrades.csv`. Buying every step of a zone unlocks its suffix; every zone of a region unlocks the region's suffix.

## 2. The rules

**Fortune and density.** With `L = fortune - density`, a block drops nothing if `L < 0`, otherwise `round(base x (1 + 0.75 L + 0.25 L^2))`, at least 1. Density is what gates a mine: the pick needs Fortune equal to it to get anything.

**Looting and mob density.** Looting is Fortune for mobs. Every mob has a density: with `L = looting - density`, it drops none of its resources while `L < 0`, then the same curve as Fortune. Drops under 100% grow with the cube root of that multiplier. Gear and keys always drop at their own chance. Killing a mob with too little Looting says "Requires Looting N".

**The right tool.** Wood, pumpkins, melons, bamboo and bee nests are mined with the axe; dirt, snow, gravel, clay, soul soil and concrete powder with the shovel, which is first sold at Emerald Peaks; everything else with the pick. The wrong tool breaks nothing. Until a player owns an axe or a shovel, a bare hand mines that tool's blocks, so the oak logs and dirt that buy them can be punched.

**Efficiency and Blast.** Efficiency is vanilla dig speed. Blast breaks extra blocks per swing: 2x1, 2x2, 3x3, then 3x3 two deep and 3x3x3 at level 5.

**Luck and Essence.** Every block mined and mob killed has a 0.1% chance to pay Essence: 1 to 3, times `1 + Luck`. Luck never changes the chance and never touches resources. On the sword it also raises the chance of a mob's gear drop.

**Player damage.** A hit is `10 + Sharpness bonus - mob defence`. Sharpness gives 1 a level for levels 1 to 5, 3 for 6 to 10, 5 for 11 to 15, 10 for 16 to 20 and so on. A spammed swing does 20%, a fully charged one (625 ms) 100%, a jump crit 1.5x. A mace hit while falling smashes: `1 + 0.2` a block fallen, at most 3x, on top of a crit. Bloodlust adds 5% a level for each of up to 3 stacks, a stack per kill, lasting 6 seconds.

**Mob damage.** Mobs hit in hearts, set by the gap between the mob's attack level and the player's average Protection, four levels to a tier: 1 heart at the same tier, rising like a log above it (2, 2.9, 3.7 ...), falling fast below it to a floor of a quarter heart. Every hit is then multiplied by 0.6, arrows by a further 0.5, and by the mob's own `damage` column (1.5 for every Gormak mob).

**Shield.** A raised shield stops hits up to an attack level set by Deflection; hits two tiers above that break through and put it on cooldown. Defence counts as extra Protection whether the shield is raised or not.

**Thorns and Sustenance.** Thorns returns a fixed 20 damage per average level. Sustenance heals over time.

**Levels.** XP for a block is the items it dropped times the block's `xp`; a kill gives the mob's health. A level costs `1.35^level` XP up to level 15 and grows by 1.1 a level after that. Levels and collection tiers pay Essence.

**Realms and stacking.** Levels are kept per realm. The Overworld realm is Origin; every other region is a realm that is part of it, counting its own levels from 1. A tool has the sum of them all, so nothing bought is ever lost by changing region.

**Upgrade order.** A tool's steps in a zone are bought one at a time, in order, and only once every step for that tool is bought in the zones played before it: the earlier zones of its own region and every zone of the regions leading there (`after`, or `portal`, in `regions.csv`). Each tool goes on its own, so a pick can be ahead of the sword. Regions side by side never wait on each other: Gormak and Verdant Groves both need Origin Isles only, and Khorvath needs Gormak but not Verdant Groves. The shop names the zone still owed. A released zone never waits on one not released yet; in a zone not released yet, which only admins reach, every zone before it counts.

**What a trader sells.** Every zone sells at least one step for the pick, the sword and each armour piece. A gathering zone (Origin Isles, Verdant Groves, Elderwood Roots, Sunken Marsh, Emerald Peaks, Cloud Isles, the Ironbound Quarry) sells 5 steps for each tool its mines need; a fighting zone (Gormak, Khorvath, the Duat, Elderwood Heart, the Sky Kingdom, and any zone with no mine, like the boss zones) sells 5 for the sword and 3 for each armour piece and the shield. The mace, where it is sold, counts as a sword. Passive mobs do not make a zone a fighting one. **First purchases.** A tool beyond the starter set is bought once before it can be upgraded: the axe at the Oak Grove (16 oak logs), the shovel at the Foothills (32 dirt), the shield at the Abandoned City, the mace at Cloudbank and the grapple at the Dark Oak Thicket (200 dark oak logs, 5,000 silver).

**Movement.** The grapple, a fishing rod bought from the Dark Oak trader at the end of Verdant Groves, throws you the way you look, on the ground or in the air. It starts at half the throw it once had and a 10 second cooldown; every zone after Verdant Groves sells a level of Thrust (a fiftieth more power each) and Recharge (a fifth of a second off the cooldown each, never under 1.5 seconds); it climbs Khorvath's cliff. Chained in the air it can also cross a void gap. The elytra and rockets, sold at the end of Emerald Peaks, cross the 40 block gaps to the Duat and the Ironbound Quarry and fly up the 20 blocks to Elderwood Heart.

## 3. The ladder

Every region has a stage. Regions of one stage are played side by side, each with its own focus, and their levels add up:

| Stage | Regions |
|---|---|
| 1 | Origin Isles |
| 2 | Gormak (fighting), Verdant Groves (gathering, the grapple at Dark Oak, the Elderwood Pass at its gate) |
| 3 | Khorvath (fighting, up a cliff), Elderwood Roots (gathering, the axe), Sunken Marsh (the pick and a little of everything) |
| 4 | Emerald Peaks (gathering, the shovel; sells the elytra, rockets and the Sky Pass) |
| 5 | The Duat (twelve gods, across a gap), Elderwood Heart (fighting, up the tree), Cloud Isles (gathering, the mace) |
| 6 | Sky Kingdom (fighting), Ironbound Quarry (everything, across a gap) |
| 7 | Golem's Rest, then the Overworld Core |

A zone is priced for a player who has finished every region of every earlier stage, and gone as far through the other regions of its own stage as through its own. Rushing one region is slower, never closed. Minutes below are real play; the model works in its own seconds, `pace` times as many (see Tuning).

| Zone | Minutes | Mines | Mobs | Steps | Total cost |
|---|---|---|---|---|---|
| **Origin Isles** (stage 1) | | | | | |
| Cobblestone Quarry | 1.6 | cobblestone | - | 10 | 160 cobblestone |
| Coal Seam | 1.6 | coal | - | 10 | 870 coal |
| Copper Vein | 2.1 | copper | chicken | 10 | 910 copper, 6 feather |
| Iron Deposit | 3.5 | iron | sheep | 10 | 2,010 iron, 3 wool |
| Gold Reef | 5.5 | gold | cow | 10 | 4,100 gold, 3 leather |
| **Gormak** (stage 2) | | | | | |
| Abandoned City | 4.9 | carved brick | hollowed serf, scavenger husk | 22 | 14 ancient scrap, 270 carved brick, 8 tattered cloth |
| The Dunes | 4.9 | sunbaked sand | dune scout, sand stalker | 21 | 13 ancient flesh, 8 ancient bone, 900 sunbaked sand |
| Sun Temple | 4.9 | raw gold | tomb guard, vizier mage | 21 | 13 ancient nugget, 8 vizier scroll, 2,800 raw gold |
| Royal Tomb | 8.2 | - | gormak minion, gormak guardian, gormak eternal (boss) | 21 | 11 tomb dust, 11 guardian sigil, 1 ancient nugget |
| **Verdant Groves** (stage 2) | | | | | |
| Oak Grove | 4.9 | oak log, tin | grove horse | 16 | 184 oak log, 8,040 tin, 8 horsehair |
| Birch Hollow | 4.9 | birch log, bronze | hollow fox | 15 | 832 birch log, 5,250 bronze, 8 fox fur |
| Spruce Ridge | 4.9 | spruce log, lead | ridge wolf | 15 | 1,244 spruce log, 15,700 lead, 7 wolf pelt |
| Dark Oak Thicket | 4.9 | dark oak log, silver | thicket sniffer | 16 | 1,720 dark oak log, 28,000 silver, 7 ancient seed |
| **Khorvath** (stage 3) | | | | | |
| Howling Canyons | 8.0 | crimson sand | scarab zealot, hollowed legionnaire | 23 | 27,800 crimson sand, 12 deserter coin, 4 legion plate |
| Ancient Ruins | 10.0 | relic stone | scarab zealot, relic warrior | 23 | 296 scarab shell, 11 relic shard, 980 relic stone |
| Catacombs | 10.0 | ossuary bone | royal myrmidon, pharaoh | 23 | 16 eternal soul, 11 royal seal, 980 ossuary bone |
| Sanctuary of the Sun | 12.0 | - | anubite executioner, sphinx, khorvath (boss) | 23 | 17 anubite mark, 11 sphinx claw |
| **Elderwood Roots** (stage 3) | | | | | |
| Jungle Floor | 10.0 | jungle log | jungle panda | 13 | 7,190 jungle log, 9 panda fur |
| Bamboo Thicket | 10.0 | bamboo | jungle ocelot | 13 | 7,390 bamboo, 9 ocelot whisker |
| Amber Hollow | 10.0 | elderbark, amber | wild bee | 13 | 3,710 elderbark, 620 amber, 9 royal jelly |
| Treant Grove | 10.0 | - | tree zombee, treant | 23 | 23 tree sap, 32 ancient seed |
| **Sunken Marsh** (stage 3) | | | | | |
| Mudflats | 8.0 | packed mud | bog frog | 12 | 16,000 packed mud, 8 frog leg |
| Peat Bog | 10.0 | peat | bog walker | 12 | 14,620 peat, 8 bog rag |
| Sunken Ruins | 10.0 | verdigris | bogged archer, sunken drowned | 12 | 7 poison quill, 18,600 verdigris |
| Hydra Pool | 12.0 | - | marsh lurker, hydra (boss) | 23 | 23 lurker fang |
| **Emerald Peaks** (stage 4) | | | | | |
| Foothills | 8.0 | dirt | mountain goat | 14 | 2,630 dirt, 1 cobblestone, 9 goat hide |
| Redstone Mines | 10.0 | redstone | frost spider | 12 | 61,700 redstone, 8 spider silk |
| Pumpkin Terraces | 10.0 | pumpkin, snow | snow fox | 18 | 638 snow, 2,113 pumpkin, 9 snow pelt |
| Lapis Ridge | 10.0 | lapis | peak stray | 12 | 54,600 lapis, 550 snow, 8 frost arrow, 300 pumpkin |
| Melon Ledges | 10.0 | melon, emerald | cliff goat | 17 | 48,900 emerald, 1,446 melon, 8 cliff horn, 600 dirt |
| Arachne Cave | 12.0 | - | cave spider, spider rider, arachne (boss) | 23 | 17 venom sac, 11 rider bone |
| **The Duat** (stage 5) | | | | | |
| Ashen River | 13.0 | basalt, ash | anubis, sobek, osiris | 23 | 35,500 basalt, 6 anubis sigil, 8 sobek sigil, 3 osiris sigil, 48 ash |
| Hall of Judgement | 15.0 | scale gold | thoth, maat, horus | 23 | 13 thoth sigil, 12 maat sigil, 7 horus sigil, 8,800 scale gold |
| Solar Forge | 15.0 | solar slag | sekhmet, ptah, khnum | 23 | 13 sekhmet sigil, 12 ptah sigil, 7 khnum sigil, 7,200 solar slag |
| Throne of Night | 17.0 | nightstone | set, apep, nephthys, sol (boss) | 23 | 13 set sigil, 12 apep sigil, 7 nephthys sigil, 8,800 nightstone |
| **Elderwood Heart** (stage 5) | | | | | |
| Root Cellar | 12.0 | heartwood log | sapling zombee | 23 | 23 sapling twig, 120 heartwood log |
| Vine Halls | 12.0 | elder vine | elder zombee, canopy spider | 23 | 17 canopy silk, 11 elder vine |
| Hive Chambers | 12.0 | wax comb, beeswax | hive guard | 23 | 22 hive stinger, 7,200 beeswax |
| Upper Canopy | 12.0 | - | treehouse sentry, canopy treant | 23 | 17 sentry arrowhead, 11 canopy bark |
| The Crown | 12.0 | - | canopy treant, elder treant (boss) | 23 | 74 canopy bark |
| **Cloud Isles** (stage 5) | | | | | |
| Cloudbank | 12.0 | cloudstone, cloud dust | sky sheep | 19 | 10 cloud fleece, 46,100 cloudstone, 3,270 cloud dust |
| Skyshard Spires | 15.0 | skyshard | sky phantom | 13 | 82,300 skyshard, 9 phantom veil |
| Storm Front | 15.0 | storm copper | storm wraith | 13 | 82,300 storm copper, 9 storm essence |
| Sky Harbour | 18.0 | - | sky pirate, pirate captain | 28 | 22 pirate doubloon, 16 captain compass |
| **Sky Kingdom** (stage 6) | | | | | |
| Outer Walls | 20.0 | royal quartz | royal guard | 23 | 22 guard insignia, 13,900 royal quartz |
| Gilded Gardens | 20.0 | cherry log, sunpetal | garden bee, gardener | 23 | 16 golden pollen, 11 gardener seed, 12,400 sunpetal |
| Royal Treasury | 22.0 | celestine, aurium | treasury knight, crossbowman | 23 | 16 knight crest, 11 crossbow bolt, 31,000 celestine |
| Throne Room | 28.0 | - | throne guard, sky king (boss) | 23 | 23 throne seal |
| **Ironbound Quarry** (stage 6) | | | | | |
| Gravel Pits | 18.0 | gravel, tungsten | quarry pillager | 17 | 77,200 tungsten, 3,690 gravel, 8 pillager ration |
| Pale Woods | 20.0 | pale log, platinum | quarry vindicator | 17 | 830 pale log, 62,200 platinum, 8 iron tag, 600 gravel |
| Maple Grove | 20.0 | maple, clay, titanium | quarry pillager, quarry vindicator | 22 | 974 maple, 12,470 clay, 21 iron tag, 30,500 titanium |
| Diamond Depths | 22.0 | diamond | quarry ravager | 12 | 222,000 diamond, 8 ravager horn, 120 maple, 800 clay, 1,200 gravel, 640 pale log |
| The Quarry Pit | 20.0 | - | pillager captain, quarry warden | 23 | 17 captain banner, 11 warden plate |
| Golem Rest | 20.0 | - | overworld golem | 23 | 23 golem shard |

The zones add up to about 10.5 hours of real play.

**Gates.** The Gormak Gate trader beside the Gold Reef sells the Gormak Key (300 gold, 20 leather), which opens the portal to Gormak. The Elderwood Gatekeeper at the end of Verdant Groves sells the Elderwood Pass, the Sky Gatekeeper at the end of Emerald Peaks the Sky Pass, the elytra and rockets. The Core Smith in Golem's Rest infuses the Overworld Core: one core, upgraded a zone at a time in play order. Each step needs its zone maxed and costs 25 times everything that zone costs, with 1 Golem Core Shard (one more every ten steps). The last step opens the Caverns. In the Royal Tomb a Tomb Key, a rare drop from the myrmidon and the pharaoh, is right-clicked to summon Gormak the Eternal. A key is used up; another has to be found.

**Bosses.** Gormak the Eternal (three phases, ghouls, minions and a guardian) ends Gormak. Khorvath, the Hydra, Arachne, Sol, the Elder Treant and the Sky King are summoned by a key their region's mobs drop, and drop a trophy the Overworld Core needs. The Overworld Golem returns every 5 minutes in Golem's Rest. The Treant ends Elderwood Roots, the Mirage is a rare mob for any Gormak zone. The Pirate Captain carries the Storm Mace, a rare drop.

## 4. Systems across the game

- **Backpack.** Holds every resource outside the inventory. `/backpack`.
- **Collections.** Counts everything ever gathered. Each resource has five tiers: the first at the amount its zone costs in all (`scripts/balance/collection_tiers.py`), the rest at 4, 15, 50 and 200 times that, each rounded to the nearest clean number (48 x 15 is 750, not 720). They pay 5, 25, 100, 400 and 1,500 Essence. Things not found yet show as `???`. A second tab holds gear: the player's own tools and armour as they are now, what mobs drop and keys. Clicking a piece that is yours takes a copy out.
- **Gear drops.** A weapon or armour piece dropped by a mob is unlocked for good and can be taken out again from the Gear tab of Collections. The first unlock pays 100 Essence. Dropped weapons are as strong as upgrades five zones on, and rare.
- **Ranks.** Playtime ranks up to 1,000 hours, donor ranks and staff ranks. `/ranks` picks the one shown.
- **Cosmetics.** `/customise`: suffixes, chat and name colour, join and death messages, trails, kill effects and armour trims. Some are free, some earned (playtime, a maxed zone or region), some bought with Essence, some come only with a donor package. See [STORE.md](STORE.md).
- **Food.** The Camp Cook sells food by the stack; a new player starts with 64 bread.
- **Essence shop.** `/shop` sells utility only: Dash (2,500, right click the Dash Feather), Double Jump (4,000), the Keyring (5,000), mounts and the Essence ranks, each bought once except mounts. A mount has three levels, level n costing its price times n, each one faster: pig 1,000, chicken 1,500, horse 3,000 (level 3 swims), camel 4,000 (dashes on jump), skeleton horse 6,000 (rides underwater), happy ghast 8,000, phantom 12,000 (the fastest flyer). Vitality and Crit Rate are no longer sold.
- **Essence Trader.** There is one, placed by hand in the last released zone and moved there with each release: the endless source of Essence, and the only place Essence is farmed. It opens for a player who has maxed every released zone and buys the last released zone's resources alone, one at a time or everything at once (a second click to confirm), priced so farming that zone pays about 100 Essence a minute at endgame strength (`scripts/balance/essence_values.py`, the `essence` column of `resources.csv`). Every other resource is worth nothing to it.
- **Mounts.** Right click a mount's saddle to ride it, shift to get off and it vanishes. The horse and happy ghast steer the vanilla way, the pig and chicken follow where you look.
- **Tutorial.** A new player starts by the Cobblestone Quarry and is walked through mining 10 cobblestone, the backpack, the trader and a first upgrade. It pays 50 Essence and the Newcomer suffix, and can be skipped.

## 5. Switched off for now

Built but not finished, so not reachable in game. The code is kept; `core/Aurora.java` lists what to register again.

- **The gauntlet** (`/gauntlet`).
- **Fishing**: the rods and their catch tables.

## 6. Tuning

Prices are not written by hand. `upgrades.csv` lists each step and the share of its time spent on each resource; `scripts/balance/price_zones.py` turns that into amounts when `generate.py` runs:

`price = step seconds x share x scale x income per second just before buying`

Mob drops are priced from a **kill budget** instead: a step's fighting time goes to the mob that drops each resource fastest, and in a zone with hostile mobs 30% of the mining time is spent killing whatever comes at you (that time is taken off mining). Every kill pays all of the mob's drops, so a drop is priced at what the kills since it was last bought gave, and nothing piles up unspent. A mob is never killed faster than its spawner refills (5 s, or its `respawn` column).

- **Step seconds:** the zone's `target_seconds` shared over its steps, rising from half the average to one and a half times it. A zone with only `minutes` (real play) gets `minutes x 60 x pace`, where pace is the model's seconds per real second over the zones that have both, about 1.2.
- **Scale:** `mine_scale` or `mob_scale` in `zones.csv`, by where the resource comes from.
- **Income:** worked out from the player's levels with the plugin's own mining and combat numbers (`balance_sim.py`).
- Prices are then rounded to tidy numbers and kept from repeating (`scripts/price_calculator.py`). `resource=amount` in the cost column sets an amount by hand.

The knobs, in the order to reach for them:

1. `minutes` (or `target_seconds`) for a zone that is too long or short overall.
2. `mine_scale` and `mob_scale` to shift a zone between mining and fighting.
3. The step list in `upgrades.csv` for how often a player gets to buy something.
4. Mob health, defence, attack, `damage`, density and drops, and block density, for how a zone feels. New regions start every block and mob at a density equal to the Fortune or Looting the player has on entering.

**Releases.** Regions go out a tier (stage) at a time. A region is live once `released` is `yes` in `regions.csv`; until then players are not shown it anywhere: its resources, gear and keys are left out of Collections, its cosmetics out of Customise, its warp out of Warps, and "maxed every zone" counts released zones only. The Overworld Core stays shut until Golem's Rest is released. Admins see everything. To release a tier: mark its regions `yes`, run `python scripts/balance/essence_values.py --write` and `python scripts/generate.py`, then `/refresh` (or a restart), and move the Essence Trader to the new last zone. At launch: Origin Isles, Gormak and Verdant Groves. Ranks and suffixes not out yet are listed in `UNRELEASED` in `rank/Ranks.java` and `cosmetic/Cosmetics.java`: hidden from `/ranks`, `/customise` and the Essence shop until taken off the list, except for players who already have one and for admins. At launch: playtime ranks up to Noble (100h), shop ranks Ember to Storm, all donor ranks; Essence suffixes Advanced, Hero, Miner, Warrior, Champion and Legend; donor-only bold suffixes Legendary, Supreme, Apex, Radiant and Ascendant; every package suffix.

After any data change: `python scripts/balance/collection_tiers.py --write`, `python scripts/balance/essence_values.py --write`, `python scripts/generate.py`, `mvn test`, then a play through the zone.

## 7. Known gaps

- **Only Gormak the Eternal has phases.** Every other boss is a strong mob summoned by its key.
- **The regions after the first three have only been priced**, not played. Their mob stats and densities come from the model.
- **The mace's smash is not in the model**, so the Cloud Isles' mob prices assume sword hits.
- **The Caverns do not exist yet**: the Overworld Core gives the `caverns` tag and nothing opens with it.
- **Verdant Groves has no danger.** Its four mobs do not attack; the treants are in Elderwood Roots now.
- **The world is the test world** from `scripts/build_test_world.py`: plain platforms, no map yet.
- **Essence** has utility to buy now, but nothing drains it after that.
