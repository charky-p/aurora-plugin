# Aurora world planning

Ideas for what comes after the regions in `scripts/data/world/`: new regions, zones, worlds and mini worlds, with their blocks and mobs. Nothing here is built or agreed. When an idea is picked, its rows go into the CSVs (`regions.csv`, `zones.csv`, `mines.csv`, `blocks.csv`, `scripts/data/mobs/`) and it leaves this file. Smaller feature ideas stay in [DESIGN.md](../DESIGN.md).

Each idea is marked **idea** (not discussed), **liked** (worth doing, details open) or **agreed** (ready to go into the CSVs).

## Rules for every new region

- **Mobs** may only melee, shoot arrows (bows and crossbows), throw potions (witches, `attack_cooldown` empty) or cast fangs (evokers, no vexes). Creepers, slimes, ghasts, blazes, shulkers, llamas and the dragon are not handled by combat. Mobs marked *(check)* below have a vanilla extra (tridents, a sonic boom, teleporting) that needs testing first.
- **Mines** use the `mines.csv` form, `id,tool,blocks` with shares adding to 100. Custom ores follow the tin/lead pattern: a concrete for the ore and its glazed terracotta for the rich block (5-9 drops). Prefer materials no other block uses yet (see `blocks.csv`).
- **Numbers** (density, xp, health, attack, prices) are not set here: they come from the balance scripts, with densities +1 a zone through the region and shops sized by the 5/3/1/0 upgrade rule.
- **Shape**: 4-6 zones, the last one a boss zone with no mine, the boss summoned by a key its region's mobs drop and its trophy paying for that zone's last five upgrades.

## Custom ore colours

Custom ores are a concrete (ore) and its glazed terracotta (rich block). Taken today: white, light gray, gray, black, brown, yellow, light blue. This plan hands out the rest, so every colour is spoken for:

| Colour | Ore | Where |
|---|---|---|
| blue | cobalt | Caverns |
| lime | mycelium wall | Fungal Deep |
| cyan | aurorite | Aurora Spire |
| orange | starmetal | Aurora Spire |
| red | obsidian brick | Obsidian Necropolis |
| purple | chorus | The End |
| magenta | frost ore | Frostbite Hollow |
| green | lunar crystal | Moonbase |
| pink | dragon scale | Dragon's Roost |

After that, new ores need another family: stained glass with concrete powder is one option, or terracotta with glazed terracotta (yellow and light blue terracotta are partly used).

## Where things stand

| Stage | Overworld line | Gormak line (duneworld) | Elderwood line (elderworld) | Sky line (skyworld) |
|---|---|---|---|---|
| 1 | Origin Isles | | | |
| 2 | Verdant Groves | Gormak | | |
| 3 | Sunken Marsh | Khorvath | Elderwood Roots | |
| 4 | Emerald Peaks | | | |
| 5 | | The Duat | Elderwood Heart | Cloud Isles |
| 6 | Ironbound Quarry | | | Sky Kingdom |
| 7 | Golem's Rest | | | |

Released: Origin Isles, Gormak, Verdant Groves. Every line ends somewhere in stages 5-7, so the next regions below continue each line, then two new worlds (Nether, End) join the lines together.

## Next regions

### Caverns (overworld, stage 8) — idea

Opened by the last Overworld Core step (the `caverns` tag already exists). Under the overworld: from bright caves down to the deep dark. New tool focus: none, pick-heavy, with a hoe zone for sculk.

| Zone | Mine | Mob |
|---|---|---|
| Tuff Tunnels | tuff, cobbled deepslate | lost miner (ZOMBIE_VILLAGER), tunnel skeleton (SKELETON) |
| Amethyst Geode | amethyst, cobalt | geode mite (SILVERFISH), crystal spider (CAVE_SPIDER) |
| Lush Grotto | azalea (hoe), rooted dirt (shovel) | grotto witch (WITCH) |
| Deepslate Halls | deepslate bricks, cobalt | deep guard (WITHER_SKELETON) |
| Sculk Depths | sculk (hoe) | echo stalker (ZOMBIE), shrieker (SKELETON) |
| The Hollow | none | The Hollow Warden (WARDEN, boss) *(check: sonic boom)* |

```
tuff,pick,tuff:60|tuff_bricks:40
cobbled_deepslate,pick,cobbled_deepslate:70|deepslate_tiles:30
amethyst,pick,amethyst_block:80|budding_amethyst:20
cobalt,pick,cobalt_ore:60|cobalt_rich:40        (BLUE_CONCRETE / BLUE_GLAZED_TERRACOTTA)
azalea,hoe,azalea_leaves:70|flowering_azalea_leaves:30
rooted_dirt,shovel,rooted_dirt:100
deepslate_bricks,pick,deepslate_bricks:70|chiseled_deepslate:30
sculk,hoe,sculk:80|sculk_catalyst:20
```

DESIGN.md also puts calcite here, but calcite is already the Stony Crags mine; tuff takes its place. The Hollow Warden could be a stronger stand-in (an IRON_GOLEM reskinned as sculk) if the warden's own attacks can't be turned off.

### Storm Citadel (skyworld, stage 7) — idea

After the Sky Kingdom, already sketched in DESIGN.md: a darker castle in the storm clouds. Polished blackstone bricks are taken by another block, so the gate uses deepslate tiles and plain blackstone.

| Zone | Mine | Mob |
|---|---|---|
| Storm Gate | blackstone, deepslate tiles | storm knight (VINDICATOR) |
| Lightning Forge | storm copper, copper bulb | forge golem (IRON_GOLEM) |
| Citadel Keep | tinted glass, obsidian | storm crossbowman (PILLAGER) |
| The Eye | none | The Storm Tyrant (EVOKER, boss: fangs) |

```
storm_stone,pick,blackstone:60|deepslate_tiles:40
copper_bulb,pick,copper_bulb:70|exposed_copper_bulb:30
tinted_glass,pick,tinted_glass:100
obsidian,pick,obsidian:80|crying_obsidian:20
```

`storm_copper` already exists (Storm Front) and can come back here at a higher density. The Storm Mace (Pirate Captain drop) could be upgraded here.

### Sunfall Savanna (elderworld, stage 6) — idea

After Elderwood Heart: the jungle opens onto a dry savanna. Acacia is the one wood not used yet. Axe and shovel focus.

| Zone | Mine | Mob |
|---|---|---|
| Acacia Plains | acacia log | savanna hyena (WOLF) |
| Termite Mounds | terracotta, hay (hoe) | mound mite (SILVERFISH), poacher (PILLAGER) |
| Baobab Ridge | acacia wood, red terracotta | savanna rhino (RAVAGER) |
| Watering Hole | mud, clay | savanna witch (WITCH) |
| Pride Rock | none | The Great Rhino (RAVAGER, boss) with a poacher rider |

```
acacia_log,axe,acacia_log:60|acacia_wood:25|stripped_acacia_log:15
terracotta,pick,terracotta:70|yellow_terracotta:30
hay,hoe,hay_block:100
baobab,axe,stripped_acacia_wood:70|red_terracotta:30
```

The Watering Hole reuses `mud` and `clay` (Mudflats, Maple Grove): fine if repeats are allowed, otherwise swap for something else.

### The Glass Sea (duneworld, stage 6) — idea

After the Duat: past the underworld, a desert where the sun fused the sand into glass, ending at a drowned pyramid.

| Zone | Mine | Mob |
|---|---|---|
| Glass Flats | sun glass | glass husk (HUSK) |
| Salt Pans | salt | salt stray (STRAY) |
| Oasis | prismarine, sea lantern | oasis drowned (DROWNED) *(check: tridents)* |
| Sunken Pyramid | dark prismarine | pyramid guard (WITHER_SKELETON), drowned priest (WITCH) |
| The Abyss | none | The Dune Behemoth (RAVAGER, boss) |

```
sun_glass,pick,glass:60|yellow_stained_glass:40
salt,pick,salt_ore:60|salt_rich:40              (WHITE_TERRACOTTA / WHITE_STAINED_GLASS)
prismarine,pick,prismarine:70|sea_lantern:30
dark_prismarine,pick,dark_prismarine:70|prismarine_bricks:30
```

### Fungal Deep (overworld, stage 9) — idea

Below the Caverns: a giant mushroom underworld lit by shroomlights. Hoe and axe focus, the first region where the hoe is the main tool.

| Zone | Mine | Mob |
|---|---|---|
| Spore Fields | mycelium (shovel), mushroom caps | spore zombie (ZOMBIE) |
| Shroomlight Grove | shroomlight (hoe) | glowcap witch (WITCH) |
| Mycelium Tunnels | mycelium wall, cobalt | fungal spider (SPIDER) |
| Rot Pits | rotting log (axe), soul mud | rot husk (HUSK), bog archer (BOGGED) |
| The Mother Cap | none | The Mother Cap (EVOKER, boss: fangs as roots) |

```
mycelium,shovel,mycelium:80|podzol:20
mushroom_cap,axe,red_mushroom_block:50|brown_mushroom_block:50     (both used in Glowmire: reuse or swap)
shroomlight,hoe,shroomlight:100
mycelium_wall,pick,mycelium_ore:60|mycelium_rich:40     (LIME_CONCRETE / LIME_GLAZED_TERRACOTTA)
rotting_log,axe,stripped_mangrove_wood:70|brown_terracotta:30
```

### Aurora Spire (skyworld, stage 8) — idea

After the Storm Citadel: above the storm, a crystal tower under the northern lights the game is named after. The sky line's finale before the End.

| Zone | Mine | Mob |
|---|---|---|
| Frostcloud Shelf | frost cloud | sky stray (STRAY) |
| Prism Gardens | prism glass | prism bee (BEE) |
| Borealis Steps | aurorite | aurora sentinel (SKELETON), lightbound knight (VINDICATOR) |
| Starfall Crater | starmetal | fallen star (ZOMBIE, glowing) |
| The Spire's Crown | none | The Aurora Warden (WITHER_SKELETON, boss) |

```
frost_cloud,shovel,light_blue_concrete_powder:100
prism_glass,pick,light_blue_stained_glass:40|magenta_stained_glass:30|lime_stained_glass:30
aurorite,pick,aurorite_ore:60|aurorite_rich:40      (CYAN_CONCRETE / CYAN_GLAZED_TERRACOTTA)
starmetal,pick,starmetal_ore:60|starmetal_rich:40   (ORANGE_CONCRETE / ORANGE_GLAZED_TERRACOTTA)
```

A good place for an Aurora-themed cosmetic set (trail, suffix) as the trophy reward.

### Obsidian Necropolis (duneworld, stage 7) — idea

After the Glass Sea: the black city of the old kings, the Gormak line's last region. Heavy pick focus, every mob hostile.

| Zone | Mine | Mob |
|---|---|---|
| Ash Gates | blackstone rubble, gilded ash | ash legionnaire (HUSK) |
| Embalming Halls | natron (shovel), linen (hoe) | mummy (ZOMBIE), embalmer (WITCH) |
| Tomb of Kings | obsidian brick | royal archer (SKELETON), tomb myrmidon (WITHER_SKELETON) |
| Hall of Mirrors | mirror glass | mirage twin (VINDICATOR, the Mirage's bigger kin) |
| The Black Throne | none | The Undying King (WITHER_SKELETON, boss with archer guards) |

```
blackstone_rubble,pick,cracked_deepslate_bricks:70|gilded_blackstone:30   (gilded_blackstone shared with the Nether)
natron,shovel,suspicious_sand:100       (check: suspicious sand loot table must be off)
linen,hoe,white_wool:70|light_gray_wool:30
obsidian_brick,pick,obsidian_brick_ore:60|obsidian_brick_rich:40   (RED_CONCRETE / RED_GLAZED_TERRACOTTA)
mirror_glass,pick,light_gray_stained_glass:60|white_stained_glass:40
```

### Thornwood (elderworld, stage 7) — idea

After the Sunfall Savanna: a cursed briar forest, the Elderwood line's finale, close in spirit to the pale garden.

| Zone | Mine | Mob |
|---|---|---|
| Bramble Edge | bramble (hoe) | thorn wolf (WOLF) |
| Pale Hollow | pale moss (hoe) | pale stalker (ZOMBIE) |
| Weeping Roots | root mass | root spider (SPIDER) |
| Witch's Glade | witchwood | coven witch (WITCH), hexed villager (ZOMBIE_VILLAGER) |
| The Briar Heart | none | The Creaking Mother (CREAKING, boss) *(check: creaking invulnerability must be off)* |

```
bramble,hoe,dark_oak_leaves:70|azalea_leaves:30              (azalea shared with Lush Grotto)
pale_moss,hoe,pale_moss_block:100
root_mass,axe,mangrove_roots:50|muddy_mangrove_roots:50              (both used: swap if repeats are off)
witchwood,axe,stripped_crimson_stem:70|crimson_hyphae:30     (crimson hyphae shared with the Nether)
```

The Elderwood line is short on unused woods; Thornwood is where the "can regions reuse materials" question matters most.

## New worlds

### Nether (aurora_nether, stages 8-9) — idea

A world of its own that the overworld, Gormak and Elderwood lines all lead into: three gates, one region with two halves. Off on Minefort until it is released (see memory: deploy only overworld and duneworld for now).

| Zone | Mine | Mob |
|---|---|---|
| Netherrack Wastes | netherrack, nether quartz | zombified piglin (ZOMBIFIED_PIGLIN) |
| Crimson Forest | crimson stem, nether wart block (hoe) | hoglin (HOGLIN) |
| Warped Forest | warped stem, warped wart block (hoe) | warped enderman (ENDERMAN) *(check: teleport)* |
| Soul Sand Valley | soul sand (shovel), bone block | soul skeleton (SKELETON) |
| Bastion | blackstone, gilded blackstone, nether gold | piglin (PIGLIN, crossbow), piglin brute (PIGLIN_BRUTE) |
| Ancient Forge | ancient debris | wither skeleton (WITHER_SKELETON) |
| Throne of Ash | none | The Piglin Warlord (PIGLIN_BRUTE, boss) |

```
netherrack,pick,netherrack:80|nether_quartz_ore:20
crimson_stem,axe,crimson_stem:70|crimson_hyphae:30
nether_wart,hoe,nether_wart_block:100
warped_stem,axe,warped_stem:70|warped_hyphae:30
warped_wart,hoe,warped_wart_block:100
soul_sand,shovel,soul_sand:100
bastion,pick,gilded_blackstone:20|nether_gold_ore:30|cracked_polished_blackstone_bricks:50
ancient_debris,pick,ancient_debris:100
```

Netherite is a look today, sold from the Quarry on; the Nether could be where netherite is actually earned (ancient debris). Bone block and soul soil are already used (Catacombs, Duat), so Soul Sand Valley may need its second block changed.

### The End (aurora_end, stage 10) — idea

The last world, after the Nether and the Storm Citadel: the sky and the underworld lines both reach it. No dragon (not handled); the boss is a caster.

| Zone | Mine | Mob |
|---|---|---|
| Outer Islands | end stone | endermite (ENDERMITE), void enderman (ENDERMAN) *(check)* |
| Chorus Groves | chorus (axe) | chorus witch (WITCH) |
| End City | purpur, end stone bricks | end sentinel (SKELETON with levitation arrows?) |
| Void Rift | void crystal | void knight (WITHER_SKELETON) |
| The Rim | none | The Void Herald (EVOKER, boss: fangs) |

```
end_stone,pick,end_stone:100
chorus,axe,chorus_wood:70|chorus_bud:30          (PURPLE_CONCRETE / PURPLE_GLAZED_TERRACOTTA)
purpur,pick,purpur_block:60|purpur_pillar:40
end_bricks,pick,end_stone_bricks:100
void_crystal,pick,void_ore:60|void_rich:40       (BLACK_STAINED_GLASS / BLACK_GLAZED_TERRACOTTA)
```

## Loot, keys and trophies

How each new region would hook into progression, in the shape the current ones use: a gate at the end of the region before it, mob drops that pay for the shop, a boss key dropped by the region's mobs (also sold in the boss zone), and a trophy that pays for the boss zone's last five upgrades and feeds the next core.

| Region | Gate (sold beside the portal) | Main mob drops | Boss key (from) | Trophy | Unlocks |
|---|---|---|---|---|---|
| Caverns | the Overworld Core's last step | miner's lamp, geode shard, deep bone, echo shard | Echo Key (sculk mobs) | Warden's Heart | the Caverns Core, the same idea as the Overworld Core, for the Fungal Deep |
| Fungal Deep | Caverns Core | spore sac, glowcap, rot fibre | Root Key (rot husk) | Mother Spore | — |
| Storm Citadel | Storm Sigil (Sky Kingdom's throne room) | storm plate, forge cog, bolt | Tyrant Key (storm knight) | Tyrant's Eye | Storm Mace upgrades |
| Aurora Spire | Aurora Pass (The Eye) | frost feather, prism dust, star fragment | Borealis Key (aurora sentinel) | Aurora Crown | an Aurora cosmetic set |
| Sunfall Savanna | Savanna Pass (The Crown, Elderwood Heart) | hyena fang, termite clay, rhino horn | Horn Key (savanna rhino) | Great Horn | — |
| Thornwood | Thorn Pass (Pride Rock) | bramble thorn, pale spore, hex charm | Briar Key (coven witch) | Briar Heart | — |
| The Glass Sea | Glass Pass (Throne of Night, the Duat) | glass shard, salt crystal, pearl | Abyss Key (pyramid guard) | Behemoth Scale | — |
| Obsidian Necropolis | Necropolis Seal (The Abyss) | linen wrap, royal ash, obsidian sigil | King's Seal (tomb myrmidon) | Undying Crown | — |
| Nether | three gates, one at each line's end | gold nugget, quartz, hoglin hide, wither bone, netherite scrap | Warlord Key (piglin brute) | Warlord's Axe | netherite earned, not only a look |
| The End | the Nether Core, with any one line's top trophy | ender pearl, chorus fruit, shulker shell (dropped, not fought), void essence | Herald Key (void knight) | Void Heart | end-game title, the last suffix |

Gear direction to decide: today materials stop at gold in shops and diamond/netherite come from the Quarry on. The Nether could be the first place netherite is earned by play rather than bought, and the End could add a look above netherite (an "End-forged" trim set) without new stats.

## Rare mobs and secrets

One rare mob a region, like the Mirage in Gormak: shows up anywhere in the region now and then, glows, and drops something only it has (a cosmetic piece or a big pile of the region's resource). Plus one secret a region, built in Axiom, no code.

| Region | Rare mob | Secret |
|---|---|---|
| Caverns | the Crystal Golem (IRON_GOLEM, amethyst look) | a hidden geode behind a fake tuff wall |
| Fungal Deep | the Wandering Shroom (MOOSHROOM, hostile look but passive) | a glowing mushroom ring that lights up in order (parkour) |
| Storm Citadel | the Ball Lightning (VEX) | a lightning rod room at the top of the keep |
| Aurora Spire | the Shooting Star (PHANTOM, glowing) | an observatory with the night sky in blocks |
| Sunfall Savanna | the White Lion (WOLF, white look) | a baobab with a room inside |
| Thornwood | the Lost Child (ZOMBIE_VILLAGER, baby) | a witch's recipe hidden in signs across the glade |
| The Glass Sea | the Glass Scorpion (SPIDER, glass look) | a buried ship visible through the glass flats |
| Obsidian Necropolis | the Grave Robber (PILLAGER), runs away when hit | a false tomb that opens into a treasure room |
| Nether | the Gold Hoarder (PIGLIN), drops gold piles | a lava fall with a room behind it |
| The End | the Lost Elytra Ghost (PHANTOM) | an end ship floating off the map, reached by grapple |

Rare mobs that run away (the Grave Robber) would need **[new code]**; the rest are spawners with long respawns, which exist.

## Mini worlds

Small worlds outside the stage ladder: 1-3 zones and often a boss, reached by a warp, a key or an event. They give something to do between regions, room for events, and a place for cosmetics that aren't bought.

**How they could fit what exists** (each is a question for you):
- **Strength.** The simplest form gives each mini world a fixed stage, like a region: Mushroom Isle is stage 2, the Volcano stage 6. A mini world that scales to the player's best region would need new code.
- **Currency.** Each has its own resource, mined or dropped there, which buys only from its own trader: cosmetics, a mount or pet look, a suffix, keys. Nothing that makes the player stronger in regions, so the balance sim is unaffected. The exception is event Fortune, which applies only in its own event world.
- **Twist.** Most have one thing that plays differently. Twists that need new code are marked **[new code]**. The rest work with the systems that exist today.

### Seasonal events

#### Frostbite Hollow (winter, December)

A snowed-in valley with a frozen lake and ice caves. It brings the event Fortune idea from DESIGN.md: **Winter Fortune**, which counts only here and adds to base Fortune up to the cap.

| Zone | Mine | Mob |
|---|---|---|
| Snowfield | ice, white wool snowdrifts | snow fox (FOX), frost stray (STRAY) |
| Frozen Lake | ice, packed ice | lake bear (POLAR_BEAR) |
| Yeti Caves | frost stone | yeti (POLAR_BEAR), frost spider (SPIDER) |
| Yeti King's Den | none | The Yeti King (POLAR_BEAR, boss, a giant one) |

```
snowdrift,shovel,white_wool:70|snow_block:30
ice,pick,ice:100
frost_stone,pick,frost_ore:60|frost_rich:40     (MAGENTA_CONCRETE / MAGENTA_GLAZED_TERRACOTTA)
```

- Currency: **Snowflakes**. Trader: winter suffix, a snow trail, a sled or reindeer mount look (a horse look), Winter Fortune upgrades.
- Twist: presents drop from the sky in the Snowfield every few minutes; first to break one gets a Snowflake pile. **[new code]**

#### Hollow's Eve (Halloween, October)

A haunted farm and graveyard under a permanent night. Brings **Spooky Fortune**.

| Zone | Mine | Mob |
|---|---|---|
| Pumpkin Patch | carved pumpkins | scarecrow (ZOMBIE with a pumpkin head) |
| Graveyard | grave dirt, tombstones | ghoul (ZOMBIE), bone archer (SKELETON) |
| Witch's Cottage | cauldron brick | coven witch (WITCH) |
| Haunted Manor | none | The Headless Horseman (SKELETON on a SKELETON_HORSE, boss) |

```
carved_pumpkin,axe,carved_pumpkin:100
grave,shovel,coarse_dirt:60|podzol:40          (coarse dirt is used twice already)
tombstone,pick,andesite:60|polished_andesite:40
cauldron_brick,pick,nether_bricks:70|red_nether_bricks:30
```

- Currency: **Candy Corn**. Trader: a pumpkin mask cosmetic, a bat trail, a spooky suffix, Spooky Fortune upgrades.
- Twist: trick-or-treat. Right-clicking a door NPC either pays Candy Corn or spawns a ghoul. **[new code]**

#### Blossom Festival (spring, April)

A lantern-lit cherry valley with a pond. Peaceful: a mining event, no hostile mobs.

| Zone | Mine | Mob |
|---|---|---|
| Petal Path | pink petals (hoe), blossom leaves | blossom rabbit (RABBIT, passive) |
| Koi Pond | pond stone | pond frog (FROG, passive) |
| Lantern Shrine | paper lantern | none |

```
blossom,hoe,cherry_leaves:100
pond_stone,pick,mossy_stone_bricks:60|cobblestone:40     (both used: swap if repeats are off)
lantern,pick,shroomlight:70|redstone_lamp:30           (shroomlight shared with Fungal Deep)
```

- Currency: **Petals**. Trader: a petal trail, a sakura suffix, a spring hat.
- This answers the sakura theme in DESIGN.md. Cherry logs stay the Gilded Gardens mine; this uses cherry leaves only.

#### Sunfest Beach (summer, July)

A tropical island with a beach, a reef and a shipwreck.

| Zone | Mine | Mob |
|---|---|---|
| Sandy Shore | beach sand | shore turtle (TURTLE, passive) |
| Coral Reef | coral blocks | reef drowned (DROWNED) *(check: tridents)* |
| Shipwreck | ship planks | pirate ghost (SKELETON), deckhand (ZOMBIE) |
| Kraken's Cove | none | Captain Bones (WITHER_SKELETON, boss) |

```
beach_sand,shovel,sand:70|sandstone:30                   (both used)
coral,pick,tube_coral_block:25|brain_coral_block:25|bubble_coral_block:25|fire_coral_block:25
ship_planks,axe,stripped_spruce_wood:60|spruce_planks:40
```

- Currency: **Seashells**. Trader: a surfboard (boat) look, a bubble trail, a summer suffix.
- Coral blocks die when they are dry, so the mine needs water touching it, or uses dead coral blocks (they look grey).

### Permanent side worlds

#### Mushroom Isle (stage 2)

A calm island of giant mushrooms for new players. The first mini world: a warp from Origin Isles, unlocked when it's maxed.

| Zone | Mine | Mob |
|---|---|---|
| Mycelium Shore | mycelium | mooshroom (MOOSHROOM, passive) |
| Giant Caps | mushroom caps | brown mooshroom (MOOSHROOM, passive) |

- Currency: **Spores**. Trader: a mooshroom mount look, a mushroom hat, a spore trail.
- No twist; the point is a breather after Origin Isles. It shares blocks with Fungal Deep, so one of them may need different ones.

#### Farmstead (stage 3)

A working farm. The DESIGN.md wheat idea from the legacy Skript fits here.

| Zone | Mine | Mob |
|---|---|---|
| Wheat Fields | wheat (hoe) | field rabbit (RABBIT) |
| Orchard | apple wood | pig (PIG), sheep (SHEEP) |
| Barn | hay | barn rat (SILVERFISH) |

```
wheat,hoe,wheat:100                    (a crop, not a full block: check the mine resets it)
apple_wood,axe,oak_log:50|oak_leaves:50   (oak shared with the Oak Grove)
hay,hoe,hay_block:100                   (shared with the Sunfall Savanna)
```

- Currency: **Harvest**. Trader: farmer suffix, a scarecrow cosmetic, a tractor (horse) look.
- Twist: crops regrow by stages instead of resetting all at once (wheat farming with regrowth, from `test.sk`). **[new code]**

#### Twilight Grove (stage 4)

A forest stuck at dusk, fireflies and fog.

| Zone | Mine | Mob |
|---|---|---|
| Dusk Woods | dusk wood | twilight wolf (WOLF) |
| Firefly Glade | glowshroom (hoe) | glade witch (WITCH) |
| Naga Courtyard | courtyard stone | naga guard (ZOMBIE) |
| Hedge Maze | none | The Hedge Knight (VINDICATOR, boss) |

```
dusk_wood,axe,dark_oak_wood:60|stripped_dark_oak_wood:40   (shared with the Dark Oak Thicket)
glowshroom,hoe,verdant_froglight:100                       (shared with Glowmire)
courtyard,pick,stone_bricks:60|chiseled_stone_bricks:40    (stone bricks used in the Abandoned City)
```

- Currency: **Dusk Motes**. Trader: firefly trail, a twilight suffix.
- Twist: a real hedge maze to reach the boss, built in Axiom, no code.

#### Volcano (stage 6)

A live volcano: lava rivers, obsidian, a caldera.

| Zone | Mine | Mob |
|---|---|---|
| Lava Rim | volcanic rock | magma husk (HUSK) |
| Obsidian Flow | obsidian | fire skeleton (SKELETON, fire arrows) |
| Caldera | molten core | ember brute (PIGLIN_BRUTE) |
| Heart of the Mountain | none | The Fire Titan (IRON_GOLEM, boss, fire look) |

```
volcanic_rock,pick,blackstone:60|basalt:40        (blackstone shared with the Storm Citadel)
obsidian,pick,obsidian:80|crying_obsidian:20       (shared with the Storm Citadel)
molten_core,pick,magma_block:100                   (used in the Solar Forge)
```

- Currency: **Embers**. Trader: fire trail, a flaming suffix, a magma cube pet look.
- Twist: every few minutes an eruption rains fire on one zone; players there take a little damage unless under cover. **[new code]**

#### Candy Kingdom (stage 5)

Bright and silly: candy cane forests, frosting hills, a gingerbread castle.

| Zone | Mine | Mob |
|---|---|---|
| Sugar Fields | sugar | gumdrop rabbit (RABBIT) |
| Candy Cane Forest | candy cane | gingerbread soldier (ZOMBIE) |
| Gummy Caves | gummy glass | jelly spider (SPIDER) |
| Gingerbread Castle | none | The Gingerbread King (ZOMBIE, boss, giant) |

```
sugar,shovel,pink_concrete_powder:70|magenta_concrete_powder:30
candy_cane,axe,red_wool:50|white_wool:50
gummy,pick,red_stained_glass:25|lime_stained_glass:25|orange_stained_glass:25|purple_stained_glass:25
```

- Currency: **Sweets**. Trader: candy trail, a lollipop sword look, a sweet suffix.
- Could double as a Christmas or Easter event world.

#### Moonbase (stage 8)

The space theme from DESIGN.md: a base on the moon, low gravity.

| Zone | Mine | Mob |
|---|---|---|
| Crater Field | moon rock | moon mite (ENDERMITE) |
| Lunar Base | base plating | rogue android (ZOMBIE in iron armour) |
| Moon Mine | lunar crystal | crystal spider (CAVE_SPIDER) |
| Mission Control | none | The Rogue AI (IRON_GOLEM, boss) |

```
moon_rock,pick,end_stone:60|light_gray_concrete_powder:40   (end stone shared with the End)
plating,pick,iron_block:50|polished_diorite:50               (iron block used twice)
lunar_crystal,pick,lunar_ore:60|lunar_rich:40   (GREEN_CONCRETE / GREEN_GLAZED_TERRACOTTA)
```

- Currency: **Moon Dust**. Trader: astronaut helmet, a rocket trail, a space suffix.
- Twist: low gravity everywhere (slow falling and jump boost as potion effects while in the world). Probably small code. **[new code]**

#### Wasteland (stage 7)

A ruined, rusted world after the fall of a kingdom, scrap everywhere.

| Zone | Mine | Mob |
|---|---|---|
| Scrapyard | scrap | scavenger (PILLAGER) |
| Rust Flats | rust | rust hound (WOLF) |
| Bunker | bunker concrete | raider (VINDICATOR) |
| Warlord's Fort | none | The Warlord (RAVAGER with a raider on its back, boss) |

```
scrap,pick,exposed_cut_copper:60|waxed_weathered_copper:40   (copper looks are close to the Sunken Ruins and Storm Front)
rust,shovel,brown_concrete_powder:70|orange_concrete_powder:30
bunker,pick,cyan_terracotta:60|light_gray_terracotta:40
```

- Currency: **Scrap**. Trader: goggles, a smoke trail, a wasteland suffix.

#### Angler's Lagoon (stage 3)

A fishing village on a lagoon, built around the fishing that already exists (`scripts/data/fishing/`). The one mini world where fishing is the main loop.

| Zone | Mine | Mob |
|---|---|---|
| Fishing Village | driftwood | harbour cat (CAT, passive) |
| Lagoon | none: fishing spots | none |
| Sea Caves | sea stone | cave drowned (DROWNED) *(check: tridents)* |

```
driftwood,axe,stripped_birch_wood:60|birch_planks:40
sea_stone,pick,prismarine_bricks:60|dark_prismarine:40     (shared with the Glass Sea)
```

- Currency: the fish themselves. Trader: rod looks, a fisherman suffix, a boat look.
- Uses only the fishing spots and rods that exist; new fish for the lagoon go in `fish.csv`.

#### Giant's Garden (stage 4)

A garden where the player is tiny: grass taller than trees, giant flowers, a watering can as a building. Pure Axiom build, with the scale as the hook.

| Zone | Mine | Mob |
|---|---|---|
| Grass Jungle | grass stalk (hoe) | garden beetle (SILVERFISH) |
| Flowerbed | petal (hoe), pollen (shovel) | giant bee (BEE) |
| Garden Shed | flowerpot clay | shed rat (SILVERFISH), garden spider (SPIDER) |
| The Gardener's Boot | none | The Giant Spider (SPIDER, boss, scaled up) |

```
grass_stalk,hoe,green_wool:70|lime_wool:30
petal,hoe,red_wool:40|yellow_wool:30|pink_wool:30          (red wool shared with Candy Kingdom)
pollen,shovel,yellow_concrete_powder:100
flowerpot,pick,terracotta:60|orange_terracotta:40          (shared with the Savanna and Duat)
```

- Currency: **Pollen**. Trader: a ladybug pet look, a garden suffix.
- Mob scale (the giant spider, tiny bees) uses the `scale` attribute: check it works through `attributes` in the mob CSV.

#### Ancient Library (stage 5)

A huge library of floating shelves and reading rooms, tied to enchants.

| Zone | Mine | Mob |
|---|---|---|
| Stacks | bookshelves (axe) | animated armour (ZOMBIE in armour) |
| Reading Room | lectern wood | librarian ghost (ZOMBIE_VILLAGER) |
| Forbidden Section | chiseled shelves | warlock (EVOKER, fangs) |
| The Archive | none | The Archivist (EVOKER, boss) |

```
bookshelf,axe,bookshelf:100
lectern,axe,oak_planks:60|stripped_oak_wood:40
chiseled_shelf,axe,chiseled_bookshelf:100
```

- Currency: **Pages**. Trader: a scholar suffix, a book-on-back cosmetic, a page trail.
- Could be where enchant ideas from DESIGN.md (feather, blind, sunder) are found, but that changes play: ask first.

#### Clockwork Foundry (stage 6)

A steam and gear factory with conveyors and pistons.

| Zone | Mine | Mob |
|---|---|---|
| Boiler Room | coal stack | stoker (HUSK) |
| Gearworks | brass gears | clockwork soldier (VINDICATOR) |
| Assembly Line | copper pipe | assembly golem (IRON_GOLEM) |
| The Great Engine | none | The Engineer (PILLAGER, boss, with golems) |

```
coal_stack,pick,coal_block:70|blackstone:30          (both used: coal in Origin, blackstone in plans)
brass,pick,waxed_cut_copper:60|cut_copper:40
copper_pipe,pick,waxed_copper_grate:60|copper_grate:40
```

- Currency: **Cogs**. Trader: goggles, a steam trail, a mechanic suffix.
- Twist: moving conveyor floors that carry players. **[new code]** (or plain soul-sand/ice paths built in Axiom, no code).

#### Sunken Atlantis (stage 7)

A drowned city under a glass dome on the sea floor.

| Zone | Mine | Mob |
|---|---|---|
| Kelp Forest | dried kelp (hoe) | kelp drowned (DROWNED) *(check)* |
| Coral Streets | dead coral | atlantean guard (WITHER_SKELETON) |
| Palace of Tides | prismarine marble | tide witch (WITCH) |
| The Abyss Gate | none | The Tide King (DROWNED, boss) *(check)* |

```
kelp,hoe,dried_kelp_block:100
dead_coral,pick,dead_tube_coral_block:50|dead_brain_coral_block:50
marble,pick,quartz_bricks:60|prismarine:40          (both shared)
```

- Currency: **Pearls**. Trader: trident look, a bubble trail, an Atlantean suffix.
- Inside the dome the air is normal, so no water-breathing code is needed.

#### Dragon's Roost (stage 9)

A mountain eyrie of dragon eggs and hoards. No real dragon (not handled); the dragons are flavour in the build.

| Zone | Mine | Mob |
|---|---|---|
| Ashen Slopes | scorched stone | kobold (ZOMBIE, baby) |
| Hoard Halls | gold hoard | hoard guard (PIGLIN_BRUTE) |
| Egg Chamber | dragon scale | dragon cultist (EVOKER) |
| The Roost | none | The Dragon Priest (WITHER_SKELETON, boss) |

```
scorched,pick,smooth_basalt:60|deepslate:40
hoard,pick,gold_block:50|raw_gold_block:50          (both used in Origin and Gormak)
dragon_scale,pick,dragon_ore:60|dragon_rich:40       (PINK_CONCRETE / PINK_GLAZED_TERRACOTTA)
```

- Currency: **Scales**. Trader: dragon wings (elytra look), a dragon suffix.

### Key worlds

Reached only with a key, one run at a time, like the Gauntlet's pasted arenas. Each needs **[new code]** for timing and the copy.

| Key world | Key from | What happens | Pays |
|---|---|---|---|
| Treasure Vault | rare drop from any mob | one room, 60 seconds: mine as much as you can | the current region's resources |
| Goblin Hoard | a goblin that sometimes appears in any mine | chase the goblin through tunnels before it escapes | Essence |
| Boss Rush | a crafted key from every trophy in a line | the line's bosses one after another, no healing between | a line title and cosmetics |
| Void Trial | a rare End drop | parkour over the void with mobs on the platforms | an End cosmetic |
| Mirage Oasis | killing the Mirage in Gormak | a bonus oasis zone with a rich mine for 3 minutes | Gormak resources |
| Smuggler's Den | a map from the Grave Robber | find the stash in a small maze before guards wake | a rare cosmetic |
| Arena of Echoes | an echo shard (Caverns) | waves of every region's mobs, scaling until you die | a leaderboard place |

## Proposed stage ladder

Where everything above would land, if it went in as written:

| Stage | Overworld | Gormak line | Elderwood line | Sky line | Mini worlds |
|---|---|---|---|---|---|
| 2 | | | | | Mushroom Isle |
| 3 | | | | | Farmstead, Angler's Lagoon |
| 4 | | | | | Twilight Grove, Giant's Garden |
| 5 | | | | | Candy Kingdom, Ancient Library |
| 6 | | The Glass Sea | Sunfall Savanna | | Volcano, Clockwork Foundry |
| 7 | | Obsidian Necropolis | Thornwood | Storm Citadel | Wasteland, Sunken Atlantis |
| 8 | Caverns | Nether | Nether | Aurora Spire | Moonbase |
| 9 | Fungal Deep | Nether | Nether | | Dragon's Roost |
| 10 | The End | The End | The End | The End | |

Seasonal events and key worlds have no stage: events would match a mid-game player or be a fixed stage each year, and key worlds pay in whatever the key's region uses.

## Open questions

- Order: which line gets the next release after the demo (Elderwood Roots and Khorvath are stage 3 and already have data)?
- Should new regions reuse existing materials (mud, clay, cherry) or always use unused ones?
- Nether and End as late worlds that join the lines, or as their own separate lines from stage 2?
- Mini worlds: their own currency and shop, or paying the player's current region resources?
- Event worlds: kept between years (same world each December) or rebuilt?
- Mini worlds: a fixed stage each (works today) or scaled to the player (new code)?
- Which twists marked [new code] are worth building, and when (after the demo)?
- Bosses on entities with vanilla extras (warden, creaking, drowned, enderman): test them, or keep to the safe list?
- Should the Nether and End replace some line finales (Thornwood, Obsidian Necropolis) to keep the content smaller?
- Loot table: are the gate items, keys and trophies above the right shape, or should later regions drop the "core" idea?
- Netherite earned in the Nether, and an End look above it: yes, or keep materials as they are?
- Rare mobs that run away, and the Arena of Echoes leaderboard: worth the [new code]?
