#!/usr/bin/env python3
"""
Aurora balance simulator.

Replays the plugin's formulas against the CSVs in scripts/data/ and plays a greedy
virtual player through every zone, using expected values (no randomness).

  player hit    = (10 + sharpnessBonus(S)) * charge - mob defence        CombatListener
  damage taken  = hearts(gap), gap = (attack level - average protection) / LEVELS_PER_TIER
                  gap >= 0: 1 + LOG_A * ln(1 + LOG_B * gap)   (1, 2, 2.9, 3.7, 4.4, 5 ...)
                  gap <  0: 0.5 + 0.5 * e^(2 * gap)           (never below half a heart)
  block drops   = roll * (fortune + 1 - density), nothing if <= 0         DataRegistry
  mob drops     = resources get their min-max range shifted up by Looting  Looting
  dig time      = vanilla: tool speed + eff^2 + 1 vs hardness, our picks always the right tool
  Not modelled: Sustenance, Bloodlust, Thorns, shield Defence and Deflection.

Usage (from the repo root):
  python scripts/balance/balance_sim.py                     current data and code
  python scripts/balance/balance_sim.py --boss-cooldown 2   try a different boss swing cooldown

Every tuning assumption is a constant in the ASSUMPTIONS block below.
"""
import argparse
import csv
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "scripts" / "data"

# ------------------------------------------------------------------ ASSUMPTIONS
PLAYER_HEARTS = 10.0           # damage is dealt in real hearts
LEVELS_PER_TIER = 4.0          # protection levels per zone tier (CombatListener)
LOG_A, LOG_B = 1 / math.log(1.15), 0.15   # shape of the hearts curve above your tier
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

# Blast level -> (width, height, depth), kept in step with MineListener.BLAST_SHAPES
BLAST_SHAPES = {
    1: (2, 1, 1),
    2: (2, 2, 1),
    3: (3, 3, 1),
}

# ------------------------------------------------------------- vanilla tables
TOOL = {"WOODEN": (0, 2), "STONE": (1, 4), "COPPER": (1, 5), "IRON": (2, 6), "GOLDEN": (0, 12),
        "DIAMOND": (3, 8), "NETHERITE": (4, 9)}
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
}
RANGED = {"SKELETON"}

# Mine layouts, copied from LocalServer mines.db (aurora_mines world): block id -> share
MINES = {
    "cobblestone": {"cobblestone": .6, "stone": .4},
    "coal": {"coal_ore": .6, "coal_block": .4},
    "copper": {"copper_ore": .6, "copper_block": .4},
    "iron": {"iron_ore": .6, "iron_block": .4},
    "gold": {"gold_ore": .6, "gold_block": .4},
    "carved_brick": {"carved_brick": .9, "cracked_brick": .1},
    "sunbaked_sand": {"sunbaked_sand": .8, "cut_sunbaked_sand": .2},
    "crimson_sand": {"crimson_sand": .8, "cut_crimson_sand": .2},
    "raw_gold": {"raw_gold_block": .2, "gilded_stone": .8},
    # Verdant Groves, assumed until the mines are built: logs 60/25/15 log/wood/stripped,
    # metals 60/40 ore/glazed like the Origin mines
    "oak_log": {"oak_log": .6, "oak_wood": .25, "stripped_oak_log": .15}, "tin": {"tin_ore": .6, "tin_rich": .4},
    "birch_log": {"birch_log": .6, "birch_wood": .25, "stripped_birch_log": .15}, "bronze": {"bronze_ore": .6, "bronze_rich": .4},
    "spruce_log": {"spruce_log": .6, "spruce_wood": .25, "stripped_spruce_log": .15}, "lead": {"lead_ore": .6, "lead_rich": .4},
    "dark_oak_log": {"dark_oak_log": .6, "darkoak_wood": .25, "stripped_dark_oak_log": .15},
    "silver": {"silver_ore": .6, "silver_rich": .4},
}
PLACED = {"gilded_stone": "GILDED_BLACKSTONE"}  # material actually placed by the raw_gold mine

ORIGIN = ["cobble", "coal", "copper", "iron", "gold"]
ORIGIN_MINE = dict(zip(ORIGIN, ["cobblestone", "coal", "copper", "iron", "gold"]))
GORMAK = ["city", "dunes", "canyons", "temple", "tomb"]
VERDANT = ["oak", "birch", "spruce", "darkoak", "treehouse"]
ZONE_MOBS = {
    "city": ["hollowed_serf", "scavenger_husk"], "dunes": ["dune_scout", "sand_stalker"],
    "canyons": ["scarab_zealot", "hollowed_legionnaire"], "temple": ["tomb_guard", "vizier_mage"],
    "tomb": ["royal_myrmidon", "pharaoh", "gormak_eternal"],
    "oak": ["grove_horse"], "birch": ["hollow_fox"], "spruce": ["ridge_wolf"], "darkoak": ["thicket_sniffer"],
    "treehouse": ["tree_zombee", "treant"],
}
BOSS = "gormak_eternal"
SLOTS = ("helmet", "chestplate", "leggings", "boots")
# Gear a player holds on arriving at, halfway through, and leaving each Gormak zone
ZONE_GEAR = {
    "city": [("gold_sword_5", "gold_{}_3"), ("city_sword_3", "city_{}_3"), ("city_sword_5", "city_{}_5")],
    "dunes": [("city_sword_5", "city_{}_5"), ("dunes_sword_3", "dunes_{}_3"), ("dunes_sword_5", "dunes_{}_5")],
    "canyons": [("dunes_sword_5", "dunes_{}_5"), ("canyons_sword_3", "canyons_{}_3"),
                ("canyons_sword_5", "canyons_{}_5")],
    "temple": [("canyons_sword_5", "canyons_{}_5"), ("temple_sword_3", "temple_{}_3"),
               ("temple_sword_5", "temple_{}_5")],
    "tomb": [("temple_sword_5", "temple_{}_5"), ("tomb_sword_3", "tomb_{}_2"), ("tomb_sword_5", "tomb_{}_4")],
    "treehouse": [("darkoak_sword_2", "darkoak_{}_2"), ("treehouse_sword_2", "treehouse_{}_2"),
                  ("treehouse_sword_3", "treehouse_{}_3")],
}

# --------------------------------------------------------------------- data


def rows(name):
    with open(DATA / f"{name}.csv", newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def parse_enchants(text):
    return {k: int(v) for k, v in (p.split(":") for p in filter(None, (text or "").split("|")))}


def parse_drops(text):
    """'feather:100:1-3|x:25:1-2|y:2' -> [(id, chance 0-1, average amount)]."""
    out = []
    for part in filter(None, (text or "").split("|")):
        bits = part.split(":")
        chance = float(bits[1]) if len(bits) > 1 else 100.0
        lo, hi = bits[2].split("-") if len(bits) > 2 else ("1", "1")
        out.append((bits[0], chance / 100, (int(lo) + int(hi)) / 2))
    return out


def parse_cost(text):
    out = {}
    for part in filter(None, (text or "").split("+")):
        k, v = part.split("*")
        out[k] = out.get(k, 0) + int(v)
    return out


GEAR = {r["id"]: {"material": r["material"], "enchants": parse_enchants(r["enchants"])}
        for f in ("gear_weapons", "gear_picks", "gear_armor", "gear_other") for r in rows(f)}
BLOCKS = {r["id"]: {"material": r["material"], "density": int(r["density"]),
                    "drops": parse_drops(r["independent"])} for r in rows("blocks")}
MOBS = {r["id"]: {"entity": r["entity"], "hp": float(r["health"]), "def": float(r["defence"]),
                  "atk": float(r["attack"]), "drops": parse_drops(r["independent"])}
        for f in ("mobs_origin", "mobs_gormak", "mobs_verdant") for r in rows(f)}
for f in ("mobs_origin", "mobs_gormak", "mobs_verdant"):
    for r in rows(f):
        if float(r.get("attack_cooldown") or 0) > 0:
            ATTACK_COOLDOWN[r["id"]] = float(r["attack_cooldown"])
RESOURCES = {r["id"] for r in rows("resources")}
SHOPS = {}
for r in rows("guis_shops_components"):
    if r["type"] == "TRADE":
        SHOPS.setdefault(r["gui"], []).append((r["id"], parse_cost(r["cost"])))

# ----------------------------------------------------------------- formulas


def ench(item, name):
    return GEAR[item]["enchants"].get(name, 0)


def sharpness_bonus(s):
    table = [1, 3, 5, 10, 15, 20, 25, 30, 35, 40]
    total, bracket = 0, 0
    while s > 0:
        n = min(s, 5)
        inc = table[bracket] if bracket < len(table) else table[-1] + (bracket - len(table) + 1) * 5
        total, s, bracket = total + n * inc, s - n, bracket + 1
    return total


def player_hit(sword, mob):
    return max(0, round(10 + sharpness_bonus(ench(sword, "sharpness")) - MOBS[mob]["def"]))


def defence(armour):
    """Average protection level over the four slots."""
    return sum(ench(a, "protection") for a in armour) / len(SLOTS)


def hearts(gap):
    if gap >= 0:
        return 1 + LOG_A * math.log(1 + LOG_B * gap)
    return 0.5 + 0.5 * math.exp(2 * gap)


def damage_taken(mob, armour):
    """Hearts lost per mob hit (CombatListener.onMobAttack)."""
    atk = MOBS[mob]["atk"]
    if atk <= 0:
        return 0.0
    scale = MOB_DAMAGE_SCALE * (RANGED_SCALE if MOBS[mob]["entity"] in RANGED else 1.0)
    return hearts((atk - defence(armour)) / LEVELS_PER_TIER) * scale


def mob_rate(mob):
    if mob in ATTACK_COOLDOWN:
        return 1 / ATTACK_COOLDOWN[mob]
    return RANGED_HITS_PER_SEC if MOBS[mob]["entity"] in RANGED else MELEE_HITS_PER_SEC


def fight(sword, armour, mob):
    hit = player_hit(sword, mob)
    ttk = math.inf if hit == 0 else math.ceil(MOBS[mob]["hp"] / hit) / HITS_PER_SEC
    taken = damage_taken(mob, armour)
    return {"hit": hit, "ttk": ttk, "taken": taken,
            "hits_to_die": math.inf if taken == 0 else math.ceil(PLAYER_HEARTS / taken),
            "left": PLAYER_HEARTS + (REGEN_HEARTS_PER_SEC - taken * mob_rate(mob)) * ttk}


def kill_cycle_time(sword, armour, mob):
    """Seconds per kill, including standing still to regenerate what the fight cost."""
    f = fight(sword, armour, mob)
    if f["ttk"] == math.inf:
        return math.inf
    t = f["ttk"] + KILL_OVERHEAD_S
    lost = f["taken"] * mob_rate(mob) * f["ttk"]
    return t + max(0.0, lost - REGEN_HEARTS_PER_SEC * t) / REGEN_HEARTS_PER_SEC


def dig_seconds(pick, block_id):
    _, speed = TOOL[GEAR[pick]["material"].split("_")[0]]
    eff = ench(pick, "efficiency")
    speed += eff * eff + 1 if eff else 0
    hardness, _ = BLOCK[PLACED.get(block_id, BLOCKS[block_id]["material"])]
    per_tick = speed / hardness / 30  # our picks are always the correct tool (DataRegistry.loadGear)
    if per_tick >= 1:
        return 1 / HUMAN_BLOCKS_PER_SEC
    return (math.ceil(1 / per_tick) + 5) / 20  # +5 tick vanilla re-dig delay


def blocks_per_swing(pick):
    level = ench(pick, "blast")
    if not level:
        return 1.0
    x, y, z = BLAST_SHAPES[min(level, max(BLAST_SHAPES))]
    return 1 + (x * y * z - 1) * BLAST_FILL


def tool_kind(mine):
    """Which tool line mines this, axes for wood (Tag.MINEABLE_AXE), picks for the rest."""
    wood = any(w in PLACED.get(b, BLOCKS[b]["material"]) for b in MINES[mine] for w in ("LOG", "WOOD"))
    return "_axe_" if wood else "_pick_"


def mine_rate(pick, mine):
    """(resource per second, density that blocks this pick or None)."""
    fortune = ench(pick, "fortune")
    per_block = secs = 0.0
    for bid, share in MINES[mine].items():
        b = BLOCKS[bid]
        mult = fortune + 1 - b["density"]
        if mult <= 0:
            return 0.0, b["density"]
        per_block += share * sum(c * avg for _, c, avg in b["drops"]) * mult
        secs += share * dig_seconds(pick, bid)
    return per_block * blocks_per_swing(pick) / (secs / MINING_EFFICIENCY), None

# --------------------------------------------------------------- playthrough


def zone_items(zone):
    shop = SHOPS.get(f"{zone}_shop", [])
    # Picks and axes by tier, each axe right after its pick: the first axe costs the
    # metal the pick mines, and later picks cost logs only an axe can get
    tools = sorted((x for x in shop if "_pick_" in x[0] or "_axe_" in x[0]),
                   key=lambda x: (int(x[0].rsplit("_", 1)[1]), "_axe_" in x[0]))
    swords = [x for x in shop if "_sword_" in x[0]]
    return tools + swords + SHOPS.get(f"{zone}_armor", []) + SHOPS.get(f"{zone}_shields", [])


ORDER = {z: n for n, z in enumerate(["starter"] + ORIGIN + GORMAK + VERDANT)}


def best(owned, kind):
    cands = [i for i in owned if kind in i]
    return max(cands, key=lambda i: (ORDER.get(i.split("_")[0], -1), int(i.rsplit("_", 1)[1])))


def armour_set(owned):
    return [best(owned, f"_{slot}_") for slot in SLOTS]


def source_of(resource, zone):
    everyone = ZONE_MOBS.get(zone, []) + [m for mobs in ZONE_MOBS.values() for m in mobs] + ["chicken", "sheep", "cow"]
    for mob in everyone:
        if any(d[0] == resource for d in MOBS[mob]["drops"]):
            return "mob", mob
    return "mine", resource


def kill_drops(mob, sword):
    """Expected resources per kill, every one the mob drops, boosted by Looting."""
    looting = ench(sword, "looting")
    # Looting.java: +1 to the range per two levels, rounded up
    return {rid: c * (avg + (looting + 1) // 2) for rid, c, avg in MOBS[mob]["drops"] if rid in RESOURCES}


def gather(resource, amount, owned, zone, warnings, stock):
    """Seconds to have `amount` more of a resource. Leftovers from earlier kills are
    used first, and every kill's extra drops go back into stock, as in the backpack."""
    held = stock.get(resource, 0.0)
    if held >= amount:
        stock[resource] = held - amount
        return 0.0
    amount -= held
    stock[resource] = 0.0

    kind, what = source_of(resource, zone)
    if kind == "mine":
        line = tool_kind(what)
        if not any(line in i for i in owned):
            warnings.append(f"nothing owned mines {what}, it needs an{line.strip('_').replace('pick', ' pick').replace('axe', ' axe')}")
            return math.inf
        pick = best(owned, line)
        rate, blocked = mine_rate(pick, what)
        if blocked is not None:
            warnings.append(f"{pick} cannot mine {what} (needs Fortune {blocked})")
            return math.inf
        return amount / rate
    sword = best(owned, "_sword_")
    drops = kill_drops(what, sword)
    kills = math.ceil(amount / drops[resource])
    for rid, per_kill in drops.items():
        stock[rid] = stock.get(rid, 0.0) + kills * per_kill
    stock[resource] -= amount
    return kills * kill_cycle_time(sword, armour_set(owned), what)


def play(zones, owned, stock):
    total, warnings = 0.0, []
    for zone in zones:
        zone_time, items = 0.0, zone_items(zone)
        for item, cost in items:
            zone_time += sum(gather(r, a, owned, zone, warnings, stock) for r, a in cost.items() if r not in GEAR)
            owned.add(item)
        total += zone_time
        print(f"  {zone:<8}{len(items):>4} buys {zone_time / 60:8.1f} min   cumulative {total / 60:7.1f} min")
    for w in dict.fromkeys(warnings):
        print(f"  !! {w}")
    return total


def fmt(x, spec=".1f"):
    return "inf" if x == math.inf else format(x, spec)


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--boss-cooldown", type=float, metavar="SECONDS",
                    help=f"seconds between {BOSS} swings (default: attack_cooldown in the CSV)")
    args = ap.parse_args()
    if args.boss_cooldown:
        ATTACK_COOLDOWN[BOSS] = args.boss_cooldown
    print(f"Damage model -> hearts curve, {LEVELS_PER_TIER:g} protection levels per tier, "
          f"boss swings every {ATTACK_COOLDOWN.get(BOSS, 1 / MELEE_HITS_PER_SEC):g}s\n")

    owned = {"starter_pick_0", "starter_sword_0"} | {f"starter_{s}_0" for s in SLOTS}
    print("ORIGIN ISLES playthrough")
    stock = {}  # the backpack, leftovers carry from one purchase to the next
    t_origin = play(ORIGIN, owned, stock)

    print("\nMining income, first and last pick of each zone")
    prev = "starter_pick_0"
    for zone in ORIGIN + ["city", "dunes", "canyons", "temple"]:
        picks = [p for p, _ in SHOPS[f"{zone}_shop"] if "_pick_" in p]
        mine = ORIGIN_MINE.get(zone) or {"city": "carved_brick", "dunes": "sunbaked_sand",
                                         "canyons": "crimson_sand", "temple": "raw_gold"}[zone]
        for p in (prev, picks[-1]):
            rate, blocked = mine_rate(p, mine)
            shown = f"blocked (Fortune < {blocked})" if blocked is not None else f"{rate * 60:9.0f}/min"
            print(f"  {mine:<14}{p:<17}{shown}   {blocks_per_swing(p):4.1f} blocks/swing")
        prev = picks[-1]

    print("\nGORMAK playthrough")
    t_gormak = play(GORMAK, owned, stock)

    print("\nVERDANT GROVES playthrough")
    t_verdant = play(VERDANT, owned, stock)

    print("\nCOMBAT - arriving / mid-zone / exit gear (face-tanking, no dodging)")
    print(f"  {'mob':<21}{'atk':>4}{'hp':>6}   {'hearts per hit':<18}{'time to kill it':<20}{'hearts left after 1v1':<22}")
    for zone, tiers in ZONE_GEAR.items():
        for mob in ZONE_MOBS[zone]:
            fs = [fight(s, [a.format(x) for x in SLOTS], mob) for s, a in tiers]
            per_hit = "/".join(fmt(f["taken"], ".2f") for f in fs)
            ttk = "/".join(fmt(f["ttk"]) for f in fs) + "s"
            left = "/".join(fmt(f["left"], ".1f") for f in fs)
            flag = "  <- loses at exit" if fs[2]["left"] <= 0 else ("  <- loses on arrival" if fs[0]["left"] <= 0 else "")
            print(f"  {mob:<21}{MOBS[mob]['atk']:>4.0f}{MOBS[mob]['hp']:>6.0f}   {per_hit:<18}{ttk:<20}{left:<22}{flag}")
        print()

    print(f"TOTAL   Origin {t_origin / 3600:.2f} h   Gormak {t_gormak / 3600:.2f} h   Verdant {t_verdant / 3600:.2f} h")


if __name__ == "__main__":
    main()
