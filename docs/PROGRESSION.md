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

**The right tool.** Wood, pumpkins, melons, bamboo and bee nests are mined with the axe; dirt, snow, gravel, clay, soul soil and concrete powder with the shovel, which is first sold at Mudflats; everything else with the pick. The wrong tool breaks nothing. A bare hand also mines the axe's and the shovel's blocks, slowly and with no Fortune, so the oak logs and dirt that buy the first axe and shovel can be punched.

**Efficiency and Blast.** Efficiency is vanilla dig speed. Blast breaks extra blocks per swing: 2x1, 2x2, 3x3, then 3x3 two deep and 3x3x3 at level 5.

**Luck and Essence.** Every block mined and mob killed has a 0.1% chance to pay Essence: 1 to 3, times `1 + Luck`. Luck never changes the chance and never touches resources. On the sword it also raises the chance of a mob's gear drop.

**Player damage.** A hit is `10 + Sharpness bonus - mob defence`. Sharpness gives 1 a level for levels 1 to 5, 3 for 6 to 10, 5 for 11 to 15, 10 for 16 to 20 and so on. A spammed swing does 20%, a fully charged one (625 ms) 100%, a jump crit 1.5x. A mace hit while falling smashes: `1 + 0.2` a block fallen, at most 3x, on top of a crit. Bloodlust adds 5% a level for each of up to 3 stacks, a stack per kill, lasting 6 seconds.

**Bow.** Bought at the Dunes and upgraded with Power, 5 steps in each of the Dunes, the Sun Temple and the Royal Tomb. An arrow hits for `10 + Power bonus` (Sharpness's brackets) from 20% for a flick to 100% fully drawn, less the mob's defence. Arrows are free: the bow keeps one in the inventory to draw with, and the arrows it fires vanish where they land.

**Fishing.** A side quest apart from the zones, never needed by one. One spot a realm, played in order: Aurora Pond (overworld), Gormak Oasis (duneworld), Rootwater Lake (Elderwood), Cloud Pool (Sky World), each with a junk, common, uncommon and rare fish. The Fish Merchant at spawn hands out the rod and buys fish for Essence; each spot's angler sells its 9 rod upgrades for that spot's fish, once the spot before is maxed. Fortune catches more past a fish's density (the block curve), Luck adds 2 a level to the 1-100 roll that picks the fish, Lure shortens the wait for a bite. `scripts/balance/fishing_prices.py` prices the rod from each spot's minutes (15, 18, 20, 25) and sets each fish's Essence from the spot's Essence a minute (15, 25, 35, 50) with the rod maxed there.

**Mob damage.** A hit starts from the mob's attack level less the player's **Defence**, which adds up over all four armour pieces and the best shield carried (the shield from the Sun Temple, armour from Khorvath on: one piece a zone, helmet, chestplate, leggings and boots in turn): four levels to a tier, 1 heart at 0, rising like a log above it (2, 2.9, 3.7 ...) and falling fast below it to a floor of a quarter heart. **Protection** then takes a share off: the four pieces' average, steeply at first and up to 90% at the cap of 100 levels (27% off at 5, 39% at 12, 52% at 25, 68% at 50); levels past 100 add nothing. Every hit is then multiplied by 0.6, arrows by a further 0.5, and by the mob's own `damage` column, re-tuned in 0.3 so a hit with the armour a player arrives with stays what it was.

**Shield.** A raised shield stops hits up to an attack level set by Deflection; hits two tiers above that break through and put it on cooldown. Its Defence counts towards the player's Defence whether it is raised or not.

**Thorns and Sustenance.** Thorns returns a fixed 20 damage per average level. Sustenance heals over time.

**Levels.** XP for a block is the items it dropped times the block's `xp`; a kill gives the mob's health. A level costs `1.35^level` XP up to level 15 and grows by 1.1 a level after that. Levels and collection tiers pay Essence: a level pays 3 Essence, 3 more for every ten levels reached, five times that on every tenth level and 1,500 on every hundredth.

**Realms and stacking.** Levels are kept per realm. The Overworld realm is Origin; every other region is a realm that is part of it, counting its own levels from 1. A tool has the sum of them all, so nothing bought is ever lost by changing region.

**Upgrade order.** A tool's steps in a zone are bought one at a time, in order, and only once every step for that tool is bought in the zones played before it: the earlier zones of its own region and every zone of the regions leading there (`after`, or `portal`, in `regions.csv`). Each tool goes on its own, so a pick can be ahead of the sword. Regions side by side never wait on each other: Gormak and Verdant Groves both need Origin Isles only, and Khorvath needs Gormak but not Verdant Groves. The shop names the zone still owed. A released zone never waits on one not released yet; in a zone not released yet, which only admins reach, every zone before it counts.

**What a trader sells.** Each zone's shop is sized by what the zone is about. A tool the zone focuses on gets 5 steps: the pick, axe, shovel or hoe when one of its mines needs it, the sword when it has any mob (passive ones too). Each armour piece gets 3 steps when its mobs can hurt you. Anything it does not focus on: 3 steps for the pick and the sword, 1 for each armour piece, none for the axe, shovel or hoe. Every step sold for a tool counts towards its number, its material, unlock, Luck, Blast, Bloodlust, Sustenance and Thorns included, and the mace counts as a sword. The grapple and the shield are on top of these. Block and mob densities go up by 1 a zone through each region, so they stay within the Fortune and Looting of someone who plays one region at a time and buys as they go. **Materials.** Shops sell tool and armour materials up to gold. Diamond and netherite come only from the Ironbound Quarry on. Past that a material is a look: every material bought can be worn in `/customise`, and a tool digs at its best material's speed whichever it shows. **First purchases.** A tool beyond the starter set is bought once before it can be upgraded: the axe at the Oak Grove (8 oak logs), the shovel at Mudflats (32 mud) or the Howling Canyons (32 canyon sand), the hoe at the Fens (32 moss), the shield at the Abandoned City, the mace at Cloudbank and the grapple at the Dark Oak Thicket (200 dark oak logs, 5,000 silver).

**Movement.** The grapple, a fishing rod bought from the Dark Oak trader at the end of Verdant Groves, throws you the way you look, on the ground or in the air. It starts at 60% of the throw it once had and a 10 second cooldown, enough to climb Khorvath's cliff. Only the overworld regions after Verdant Groves upgrade it (Sunken Marsh, Emerald Peaks, the Ironbound Quarry): every zone there sells one Thrust and one Recharge, and no other region sells grapple upgrades. Thrust adds 7% of full power for each of the first five levels, so it throws at 95% at the end of the Marsh, then 9% a level. Recharge takes half a second off the cooldown a level, never under 2 seconds: 7.5 seconds after the Marsh, 4.5 after Emerald Peaks, and 2 at the Quarry Pit, so Golem's Rest sells only Thrust. With every zone bought it throws at about 200% of full power. Chained in the air it can also cross a void gap. The elytra and rockets, sold at the end of Emerald Peaks, cross the 40 block gaps to the Duat and the Ironbound Quarry and fly up the 20 blocks to Elderwood Heart.

## 3. The ladder

Every region has a stage. Regions of one stage are played side by side, each with its own focus, and their levels add up:

| Stage | Regions |
|---|---|
| 1 | Origin Isles |
| 2 | Gormak (fighting), Verdant Groves (gathering, the grapple at Dark Oak, the Elderwood Pass at its gate) |
| 3 | Khorvath (fighting, up a cliff), Elderwood Roots (gathering, the axe), Sunken Marsh (gathering, the pick, the first shovel and hoe; Glowmire ends it with the late combat gear) |
| 4 | Emerald Peaks (gathering, the shovel; sells the elytra, rockets and the Sky Pass) |
| 5 | The Duat (twelve gods, across a gap), Elderwood Heart (fighting, up the tree), Cloud Isles (gathering, the mace) |
| 6 | Sky Kingdom (fighting), Ironbound Quarry (everything, across a gap) |
| 7 | Golem's Rest, then the Overworld Core |

A zone is priced for a player who has finished every released region of every earlier stage, and gone a third of the way (`PARALLEL_SHARE` 0.35 in `price_zones.py`) through the other released regions of its own stage as through its own. A region not released yet counts only when it leads to the zone. So Verdant Groves alone is close to its targets (the Dark Oak Thicket about 1.6x), and about twice as fast with Gormak played alongside; the Sunken Marsh without Gormak is meant to be hard, up to about 4.5x its targets at Mudflats, and on target with it. Marsh and Peaks mobs hit about 6 attack levels above the Protection a player with Gormak arrives with, about 1.5 hearts a hit, about 2.6 without Gormak's armour. Rushing one region is slower, never closed. Minutes below are real play; the model works in its own seconds, `pace` times as many (see Tuning).

| Zone | Minutes | Mines | Mobs | Steps | Total cost |
|---|---|---|---|---|---|
| **Origin Isles** (stage 1) | | | | | |
| Cobblestone Quarry | 1.6 | cobblestone | - | 12 | 142 cobblestone |
| Coal Seam | 1.6 | coal | - | 12 | 856 coal |
| Copper Vein | 2.1 | copper | chicken | 14 | 684 copper, 18 feather |
| Iron Deposit | 3.5 | iron | sheep | 14 | 1,440 iron, 45 wool |
| Gold Reef | 5.5 | gold | cow | 14 | 3,260 gold, 112 leather |
| **Gormak** (stage 2) | | | | | |
| Abandoned City | 4.9 | carved brick | hollowed serf, scavenger husk | 26 | 68 ancient scrap, 1,390 carved brick, 84 tattered cloth |
| The Dunes | 4.9 | sunbaked sand | dune scout, sand stalker | 25 | 110 ancient flesh, 300 ancient bone, 7,740 sunbaked sand |
| Sun Temple | 4.9 | raw gold | tomb guard, vizier mage | 25 | 147 ancient nugget, 296 vizier scroll, 38,400 raw gold |
| Royal Tomb | 8.2 | - | gormak minion, gormak guardian, gormak eternal (boss) | 23 | 1,276 tomb dust, 708 guardian sigil, 40 ancient nugget |
| **Verdant Groves** (stage 2) | | | | | |
| Oak Grove | 4.9 | oak log, tin | grove horse | 19 | 120 oak log, 4,010 tin, 600 horsehair |
| Birch Hollow | 4.9 | birch log, bronze | hollow fox | 19 | 420 birch log, 9,800 bronze, 1,082 fox fur |
| Spruce Ridge | 4.9 | spruce log, lead | ridge wolf | 19 | 500 spruce log, 43,900 lead, 1,660 wolf pelt |
| Dark Oak Thicket | 4.9 | dark oak log, silver | thicket sniffer | 20 | 1,072 dark oak log, 78,000 silver, 2,260 ancient seed |
| **Khorvath** (stage 3) | | | | | |
| Howling Canyons | 8.0 | crimson sand | scarab zealot, hollowed legionnaire | 25 | 143,000 crimson sand, 45 deserter coin, 3,750 legion plate |
| Ancient Ruins | 10.0 | relic stone | scarab zealot, relic warrior | 25 | 18,260 scarab shell, 1,840 relic shard, 98,000 relic stone |
| Catacombs | 10.0 | ossuary bone | royal myrmidon, pharaoh | 25 | 16,090 eternal soul, 11,100 royal seal, 138,000 ossuary bone |
| Sanctuary of the Sun | 12.0 | - | anubite executioner, sphinx, khorvath (boss) | 23 | 5,980 anubite mark, 3,450 sphinx claw |
| **Elderwood Roots** (stage 3) | | | | | |
| Jungle Floor | 10.0 | jungle log | jungle panda | 17 | 8,150 jungle log, 3,240 panda fur |
| Bamboo Thicket | 10.0 | bamboo | jungle ocelot | 17 | 8,390 bamboo, 6,690 ocelot whisker |
| Amber Hollow | 10.0 | elderbark, amber | wild bee | 25 | 8,720 elderbark, 1,140 amber, 3,916 royal jelly |
| Treant Grove | 10.0 | - | tree zombee, treant | 23 | 72,290 tree sap, 2,550 ancient seed |
| **Sunken Marsh** (stage 3) | | | | | |
| Mudflats | 8.0 | packed mud, mud | bog frog | 21 | 82,300 packed mud, 1,334 mud, 1,920 frog leg |
| Slime Hollow | 10.0 | bog slime, mangrove log | swamp slime | 24 | 1,872 bog slime, 3,500 mangrove log, 3,810 swamp gel |
| The Fens | 10.0 | bog stone, fen soil, moss | bog walker | 34 | 372 moss, 59,600 bog stone, 1,276 fen soil, 3,928 bog rag |
| Sunken Ruins | 10.0 | verdigris, mangrove leaves | bogged archer | 34 | 1,582 mangrove leaves, 140,000 verdigris, 15,470 poison quill, 8,800 mushroom stem |
| Glowmire | 12.0 | ochre, verdant and pearlescent froglight | glow wisp | 27 | 820 ochre froglight, 12,670 wisp dust, 7,490 verdant froglight, 5,250 pearlescent froglight, 21,900 red mushroom, 25,500 brown mushroom |
| **Emerald Peaks** (stage 4) | | | | | |
| Foothills | 8.0 | dirt, pumpkin | mountain pig | 24 | 24,200 dirt, 43,700 pumpkin, 10,990 pork |
| Stony Crags | 10.0 | melon, calcite | mountain goat | 21 | 725,000 calcite, 44,100 melon, 14,590 goat hide |
| Frozen Slopes | 10.0 | snow, packed ice | peak stray | 29 | 21,800 snow, 468,000 packed ice, 34,840 frost arrow |
| Redstone Grotto | 10.0 | redstone | frost spider | 24 | 790,000 redstone, 40,290 spider silk |
| Emerald Depths | 10.0 | emerald, lapis | cave spider | 24 | 646,000 emerald, 142,000 lapis, 46,370 venom sac, 32,000 calcite |
| Arachne Cave | 12.0 | - | cave skeleton, arachne (boss, with her rider) | 25 | 58,300 venom sac, 22,600 rider bone |
| **The Duat** (stage 5) | | | | | |
| Ashen River | 13.0 | basalt, ash | anubis, sobek, osiris | 30 | 1,419,000 basalt, 11,600 anubis sigil, 19,100 sobek sigil, 9,500 osiris sigil, 1,500 ash |
| Hall of Judgement | 15.0 | scale gold | thoth, maat, horus | 25 | 16,400 thoth sigil, 22,800 maat sigil, 31,400 horus sigil, 980,000 scale gold |
| Solar Forge | 15.0 | solar slag | sekhmet, ptah, khnum | 25 | 28,800 sekhmet sigil, 41,900 ptah sigil, 22,300 khnum sigil, 1,180,000 solar slag |
| Throne of Night | 17.0 | nightstone | set, apep, nephthys, sol (boss) | 25 | 57,600 set sigil, 83,900 apep sigil, 40,200 nephthys sigil, 1,380,000 nightstone |
| **Elderwood Heart** (stage 5) | | | | | |
| Root Cellar | 12.0 | heartwood log | sapling zombee | 28 | 21,500 heartwood log, 120,000 sapling twig |
| Vine Halls | 12.0 | elder vine | elder zombee, canopy spider | 28 | 98,600 canopy silk, 91,500 elder vine |
| Hive Chambers | 12.0 | wax comb, beeswax | hive guard | 30 | 100,300 hive stinger, 589,000 beeswax, 52,500 wax comb |
| Upper Canopy | 12.0 | - | treehouse sentry, canopy treant | 23 | 116,100 sentry arrowhead, 60,100 canopy bark |
| The Crown | 12.0 | - | canopy treant, elder treant (boss) | 23 | 190,000 canopy bark |
| **Cloud Isles** (stage 5) | | | | | |
| Cloudbank | 12.0 | cloudstone, cloud dust | sky sheep | 19 | 31,200 cloud fleece, 749,000 cloudstone, 50,800 cloud dust |
| Skyshard Spires | 15.0 | skyshard | sky phantom | 22 | 949,000 skyshard, 94,700 phantom veil |
| Storm Front | 15.0 | storm copper | storm wraith | 22 | 1,424,000 storm copper, 94,700 storm essence |
| Sky Harbour | 18.0 | - | sky pirate, pirate captain | 23 | 85,900 pirate doubloon, 54,000 captain compass |
| **Sky Kingdom** (stage 6) | | | | | |
| Outer Walls | 20.0 | royal quartz | royal guard | 25 | 218,300 guard insignia, 2,600,000 royal quartz |
| Gilded Gardens | 20.0 | cherry log, sunpetal | garden bee, gardener | 30 | 137,600 golden pollen, 101,100 gardener seed, 1,950,000 sunpetal, 106,000 cherry log |
| Royal Treasury | 22.0 | celestine, aurium | treasury knight, crossbowman | 25 | 221,000 knight crest, 151,000 crossbow bolt, 3,370,000 celestine |
| Throne Room | 28.0 | - | throne guard, sky king (boss) | 23 | 574,000 throne seal |
| **Ironbound Quarry** (stage 6) | | | | | |
| Gravel Pits | 18.0 | gravel, tungsten | quarry pillager | 29 | 2,251,000 tungsten, 35,300 gravel, 111,500 pillager ration |
| Pale Woods | 20.0 | pale log, platinum | quarry vindicator | 29 | 152,300 pale log, 1,500,000 platinum, 130,700 iron tag, 3,200 gravel |
| Maple Grove | 20.0 | maple, clay, titanium | quarry pillager, quarry vindicator | 34 | 145,900 maple, 72,400 clay, 130,700 iron tag, 730,000 titanium |
| Diamond Depths | 22.0 | diamond | quarry ravager | 24 | 3,970,000 diamond, 214,000 ravager horn, 7,500 maple, 3,200 clay, 3,200 gravel, 10,000 pale log |
| The Quarry Pit | 20.0 | - | pillager captain, quarry warden | 25 | 161,100 captain banner, 70,400 warden plate |
| Golem Rest | 20.0 | - | overworld golem | 24 | 7,130 golem shard |

The zones add up to about 10.5 hours of real play.

**Gates.** The Gormak Gate trader beside the Gold Reef sells the Gormak Key (300 gold, 20 leather), which opens the portal to Gormak. The Elderwood Gatekeeper at the end of Verdant Groves sells the Elderwood Pass, the Sky Gatekeeper at the end of Emerald Peaks the Sky Pass, the elytra and rockets. The Core Smith in Golem's Rest infuses the Overworld Core: one core, upgraded a zone at a time in play order. Each step needs its zone maxed and costs 25 times everything that zone costs, with 1 Golem Core Shard (one more every ten steps). The last step opens the Caverns. In the Royal Tomb a Tomb Key, a rare drop from the myrmidon and the pharaoh, is right-clicked to summon Gormak the Eternal. A key is used up; another has to be found.

**Bosses.** Gormak the Eternal (three phases, ghouls, minions and a guardian) ends Gormak; its Ancient Hearts pay for the Royal Tomb's last five upgrades, one each. The Marsh Hydra, woken at Glowmire by a Hydra Key (a 4% Glow Wisp drop), is a fast zoglin with a Bogged Rider shooting from its back (killed, the rider stays dead). Every 7-9 seconds it roars, paws the ground for a second and charges a player, hitting for twice a normal hit and throwing them back; every 11-13 seconds it lobs sludge where a player stands, a puddle that after a second's warning slows and stings anyone in it for 4 seconds. It drops 1-2 Hydra Fangs, which pay for Glowmire's last five upgrades, one each. The Glowmire Trader also sells the Hydra Key for 400 Wisp Dust and 400 verdant froglights, as the Tomb Trader sells the Tomb Key. Khorvath, Arachne, Sol, the Elder Treant and the Sky King are summoned by a key their region's mobs drop, also sold in their boss zone's shop beside its upgrades, and drop 1-2 of a trophy (Solar Core, Arachne's Eye, Heart of Sol, Elder Heart, King's Crown) that pays for that zone's last five upgrades, one each, and that the Overworld Core needs. The Overworld Golem returns every 5 minutes in Golem's Rest. The Treant ends Elderwood Roots, the Mirage is a rare mob for any Gormak zone. The Pirate Captain carries the Storm Mace, a rare drop.

## 4. Systems across the game

- **Backpack.** Holds every resource outside the inventory. `/backpack`.
- **Collections.** Counts everything ever gathered. Each resource has five tiers: the first at the amount its zone costs in all (`scripts/balance/collection_tiers.py`), the rest at 4, 15, 50 and 200 times that, each rounded to the nearest clean number (48 x 15 is 750, not 720). They pay 5, 25, 100, 400 and 1,500 Essence. Things not found yet show as `???`. A second tab holds gear: the player's own tools and armour as they are now, what mobs drop and keys. Clicking a piece that is yours takes a copy out.
- **Gear drops.** A weapon or armour piece dropped by a mob is unlocked for good and can be taken out again from the Gear tab of Collections. The first unlock pays 100 Essence. Dropped weapons are as strong as upgrades five zones on, and rare.
- **Ranks.** Playtime ranks up to 1,000 hours, donor ranks and staff ranks. `/ranks` picks the one shown.
- **Cosmetics.** `/customise`: suffixes, chat and name colour, join and death messages, trails, kill effects, armour trims, and tool and armour looks (any material bought, wood to gold). Some are free, some earned (playtime, a maxed zone or region), some bought with Essence, some come only with a donor package. See [STORE.md](STORE.md).
- **Food.** The Food Trader at spawn sells food by the stack; a new player starts with 64 bread.
- **Essence shop.** `/shop` sells utility only: Dash (2,500, right click the Dash Feather), Double Jump (4,000), the Keyring (5,000), mounts and the Essence ranks, each bought once except mounts. A mount has three levels, level n costing its price times n, each one faster: pig 1,000, chicken 1,500, horse 3,000 (level 3 swims), camel 4,000 (dashes on jump), skeleton horse 6,000 (rides underwater), happy ghast 8,000, phantom 12,000 (the fastest flyer). Vitality and Crit Rate are no longer sold.
- **Essence Trader.** There is one, placed by hand in the last released zone and moved there with each release: the endless source of Essence, and the only place Essence is farmed. It opens for a player who has maxed every released zone and buys the last released zone's resources alone, one at a time or everything at once (a second click to confirm), priced so farming that zone pays about 100 Essence a minute at endgame strength (`scripts/balance/essence_values.py`, the `essence` column of `resources.csv`). Every other resource is worth nothing to it.
- **PvP.** On everywhere but safe zones; dying to a player costs nothing. Killing the same player 3 times in a row stops you hurting them for 5 minutes.
- **Mounts.** Right click a mount's saddle to ride it, shift to get off and it vanishes. The horse and happy ghast steer the vanilla way, the pig and chicken follow where you look.
- **Tutorial.** A new player starts by the Cobblestone Quarry and is walked through mining 10 cobblestone, the backpack, the trader and a first upgrade. It pays 50 Essence and the Newcomer suffix, and can be skipped.
- **Origin Isles questline.** After the tutorial a boss bar leads the player zone by zone: max out the zone, then follow a particle trail to the next zone's trader, and after the Gold Reef to the Dunesworld Gate. 25 Essence a maxed zone, 100 and the Islander suffix at the gate. Players past the tutorial before it existed join at their first zone not maxed.

## 4b. The Gauntlet

A tower mode apart from the zones, in a world of its own, solo (setup in [COMMANDS.md](COMMANDS.md)). Its data is in `scripts/data/gauntlet/`; `scripts/balance/gauntlet_balance.py --write` fills the numbers in `floors.csv` and writes `gear_levels.csv`, and `generate.py` writes `gauntlet.yml` and `mobs/gauntlet.yml`.

- **Apart from the main game.** Entering its world puts the inventory away and hands out the gauntlet setup; nothing crosses over.
- **The setup.** In the lobby the inventory is the player's setup: their gear, their starting kit and the Gauntlet menu item (a nether star, right click for the menu), laid out however they like; moving things around there is remembered. A run starts with the setup, and it comes back whole after a run, a death, or walking out of the PvP zone, whatever was used up or picked up meanwhile. Setup items can't be dropped in the lobby, gear and the menu item nowhere in the world.
- **Starting kit** (`setup.csv`, Starting Kit in the menu): bought for good with Shards and some with Cores, a level at a time, level n costing n times the base, on top of the care packages: arrows (16 a level), golden apples, splash healing, swiftness, ender pearls, strength, regeneration, an elytra (worn in place of the chestplate, swap it in), firework rockets, a totem of undying (3 Cores, held in a hand to save you) and an enchanted golden apple. Its progress is the person's, the same in every loadout. Milestones pay into the main game: Essence and cosmetics.
- **A run.** `/gauntlet`, Start a Run: the player gets their own copy of the arena and fights floor after floor. Each floor is a wave from `floors.csv`, the mobs from `mobs_gauntlet.csv` (`g_` ids) with health times the floor's `health` and the floor's `attack` added. A new mob comes in every couple of floors, few at first; every 10th floor is a boss (the Rot King, the Bone Tyrant, Ironhorn, the Blight Matriarch, the Tower Warden). Past floor 50 the last floor repeats, 8% tougher and 2.2 attack stronger a floor.
- **Care packages and boons.** A care package (a chest at the package spot) lands as a run starts and every 5 floors; every 5 floors the player also picks one of three boons (`boons.csv`): Might, Keen Eye, Bloodthirst, Vitality, Swiftness, Mending, Iron Skin, Evasion, Bulwark. Boons stack up to their `max` and last for the run. Care package loot, arrows (8 at level 1, up to 48-64), food, gapples, potions, wind charges and at high levels enchanted gapples and totems, is taken back when the run ends. Gauntlet bows and crossbows use up arrows, so they come only from care packages. Vanilla Strength adds 30% a level to hits in a run.
- **Ending.** A hit that would kill ends the run instead (a totem in hand still saves). Leaving or logging out ends it too. Every kill pays its floor's `kill` Shards at once (a boss, a boss floor's one mob, five times that), clearing a floor adds its `shards` as a bonus and boss floors pay `cores`, all kept however the run ends, kills on a floor not cleared included.
- **The forge.** Shards and Cores buy gear levels (`gear_levels.csv`): every level adds 2 Sharpness or Power (a level 5 sword hits for 30, a level 10 one for 60; gear shows its level in its name, Iron Sword (12)), 1 Defence and 2 Protection a piece of armour, and Deflection on the shield; the forge shows each upgrade's gain; every 5th level is the next material, and from level 10 these cost Cores. A new player starts with the sword and four armour pieces at +0, which give no Defence or Protection until upgraded; the bow (60 Shards), axe, shield, spear, crossbow, mace and trident are unlocked with Shards, the later ones once the best floor is high enough (`gear.csv`). A weapon's `damage` is its share of a melee hit (the axe 120%, the mace 90% plus its smash). The care package's level (`care_package.csv`) is bought here too.
- **Tuned for** gear at about half the floor: a mob takes as many hits on floor 40 as on floor 1 with gear that has kept up. Shards: 1 to 2 a kill early, 5 by floor 50, plus a bonus of 1 + 0.6 a floor for clearing it, about 195 by floor 10, 560 by floor 20 and 2,570 by floor 50; a level costs 10 x 1.17^level, about 7,500 for one piece from 0 to 30.
- **Milestones** (`milestones.csv`), for the best floor, each paid once: Essence at every one (100 at floor 5 up to 5,000 at 50), and the Challenger (10), Gladiator (20), Conqueror (30) and Unbroken (50) suffixes, and the Bloodsteel trail (25).
- **PvP zone.** Players fight each other only inside it, with their setup and no boons. Anyone can walk in, nobody walks or pearls out: `/spawn` (or `/gauntlet leave`, or Leave in the menu) takes them out after 5 seconds, to the lobby (or out of the Gauntlet) with their setup back. Logging out inside counts as dying, the kill going to whoever hit them in the last 15 seconds. A run can't start from inside it.

## 5. Tuning

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

## 6. Known gaps

- **Only Gormak the Eternal has phases.** The Marsh Hydra charges and lobs sludge, Arachne carries a rider; every other boss is a strong mob summoned by its key.
- **The regions after the first three have only been priced**, not played. Their mob stats and densities come from the model.
- **The mace's smash is not in the model**, so the Cloud Isles' mob prices assume sword hits.
- **The Caverns do not exist yet**: the Overworld Core gives the `caverns` tag and nothing opens with it.
- **Verdant Groves has no danger.** Its four mobs do not attack; the treants are in Elderwood Roots now.
- **The real map is in progress** in `aurora_overworld` (the Origin Isles' traders stand there); the other regions are still only the test world's plain platforms from `scripts/build_test_world.py`.
- **Essence** has utility to buy now, but nothing drains it after that.
- **The Gauntlet has not been played yet.** Its floor and gear numbers come from `gauntlet_balance.py` alone, and its world, lobby and arena are not built.
- **PvP is vanilla damage.** Aurora's damage rules are for mobs: between players, armour gives no protection (its vanilla armour points are removed) and Sharpness is vanilla's. This matters most in the Gauntlet's PvP zone, where gear goes up to Sharpness 30.
