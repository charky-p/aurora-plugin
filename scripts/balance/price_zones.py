#!/usr/bin/env python3
"""
Prices every upgrade step in scripts/data/upgrades.csv with the price formula.

  zones.csv      each zone's time target, its mines and its mobs (more than one
                 with |), the zone it comes after, and two scales
                 its prices are multiplied by: mine_scale for what is mined and
                 mob_scale for what mobs drop. 1 is the time target as it stands,
                 0.5 half the price, 1.5 half as much again

The price of a step, for each resource it costs:

  price = step seconds x the resource's share x its scale x income per second

  step seconds   the zone's target shared over its steps, rising in a straight
                 line from half the average to one and a half times it (prices.py)
  share          the cost column of upgrades.csv
  scale          mine_scale or mob_scale, by where the resource comes from
  income         what the player's tools earn of it a second just before buying

and the prices of each resource are then rounded, kept from repeating and scaled
back to the budget, see prices.fit. The rounding, the ramp and the Fortune formula
are scripts/price_calculator.py, which can also work one price out by hand.
  upgrades.csv   the steps a zone's shop sells, in purchase order: a tool, a stat,
                 the level it reaches and what it costs in. The cost column holds
                 shares of the step's time, not amounts: coal:1 is "all of this
                 step's time is spent on coal", and copper:0.8|feather:0.2 is four
                 fifths mining and a fifth killing. The amounts are worked out here
                 and written to upgrades.yml.
                 To set an amount by hand, write it with = instead: cobblestone=40
                 is 40 cobblestone, whatever the formula says. A cost can mix both.
                 A level is counted within the zone's realm: Gormak and Verdant
                 Groves each start again from 1 and add to what Origin gave

The player's tools are levels, not items: (tool, stat) -> level, carried from one
zone into the one that comes after it. Two zones can come after the same one: the
Abandoned City and the Oak Grove both follow the Gold Reef. Gormak sells the sword
and the armour and Verdant Groves the pick and the axe, with a little of the other
in each, and their levels add up. So a branch zone is priced for a player who keeps
both branches level: the Dunes for someone who has finished the City and the Oak
Grove. Ahead of that pace a zone is slower, never closed. Income is worked out from
those levels with the same mining and combat numbers as balance_sim.py.

  python scripts/balance/price_zones.py          every zone
  python scripts/balance/price_zones.py copper   one zone
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import balance_sim as sim   # noqa: E402
import prices               # noqa: E402

DATA = sim.DATA

# What a new player holds: wooden tools, leather armour, nothing levelled
START = {("pick", "material"): "WOODEN", ("axe", "material"): "WOODEN", ("sword", "material"): "WOODEN"}

rows = sim.rows
# Tool materials from the first to the last
MATERIALS = list(sim.TOOL)


def level(state, tool, stat):
    return state.get((tool, stat), 0)


def dig_seconds(state, block_id, tool):
    """Vanilla dig time with the tool that mines the block, from its levels."""
    _, speed = sim.TOOL[state[(tool, "material")]]
    eff = level(state, tool, "efficiency")
    speed += eff * eff + 1 if eff else 0
    hardness, _ = sim.BLOCK[sim.PLACED.get(block_id, sim.BLOCKS[block_id]["material"])]
    per_tick = speed / hardness / 30
    if per_tick >= 1:
        return 1 / sim.HUMAN_BLOCKS_PER_SEC
    return (math.ceil(1 / per_tick) + 5) / 20


def blocks_per_swing(state, tool):
    blast = level(state, tool, "blast")
    if not blast:
        return 1.0
    x, y, z = sim.BLAST_SHAPES[min(blast, max(sim.BLAST_SHAPES))]
    return 1 + (x * y * z - 1) * sim.BLAST_FILL


def mine_income(state, mine):
    """Resource per second from a mine, 0 if a block's density is out of reach."""
    tool = sim.tool_kind(mine)
    fortune = level(state, tool, "fortune")
    per_block = seconds = 0.0
    for block_id, share in sim.MINES[mine].items():
        block = sim.BLOCKS[block_id]
        per_block += share * prices.block_income(1, sum(c * avg for _, c, avg in block["drops"]),
                                                 fortune, block["density"])
        seconds += share * dig_seconds(state, block_id, tool)
    return per_block * blocks_per_swing(state, tool) / (seconds / sim.MINING_EFFICIENCY)


def kill_seconds(state, mob_id):
    """Seconds a kill takes: the fight, the walk to the next mob
    and the wait to heal what the fight cost. Infinite if the sword cannot hurt it."""
    mob = sim.MOBS[mob_id]
    hit = max(0, round(10 + sim.sharpness_bonus(level(state, "sword", "sharpness")) - mob["def"]))
    if hit == 0:
        return math.inf
    fight = math.ceil(mob["hp"] / hit) / sim.HITS_PER_SEC
    total = fight + sim.KILL_OVERHEAD_S
    if mob["atk"] <= 0:
        return total

    protection = sum(level(state, slot, "protection") for slot in sim.SLOTS) / len(sim.SLOTS)
    scale = sim.MOB_DAMAGE_SCALE * (sim.RANGED_SCALE if mob["entity"] in sim.RANGED else 1.0)
    taken = sim.hearts((mob["atk"] - protection) / sim.LEVELS_PER_TIER) * scale
    lost = taken * sim.mob_rate(mob_id) * fight
    return total + max(0.0, lost - sim.REGEN_HEARTS_PER_SEC * total) / sim.REGEN_HEARTS_PER_SEC


def mob_income(state, mob_id, resource):
    """Resource per second from killing one kind of mob."""
    mob = sim.MOBS[mob_id]
    seconds = kill_seconds(state, mob_id)
    if seconds == math.inf:
        return 0.0
    # Looting is Fortune for mobs, rare drops (under 100%) by its cube root (Looting.java)
    looting = level(state, "sword", "looting")
    common, rare = prices.fortune_multiplier(looting, 0), prices.rare_multiplier(looting, 0)
    per_kill = sum(chance * avg * (common if chance >= 1 else rare)
                   for drop, chance, avg in mob["drops"] if drop == resource)
    return prices.mob_income(per_kill, seconds)


def sources(zones):
    """resource -> ("mine", mine) or ("mob", [mobs that drop it]), from every zone."""
    found = {}
    for zone in zones:
        for mine in filter(None, zone["mine"].split("|")):
            found[mine] = ("mine", mine)
        for mob in filter(None, zone["mob"].split("|")):
            for drop, _, _ in sim.MOBS[mob]["drops"]:
                if drop in sim.RESOURCES:
                    found.setdefault(drop, ("mob", []))[1].append(mob)
    return found


def price_all():
    """[(zone row, priced steps)] for every zone in order, tools carried between them."""
    zones = sorted(rows("zones"), key=lambda z: int(z["order"]))
    where = sources(zones)
    steps_by_zone = {}
    for row in sorted(rows("upgrades"), key=lambda r: int(r["order"])):
        parts = row["cost"].split("|")
        cost = {k: float(v) for k, v in (part.split(":") for part in parts if "=" not in part)}
        fixed = {k: int(v) for k, v in (part.split("=") for part in parts if "=" in part)}
        step = {"tool": row["tool"], "stat": row["stat"], "cost": cost, "fixed": fixed}
        # The level a step reaches is in the data, never counted up
        step["level"] = row["level"] if row["stat"] == "material" else int(row["level"])
        steps_by_zone.setdefault(row["zone"], []).append(step)

    def income(state, resource):
        kind, source = where[resource]
        if kind == "mine":
            return mine_income(state, source)
        # Whichever mob gives it fastest, the way a player would farm it
        return max(mob_income(state, mob, resource) for mob in source)

    # Realms that are part of the same one are branches: Gormak and Verdant Groves
    # both hang off the Overworld. Their levels add up, so a branch zone is priced
    # for a player who keeps the branches level: on reaching a zone they have
    # finished the zones before it in every branch
    part_of = {r["id"]: r.get("part_of") or "" for r in rows("realms")}
    chain = {}
    for zone in zones:
        chain.setdefault(zone["realm"], []).append(zone["id"])

    def own(realm, depth):
        """What the first `depth` zones of a realm sell, in the realm's own levels."""
        levels = {}
        for zone_id in chain[realm][:depth]:
            for step in steps_by_zone.get(zone_id, []):
                levels[(step["tool"], step["stat"])] = step["sold"]
        return levels

    def add(state, levels):
        """Levels stacked on a state: enchants add up, the further material wins."""
        total = dict(state)
        for key, value in levels.items():
            if key[1] == "material":
                if MATERIALS.index(value) > MATERIALS.index(total.get(key, MATERIALS[0])):
                    total[key] = value
            else:
                total[key] = total.get(key, 0) + value
        return total

    for steps in steps_by_zone.values():
        for step in steps:
            step["sold"] = step["level"]

    # What the player holds on leaving each zone, the next one starts from it
    leaving = {"": dict(START)}
    result = []
    # What the player had on entering each realm: a realm's levels start from 1 and
    # add to that
    entered = {}
    for zone in zones:
        state = leaving[zone.get("after") or ""]
        realm = zone["realm"]
        entry = entered.setdefault(realm, state)
        steps = steps_by_zone.get(zone["id"], [])
        bought = [step["level"] for step in steps]
        if part_of[realm]:
            depth = chain[realm].index(zone["id"])
            for other in chain:
                if other != realm and part_of[other] == part_of[realm]:
                    entry = add(entry, own(other, depth))
            state = add(entry, own(realm, depth))
        for step in steps:
            if step["stat"] != "material":
                step["level"] += entry.get((step["tool"], step["stat"]), 0)
        def scale(resource, zone=zone):
            return float(zone.get("mine_scale" if where[resource][0] == "mine" else "mob_scale") or 1)

        priced = prices.zone_prices(int(zone["target_seconds"]), steps, income, state, scale=scale)
        # The state holds what the tool has in all, the shop sells the realm's own level
        for row, level in zip(priced, bought):
            row["level"] = level
        leaving[zone["id"]] = priced[-1]["state"] if priced else state
        result.append((zone, priced))
    return result


def show(zone, priced):
    print(f"\n{zone['name']}  (target {int(zone['target_seconds']) / 60:.1f} min, mined x{zone.get('mine_scale') or 1},"
          f" looted x{zone.get('mob_scale') or 1}, {len(priced)} steps)")
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
