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

**The right tool.** Wood is only mined with the axe, everything else with the pick. The wrong tool breaks nothing.

**Efficiency and Blast.** Efficiency is vanilla dig speed. Blast breaks extra blocks per swing: 2x1, 2x2, then 3x3.

**Luck and Essence.** Every block mined and mob killed has a 0.1% chance to pay Essence: 1 to 3, times `1 + Luck`. Luck never changes the chance and never touches resources. On the sword it also raises the chance of a mob's gear drop.

**Player damage.** A hit is `10 + Sharpness bonus - mob defence`. Sharpness gives 1 a level for levels 1 to 5, 3 for 6 to 10, 5 for 11 to 15, 10 for 16 to 20 and so on. A spammed swing does 20%, a fully charged one (625 ms) 100%, a jump crit 1.5x. Bloodlust adds 5% a level for each of up to 3 stacks, a stack per kill, lasting 6 seconds.

**Mob damage.** Mobs hit in hearts, set by the gap between the mob's attack level and the player's average Protection, four levels to a tier: 1 heart at the same tier, rising like a log above it (2, 2.9, 3.7 ...), falling fast below it to a floor of a quarter heart. Every hit is then multiplied by 0.6, arrows by a further 0.5, and by the mob's own `damage` column (1.5 for every Gormak mob).

**Shield.** A raised shield stops hits up to an attack level set by Deflection; hits two tiers above that break through and put it on cooldown. Defence counts as extra Protection whether the shield is raised or not.

**Thorns and Sustenance.** Thorns returns a fixed 20 damage per average level. Sustenance heals over time.

**Levels.** XP for a block is the items it dropped times the block's `xp`; a kill gives the mob's health. A level costs `1.35^level` XP up to level 15 and grows by 1.1 a level after that. Levels and collection tiers pay Essence.

**Realms and stacking.** Levels are kept per realm. The Overworld realm is Origin; Gormak and Verdant Groves are realms that are part of it, each counting its own levels from 1. A tool has the sum of all three, so nothing bought is ever lost by changing region.

## 3. The ladder

Origin is one line. After the Gold Reef it splits into two branches that can be played in any order and add up:

- **Gormak, the fighting branch.** Sells Sharpness, Protection, the shield, Looting, Sustenance, Bloodlust, Thorns and sword Luck, with one small pick step a zone. Its mobs are the hard ones.
- **Verdant Groves, the gathering branch.** Sells the pick (Efficiency, Fortune, Luck, Blast) and the axe, with one small combat level a zone. Its mines are the dense ones.

Prices assume the branches are kept level: a branch zone is priced for a player who has finished the zones before it in both. Rushing one branch is slower, never closed. With both finished a player has about Fortune 12, Protection 27 and Sharpness 20.

| Zone | Minutes | Mines | Mobs | Steps | Total cost |
|---|---|---|---|---|---|
| **Origin Isles** | | | | | |
| Cobblestone Quarry | 2 | cobblestone | - | 11 | 160 cobblestone |
| Coal Seam | 2 | coal | - | 11 | 820 coal |
| Copper Vein | 2.6 | copper | chicken | 22 | 716 copper, 10 feather |
| Iron Deposit | 4.2 | iron | sheep | 21 | 1,184 iron, 32 wool |
| Gold Reef | 6.7 | gold | cow | 22 | 2,860 gold, 39 leather |
| **Gormak** | | | | | |
| Abandoned City | 6 | carved brick | hollowed serf, scavenger husk | 28 | 910 carved brick, 22 ancient scrap, 103 tattered cloth |
| The Dunes | 6 | sunbaked sand | dune scout, sand stalker | 29 | 2,800 sunbaked sand, 23 ancient flesh, 92 ancient bone |
| Howling Canyons | 6 | crimson sand | scarab zealot, hollowed legionnaire | 28 | 5,550 crimson sand, 22 scarab shell, 53 legion plate |
| Sun Temple | 6 | raw gold | tomb guard, vizier mage | 29 | 13,600 raw gold, 29 ancient nugget, 108 vizier scroll |
| Royal Tomb | 10 | - | royal myrmidon, pharaoh | 16 | 44 eternal soul, 132 royal seal, 12 ancient nugget |
| **Verdant Groves** | | | | | |
| Oak Grove | 6 | oak log, tin | grove horse | 10 | 8,650 tin, 268 oak log, 40 horsehair |
| Birch Hollow | 6 | birch log, bronze | hollow fox | 13 | 6,600 bronze, 446 birch log, 80 fox fur |
| Spruce Ridge | 6 | spruce log, lead | ridge wolf | 9 | 19,800 lead, 884 spruce log, 36 wolf pelt |
| Dark Oak Thicket | 6 | dark oak log, silver | thicket sniffer | 12 | 12,700 silver, 982 dark oak log, 84 ancient seed |
| Great Treehouse | 10 | - | tree zombee, treant | 6 | 180 tree sap, 32 ancient seed |

The targets add up to about 86 minutes. The first full playtest took 100.

**Gates.** The Gormak Gate trader beside the Gold Reef sells the Gormak Key (300 gold, 20 leather), which opens the portal to Gormak. Verdant Groves follows the Origin Isles in the same world with no gate. In the Royal Tomb a Tomb Key, a rare drop from the myrmidon and the pharaoh, is right-clicked to summon Gormak the Eternal. A key is used up; another has to be found.

**Bosses.** Gormak the Eternal (2,150 health, three phases, summons ghouls, minions and a guardian) ends Gormak. The Treant ends Verdant Groves. The Mirage is a rare mob for any Gormak zone.

## 4. Systems across the game

- **Backpack.** Holds every resource outside the inventory. `/backpack`.
- **Collections.** Counts everything ever gathered. Each resource has tiers, the first at the amount its zone costs in all, and each tier pays Essence. Things not found yet show as `???`. A second tab holds gear: the player's own tools and armour as they are now, what mobs drop and keys. Clicking a piece that is yours takes a copy out.
- **Gear drops.** A weapon or armour piece dropped by a mob is unlocked for good and can be taken out again from the Gear tab of Collections. The first unlock pays 100 Essence. Dropped weapons are as strong as upgrades five zones on, and rare.
- **Ranks.** Playtime ranks up to 1,000 hours, donor ranks and staff ranks. `/ranks` picks the one shown.
- **Cosmetics.** `/customise`: suffixes, chat and name colour, join and death messages, trails, kill effects and armour trims. Some are free, some earned (playtime, a maxed zone or region), some bought with Essence, some come only with a donor package. See [STORE.md](STORE.md).
- **Food.** The Camp Cook sells food by the stack; a new player starts with 64 bread.
- **Tutorial.** A new player starts by the Cobblestone Quarry and is walked through mining 20 cobblestone, the backpack, the trader and a first upgrade. It pays 50 Essence and the Newcomer suffix, and can be skipped.

## 5. Switched off for now

Built but not finished, so not reachable in game. The code is kept; `core/Aurora.java` lists what to register again.

- **The Essence shop** (`/shop`): perks, including the Keyring that hands keys back, and the three ranks bought with Essence. Until it returns, Essence is spent on cosmetics only.
- **The gauntlet** (`/gauntlet`).
- **Fishing**: the rods and their catch tables.

## 6. Tuning

Prices are not written by hand. `upgrades.csv` lists each step and the share of its time spent on each resource; `scripts/balance/price_zones.py` turns that into amounts when `generate.py` runs:

`price = step seconds x share x scale x income per second just before buying`

- **Step seconds:** the zone's `target_seconds` shared over its steps, rising from half the average to one and a half times it.
- **Scale:** `mine_scale` or `mob_scale` in `zones.csv`, by where the resource comes from.
- **Income:** worked out from the player's levels with the plugin's own mining and combat numbers (`balance_sim.py`).
- Prices are then rounded to tidy numbers and kept from repeating (`scripts/price_calculator.py`). `resource=amount` in the cost column sets an amount by hand.

The knobs, in the order to reach for them:

1. `target_seconds` for a zone that is too long or short overall.
2. `mine_scale` and `mob_scale` to shift a zone between mining and fighting.
3. The step list in `upgrades.csv` for how often a player gets to buy something.
4. Mob health, defence, attack, `damage` and drops, and block density, for how a zone feels.

After any data change: `python scripts/balance/collection_tiers.py --write`, `python scripts/generate.py`, `mvn test`, then a play through the zone.

## 7. Known gaps

- **No end game.** A finished player has nothing left to spend resources on.
- **Verdant Groves has no danger until the Great Treehouse.** Its first four mobs do not attack.
- **A Verdant-only player meets a wall at the Treant**, by design, but it is steep.
- **The world is the test world** from `scripts/build_test_world.py`: plain platforms, no map yet.
- **Essence has few uses** while the shop is off, and nothing drains it over time.
- **The specialised branches have not had a full playtest.**
