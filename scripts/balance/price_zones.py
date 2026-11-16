#!/usr/bin/env python3
"""
Prices every upgrade step in scripts/data/upgrades.csv with the price formula.

  zones.csv      each zone's time target, its mine and its mob
  upgrades.csv   the steps a zone's shop sells, in purchase order: a tool, a stat,
                 the level it reaches and what it costs in. The cost column holds
                 shares of the step's time, not amounts: coal:1, or
                 copper:0.8|feather:0.2 for four fifths mining and a fifth killing

The player's tools are levels, not items: (tool, stat) -> level, carried from one
zone into the next. Income is worked out from those levels with the same mining
and combat numbers as balance_sim.py.

  python scripts/balance/price_zones.py          every zone
  python scripts/balance/price_zones.py copper   one zone
"""
import csv
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import balance_sim as sim   # noqa: E402
import prices               # noqa: E402

DATA = sim.DATA

# What a new player holds: wooden tools, leather armour, nothing levelled
START = {("pick", "material"): "WOODEN", ("sword", "material"): "WOODEN"}


def rows(name):
    with open(DATA / f"{name}.csv", newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def level(state, tool, stat):
    return state.get((tool, stat), 0)


def dig_seconds(state, block_id):
    """balance_sim.dig_seconds, from levels instead of a pick item."""
    _, speed = sim.TOOL[state[("pick", "material")]]
    eff = level(state, "pick", "efficiency")
    speed += eff * eff + 1 if eff else 0
    hardness, _ = sim.BLOCK[sim.PLACED.get(block_id, sim.BLOCKS[block_id]["material"])]
    per_tick = speed / hardness / 30
    if per_tick >= 1:
        return 1 / sim.HUMAN_BLOCKS_PER_SEC
    return (math.ceil(1 / per_tick) + 5) / 20


def blocks_per_swing(state):
    blast = level(state, "pick", "blast")
    if not blast:
        return 1.0
    x, y, z = sim.BLAST_SHAPES[min(blast, max(sim.BLAST_SHAPES))]
    return 1 + (x * y * z - 1) * sim.BLAST_FILL


def mine_income(state, mine):
    """Resource per second from a mine, 0 if a block's density is out of reach."""
    fortune = level(state, "pick", "fortune")
    per_block = seconds = 0.0
    for block_id, share in sim.MINES[mine].items():
        block = sim.BLOCKS[block_id]
        per_block += share * prices.block_income(1, sum(c * avg for _, c, avg in block["drops"]),
                                                 fortune, block["density"])
        seconds += share * dig_seconds(state, block_id)
    return per_block * blocks_per_swing(state) / (seconds / sim.MINING_EFFICIENCY)


def mob_income(state, mob_id, resource):
    """Resource per second from killing one kind of mob."""
    mob = sim.MOBS[mob_id]
    hit = max(0, round(10 + sim.sharpness_bonus(level(state, "sword", "sharpness")) - mob["def"]))
    if hit == 0:
        return 0.0
    kill_seconds = math.ceil(mob["hp"] / hit) / sim.HITS_PER_SEC + sim.KILL_OVERHEAD_S
    looting = (level(state, "sword", "looting") + 1) // 2
    per_kill = sum(chance * (avg + looting) for drop, chance, avg in mob["drops"] if drop == resource)
    return prices.mob_income(per_kill, kill_seconds)


def sources(zones):
    """resource -> ("mine", mine) or ("mob", mob), from every zone so far."""
    found = {}
    for zone in zones:
        found[zone["mine"]] = ("mine", zone["mine"])
        if zone["mob"]:
            for drop, _, _ in sim.MOBS[zone["mob"]]["drops"]:
                found.setdefault(drop, ("mob", zone["mob"]))
    return found


def price_all():
    """[(zone row, priced steps)] for every zone in order, tools carried between them."""
    zones = sorted(rows("zones"), key=lambda z: int(z["order"]))
    where = sources(zones)
    steps_by_zone = {}
    for row in sorted(rows("upgrades"), key=lambda r: int(r["order"])):
        cost = {k: float(v) for k, v in (part.split(":") for part in row["cost"].split("|"))}
        step = {"tool": row["tool"], "stat": row["stat"], "cost": cost}
        if row["stat"] == "material":
            step["level"] = row["level"]
        steps_by_zone.setdefault(row["zone"], []).append(step)

    def income(state, resource):
        kind, source = where[resource]
        return mine_income(state, source) if kind == "mine" else mob_income(state, source, resource)

    state = dict(START)
    result = []
    for zone in zones:
        priced = prices.zone_prices(int(zone["target_seconds"]), steps_by_zone.get(zone["id"], []), income, state)
        if priced:
            state = priced[-1]["state"]
        result.append((zone, priced))
    return result


def show(zone, priced):
    print(f"\n{zone['name']}  (target {int(zone['target_seconds']) / 60:.1f} min, {len(priced)} steps)")
    totals = {}
    for row in priced:
        name = f"{row['tool']} {row['stat']} {row['level']}"
        cost = ", ".join(f"{amount} {resource}" for resource, amount in row["cost"].items())
        print(f"  {name:<28}{row['seconds']:>6.1f}s  {cost}")
        for resource, amount in row["cost"].items():
            totals[resource] = totals.get(resource, 0) + amount
    print("  total: " + ", ".join(f"{amount} {resource}" for resource, amount in totals.items()))


if __name__ == "__main__":
    only = sys.argv[1] if len(sys.argv) > 1 else None
    for zone, priced in price_all():
        if only in (None, zone["id"]):
            show(zone, priced)
