# Aurora progression plan

A concrete plan for how a player moves through Aurora, from the first cobblestone to the end of the content that exists, and a scaffold for what comes after.

Written 2026-11-14 from the CSVs in `scripts/data/`, the plugin code, `DESIGN.md`, `PLANNING.md` and a run of `scripts/balance/balance_sim.py`. Every number marked **live** was read from the data or the sim on that date. Rerun the sim after any data change; this file does not update itself.

## How to read this

Each item carries one of four labels.

| Label | Meaning |
|:--|:--|
| **Live** | In the CSVs and working in code today |
| **Data only** | In the CSVs, but the code that would make it work is missing or partial |
| **Proposed** | A suggestion in this plan. Nothing is built, and it is yours to accept, change or drop |
| **Decision** | A choice only you can make. All of them are collected in section 13 |

Nothing in this file changes how the game plays. It describes what is there and lays out what could come next.

## Contents

1. Pillars and the core loop
2. The rules a player progresses through
3. The whole ladder on one page
4. Act I: Origin Isles
5. Act II: Gormak
6. Act III: Verdant Groves
7. Act IV and beyond: scaffolds
8. Systems that run across every act
9. Gates, keys and travel
10. Pacing budget and how to tune it
11. Gaps found while writing this
12. Roadmap to the demo
13. Decisions that are yours
14. Playtest protocol
15. Appendix: adding a zone, file map, naming

---

## 1. Pillars and the core loop

### 1.1 Pillars

These are read off what the game already does, not invented here.

1. **One linear path.** Every pick beats the last one and costs the last one. There is never a choice between two picks. The same holds for swords and armour. (`icon_howto_gear`: "One linear path - each pick beats the last.")
2. **Mining feeds everything.** Resources go straight to an unlimited backpack and shops spend from it. Inventory space is never the constraint.
3. **Gear is the gate, not a level number.** A block you cannot mine tells you the Fortune it needs. A mob that hurts too much is telling you to buy armour.
4. **The challenge is using your resources well, not running away.** From the 2026-11-13 playtest: mobs should not outpace the player and should not one-shot.
5. **Nothing is lost.** Gear is unbreakable, death keeps the inventory in the test worlds, mob gear drops unlock for good in the Armory.

### 1.2 The loop

```
mine the zone's block  ->  buy the next pick  ->  mine faster / more per block
        |                                                    |
        v                                                    v
  kill the zone's mob  ->  buy sword and armour  ->  survive the next zone's mob
        |
        v
  last tier of the zone needs both  ->  it is also the first cost of the next zone
```

Each zone is one turn of this loop. The last item of a zone is always the input to the first item of the next, so a player cannot skip a zone and cannot arrive under-geared.

### 1.3 What a zone is made of

| Part | Origin Isles | Gormak | Verdant Groves |
|:--|:--|:--|:--|
| Mine blocks | 2 per zone (plain and rich) | 2 per zone | 5 per zone (3 log, 2 ore) |
| Mobs | 0 or 1, passive | 2, hostile | 1 passive, hostile at the end |
| Pick tiers | 5 | 3 (2 in the Tomb) | 3 pick + 3 axe (2 pick in the Treehouse) |
| Sword tiers | 2 then 5 | 5 | 2 (3 in the Treehouse) |
| Armour tiers | 1 then 3 | 5 (4 in the Tomb) | 2 (3 in the Treehouse) |
| Shield tiers | none | 3 (none in the Tomb) | none |
| Purchases | 11 then 22 | 31 (23 in the Tomb) | 16 (17 in the Treehouse) |

---

## 2. The rules a player progresses through

All **live**, with the file that holds each rule. This is what every number later in the plan is measured against.

### 2.1 Fortune and density (`DataRegistry.getBlockDrops`, `MineListener`)

- Drops from a block are multiplied by `fortune + 1 - density`.
- If that is zero or less the block does not break and the action bar says `Requires Fortune N`.
- Fortune only counts with the right tool. A pick on a log counts as Fortune 0, so the message becomes `Wrong tool, Fortune N axe needed`.
- **So a block of density `d` needs Fortune `d` to mine at all, and pays `x1` at exactly that level.**

| Fortune over density | Multiplier |
|:--|:--|
| 0 | x1 |
| 1 | x2 |
| 2 | x3 |
| 3 | x4 |

### 2.2 Blast (`BlastShape`)

A patch on the face that was hit, like a hammer: mining a wall breaks a patch of that wall, mining a floor or ceiling a patch of it. Width runs to the player's right; length runs up a wall, or away from the player on a floor.

| Blast | Shape | Blocks per swing at most |
|:--|:--|:--|
| 1 | 2 x 1 | 2 |
| 2 | 2 x 2 | 4 |
| 3 | 3 x 3, centred | 9 |

Levels past 3 hold at 3 x 3. The sim counts 1.5, 2.5 and 5.0 blocks a swing, since part of the area is often already air.

### 2.3 Efficiency

Vanilla. Each tool material also has its own base speed (`ItemFactory.tierSpeed`): wood 2, stone 4, copper 5, iron 6, diamond 8, netherite 9, gold 12. This is why the Gormak picks stay golden: gold is the fastest material.

### 2.4 Luck and Essence (`Essence`)

- One roll per block mined and per mob killed.
- Chance is `0.1% x (1 + Luck)`, giving 1 to 3 Essence.
- Luck is read from the item in the main hand only.

| Luck | Chance per block or kill | Blocks per Essence drop, on average |
|:--|:--|:--|
| 0 | 0.1% | 1000 |
| 5 | 0.6% | 167 |
| 10 | 1.1% | 91 |
| 15 | 1.6% | 63 |

Essence buys ranks in `/shop`. See 8.1.

### 2.5 Looting (`Looting`)

Shifts the whole min-max range of a resource drop up by `ceil(level / 2)`. A 2-5 drop becomes 3-6 at Looting 1 or 2, and 7-10 at Looting 9 or 10. Gear and keys are never boosted.

### 2.6 Player damage (`CombatFormulas`, `PlayerDamage`)

`hit = (10 + sharpness bonus) x Bloodlust x swing charge x crit, minus the mob's defence`, never below 0.

Sharpness is bracketed: levels 1-5 add 1 each, 6-10 add 3, 11-15 add 5, 16-20 add 10, 21-25 add 15, 26-30 add 20.

| Sharpness | Hit before defence | Where a player first has it |
|:--|:--|:--|
| 0 | 10 | Starter |
| 2 | 12 | Coal |
| 5 | 15 | Copper |
| 8 | 24 | Gold, arriving in Gormak |
| 10 | 30 | Abandoned City |
| 12 | 40 | Dunes |
| 14 | 50 | Canyons |
| 16 | 65 | Sun Temple |
| 18 | 85 | Royal Tomb |
| 20 | 105 | Oak Grove |
| 23 | 150 | Dark Oak Thicket |
| 26 | 200 | Great Treehouse |

- **Swing charge:** 20% on a spammed swing, 100% at 625 ms, squared in between. Half charge pays 40%.
- **Crit:** x1.5 on a vanilla jump crit. The sim leaves crits out.
- **Bloodlust:** each kill adds a stack for 6 seconds, up to 3. Each stack is +5% per level. Bloodlust 2 at full stacks is +30%.

### 2.7 Mob damage (`CombatFormulas.heartsPerHit`, `MobDamage`)

A mob's hit is set by how far its attack level sits above the player's protection. Four protection levels make one tier. Every hit is then scaled by 0.6, and projectiles by a further 0.5.

Protection is the average over the four armour slots, plus Defence from the best shield carried anywhere.

| Attack minus protection | Tiers | Hearts per melee hit |
|:--|:--|:--|
| -8 | -2 | 0.31 |
| -4 | -1 | 0.34 |
| 0 | 0 | 0.60 |
| +2 | 0.5 | 0.91 |
| +3 | 0.75 | 1.06 |
| +4 | 1 | 1.20 |
| +8 | 2 | 1.73 |
| +12 | 3 | 2.19 |

A mob with attack 0 is passive: it never targets or hurts a player. A mob with an attack cooldown lands one hit per cooldown.

### 2.8 Shields (`ShieldBlock`)

A raised shield covers attack level `11 + Deflection x 4/3`.

- Up to 2 attack levels over that: nothing gets through.
- From there it rises evenly to the full hit at 8 levels over.
- At 8 levels over, the shield is broken through and goes on a 5 second cooldown.

| Deflection | Covers attack | Fully blocks up to | Breaks at |
|:--|:--|:--|:--|
| 0 | 11 | 13 | 19 |
| 2 | 13.7 | 15.7 | 21.7 |
| 5 | 17.7 | 19.7 | 25.7 |
| 8 | 21.7 | 23.7 | 29.7 |
| 10 | 24.3 | 26.3 | 32.3 |

### 2.9 Thorns, Sustenance

- **Thorns:** returns 25% of the wearer's own base hit per level of average Thorns, to the mob that hit them.
- **Sustenance:** one hunger point every `30 s / average Sustenance`. A full Sustenance 5 set feeds a point every 6 seconds.

### 2.10 The Armory (`ArmoryManager`)

Gear dropped by a mob unlocks once per player and can then be taken from the Armory for free, one copy at a time. Shop gear never goes in, because upgrades consume it.

### 2.11 Keys (`KeyItems`)

- An **unlocks** key gives the player a scoreboard tag. Portals can require a tag.
- A **summons** key wakes its mob at that mob's spawner, within 24 blocks. Players within 64 blocks are told who summoned it. A summoned mob's spawner never spawns by itself.

---

## 3. The whole ladder on one page

All **live**. Minutes are from the sim: face-tanking, no crits, no dodging.

| # | Region | Zone | Mine density | Fortune arriving / leaving | Mobs (attack) | Protection arriving / leaving | Sharpness leaving | Minutes | Running total |
|:--|:--|:--|:--|:--|:--|:--|:--|:--|:--|
| 0 | Origin | Starter | - | 0 | - | 0 | 0 | 0 | 0 |
| 1 | Origin | Cobblestone | 0 | 0 / 1 | - | 0 / 1 | 1 | 2.0 | 2.0 |
| 2 | Origin | Coal | 0 | 1 / 2 | - | 1 / 2 | 2 | 2.0 | 4.0 |
| 3 | Origin | Copper | 1 | 2 / 3 | Chicken (0) | 2 / 4 | 5 | 2.6 | 6.7 |
| 4 | Origin | Iron | 2 | 3 / 4 | Sheep (0) | 4 / 6 | 7 | 4.4 | 11.0 |
| 5 | Origin | Gold | 3 | 4 / 5 | Cow (0) | 6 / 8 | 8 | 7.7 | 18.7 |
| - | Origin | Gormak Gate | - | - | - | - | - | key: 300 gold, 20 hide | - |
| 6 | Gormak | Abandoned City | 4 | 5 / 6 | Serf (11), Husk (11) | 8 / 12 | 10 | 4.4 | 23.1 |
| 7 | Gormak | The Dunes | 5 | 6 / 7 | Scout (15), Stalker (11) | 12 / 16 | 12 | 4.0 | 27.1 |
| 8 | Gormak | Howling Canyons | 6 | 7 / 8 | Zealot (19), Legionnaire (19) | 16 / 20 | 14 | 5.1 | 32.2 |
| 9 | Gormak | Sun Temple | 7 | 8 / 9 | Guard (23), Vizier (23, ranged) | 20 / 24 | 16 | 4.5 | 36.7 |
| 10 | Gormak | Royal Tomb | none | 9 / 10 | Myrmidon (25), Pharaoh (25), Gormak (29, boss) | 24 / 25 | 18 | 9.8 | 46.5 |
| 11 | Verdant | Oak Grove | 10 | 10 / 12 | Grove Horse (0) | 25 / 27 | 20 | 6.3 | 52.8 |
| 12 | Verdant | Birch Hollow | 11 | 12 / 13 | Hollow Fox (0) | 27 / 29 | 21 | 7.6 | 60.4 |
| 13 | Verdant | Spruce Ridge | 12 | 13 / 14 | Ridge Wolf (0) | 29 / 31 | 22 | 7.6 | 68.0 |
| 14 | Verdant | Dark Oak Thicket | 13 | 14 / 15 | Thicket Sniffer (0) | 31 / 33 | 23 | 8.7 | 76.7 |
| 15 | Verdant | Great Treehouse | none | 15 / 17 | Tree Zombee (33), Treant (37, boss) | 33 / 36 | 26 | 9.5 | 86.2 |

Three things this table shows:

- **The density of a zone equals the Fortune of the pick you arrive with.** Every zone starts at x1 and ends at x2 or x3. That rule is the spine of the whole game, and section 7 keeps it.
- **Mob attack rises by 4 per Gormak zone and armour by 4.** A player in the zone's own armour sits within one tier of its mobs from arrival to exit: 1.06 hearts a hit on arrival, 0.48 on leaving.
- **Densities 8 and 9 are unused.** The Royal Tomb has no mine, yet its two picks carry Fortune from 9 to 10, which is exactly what the Oak Grove's density 10 needs. See 11.4.

---

## 4. Act I: Origin Isles

**Purpose:** teach the loop one piece at a time. Mining first, then upgrading, then mobs, then needing both.
**Live.** Sim total 18.7 minutes. Your target after the 2026-11-13 playtest is about 18.

### 4.0 Starter

| | |
|:--|:--|
| Trader | Isle Outfitter (`starter_shop`) |
| Gear | Wooden pick, wooden sword, leather armour, all tier 0 with no enchants |
| Teaches | Where the shop is, what the menu is (`/menu`), that the backpack exists |
| Leaves with | The three tier 0 items, which are the first cost of everything in Cobblestone |

**Build checklist**
- [ ] Spawn set with `/a setspawn`, so every join and death lands here
- [ ] Isle Outfitter within sight of spawn
- [ ] The How to Play menu reachable from `/menu` (**live**: 5 pages)
- [ ] A visible path from spawn to the Cobblestone Quarry

### 4.1 Cobblestone Quarry

| | |
|:--|:--|
| Blocks | `cobblestone` (1), `stone` (2-4), density 0 |
| Mobs | none |
| Trader | Quarry Foreman |
| Picks | 5: plain, Efficiency 1, Efficiency 2, +Fortune 1, +Luck 1 |
| Swords | 2: plain, Sharpness 1 |
| Armour | 1 set: Protection 1 |
| Costs | Cobblestone only. Picks 16, 28, 44, 70, 110. Swords 14, 22 |
| Income | 54 a minute on the starter pick, 309 on the last |
| Sim time | 2.0 min, 11 purchases |

**The beat:** the first Efficiency level is felt within a minute. Pick 4 is the first Fortune, and the first time a block gives two.

**Exit condition:** `cobble_pick_5`, which is the cost of `coal_pick_1`.

### 4.2 Coal Seam

| | |
|:--|:--|
| Blocks | `coal_ore` (1), `coal_block` (5-9), density 0 |
| Mobs | none |
| Trader | Coal Broker |
| Picks | 5: Efficiency 3 to 4, Fortune 1 to 2, Luck 1 to 3 |
| Swords | 2: Sharpness 2, then +Luck 1 |
| Armour | 1 set: Protection 2 |
| Income | 367 a minute arriving, 874 leaving |
| Sim time | 2.0 min, 11 purchases |

**The beat:** the rich block. A coal block gives 5 to 9, so players learn to look for the better block in a mine.

**Exit condition:** `coal_pick_5` with Fortune 2. Copper is density 1, so the player arrives already at x2.

### 4.3 Copper Vein

| | |
|:--|:--|
| Blocks | `copper_ore`, `copper_block`, density 1 |
| Mobs | Isle Chicken, 20 health, passive, drops 1-3 feathers |
| Trader | Copper Smith |
| Picks | 5: Efficiency 4 to 5, Fortune 2 to 3, Luck 3 to 5 |
| Swords | 5: Sharpness 2 to 5, last adds Looting 1 |
| Armour | 3 sets: Protection 2, 3, 4 |
| Cost pattern | Tiers 1-2 copper, tiers 3-4 feathers, tier 5 both |
| Income | 653 a minute arriving, 1224 leaving |
| Sim time | 2.6 min, 22 purchases |

**The beat:** the first mob, and the first upgrade that mining alone cannot buy. Chickens are fast (`MOVEMENT_SPEED 0.3`) and harmless, so this teaches swinging and the charge timer with nothing at stake.

**Exit condition:** `copper_pick_5` with Fortune 3 for iron's density 2.

### 4.4 Iron Deposit

| | |
|:--|:--|
| Blocks | `iron_ore`, `iron_block`, density 2 |
| Mobs | Isle Sheep, 30 health, passive, drops 1-3 wool |
| Trader | Iron Smith |
| Picks | 5: Efficiency 5 to 7, Fortune 3 to 4, Luck 4 to 5 |
| Swords | 5: Sharpness 5 to 7, Luck 1 to 2, Looting 1 to 2 |
| Armour | 3 sets: Protection 4, 5, 6 |
| Income | 742 a minute arriving, 1323 leaving |
| Sim time | 4.4 min, 22 purchases |

**The beat:** same shape as copper, longer. This is the zone that tests whether the loop holds attention once nothing new is being taught.

### 4.5 Gold Reef

| | |
|:--|:--|
| Blocks | `gold_ore`, `gold_block`, density 3 |
| Mobs | Isle Cow, 40 health, passive, drops 1-3 hide |
| Trader | Gold Assayer |
| Picks | 5: Efficiency 7 to 8, Fortune 4 to 5, Luck 5 to 6, last adds **Blast 1** |
| Swords | 5: Sharpness 7 to 8, Luck 2 to 4, Looting 2 to 3 |
| Armour | 3 sets: Protection 6, 7, 8 |
| Income | 933 a minute arriving, 2098 leaving |
| Sim time | 7.7 min, 22 purchases |

**The beat:** Blast. The last pick breaks two blocks a swing, and it is the reward for finishing the act.

**Exit condition:** the full gold set, and 300 gold with 20 hide for the Gormak Key.

> The sim puts Gold at 7.7 minutes against the 6.7 you settled on after the playtest. See 10.3.

### 4.6 The Gormak Gate

| | |
|:--|:--|
| Trader | Gate Warden (`gate_shop`) |
| Sells | Gormak Key for 300 gold and 20 hide |
| Key | Right click to use. Gives the tag `gormak`, consumed |
| Gate | A portal with `/a portal settag <portal> gormak`. Without the tag the player is thrown back and told which item they need |

**Build checklist**
- [ ] Portal framed and visible from the Gold Reef
- [ ] Portal tagged `gormak`
- [ ] Gate Warden beside it
- [ ] A return portal on the Gormak side, untagged

---

## 5. Act II: Gormak

**Purpose:** combat becomes real. Mobs hit back, armour matters every zone, the shield arrives.
**Live.** Sim total 27.8 minutes. Your targets: 4 to 8 minutes for each of the first four zones, about 10 for the Tomb.

**How a Gormak zone is laid out (live in the costs):**
- Two hostile mobs and a supplementary mine.
- Sword and armour tiers 1-2 cost the first mob's drop, 3-4 the second's, tier 5 all three.
- Picks cost the mine block plus mob drops.
- Each mob has a 2% chance to drop its own weapon, which goes to the Armory.
- Each zone's shop has two sub-menus: Armour and Shields.

### 5.1 Abandoned City

| | |
|:--|:--|
| Blocks | `carved_brick`, `cracked_brick` (2-4), density 4 |
| Mob 1 | Hollowed Serf: 40 health, 2 defence, attack 11. Drops tattered cloth 2-5, ancient scrap 25%, Serf Cleaver 2% |
| Mob 2 | Scavenger Husk: 80 health, 3 defence, attack 11, follow range 32. Drops ancient scrap 3-6, tattered cloth 25%, Scavenger Shiv 2% |
| Trader | City Scavenger |
| Picks | 3: Efficiency 8 to 9, Fortune 5 to 6, Luck 7, Blast 1 |
| Swords | 5: Sharpness 8 to 10, Looting 3 to 4, last adds **Bloodlust 1** |
| Armour | 5 sets: Protection 9 to 12, last adds **Sustenance 1** |
| Shields | 3: plain, Deflection 1, Deflection 2 |
| Combat on arrival | 1.06 hearts a hit, Serf dies in 1.2 s, Husk in 2.5 s |
| Combat on leaving | 0.48 hearts a hit |
| Sim time | 4.4 min, 31 purchases |

**The beat:** the first thing that can kill you. Arriving in gold armour (Protection 8) against attack 11 is three levels under: about a heart a hit. Two armour tiers later it is under half a heart.

**New here:** shields, Bloodlust, Sustenance, the Armory (first weapon drop).

### 5.2 The Dunes

| | |
|:--|:--|
| Blocks | `sunbaked_sand`, `cut_sunbaked_sand`, density 5 |
| Mob 1 | Dune Scout: 80 health, 4 defence, attack 15, fast (0.34), knockback 1. Drops ancient flesh 2-5, Scout Blade 2% |
| Mob 2 | Sand Stalker: 130 health, 5 defence, attack 11, follow range 48. Drops ancient bone 1-3, ancient flesh 30%, Stalker Fang 2% |
| Trader | Dune Nomad |
| Picks | 3: Efficiency 9 to 10, Fortune 6 to 7, Luck 8, **Blast 2** |
| Swords | 5: Sharpness 10 to 12, Luck 6 to 7, Looting 4 to 5 |
| Armour | 5 sets: Protection 13 to 16, Sustenance 1 to 2 |
| Shields | 3: Deflection 3, 4, 5 |
| Sim time | 4.0 min, 31 purchases |

**The beat:** a fast mob. The Scout closes the distance and knocks back. The Stalker was nerfed after the playtest to attack 11, so it is the slow, tanky one of the pair.

**Planned but not built:** parkour and obstacles across the dunes so it plays differently from the city (`PLANNING.md`); an oasis where the Mirage is more likely (`DESIGN.md`). See 11.2.

### 5.3 Howling Canyons

| | |
|:--|:--|
| Blocks | `crimson_sand`, `cut_crimson_sand`, density 6 |
| Mob 1 | Scarab Zealot: 180 health, 6 defence, attack 19. Drops scarab shell 2-5, Scarab Khopesh 2% |
| Mob 2 | Hollowed Legionnaire: 200 health, 8 defence, attack 19, slow (0.22), knockback resistance 0.8. Drops legion plate 2-4, scarab shell 25%, Legion Halberd 2% |
| Trader | Canyon Hermit |
| Picks | 3: Efficiency 10, Fortune 7 to 8, Luck 9 to 10, Blast 2 |
| Swords | 5: Sharpness 12 to 14, Luck 7 to 8, Looting 5 to 6 |
| Armour | 5 sets: Protection 17 to 20, Sustenance 2 to 3 |
| Shields | 3: Deflection 6, 7, 8 |
| Combat on arrival | 1.06 hearts a hit, Legionnaire takes 4.4 s and leaves 5.9 hearts of 10 |
| Sim time | 5.1 min, 31 purchases |

**The beat:** the first mobs that take a real fight. The Legionnaire is the hardest plain mob so far on arrival; this is where a shield earns its slot.

### 5.4 Sun Temple

| | |
|:--|:--|
| Blocks | `raw_gold_block`, `gilded_stone`, density 7 |
| Mob 1 | Tomb Guard: 250 health, 10 defence, attack 23, cannot be knocked back. Drops ancient nugget 3-7, Tomb Halberd 2% |
| Mob 2 | Vizier Mage: 220 health, 12 defence, attack 23, **ranged** (skeleton), follow range 64. Drops vizier scroll 1-3, ancient nugget 40%, Vizier Bow 2% |
| Trader | Temple Keeper |
| Picks | 3: Efficiency 10 to 11, Fortune 8 to 9, Luck 11, **Blast 3** |
| Swords | 5: Sharpness 14 to 16, Luck 9 to 10, Looting 6 to 7 |
| Armour | 5 sets: Protection 21 to 24, Sustenance 3 to 4 |
| Shields | 3: Deflection 9, 10, then **Defence 1** |
| Income | 2713 a minute arriving, 8139 leaving (Blast 3) |
| Sim time | 4.5 min, 31 purchases |

**The beat:** the first ranged mob. Arrows deal half damage (0.53 hearts on arrival), so the Vizier is a nuisance that makes the Guard fight harder, not a threat alone.

**New here:** Defence, which counts as protection even when the shield is only carried. Blast 3, a 3 x 3 layer.

### 5.5 Royal Tomb

| | |
|:--|:--|
| Blocks | none |
| Mob 1 | Royal Myrmidon: 250 health, 13 defence, attack 25. Drops eternal soul 1-2, **Tomb Key 8%**, Myrmidon Blade 2% |
| Mob 2 | Pharaoh: 340 health, 15 defence, attack 25. Drops royal seal 1-2, **Tomb Key 8%**, Pharaoh Scepter 2% |
| Boss | Gormak the Eternal: 1500 health, 18 defence, attack 29, one hit per 3 s, twice normal size, follow range 100. Drops ancient heart 1-2, royal seal 3-6, Gormak Blade 15% |
| Trader | Tomb Broker |
| Picks | 2: Efficiency 12, Fortune 9 to 10 (cost eternal souls) |
| Swords | 5: Sharpness 16 to 18, Looting 7 to 8, last is **Bloodlust 2** |
| Armour | 4 sets: Protection 25, Sustenance 3 to 5, last adds **Thorns 1** |
| Utility | Totem of Undying (170 ancient nugget, 5 eternal soul). Gormak Talisman (3 ancient heart, 5 royal seal) |
| Boss fight | 14 to 20 s of damage dealt, 1.20 to 1.34 hearts per hit landed. A Deflection 10 shield no longer stops his hit outright, about 45% gets through |
| Sim time | 9.8 min, 23 purchases |

**The beat:** the first boss. Tomb Keys drop at 8% from both mobs, so a key comes about every 12 kills. Use it within 24 blocks of the throne and the boss wakes; everyone within 64 blocks is told.

**Exit condition for the demo:** Gormak the Eternal killed once. The Talisman needs 3 ancient hearts, so two or three kills.

**The fight** (built 2026-11-14, not yet played; the numbers are constants in `features/boss/GormakEternal.java` and `GormakPhases.java`):

| Part | What happens |
|:--|:--|
| Boss bar | Shown to everyone within 48 blocks while he lives. Red, purple once the totem has popped |
| Stomp | 3 s into the fight, then every 6 s while someone is within 6 blocks. He hops with a roar, and lands 0.75 s later. Everyone standing within 6 blocks is thrown back and up. A player in the air when he lands is not. No damage |
| Gormak Ghouls | Three waves of 3, one each time his health passes 75%, 50% and 25%. Never more than 6 alive |
| Totem | The first killing blow pops the totem in his off hand. He returns at 35% health, 20% faster, stomping every 4 s, with two Gormak Guardians (skeletons in his armour, 500 health, 16 defence, attack 25) that stay until killed. No further ghoul wave |
| When he dies | Every ghoul goes with him |

**Gormak Ghoul** (`gormak_ghoul`, in `mobs_gormak.csv`): a husk with a scythe, 60 health, no defence, attack 21, speed 0.4. Drops nothing. Fades after 30 s. A scythe hit that lands takes 2 food points, all saturation, and gives Hunger for 6 s; a blocked hit takes nothing. At attack 21 against Protection 25 it deals about a third of a heart.

With his 1500 health and the totem he has 2025 to get through, which the sim's 14 to 20 s becomes 19 to 27 s of dealing damage. The sim does not model the totem, the ghouls or the stomp.

### 5.6 The Mirage

**Data only.** Mirage: 400 health, 5 defence, attack 17, fast. Drops mirage shard 1-2 and Mirage Edge 5% (Sharpness 16, Luck 15).

The plan in `PLANNING.md` is a rare mob that can appear in any Gormak zone and turns invisible for 4 seconds in every 5. Today it is an ordinary mob that only exists if a spawner is made for it, and `mirage_shard` is not a cost of anything. See 11.2.

---

## 6. Act III: Verdant Groves

**Purpose:** two tool lines. Logs need an axe, ores need a pick, and each zone's gear costs both.
**Live in data.** Sim total 39.7 minutes. **No time targets have been set for this act.** It is not part of the demo.

**How a Verdant zone is laid out (live in the costs):**
- Five blocks: log, wood (2-4), stripped log (5-9), ore, rich ore (5-9), all at the zone's density.
- One passive mob with high health and defence. It is a resource node that takes a real sword to farm.
- Three picks and three axes with identical enchants.
- Picks and axes are diamond; the Treehouse is netherite.

### 6.1 Oak Grove

| | |
|:--|:--|
| Blocks | Oak log line, tin line, density 10 |
| Mob | Grove Horse: 400 health, 15 defence, passive, drops horsehair 2-4 |
| Trader | Grove Warden |
| Picks and axes | 3 each: Efficiency 13 to 14, Fortune 11 to 12, Luck 11 to 12, Blast 3 |
| Swords | 2: Sharpness 19 to 20, Bloodlust 2 |
| Armour | 2 sets: Protection 26 to 27, Sustenance 5, Thorns 1 |
| Sim time | 6.3 min, 16 purchases |

**The beat:** the pick stops working on logs. The first axe.

### 6.2 Birch Hollow

Density 11. Hollow Fox: 450 health, 17 defence, slow after the last tuning pass (0.15). Hollow Smith. Fortune 12 to 13, Sharpness 20 to 21, Protection 28 to 29. Sim time 7.6 min.

### 6.3 Spruce Ridge

Density 12. Ridge Wolf: 520 health, 19 defence, fast (0.28), passive: it stays calm when hit. Ridge Forester. Fortune 13 to 14, Sharpness 21 to 22, Protection 30 to 31. Sim time 7.6 min.

### 6.4 Dark Oak Thicket

Density 13. Thicket Sniffer: 700 health, 21 defence, very slow (0.1). Thicket Hermit. Fortune 14 to 15, Sharpness 22 to 23, Protection 32 to 33. Sim time 8.7 min.

### 6.5 Great Treehouse

| | |
|:--|:--|
| Blocks | none |
| Mob | Tree Zombee: 450 health, 20 defence, attack 33. Drops tree sap 2-4 |
| Boss | Treant: 2400 health, 24 defence, attack 37, one hit per 2 s, 1.8 times normal size. Drops heartwood 1-2, tree sap 3-6 |
| Trader | Canopy Keeper |
| Picks | 2, netherite: Efficiency 19 to 20, Fortune 16 to 17 |
| Swords | 3, netherite: Sharpness 24 to 26 |
| Armour | 3 sets, netherite: Protection 34 to 36, last is Thorns 2 |
| Boss fight | 8.8 to 12.5 s of damage dealt, 0.76 to 1.20 hearts per hit landed, 4.1 hearts left on arrival |
| Sim time | 9.5 min, 17 purchases |

**The beat:** the first hostile mobs since Gormak, after four zones of peace. The Treant on arrival is the most dangerous fight in the data: 1.2 hearts every two seconds.

**Open points for this act**
- No key or gate to Verdant exists. See 9 and 11.5.
- The Treant has no summon key, so its spawner spawns it on a timer like any mob.
- `PLANNING.md` plans to move the Tree Zombee, Treant and Treehouse gear out to the Great Canopy once that is built, ending Verdant at the Dark Oak Thicket.

---

## 7. Act IV and beyond: scaffolds

Everything in this section is **proposed**. The regions and their themes come from `PLANNING.md`; the numbers are this plan's suggestion for where they would have to sit to keep the rules from section 3. None of it has been through the sim.

### 7.1 The rules any new zone has to keep

1. **Density equals the Fortune the player arrives with.** The first swing in a new zone pays x1.
2. **Fortune rises by 1 or 2 per zone.** So does density, one zone behind.
3. **Mob attack rises by 4 per combat zone** and armour gives 4 protection levels across the zone, so the zone's own armour takes a hit from about 1.06 hearts to 0.48.
4. **The last item of a zone is a cost of the first item of the next.**
5. **Tier 5 (or the last tier) costs everything the zone drops.**
6. **A mob that does not melee or shoot arrows cannot be used.** The combat system handles only those. No witches, creepers, slimes or evokers.
7. **Every zone introduces at most one new thing.**

### 7.2 Where Verdant leaves the player

| Stat | Leaving the Treehouse |
|:--|:--|
| Fortune | 17 |
| Efficiency | 20 |
| Luck (pick) | 16 |
| Blast | 3 |
| Sharpness | 26 (hit of 200) |
| Protection | 36 |
| Bloodlust | 2 |
| Thorns | 2 |
| Sustenance | 5 |

### 7.3 The Great Canopy

From `PLANNING.md`: a jungle region around one huge tree, reached from the Dark Oak Thicket. Each level up the tree is a zone, linked by portals.

If the Treehouse content moves here as planned, Verdant ends at Fortune 15, Protection 33, Sharpness 23, and the Canopy starts from there.

| Level | Zone | Mine | Proposed density | Mobs | Proposed attack | Proposed protection across the zone |
|:--|:--|:--|:--|:--|:--|:--|
| 0 | Jungle Floor | jungle log, jungle wood, stripped | 15 | panda, ocelot (passive) | 0 | 33 to 35 |
| 1 | The Roots | mud, rooted dirt, moss block | 16 | sapling zombee | 37 | 35 to 39 |
| 2 | Vine Halls | mossy cobblestone, mossy stone bricks | 17 | tree zombee, canopy spider | 41 | 39 to 43 |
| 3 | The Treehouse | bamboo block, bamboo mosaic | 18 | tree zombee, treehouse sentry (archer) | 45 | 43 to 47 |
| 4 | Upper Canopy | azalea leaves, flowering azalea leaves | 19 | treant | 49 | 47 to 51 |
| 5 | The Crown | none | - | elder treant (boss) | 53 | 51 to 52 |

**What it could introduce, one per level:**
- Level 0: a third tool line, or the "god tool" that merges pick and axe (`PLANNING.md`). **Decision.**
- Level 1: the first hostile mob in a mining zone since Gormak.
- Level 2: spiders are melee, so they fit the combat system. They climb, which changes how a fight in a vertical zone plays.
- Level 3: a second ranged mob.
- Level 5: a summon key for the elder treant, reusing the Tomb Key code.

**Locked lifts** (`PLANNING.md`): a level's portal only opens once you own that level's gear. The tag system already does this if the last item of each level is sold alongside a key, or if the shop itself hands out the tag. **Decision.**

**Attack levels here assume the Tree Zombee stays at 33 and the ladder continues at +4.** Sharpness past 26 is in the 20-per-level bracket, so mob health has to climb fast to keep kill times near 2 to 4 seconds: about 700 at level 1, rising to roughly 1500 at level 4.

### 7.4 Emerald Peaks

From `PLANNING.md`: a large mountain island, where the elytra unlocks.

| Zone | Mine | Requirement in `PLANNING.md` |
|:--|:--|:--|
| Snowfield | snow, dirt | - |
| Patch | pumpkin, melon | - |
| Redstone Vein | redstone | - |
| Lapis Seam | lapis | needs snow and pumpkin |
| Emerald Lode | emerald | needs dirt and melon |
| Arachne's Nest | none | Arachne (boss) |

**What is new here:** an ore whose gear costs two earlier zones' resources. That is the first time a player has to go back, and backpack totals make it painless.

**Proposed bands:** density 20 to 24, attack 57 to 73, protection 52 to 72.

### 7.5 Ironbound Quarry

From `PLANNING.md`: a detached quarry island that needs the elytra to reach.

| Zone | Mine | Requirement in `PLANNING.md` |
|:--|:--|:--|
| Dead Wood | pale log, maple (mangrove) | - |
| Gravel Pit | gravel, clay, ashen soil | - |
| Tungsten | tungsten | needs gravel |
| Platinum | platinum | needs gravel and pale log |
| Titanium | titanium | needs clay and maple |
| Diamond | diamond | needs maple, clay, gravel and pale log |
| The Quarry | none | pillagers, temple guards, Overworld Golem (world boss) |

**What is new here:** a world boss on a timer (every 5 minutes) instead of a key, and a crafted drop: overworld shards from the temple guards combine into an overworld core.

Pillagers shoot crossbow bolts, which are projectiles, so they fit rule 6.

### 7.6 Other worlds

`PLANNING.md` lists: space, farm, void, twilight, candy, wasteland, sakura, volcano, mushroom, and a cave world with cobalt.

The world system (`/a world create`) already supports any number of void worlds, each with its own spawn and portals. A new world costs no code.

**Proposed order of work for any new world:** theme and four to six zones on paper, then the CSV rows, then the sim, then the build. Never the build first.

---

## 8. Systems that run across every act

### 8.1 Essence

**Live** since 2026-11-14. Earned three ways and spent in `/shop`.

| Source | How much |
|:--|:--|
| Luck | 0.1% x (1 + Luck) per block or kill, 1 to 3 each |
| Levels | 1 per level, plus 1 for every ten levels reached. Levels ending in 0 pay five times over, every hundredth pays 500. Level 1 to 50 pays 235 in all |
| Collections | 5, 25, 100 and 400 for the four tiers of each resource (8.4) |

**The only sink is ranks.** Permanent upgrades, crates and talisman levels from `PLANNING.md` are still open.

### 8.2 Levels and ranks

**Live** since 2026-11-14, cosmetic only.

**Levels** are on the vanilla XP bar. A mined block gives the number of its mine (cobblestone 1, coal 2, up to raw gold 9), a kill gives the mob's health, never less than 10. Level n to n + 1 costs 60 x (n + 1) XP. Tuned so a full Origin and Gormak run ends around level 50; level 100 is 303,000 XP, about four of them. The estimate behind that is rough and is the first thing to check in play.

**Ranks** show in chat, the tab list and the sidebar. Changed with `/ranks`.

| Kind | Ranks | How |
|:--|:--|:--|
| Start | MEMBER | everyone |
| Playtime | PLAYER 1h, ROOKIE 5h, APPRENTICE 10h, NOVICE 25h, EXPERT 50h, NOBLE 100h, LORD 150h, DUKE 250h, MASTER 350h, GRANDMASTER 500h | hours online. Put on automatically unless a shop rank is worn |
| Shop | EMBER 100, FROST 300, VERDANT 750, GILDED 2,000, ECLIPSE 5,000, AURORA 15,000 | Essence in `/shop` |

### 8.3 The Armory

**Live.** 12 weapons unlock there today: one from each of the ten Gormak mobs, the Gormak Blade and the Mirage Edge.

It already works as a collection: the menu shows `Unlocked: n/12` and hints at which mob drops each locked one.

**Proposed:** a reward for filling a region's row. That turns the 2% drops into a goal that outlasts the zone.

### 8.4 Collections and the bestiary

**Collections are live** since 2026-11-14, in `/collections`. Every resource counts how many of it a player has ever mined or looted, keys excluded. Four tiers each: 250, 2,500, 25,000 and 250,000, paying 5, 25, 100 and 400 Essence. Nothing else, so they do not touch mining income.

**The bestiary is still proposed:** kills per mob. `kills` is counted per player, not per mob. It would be one more table shaped like the collections one.

### 8.5 Talismans

**Data only.** The Gormak Talisman is sold and says "Held in the off hand", with Luck 10 and Bloodlust 1. Nothing reads enchants from the off hand except Defence on a shield, so it does nothing today. See 11.3.

`DESIGN.md` leaves open whether talismans should start from the first zone and upgrade.

**Decision:** one talisman per region bought at the end, or a talisman line that upgrades like a pick.

### 8.6 Movement items

**Proposed**, from `PLANNING.md`: a grapple unlocked in Verdant Groves, the elytra in Emerald Peaks, and the Ironbound Quarry reachable only by elytra. That makes movement a gate of its own kind, alongside density and keys.

### 8.7 The menu

**Live:** `/menu` opens the hub with How to Play, Origin Isles, Backpack, Armory, Spawn and a progress icon.

The hub now also opens Gormak, Collections, Ranks and the Essence Shop. `verdant_hub` exists but is not linked, Verdant is not in the launch.

---

## 9. Gates, keys and travel

| From | To | Gate today | Status |
|:--|:--|:--|:--|
| Spawn | any Origin zone | none, walk | **Live** |
| Zone n | zone n+1 | density, and the last item being a cost | **Live** |
| Origin Isles | Gormak | Gormak Key (300 gold, 20 hide), tag `gormak` on the portal | **Live** |
| Gormak zone n | zone n+1 | density, mob strength | **Live** |
| Royal Tomb | boss | Tomb Key, 8% from both tomb mobs | **Live** |
| Gormak | Verdant Groves | nothing | **Missing**, see 11.5 |
| Verdant zone n | zone n+1 | density, and needing an axe | **Live** |
| Treehouse | Treant | nothing, spawns on a timer | **Decision** |

**Every gate between regions should cost something from the region's last zone.** The Gormak Key does: gold and hide are both Gold Reef resources.

**Proposed Verdant gate:** a Verdant Key at the Tomb Broker for ancient hearts and royal seals, giving the tag `verdant`. It needs one row in `resources_special.csv`, one in `guis_shops_components.csv`, and a tagged portal. No code.

---

## 10. Pacing budget and how to tune it

### 10.1 Targets against the sim

| Zone | Your target (min) | Sim (min) | Inside? |
|:--|:--|:--|:--|
| Cobblestone | about 2 | 2.0 | yes |
| Coal | about 2 | 2.0 | yes |
| Copper | about 2.6 | 2.6 | yes |
| Iron | about 4.2 | 4.4 | close |
| Gold | about 6.7 | 7.7 | 1 min over |
| **Origin total** | about 18 | 18.7 | yes |
| Abandoned City | 4 to 8 | 4.4 | yes |
| Dunes | 4 to 8 | 4.0 | on the floor |
| Canyons | 4 to 8 | 5.1 | yes |
| Sun Temple | 4 to 8 | 4.5 | yes |
| Royal Tomb | about 10 | 9.8 | yes |
| **Gormak total** | about 35 | 27.8 | 7 min under |
| Verdant, all five | none set | 39.7 | - |

### 10.2 What the sim does and does not count

- Counts: mining time by income, kills by time to kill, every purchase.
- Does not count: walking, finding the shop, reading, dying, dodging, crits, waiting for a mine to reset, waiting for a spawner.

Real play runs longer than the sim. How much longer is the main thing the next playtest should measure (section 14).

### 10.3 Things to look at

- **Gold is a minute over** the figure you settled on. Whether that matters depends on whether 6.7 was a target or just what the sim said that day.
- **The Dunes are at the bottom of the range.** With the planned parkour they will run longer in play than in the sim.
- **Gormak totals 27.8 against about 35.** Each zone is inside its own range; the total is under because four zones sit near the low end.

### 10.4 The tuning knobs, in the order to reach for them

1. **Resource amounts in `guis_shops_components.csv`.** Your standing rule: tune here, never gear costs.
2. **Drop ranges** in the mob and block CSVs.
3. **Mob health and defence** for time to kill.
4. **Mob attack** for danger. One protection level is a quarter of a tier.
5. **`MOB_DAMAGE_SCALE` and `RANGED_SCALE`** in `CombatFormulas`, which move every mob at once. Keep `balance_sim.py` in step.

### 10.5 The procedure after any data change

```bash
python scripts/generate.py
```

```bash
python scripts/balance/balance_sim.py
```

Then check every zone against 10.1. With the server running, `python scripts/generate.py --live` then `/refresh` loads the change without a restart.

---

## 11. Gaps found while writing this

Each is something the data or the design promises that a player would not get today.

### 11.1 The Gormak boss fight is untested

Built on 2026-11-14 (see 5.5): boss bar, stomp, Gormak Ghoul waves and the totem. It has been compiled and booted, never fought. Whether 1500 health gives the fight room for all of it is the first thing to find out.

### 11.2 The Mirage does not appear

It has stats, gear and drops, but no spawn rule and no invisibility. `mirage_shard` is not a cost of anything, and `generate.py --check` reports it.

### 11.3 The Gormak Talisman does nothing

Luck and Bloodlust are read from the main hand. An off-hand talisman contributes neither. A player who farms three boss kills for it gets no effect.

### 11.4 The Tomb picks and densities 8 and 9

The Tomb has no mine. Its two picks cost eternal souls and exist to carry Fortune from 9 to 10. Densities 8 and 9 are skipped; the next block after the Sun Temple's 7 is the Oak Grove's 10.

Either the Tomb was meant to have a density 8 mine, or the jump is deliberate. **Decision.**

### 11.5 No way into Verdant Groves

There is no key, no gate trader and no menu link. A portal can be placed by hand, but nothing in the data makes a player earn it.

### 11.6 The hub menu reaches Origin Isles only

`gormak_hub` and `verdant_hub` are defined and unreachable from `/menu`.

### 11.7 Stats that go down on an upgrade

| Upgrade | What drops |
|:--|:--|
| `copper_pick_4` to `copper_pick_5` | Luck 5 to 4 (Fortune rises 2 to 3) |
| `temple` armour 5 to `tomb` armour 1 | Sustenance 4 to 3 |
| `gormak_blade` (drop) to `oak_sword_1` | Luck 12 to 10, Looting 9 to 8 |
| `mirage_edge` (drop) to anything | Luck 15 is not matched until the Dark Oak Thicket picks |

The first two match `DESIGN.md` exactly, so they may be intended. They do break "each pick beats the last". **Decision.**

### 11.8 Mob gear nobody wears

`generate.py --check` lists ten armour pieces defined for mobs that no mob has equipped: serf leggings and boots, scavenger boots, scout leggings, stalker boots, zealot boots, vizier leggings and boots, mirage leggings and boots.

### 11.9 Rank and level never change

Fixed on 2026-11-14, see 8.2.

### 11.10 Essence has no use

Fixed on 2026-11-14 for ranks, see 8.1. Luck on a pick still only matters through Essence.

---

## 12. Roadmap to the demo

The demo is the Origin Isles. Gormak is tuned and could follow. Each milestone ends in something you can play.

### M0: the data is settled

- [ ] Every Origin zone inside its target in 10.1
- [ ] The decisions in section 13 that touch Origin are made (rank and level lines, Essence)
- [ ] `python scripts/generate.py --check` passes with no warning you have not accepted

### M1: the Origin Isles are built

Per zone, in the order `docs/COMMANDS.md` gives:

- [ ] Mine created, blocks set, threshold and delay set, teleport point set
- [ ] Spawner created for the zone's mob (Copper, Iron, Gold), limit and radius tuned so mobs are never the wait
- [ ] Trader placed
- [ ] A path from the previous zone that a new player finds without being told

Then for the region:

- [ ] Spawn set
- [ ] Gormak Gate built, portal tagged, Gate Warden placed
- [ ] `scripts/server.sh backup` taken

### M2: the first ten minutes

- [ ] A new player reaches the Cobblestone Quarry and buys a pick with no help
- [ ] The action bar message for a block that is too dense is understood on sight
- [ ] The How to Play pages match what the game does
- [ ] Nothing on the sidebar reads as broken

### M3: a full Origin run

- [ ] One uninterrupted run from join to holding the Gormak Key, timed per zone
- [ ] Real time against sim time recorded for each zone (section 14)
- [ ] No shop sells something a player cannot afford the inputs to
- [ ] No mine empties faster than it resets with one player in it

### M4: it survives a server

- [ ] Two players in the same mine at once
- [ ] A restart mid-session: backpack, unlocks, profile and position all come back
- [ ] A crash (kill the process): at most 5 minutes of mining lost (the backpack autosave) and 10 minutes of kills, blocks mined and Essence (the profile autosave)
- [ ] `python scripts/deploy.py bundle` deployed to the real host and joined from outside

### M5: Gormak, if it is in the demo

- [ ] All five zones built as in M1
- [ ] The boss fight played and tuned (11.1), the decisions on 11.2 and 11.3 made
- [ ] A full run from the gate to the boss, timed
- [ ] The boss killed by a player in exit gear without a totem

### Explicitly not in the demo

Verdant Groves, every proposal in section 7, ranks, collections, the bestiary, crates, the gauntlet.

---

## 13. Decisions that are yours

Grouped by how soon each one blocks something.

### Blocks the demo

| # | Decision | Options |
|:--|:--|:--|
| D1 | The `RANK` and `LEVEL` sidebar lines | Hide for the demo. Or leave as they are. Or wire ranks to milestones (8.2) |
| D2 | Essence with nothing to spend it on | Ship as is, a number that only goes up. Or hide it. Or build one sink first (8.1) |
| D3 | Is Gormak in the demo | Origin only. Or Origin and Gormak |
| D4 | Gold at 7.7 min against 6.7 | Accept. Or trim gold costs |

### Blocks Gormak

| # | Decision | Options |
|:--|:--|:--|
| D5 | The boss fight (11.1) | Built. After playing it: his health, the stomp's reach and force, wave sizes |
| D6 | The Mirage (11.2) | Remove it from the data for now. Or give it ordinary spawners. Or build the rare spawn and invisibility |
| D7 | The Talisman (11.3) | Remove it from the shop for now. Or make off-hand Luck and Bloodlust count |
| D8 | The Tomb and densities 8 and 9 (11.4) | Leave the jump. Or give the Tomb a density 8 mine |
| D9 | Gormak's total, 27.8 against about 35 | Accept, since every zone is in range. Or raise costs in the zones near 4 minutes |

### Blocks Verdant

| # | Decision | Options |
|:--|:--|:--|
| D10 | The gate into Verdant (11.5) | A Verdant Key as in section 9. Or something else |
| D11 | Time targets for Verdant | Not set. The sim says 6 to 10 minutes a zone |
| D12 | The Treant | Timer spawn as now. Or a summon key like Gormak |
| D13 | Stats that drop on upgrade (11.7) | Intended. Or fix the rows |

### Shapes everything after

| # | Decision | Options |
|:--|:--|:--|
| D14 | Pick and axe | Stay separate. Or merge into the god tool, and where |
| D15 | Talismans | One per region. Or an upgrading line from the start |
| D16 | Ranks | From milestones. Or from blocks mined and playtime |
| D17 | Which region after Verdant | The Great Canopy. Or Emerald Peaks |
| D18 | Does the Treehouse content move to the Canopy | As `PLANNING.md` says. Or it stays in Verdant |

---

## 14. Playtest protocol

### 14.1 What to record on every run

| Per zone | How |
|:--|:--|
| Real minutes from first block to last purchase | A stopwatch, or timestamps in the server log |
| Sim minutes | 10.1 |
| Deaths | Count |
| Times the player did not know what to do next | Count, and what they tried |
| Times a mine was empty or a mob was missing | Count |

The ratio of real to sim minutes is the number to carry forward. If Origin runs at 1.5 times the sim, a 4 minute sim zone is a 6 minute zone, and targets should be read that way.

### 14.2 The three runs worth doing

1. **Cold:** someone who has never seen the game, no help, Origin only. This tests M2.
2. **Clean:** you, playing straight through, no admin commands. This gives the real-to-sim ratio.
3. **Abusive:** trying to skip zones, sell loops, placing blocks, anvil renames, dropping gear, relogging mid-purchase, two players on one mine.

### 14.3 After the playtest

- Tuning goes into the CSVs, one commit per pass, with the reason in the message, as the last three were.
- Any target that moves is your call, and the sim is rerun before the next test.

---

## 15. Appendix

### 15.1 Adding a zone, step by step

All data, no code, as long as the zone keeps the rules in 7.1.

1. `resources.csv`: the mine's resource and each mob's drop. Order in this file is the order in the backpack.
2. `blocks.csv`: the plain and rich block, with the zone's density.
3. `mobs_<region>.csv`: each mob with health, defence, attack, equipment, attributes and drops.
4. `gear_picks.csv`, `gear_weapons.csv`, `gear_armor.csv`, `gear_other.csv`: every tier.
5. `icons.csv`: the zone's menu icon.
6. `guis_shops.csv`: the shop, its armour menu and any others.
7. `guis_shops_components.csv`: every trade, with the previous tier as a cost.
8. `traders.csv`: the trader.
9. Add the shop to the region's hub.
10. `python scripts/generate.py`, then the sim.
11. In game, in the order in `docs/COMMANDS.md`: mine, blocks, spawner, trader, portal.

### 15.2 Where each rule lives

| Rule | File |
|:--|:--|
| Fortune, density, drops | `core/DataRegistry.java`, `features/mine/MineListener.java` |
| Blast shapes | `features/mine/MineListener.java` |
| Tool speed, right tool | `core/data/ItemFactory.java` |
| Essence and Luck | `features/player/Essence.java` |
| Looting | `common/loot/Looting.java` |
| All combat maths | `features/combat/CombatFormulas.java` |
| Shields | `features/combat/ShieldBlock.java` |
| Keys and summons | `features/keys/KeyItems.java` |
| Portals and tags | `features/portal/` |
| The Armory | `features/armory/` |
| The sim | `scripts/balance/balance_sim.py` |

### 15.3 Naming

- Gear: `<zone>_<kind>_<tier>`, for example `copper_pick_3`. Tier 0 is the starter.
- Mob gear: `<mob>_<piece>_1`, mob weapons by their own name.
- Shops: `<zone>_shop`, `<zone>_armor`, `<zone>_shields`.
- Traders: `<zone>_trader`.
- Regions on gear and resources: `origin_isles`, `gormak_<zone>`, `verdant_<zone>`.

### 15.4 The numbers to remember

- A block of density `d` needs Fortune `d`.
- Four protection levels are one tier. Even is 0.6 hearts a hit; one tier under is 1.2.
- Mob attack goes up 4 a zone.
- Every zone in Gormak is 31 purchases. Origin's are 11 or 22.
- The demo is 18 minutes of Origin by the sim, 46 with Gormak.
