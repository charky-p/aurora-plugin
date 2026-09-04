# Aurora Server Design

## 🌍 Overworld: Origin Isles

### Zone 0: Starter
| Tier   | Type    | Material | Enchants |
|:-------|:--------|:---------|:---------|
| **T0** | Pickaxe | Wooden   | *None*   |
| **T0** | Sword   | Wooden   | *None*   |
| **T0** | Armor   | Leather  | *None*   |

### Zone 1: Cobblestone (Mine)
| Tier | Type    | Material | Enchants                        |
|:-----|:--------|:---------|:--------------------------------|
| 1    | Pickaxe | Stone    | *None*                          |
| 2    | Pickaxe | Stone    | Efficiency 1                    |
| 3    | Pickaxe | Stone    | Efficiency 2                    |
| 4    | Pickaxe | Stone    | Efficiency 2, Fortune 1         |
| 5    | Pickaxe | Stone    | Efficiency 2, Fortune 1, Luck 1 |
| 1    | Sword   | Stone    | *None*                          |
| 2    | Sword   | Stone    | Sharpness 1                     |
| 1    | Armor   | Leather  | Protection 1                    |

### Zone 2: Coal (Mine)
| Tier | Type    | Material | Enchants                        |
|:-----|:--------|:---------|:--------------------------------|
| 1    | Pickaxe | Stone    | Efficiency 3, Fortune 1, Luck 1 |
| 2    | Pickaxe | Stone    | Efficiency 3, Fortune 1, Luck 2 |
| 3    | Pickaxe | Stone    | Efficiency 4, Fortune 1, Luck 2 |
| 4    | Pickaxe | Stone    | Efficiency 4, Fortune 2, Luck 2 |
| 5    | Pickaxe | Stone    | Efficiency 4, Fortune 2, Luck 3 |
| 1    | Sword   | Stone    | Sharpness 2                     |
| 2    | Sword   | Stone    | Sharpness 2, Luck 1             |
| 1    | Armor   | Leather  | Protection 2                    |

### Zone 3: Copper (Mine) + Chicken (Mob)
Note: First 2 tiers of pick and sword, and first tier of armor needs
copper, next 2 tiers need feathers, the final tier needs both. This pattern
follows for the next zones.

| Tier | Type    | Material | Enchants                        |
|:-----|:--------|:---------|:--------------------------------|
| 1    | Pickaxe | Copper   | Efficiency 4, Fortune 2, Luck 3 |
| 2    | Pickaxe | Copper   | Efficiency 4, Fortune 2, Luck 4 |
| 3    | Pickaxe | Copper   | Efficiency 5, Fortune 2, Luck 4 |
| 4    | Pickaxe | Copper   | Efficiency 5, Fortune 2, Luck 5 |
| 5    | Pickaxe | Copper   | Efficiency 5, Fortune 3, Luck 4 |
| 1    | Sword   | Copper   | Sharpness 2, Luck 1             |
| 2    | Sword   | Copper   | Sharpness 3, Luck 1             |
| 3    | Sword   | Copper   | Sharpness 4, Luck 1             |
| 4    | Sword   | Copper   | Sharpness 5, Luck 1             |
| 5    | Sword   | Copper   | Sharpness 5, Luck 1, Looting 1  |
| 1    | Armor   | Copper   | Protection 2                    |
| 2    | Armor   | Copper   | Protection 3                    |
| 3    | Armor   | Copper   | Protection 4                    |

### Zone 4: Iron (Mine) + Sheep (Mob)
| Tier | Type    | Material | Enchants                        |
|:-----|:--------|:---------|:--------------------------------|
| 1    | Pickaxe | Iron     | Efficiency 5, Fortune 3, Luck 4 |
| 2    | Pickaxe | Iron     | Efficiency 5, Fortune 3, Luck 5 |
| 3    | Pickaxe | Iron     | Efficiency 6, Fortune 3, Luck 5 |
| 4    | Pickaxe | Iron     | Efficiency 7, Fortune 3, Luck 5 |
| 5    | Pickaxe | Iron     | Efficiency 7, Fortune 4, Luck 5 |
| 1    | Sword   | Iron     | Sharpness 5, Luck 1, Looting 1  |
| 2    | Sword   | Iron     | Sharpness 6, Luck 1, Looting 1  |
| 3    | Sword   | Iron     | Sharpness 6, Luck 2, Looting 1  |
| 4    | Sword   | Iron     | Sharpness 7, Luck 2, Looting 1  |
| 5    | Sword   | Iron     | Sharpness 7, Luck 2, Looting 2  |
| 1    | Armor   | Iron     | Protection 4                    |
| 2    | Armor   | Iron     | Protection 5                    |
| 3    | Armor   | Iron     | Protection 6                    |

### Zone 5: Gold (Mine) + Cow (Mob)
| Tier | Type    | Material | Enchants                                 |
|:-----|:--------|:---------|:-----------------------------------------|
| 1    | Pickaxe | Gold     | Efficiency 7, Fortune 4, Luck 5          |
| 2    | Pickaxe | Gold     | Efficiency 8, Fortune 4, Luck 5          |
| 3    | Pickaxe | Gold     | Efficiency 8, Fortune 4, Luck 6          |
| 4    | Pickaxe | Gold     | Efficiency 8, Fortune 5, Luck 6          |
| 5    | Pickaxe | Gold     | Efficiency 8, Fortune 5, Luck 6, Blast 1 |
| 1    | Sword   | Gold     | Sharpness 7, Luck 2, Looting 2           |
| 2    | Sword   | Gold     | Sharpness 7, Luck 3, Looting 2           |
| 3    | Sword   | Gold     | Sharpness 8, Luck 3, Looting 2           |
| 4    | Sword   | Gold     | Sharpness 8, Luck 4, Looting 2           |
| 5    | Sword   | Gold     | Sharpness 8, Luck 4, Looting 3           |
| 1    | Armor   | Gold     | Protection 6                             |
| 2    | Armor   | Gold     | Protection 7                             |
| 3    | Armor   | Gold     | Protection 8                             |

## 🌍 Overworld: Gormak

### Zone 1: Abandoned City - Scavenger Husk (Mob) + Hollowed Serf (Mob) + Stone Bricks (Mine)
Note: The Gormak zones have 2 mobs and a mine, where the mine is a supplement. The
first 2 tiers are scavenger husk, the next 2 tiers are hollowed serf,
the final tier is all 3. For pick it is scavenger husk + stone bricks, hollowed serf + stone bricks,
then all 3. Sustenance restores a hunger point very slowly, bloodlust increases damage after kills.
Shield deflection will allow the shield to work normally for high levels (relative to mob), but if
not high enough, reduced damage is taken.

| Tier | Type    | Material | Enchants                                     |
|:-----|:--------|:---------|:---------------------------------------------|
| 1    | Pickaxe | Gold     | Efficiency 8, Fortune 5, Luck 7, Blast 1     |
| 2    | Pickaxe | Gold     | Efficiency 9, Fortune 5, Luck 7, Blast 1     |
| 3    | Pickaxe | Gold     | Efficiency 9, Fortune 6, Luck 7, Blast 1     |
| 1    | Sword   | Gold     | Sharpness 8, Luck 5, Looting 3               |
| 2    | Sword   | Gold     | Sharpness 9, Luck 5, Looting 3               |
| 3    | Sword   | Gold     | Sharpness 10, Luck 5, Looting 3,             |
| 4    | Sword   | Gold     | Sharpness 10, Luck 5, Looting 4,             |
| 5    | Sword   | Gold     | Sharpness 10, Luck 5, Looting 4, Bloodlust 1 |
| 1    | Armor   | Gold     | Protection 9                                 |
| 2    | Armor   | Gold     | Protection 10                                |
| 3    | Armor   | Gold     | Protection 11                                |
| 4    | Armor   | Gold     | Protection 12                                |
| 5    | Armor   | Gold     | Protection 12, Sustenance 1                  |
| 1    | Shield  | N/A      | *None*                                       |
| 2    | Shield  | N/A      | Deflection 1                                 |
| 3    | Shield  | N/A      | Deflection 2                                 |

### Zone 2: Dunes - Dune Scout (Mob) + Sand Stalker (Mob) + Sandstone (Mine)
Note: Has an Oasis, which has a higher chance of spawning a mirage (mob), mirages can spawn
anywhere in gormak though.

| Tier | Type    | Material | Enchants                                     |
|:-----|:--------|:---------|:---------------------------------------------|
| 1    | Pickaxe | Gold     | Efficiency 9, Fortune 6, Luck 8, Blast 1     |
| 2    | Pickaxe | Gold     | Efficiency 10, Fortune 6, Luck 8, Blast 1    |
| 3    | Pickaxe | Gold     | Efficiency 10, Fortune 7, Luck 8, Blast 1    |
| 1    | Sword   | Gold     | Sharpness 10, Luck 6, Looting 4, Bloodlust 1 |
| 2    | Sword   | Gold     | Sharpness 11, Luck 6, Looting 4, Bloodlust 1 |
| 3    | Sword   | Gold     | Sharpness 11, Luck 7, Looting 4, Bloodlust 1 |
| 4    | Sword   | Gold     | Sharpness 12, Luck 7, Looting 4, Bloodlust 1 |
| 5    | Sword   | Gold     | Sharpness 12, Luck 7, Looting 5, Bloodlust 1 |
| 1    | Armor   | Gold     | Protection 13, Sustenance 1                  |
| 2    | Armor   | Gold     | Protection 14, Sustenance 1                  |
| 3    | Armor   | Gold     | Protection 15, Sustenance 1                  |
| 4    | Armor   | Gold     | Protection 16, Sustenance 1                  |
| 5    | Armor   | Gold     | Protection 16, Sustenance 2                  |
| 1    | Shield  | N/A      | Deflection 3                                 |
| 2    | Shield  | N/A      | Deflection 4                                 |
| 3    | Shield  | N/A      | Deflection 5                                 |

### Zone 3: Howling Canyons - Scarab Zealot (Mob) + Hollowed Legionnaire (Mob) + Red Sandstone (Mine)
Note: Scarab Zealot uses a Khopesh, Hollowed Legionnaire uses a Halberd (Spear).

| Tier | Type    | Material | Enchants                                     |
|:-----|:--------|:---------|:---------------------------------------------|
| 1    | Pickaxe | Gold     | Efficiency 10, Fortune 7, Luck 9, Blast 1    |
| 2    | Pickaxe | Gold     | Efficiency 10, Fortune 7, Luck 10, Blast 1    |
| 3    | Pickaxe | Gold     | Efficiency 10, Fortune 8, Luck 10, Blast 1   |
| 1    | Sword   | Gold     | Sharpness 12, Luck 7, Looting 5, Bloodlust 1 |
| 2    | Sword   | Gold     | Sharpness 13, Luck 7, Looting 5, Bloodlust 1 |
| 3    | Sword   | Gold     | Sharpness 13, Luck 8, Looting 5, Bloodlust 1 |
| 4    | Sword   | Gold     | Sharpness 14, Luck 8, Looting 5, Bloodlust 1 |
| 5    | Sword   | Gold     | Sharpness 14, Luck 8, Looting 6, Bloodlust 1 |
| 1    | Armor   | Gold     | Protection 17, Sustenance 2                  |
| 2    | Armor   | Gold     | Protection 18, Sustenance 2                  |
| 3    | Armor   | Gold     | Protection 19, Sustenance 2                  |
| 4    | Armor   | Gold     | Protection 20, Sustenance 2                  |
| 5    | Armor   | Gold     | Protection 20, Sustenance 3                  |
| 1    | Shield  | N/A      | Deflection 6                                 |
| 2    | Shield  | N/A      | Deflection 7                                 |
| 3    | Shield  | N/A      | Deflection 8                                 |

### Zone 4: Sun Temple - Tomb Guard (Mob) + Vizier Mage (Mob) + Raw Gold (Mine)
Note: Defence gives protection points even when not held (or in offhand)

| Tier | Type    | Material | Enchants                                      |
|:-----|:--------|:---------|:----------------------------------------------|
| 1    | Pickaxe | Gold     | Efficiency 10, Fortune 8, Luck 11, Blast 1    |
| 2    | Pickaxe | Gold     | Efficiency 11, Fortune 8, Luck 11, Blast 1    |
| 3    | Pickaxe | Gold     | Efficiency 11, Fortune 9, Luck 11, Blast 1    |
| 1    | Sword   | Gold     | Sharpness 14, Luck 9, Looting 6, Bloodlust 1  |
| 2    | Sword   | Gold     | Sharpness 15, Luck 9, Looting 6, Bloodlust 1  |
| 3    | Sword   | Gold     | Sharpness 15, Luck 10, Looting 6, Bloodlust 1 |
| 4    | Sword   | Gold     | Sharpness 16, Luck 10, Looting 6, Bloodlust 1 |
| 5    | Sword   | Gold     | Sharpness 16, Luck 10, Looting 7, Bloodlust 1 |
| 1    | Armor   | Gold     | Protection 21, Sustenance 3                   |
| 2    | Armor   | Gold     | Protection 22, Sustenance 3                   |
| 3    | Armor   | Gold     | Protection 23, Sustenance 3                   |
| 4    | Armor   | Gold     | Protection 24, Sustenance 3                   |
| 5    | Armor   | Gold     | Protection 24, Sustenance 4                   |
| 1    | Shield  | N/A      | Deflection 9                                  |
| 2    | Shield  | N/A      | Deflection 10                                 |
| 3    | Shield  | N/A      | Deflection 10, Defence 1                      |

### Zone 5: Royal Tomb - Royal Myrmidon (Mob) + Pharaoh (Mob) + Gormak The Eternal (Boss)
Note: Gormak the Eternal is a boss, that can be summoned with a key dropped by the other 2 mobs,
it will summon fallen husks periodically which chase after the player fast, it also has a totem
of undying that it will use, and has a boss bar, only 1 can be summoned at a time, and he is
summoned on a golden throne in the tomb. Totem of Undying is just a regular totem of undying,
can be purchased cheaply (it is not supposed to replace the shield yet). The Gormak talisman
(TODO: not sure whether it should be upgradeable from the previous stuff, or upgradeable from
the next mine, and also idk what buffs it should give, maybe i should do talisman from the start?)

| Tier | Type     | Material         | Enchants                                      |
|:-----|:---------|:-----------------|:----------------------------------------------|
| 1    | Pickaxe  | Gold             | Efficiency 12, Fortune 9, Luck 11, Blast 1    |
| 2    | Pickaxe  | Gold             | Efficiency 12, Fortune 10, Luck 11, Blast 1   |
| 1    | Sword    | Gold             | Sharpness 16, Luck 10, Looting 7, Bloodlust 1 |
| 2    | Sword    | Gold             | Sharpness 17, Luck 10, Looting 7, Bloodlust 1 |
| 3    | Sword    | Gold             | Sharpness 17, Luck 10, Looting 8, Bloodlust 1 |
| 4    | Sword    | Gold             | Sharpness 18, Luck 10, Looting 8, Bloodlust 1 |
| 5    | Sword    | Gold             | Sharpness 18, Luck 10, Looting 8, Bloodlust 2 |
| 1    | Armor    | Gold             | Protection 25, Sustenance 3                   |
| 2    | Armor    | Gold             | Protection 25, Sustenance 4                   |
| 3    | Armor    | Gold             | Protection 25, Sustenance 5                   |
| 4    | Armor    | Gold             | Protection 25, Sustenance 5, Thorns 1         |
| N/A  | Utility  | Totem of Undying | N/A                                           |
| N/A  | Talisman | Gormak           | N/A                                           |

## 🌍 Overworld: Verdant Groves
TODO
