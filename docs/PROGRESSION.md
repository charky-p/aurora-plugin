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

**The right tool.** Wood, pumpkins, melons, bamboo and bee nests are mined with the axe; dirt, snow, gravel, clay, soul soil and concrete powder with the shovel, which is first sold at Emerald Peaks; everything else with the pick. The wrong tool breaks nothing.

**Efficiency and Blast.** Efficiency is vanilla dig speed. Blast breaks extra blocks per swing: 2x1, 2x2, 3x3, then 3x3 two deep and 3x3x3 at level 5.

**Luck and Essence.** Every block mined and mob killed has a 0.1% chance to pay Essence: 1 to 3, times `1 + Luck`. Luck never changes the chance and never touches resources. On the sword it also raises the chance of a mob's gear drop.

**Player damage.** A hit is `10 + Sharpness bonus - mob defence`. Sharpness gives 1 a level for levels 1 to 5, 3 for 6 to 10, 5 for 11 to 15, 10 for 16 to 20 and so on. A spammed swing does 20%, a fully charged one (625 ms) 100%, a jump crit 1.5x. A mace hit while falling smashes: `1 + 0.2` a block fallen, at most 3x, on top of a crit. Bloodlust adds 5% a level for each of up to 3 stacks, a stack per kill, lasting 6 seconds.

**Mob damage.** Mobs hit in hearts, set by the gap between the mob's attack level and the player's average Protection, four levels to a tier: 1 heart at the same tier, rising like a log above it (2, 2.9, 3.7 ...), falling fast below it to a floor of a quarter heart. Every hit is then multiplied by 0.6, arrows by a further 0.5, and by the mob's own `damage` column (1.5 for every Gormak mob).

**Shield.** A raised shield stops hits up to an attack level set by Deflection; hits two tiers above that break through and put it on cooldown. Defence counts as extra Protection whether the shield is raised or not.

**Thorns and Sustenance.** Thorns returns a fixed 20 damage per average level. Sustenance heals over time.

**Levels.** XP for a block is the items it dropped times the block's `xp`; a kill gives the mob's health. A level costs `1.35^level` XP up to level 15 and grows by 1.1 a level after that. Levels and collection tiers pay Essence.

**Realms and stacking.** Levels are kept per realm. The Overworld realm is Origin; every other region is a realm that is part of it, counting its own levels from 1. A tool has the sum of them all, so nothing bought is ever lost by changing region.

**Movement.** The grapple, a fishing rod sold at the end of Verdant Groves, throws you the way you look, on the ground or in the air, every 3 seconds; it climbs Khorvath's cliff. Chained in the air it can also cross a void gap. The elytra and rockets, sold at the end of Emerald Peaks, cross the 40 block gaps to the Duat and the Ironbound Quarry and fly up the 20 blocks to Elderwood Heart.

## 3. The ladder

Every region has a stage. Regions of one stage are played side by side, each with its own focus, and their levels add up:

| Stage | Regions |
|---|---|
| 1 | Origin Isles |
| 2 | Gormak (fighting), Verdant Groves (gathering, sells the grapple and the Elderwood Pass) |
| 3 | Khorvath (fighting, up a cliff), Elderwood Roots (gathering, the axe), Sunken Marsh (the pick and a little of everything) |
| 4 | Emerald Peaks (gathering, the shovel; sells the elytra, rockets and the Sky Pass) |
| 5 | The Duat (twelve gods, across a gap), Elderwood Heart (fighting, up the tree), Cloud Isles (gathering, the mace) |
| 6 | Sky Kingdom (fighting), Ironbound Quarry (everything, across a gap) |
| 7 | Golem's Rest, then the Overworld Core |

A zone is priced for a player who has finished every region of every earlier stage, and gone as far through the other regions of its own stage as through its own. Rushing one region is slower, never closed. Minutes below are real play; the model works in its own seconds, `pace` times as many (see Tuning).

| Zone | Minutes | Mines | Mobs | Steps | Total cost |
|---|---|---|---|---|---|
| **Origin Isles** (stage 1) | | | | | |
| Cobblestone Quarry | 1.6 | cobblestone | - | 11 | 160 cobblestone |
| Coal Seam | 1.6 | coal | - | 11 | 820 coal |
| Copper Vein | 2.1 | copper | chicken | 22 | 716 copper, 10 feather |
| Iron Deposit | 3.5 | iron | sheep | 21 | 1,184 iron, 22 wool |
| Gold Reef | 5.5 | gold | cow | 22 | 2,860 gold, 28 leather |
| **Gormak** (stage 2) | | | | | |
| Abandoned City | 4.9 | carved brick | hollowed serf, scavenger husk | 28 | 36 ancient scrap, 660 carved brick, 69 tattered cloth |
| The Dunes | 4.9 | sunbaked sand | dune scout, sand stalker | 29 | 22 ancient flesh, 49 ancient bone, 2,120 sunbaked sand |
| Sun Temple | 4.9 | raw gold | tomb guard, vizier mage | 29 | 17 ancient nugget, 96 vizier scroll, 6,550 raw gold |
| Royal Tomb | 8.2 | - | gormak minion, gormak guardian, gormak eternal (boss) | 16 | 20 tomb dust, 61 guardian sigil, 8 ancient nugget |
| **Verdant Groves** (stage 2) | | | | | |
| Oak Grove | 4.9 | oak log, tin | grove horse | 10 | 8,650 tin, 268 oak log, 27 horsehair |
| Birch Hollow | 4.9 | birch log, bronze | hollow fox | 13 | 446 birch log, 6,600 bronze, 36 fox fur |
| Spruce Ridge | 4.9 | spruce log, lead | ridge wolf | 9 | 884 spruce log, 19,800 lead, 15 wolf pelt |
| Dark Oak Thicket | 4.9 | dark oak log, silver | thicket sniffer | 12 | 982 dark oak log, 21,400 silver, 36 ancient seed |
| **Khorvath** (stage 3) | | | | | |
| Howling Canyons | 8.0 | crimson sand | scarab zealot, hollowed legionnaire | 33 | 21,500 crimson sand, 31 deserter coin, 124 legion plate |
| Ancient Ruins | 10.0 | relic stone | scarab zealot, relic warrior | 18 | 692 scarab shell, 33 relic shard, 1,440 relic stone |
| Catacombs | 10.0 | ossuary bone | royal myrmidon, pharaoh | 18 | 120 eternal soul, 164 royal seal, 1,440 ossuary bone |
| Sanctuary of the Sun | 12.0 | - | anubite executioner, sphinx, khorvath (boss) | 17 | 12 anubite mark, 23 sphinx claw |
| **Elderwood Roots** (stage 3) | | | | | |
| Jungle Floor | 10.0 | jungle log | jungle panda | 12 | 6,290 jungle log, 60 panda fur |
| Bamboo Thicket | 10.0 | bamboo | jungle ocelot | 11 | 6,140 bamboo, 60 ocelot whisker |
| Amber Hollow | 10.0 | elderbark, amber | wild bee | 11 | 2,820 elderbark, 880 amber, 15 royal jelly |
| Treant Grove | 10.0 | - | tree zombee, treant | 6 | 224 tree sap, 600 ancient seed |
| **Sunken Marsh** (stage 3) | | | | | |
| Mudflats | 8.0 | packed mud | bog frog | 12 | 12,620 packed mud, 36 frog leg |
| Peat Bog | 10.0 | peat | bog walker | 13 | 8,620 peat, 21 bog rag |
| Sunken Ruins | 10.0 | verdigris | bogged archer, sunken drowned | 18 | 59 poison quill, 17 drowned scale, 1,550 verdigris |
| Hydra Pool | 12.0 | - | marsh lurker, hydra (boss) | 17 | 17 lurker fang |
| **Emerald Peaks** (stage 4) | | | | | |
| Foothills | 8.0 | dirt | mountain goat | 13 | 1 cobblestone, 2,768 dirt, 36 goat hide |
| Redstone Mines | 10.0 | redstone | frost spider | 14 | 27,840 redstone, 15 spider silk |
| Pumpkin Terraces | 10.0 | pumpkin, snow | snow fox | 14 | 1,220 snow, 3,310 pumpkin, 60 snow pelt |
| Lapis Ridge | 10.0 | lapis | peak stray | 13 | 18,390 lapis, 1,010 snow, 640 pumpkin, 60 frost arrow |
| Melon Ledges | 10.0 | melon, emerald | cliff goat | 15 | 21,440 emerald, 1,980 dirt, 1,254 melon, 36 cliff horn |
| Arachne Cave | 12.0 | - | cave spider, spider rider, arachne (boss) | 17 | 16 venom sac, 83 rider bone |
| **The Duat** (stage 5) | | | | | |
| Ashen River | 13.0 | basalt, ash | anubis, sobek, osiris | 23 | 4,500 basalt, 10 anubis sigil, 10 sobek sigil, 7 osiris sigil, 100 ash |
| Hall of Judgement | 15.0 | scale gold | thoth, maat, horus | 18 | 10 thoth sigil, 10 maat sigil, 19 horus sigil, 3,600 scale gold |
| Solar Forge | 15.0 | solar slag | sekhmet, ptah, khnum | 18 | 10 sekhmet sigil, 10 ptah sigil, 7 khnum sigil, 3,100 solar slag |
| Throne of Night | 17.0 | nightstone | set, apep, nephthys, sol (boss) | 18 | 10 set sigil, 10 apep sigil, 19 nephthys sigil, 3,600 nightstone |
| **Elderwood Heart** (stage 5) | | | | | |
| Root Cellar | 12.0 | heartwood log | sapling zombee | 19 | 820 heartwood log, 55 sapling twig |
| Vine Halls | 12.0 | elder vine | elder zombee, canopy spider | 18 | 33 canopy silk, 2,580 elder vine |
| Hive Chambers | 12.0 | wax comb, beeswax | hive guard | 19 | 49 hive stinger, 3,100 beeswax, 300 wax comb |
| Upper Canopy | 12.0 | - | treehouse sentry, canopy treant | 17 | 120 sentry arrowhead, 17 canopy bark |
| The Crown | 12.0 | - | canopy treant, elder treant (boss) | 17 | 360 canopy bark |
| **Cloud Isles** (stage 5) | | | | | |
| Cloudbank | 12.0 | cloudstone, cloud dust | sky sheep | 16 | 18,240 cloudstone, 1,070 cloud dust, 104 cloud fleece |
| Skyshard Spires | 15.0 | skyshard | sky phantom | 14 | 23,700 skyshard, 33 phantom veil |
| Storm Front | 15.0 | storm copper | storm wraith | 14 | 23,700 storm copper, 33 storm essence |
| Sky Harbour | 18.0 | - | sky pirate, pirate captain | 20 | 81 pirate doubloon, 14 captain compass |
| **Sky Kingdom** (stage 6) | | | | | |
| Outer Walls | 20.0 | royal quartz | royal guard | 18 | 94 guard insignia, 6,200 royal quartz |
| Gilded Gardens | 20.0 | cherry log, sunpetal | garden bee, gardener | 19 | 33 golden pollen, 54 gardener seed, 5,000 sunpetal, 480 cherry log |
| Royal Treasury | 22.0 | celestine, aurium | treasury knight, crossbowman | 19 | 18 knight crest, 256 crossbow bolt, 5,500 celestine, 5,000 aurium |
| Throne Room | 28.0 | - | throne guard, sky king (boss) | 17 | 55 throne seal |
| **Ironbound Quarry** (stage 6) | | | | | |
| Gravel Pits | 18.0 | gravel, tungsten | quarry pillager | 15 | 22,900 tungsten, 1,430 gravel, 108 pillager ration |
| Pale Woods | 20.0 | pale log, platinum | quarry vindicator | 14 | 5,660 pale log, 10,500 platinum, 24 iron tag, 320 gravel |
| Maple Grove | 20.0 | maple, clay, titanium | quarry pillager, quarry vindicator | 15 | 6,370 maple, 1,950 clay, 116 iron tag, 4,500 titanium |
| Diamond Depths | 22.0 | diamond | quarry ravager | 13 | 39,800 diamond, 13 ravager horn, 1,000 maple, 200 clay, 400 gravel, 2,000 pale log |
| The Quarry Pit | 20.0 | - | pillager captain, quarry warden | 17 | 33 captain banner, 13 warden plate |
| Golem Rest | 20.0 | - | overworld golem | 7 | 7 golem shard |

The zones add up to about 10.5 hours of real play.

**Gates.** The Gormak Gate trader beside the Gold Reef sells the Gormak Key (300 gold, 20 leather), which opens the portal to Gormak. The Elderwood Gatekeeper at the end of Verdant Groves sells the Elderwood Pass and the grapple, the Sky Gatekeeper at the end of Emerald Peaks the Sky Pass, the elytra and rockets. The Core Smith in Golem's Rest infuses the Overworld Core: one core, upgraded a zone at a time in play order. Each step needs its zone maxed and costs 25 times everything that zone costs, with 1 Golem Core Shard (one more every ten steps). The last step opens the Caverns. In the Royal Tomb a Tomb Key, a rare drop from the myrmidon and the pharaoh, is right-clicked to summon Gormak the Eternal. A key is used up; another has to be found.

**Bosses.** Gormak the Eternal (three phases, ghouls, minions and a guardian) ends Gormak. Khorvath, the Hydra, Arachne, Sol, the Elder Treant and the Sky King are summoned by a key their region's mobs drop, and drop a trophy the Overworld Core needs. The Overworld Golem returns every 5 minutes in Golem's Rest. The Treant ends Elderwood Roots, the Mirage is a rare mob for any Gormak zone. The Pirate Captain carries the Storm Mace, a rare drop.

## 4. Systems across the game

- **Backpack.** Holds every resource outside the inventory. `/backpack`.
- **Collections.** Counts everything ever gathered. Each resource has tiers, the first at the amount its zone costs in all, and each tier pays Essence. Things not found yet show as `???`. A second tab holds gear: the player's own tools and armour as they are now, what mobs drop and keys. Clicking a piece that is yours takes a copy out.
- **Gear drops.** A weapon or armour piece dropped by a mob is unlocked for good and can be taken out again from the Gear tab of Collections. The first unlock pays 100 Essence. Dropped weapons are as strong as upgrades five zones on, and rare.
- **Ranks.** Playtime ranks up to 1,000 hours, donor ranks and staff ranks. `/ranks` picks the one shown.
- **Cosmetics.** `/customise`: suffixes, chat and name colour, join and death messages, trails, kill effects and armour trims. Some are free, some earned (playtime, a maxed zone or region), some bought with Essence, some come only with a donor package. See [STORE.md](STORE.md).
- **Food.** The Camp Cook sells food by the stack; a new player starts with 64 bread.
- **Essence shop.** `/shop` sells utility only: Dash (2,500, right click the Dash Feather), Double Jump (4,000), the God Tool (10,000), the Keyring (5,000), mounts and the Essence ranks, each bought once except mounts. A mount has three levels, level n costing its price times n, each one faster: pig 1,000, chicken 1,500, horse 3,000 (level 3 swims), camel 4,000 (dashes on jump), skeleton horse 6,000 (rides underwater), happy ghast 8,000, phantom 12,000 (the fastest flyer). Vitality and Crit Rate are no longer sold.
- **Essence Trader.** In Golem's Rest, for a player who has maxed every zone: the endless source of Essence. It buys any resource in the backpack, one at a time or everything at once (a second click to confirm). A resource's price is set so farming it pays about `20 x (1 + 0.1 x density)` Essence a minute at endgame strength, the same for every resource of a density and more for denser blocks and mobs (`scripts/balance/essence_values.py`, the `essence` column of `resources.csv`).
- **God Tool.** One tool with the pick, axe and shovel rules. Every block is mined with the levels of the tool it needs: a log with the axe's Fortune and Efficiency, dirt with the shovel's, ore with the pick's. Upgrades stay on the three tools.
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

After any data change: `python scripts/balance/collection_tiers.py --write`, `python scripts/balance/essence_values.py --write`, `python scripts/generate.py`, `mvn test`, then a play through the zone.

## 7. Known gaps

- **Only Gormak the Eternal has phases.** Every other boss is a strong mob summoned by its key.
- **The regions after the first three have only been priced**, not played. Their mob stats and densities come from the model.
- **The mace's smash is not in the model**, so the Cloud Isles' mob prices assume sword hits.
- **The Caverns do not exist yet**: the Overworld Core gives the `caverns` tag and nothing opens with it.
- **Verdant Groves has no danger.** Its four mobs do not attack; the treants are in Elderwood Roots now.
- **The world is the test world** from `scripts/build_test_world.py`: plain platforms, no map yet.
- **Essence** has utility to buy now, but nothing drains it after that.
