#!/usr/bin/env python3
"""
The numbers the price formula stands on: how fast a player mines and fights.

Nothing here prices anything, price_zones.py does that. This holds the plugin's own
formulas and the vanilla tables they need, read against the CSVs in scripts/data/:

  player hit    = 10 + sharpnessBonus(S) - mob defence                     CombatListener
  damage taken  = hearts(gap), gap = (attack level - average protection) / LEVELS_PER_TIER
                  gap >= 0: 1 + LOG_A * ln(1 + LOG_B * gap)   (1, 2, 2.9, 3.7, 4.4, 5 ...)
                  gap <  0: 0.25 + 0.75 * e^(2 * gap)         (never below a quarter of a heart)
  block drops   = roll * (1 + 0.75 L + 0.25 L^2), L = fortune - density, nothing if L < 0   DataRegistry
  dig time      = vanilla: tool speed + eff^2 + 1 vs hardness, with the right tool
  Not modelled: Sustenance, Bloodlust, Thorns, shield Defence and Deflection.

Every tuning assumption is a constant in the ASSUMPTIONS block below.
"""
import csv
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "scripts" / "data"

# ------------------------------------------------------------------ ASSUMPTIONS
LEVELS_PER_TIER = 4.0          # protection levels per zone tier (CombatListener)
LOG_A, LOG_B = 1 / math.log(1.15), 0.15   # shape of the hearts curve above your tier
ARMOUR_FLOOR = 0.25            # the least a hit does to armour far ahead of the mob, in hearts
MOB_DAMAGE_SCALE, RANGED_SCALE = 0.6, 0.5  # CombatFormulas, every mob hit, and arrows on top
HUMAN_BLOCKS_PER_SEC = 5.0     # cap when blocks insta-break
MINING_EFFICIENCY = 0.8        # aiming and walking inside the mine
BLAST_FILL = 0.5               # share of blast neighbours still solid when you swing
HITS_PER_SEC = 1.6             # fully charged swings (625 ms)
KILL_OVERHEAD_S = 3.0          # finding and approaching the next mob
MELEE_HITS_PER_SEC = 1.0       # vanilla melee mobs swing about once a second
RANGED_HITS_PER_SEC = 0.5
REGEN_HEARTS_PER_SEC = 0.125   # vanilla: 1 health point (half a heart) per 4 s at food >= 18
ATTACK_COOLDOWN = {}           # mob id -> seconds between swings, filled from attack_cooldown

# Blast level -> (width, height, depth), kept in step with BlastShape.SHAPES
BLAST_SHAPES = {
    1: (2, 1, 1),
    2: (2, 2, 1),
    3: (3, 3, 1),
    4: (3, 3, 2),
    5: (3, 3, 3),
}

# ------------------------------------------------------------- vanilla tables
# material: (harvest tier, speed)
TOOL = {"WOODEN": (0, 2), "STONE": (1, 4), "COPPER": (1, 5), "IRON": (2, 6), "GOLDEN": (0, 12),
        "DIAMOND": (3, 14), "NETHERITE": (4, 16)}
# material: (hardness, harvest tier needed: 0 wood, 1 stone, 2 iron)
BLOCK = {
    "COBBLESTONE": (2.0, 0), "STONE": (1.5, 0), "COAL_ORE": (3.0, 0), "COAL_BLOCK": (5.0, 0),
    "COPPER_ORE": (3.0, 1), "COPPER_BLOCK": (3.0, 1), "IRON_ORE": (3.0, 1), "IRON_BLOCK": (5.0, 1),
    "GOLD_ORE": (3.0, 2), "GOLD_BLOCK": (3.0, 2), "STONE_BRICKS": (1.5, 0),
    "CRACKED_STONE_BRICKS": (1.5, 0), "SANDSTONE": (0.8, 0), "CUT_SANDSTONE": (0.8, 0),
    "RED_SANDSTONE": (0.8, 0), "CUT_RED_SANDSTONE": (0.8, 0), "RAW_GOLD_BLOCK": (5.0, 1),
    "GILDED_BLACKSTONE": (1.5, 0),
    # Verdant Groves, logs need an axe for Fortune (MineListener.rightTool), metals a pick
    "OAK_LOG": (2.0, 0), "OAK_WOOD": (2.0, 0), "BIRCH_LOG": (2.0, 0), "BIRCH_WOOD": (2.0, 0),
    "SPRUCE_LOG": (2.0, 0), "SPRUCE_WOOD": (2.0, 0), "DARK_OAK_LOG": (2.0, 0), "DARK_OAK_WOOD": (2.0, 0),
    "LIGHT_GRAY_CONCRETE": (1.8, 0), "BROWN_CONCRETE": (1.8, 0), "GRAY_CONCRETE": (1.8, 0),
    "STRIPPED_OAK_LOG": (2.0, 0), "STRIPPED_BIRCH_LOG": (2.0, 0), "STRIPPED_SPRUCE_LOG": (2.0, 0),
    "STRIPPED_DARK_OAK_LOG": (2.0, 0), "LIGHT_GRAY_GLAZED_TERRACOTTA": (1.4, 0), "BROWN_GLAZED_TERRACOTTA": (1.4, 0),
    "GRAY_GLAZED_TERRACOTTA": (1.4, 0), "WHITE_GLAZED_TERRACOTTA": (1.4, 0),
    # Regions after the first three, vanilla hardness (tiers are not checked, every Aurora
    # tool is the right tool for its blocks at its own speed)
    "CHISELED_RED_SANDSTONE": (0.8, 0), "ORANGE_TERRACOTTA": (1.25, 0), "BONE_BLOCK": (2.0, 0),
    "SMOOTH_SANDSTONE": (2.0, 0), "JUNGLE_LOG": (2.0, 0), "STRIPPED_JUNGLE_LOG": (2.0, 0),
    "BAMBOO_BLOCK": (2.0, 0), "BAMBOO_MOSAIC": (2.0, 0), "JUNGLE_WOOD": (2.0, 0), "BEE_NEST": (0.3, 0),
    "PACKED_MUD": (1.0, 0), "MUD_BRICKS": (1.5, 0), "BROWN_TERRACOTTA": (1.25, 0), "MOSSY_COBBLESTONE": (2.0, 0),
    "MOSSY_STONE_BRICKS": (1.5, 0), "OXIDIZED_COPPER": (3.0, 1), "DIRT": (0.5, 0), "COARSE_DIRT": (0.5, 0),
    "REDSTONE_ORE": (3.0, 2), "DEEPSLATE_REDSTONE_ORE": (4.5, 2), "PUMPKIN": (1.0, 0), "SNOW_BLOCK": (0.2, 0), "JACK_O_LANTERN": (1.0, 0), "CALCITE": (0.75, 0), "DRIPSTONE_BLOCK": (1.5, 0),
    "PACKED_ICE": (0.5, 0), "BLUE_ICE": (2.8, 0), "REDSTONE_BLOCK": (5.0, 0), "LAPIS_BLOCK": (3.0, 1),
    "LAPIS_ORE": (3.0, 1), "DEEPSLATE_LAPIS_ORE": (4.5, 1), "MELON": (1.0, 0), "EMERALD_ORE": (3.0, 2),
    "DEEPSLATE_EMERALD_ORE": (4.5, 2), "BASALT": (1.25, 0), "POLISHED_BASALT": (1.25, 0), "SOUL_SOIL": (0.5, 0),
    "POLISHED_BLACKSTONE_BRICKS": (1.5, 0), "WAXED_EXPOSED_COPPER": (3.0, 1), "MAGMA_BLOCK": (0.5, 0),
    "CRYING_OBSIDIAN": (50.0, 3), "BLACK_CONCRETE": (1.8, 0), "STRIPPED_JUNGLE_WOOD": (2.0, 0),
    "MANGROVE_ROOTS": (0.7, 0), "MUSHROOM_STEM": (0.2, 0), "RED_MUSHROOM_BLOCK": (0.2, 0), "BROWN_MUSHROOM_BLOCK": (0.2, 0), "BEEHIVE": (0.6, 0),
    "RESIN_BRICKS": (1.5, 0), "WHITE_CONCRETE": (1.8, 0), "WHITE_CONCRETE_POWDER": (0.5, 0),
    "LIGHT_BLUE_GLAZED_TERRACOTTA": (1.4, 0), "QUARTZ_BLOCK": (0.8, 0), "WEATHERED_CUT_COPPER": (3.0, 1),
    "WEATHERED_COPPER_GRATE": (3.0, 1), "QUARTZ_BRICKS": (0.8, 0), "QUARTZ_PILLAR": (0.8, 0), "CHERRY_LOG": (2.0, 0),
    "YELLOW_CONCRETE": (1.8, 0), "LIGHT_BLUE_CONCRETE": (1.8, 0), "YELLOW_GLAZED_TERRACOTTA": (1.4, 0),
    "GRAVEL": (0.6, 0), "DEEPSLATE_IRON_ORE": (4.5, 1), "RAW_IRON_BLOCK": (5.0, 1), "PALE_OAK_LOG": (2.0, 0),
    "STRIPPED_PALE_OAK_LOG": (2.0, 0), "SMOOTH_QUARTZ": (2.0, 0), "MANGROVE_LOG": (2.0, 0),
    "STRIPPED_MANGROVE_LOG": (2.0, 0), "CLAY": (0.6, 0), "LIGHT_BLUE_TERRACOTTA": (1.25, 0),
    "DIAMOND_ORE": (3.0, 2), "DEEPSLATE_DIAMOND_ORE": (4.5, 2),
    # Sunken Marsh. Slime breaks at once in vanilla (hardness 0), a tiny number keeps
    # dig_seconds from dividing by zero and still reads as instant
    "MUD": (0.5, 0), "MUDDY_MANGROVE_ROOTS": (0.7, 0), "SLIME_BLOCK": (0.001, 0), "MANGROVE_WOOD": (2.0, 0),
    "MOSS_BLOCK": (0.1, 0), "OXIDIZED_CUT_COPPER": (3.0, 1), "MANGROVE_LEAVES": (0.2, 0),
    "OCHRE_FROGLIGHT": (0.3, 0), "VERDANT_FROGLIGHT": (0.3, 0), "PEARLESCENT_FROGLIGHT": (0.3, 0),
}
# Mobs that shoot arrows instead of hitting, they swing half as often and hit half as hard
RANGED = {"SKELETON", "STRAY", "BOGGED", "PILLAGER"}

PLACED = {"gilded_stone": "GILDED_BLACKSTONE"}  # material actually placed by the raw_gold mine

SLOTS = ("helmet", "chestplate", "leggings", "boots")
# One file per region, the same list as MOB_REGIONS in generate.py
MOB_FILES = tuple(f"mobs_{region}" for region in (
    "origin", "gormak", "verdant", "khorvath", "elderwood", "marsh", "peaks", "duat", "sky", "quarry"))

# --------------------------------------------------------------------- data


def data_file(name):
    """scripts/data/<some folder>/<name>.csv, the CSVs are sorted into folders."""
    found = sorted(DATA.rglob(f"{name}.csv"))
    if len(found) != 1:
        raise FileNotFoundError(f"{len(found)} files called {name}.csv under {DATA}")
    return found[0]


def rows(name):
    with open(data_file(name), newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def parse_drops(text):
    """'feather:100:1-3|x:25:1-2|y:2' -> [(id, chance 0-1, average amount)]."""
    out = []
    for part in filter(None, (text or "").split("|")):
        bits = part.split(":")
        chance = float(bits[1]) if len(bits) > 1 else 100.0
        lo, _, hi = bits[2].partition("-") if len(bits) > 2 else ("1", "", "1")
        hi = hi or lo
        out.append((bits[0], chance / 100, (int(lo) + int(hi)) / 2))
    return out


BLOCKS = {r["id"]: {"material": r["material"], "density": int(r["density"]),
                    "drops": parse_drops(r["independent"])} for r in rows("blocks")}
MOBS = {r["id"]: {"entity": r["entity"], "hp": float(r["health"] or 20), "def": float(r["defence"] or 0),
                  "atk": float(r["attack"] or 0), "density": int(r.get("density") or 0),
                  "respawn": float(r.get("respawn") or 0),
                  "drops": parse_drops(r["independent"])}
        for f in MOB_FILES for r in rows(f)}
# Mine layouts, the mix scripts/build_test_world.py builds: mine id -> {block id: share},
# and the tool each one is mined with (pick, axe or shovel)
MINES = {r["id"]: {b.split(":")[0]: int(b.split(":")[1]) / 100 for b in r["blocks"].split("|")}
         for r in rows("mines")}
MINE_TOOL = {r["id"]: r["tool"] for r in rows("mines")}
for f in MOB_FILES:
    for r in rows(f):
        if float(r.get("attack_cooldown") or 0) > 0:
            ATTACK_COOLDOWN[r["id"]] = float(r["attack_cooldown"])
RESOURCES = {r["id"] for r in rows("resources")}

# ----------------------------------------------------------------- formulas


def sharpness_bonus(s):
    table = [1, 3, 5, 10, 15, 20, 25, 30, 35, 40]
    total, bracket = 0, 0
    while s > 0:
        n = min(s, 5)
        inc = table[bracket] if bracket < len(table) else table[-1] + (bracket - len(table) + 1) * 5
        total, s, bracket = total + n * inc, s - n, bracket + 1
    return total


def hearts(gap):
    if gap >= 0:
        return 1 + LOG_A * math.log(1 + LOG_B * gap)
    return ARMOUR_FLOOR + (1 - ARMOUR_FLOOR) * math.exp(2 * gap)


def mob_rate(mob):
    if mob in ATTACK_COOLDOWN:
        return 1 / ATTACK_COOLDOWN[mob]
    return RANGED_HITS_PER_SEC if MOBS[mob]["entity"] in RANGED else MELEE_HITS_PER_SEC


def tool_kind(mine):
    """The tool that mines this, from mines.csv: pick, axe or shovel (MineListener.rightTool)."""
    return MINE_TOOL[mine]