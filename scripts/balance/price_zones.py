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

# A test world spawner brings a mob every 100 ticks, so one mob kind is never killed faster
SPAWN_SECONDS = 5.0
# Share of mining time spent killing hostile mobs that come at you, in a zone that has them
AMBIENT_FIGHT = 0.3
# How far through the other regions of its own stage a zone assumes the player is, as a
# share of how far they are through its own: 0 prices for someone playing one region at
# a time, 1 for someone keeping them all level. In between, a region played alone is
# slower than its target and one played alongside the others faster
PARALLEL_SHARE = 0.35

# What a new player holds: wooden tools, leather armour, nothing levelled
START = {("pick", "material"): "WOODEN", ("axe", "material"): "WOODEN", ("sword", "material"): "WOODEN"}

rows = sim.rows
# Tool materials from the first to the last
MATERIALS = list(sim.TOOL)


def level(state, tool, stat):
    return state.get((tool, stat), 0)


def dig_seconds(state, block_id, tool):
    """Vanilla dig time with the tool that mines the block, from its levels."""
    _, speed = sim.TOOL[state.get((tool, "material"), "WOODEN")]
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
    """Seconds a kill takes: the fight, the walk to the next mob and the wait to heal
    what the fight cost, and never less than its spawner takes to bring the next one.
    Infinite if the sword cannot hurt it."""
    mob = sim.MOBS[mob_id]
    hit = max(0, round(10 + sim.sharpness_bonus(level(state, "sword", "sharpness")) - mob["def"]))
    if hit == 0:
        return math.inf
    fight = math.ceil(mob["hp"] / hit) / sim.HITS_PER_SEC
    total = fight + sim.KILL_OVERHEAD_S
    if mob["atk"] > 0:
        protection = sum(level(state, slot, "protection") for slot in sim.SLOTS) / len(sim.SLOTS)
        scale = sim.MOB_DAMAGE_SCALE * (sim.RANGED_SCALE if mob["entity"] in sim.RANGED else 1.0)
        taken = sim.hearts((mob["atk"] - protection) / sim.LEVELS_PER_TIER) * scale
        lost = taken * sim.mob_rate(mob_id) * fight
        total += max(0.0, lost - sim.REGEN_HEARTS_PER_SEC * total) / sim.REGEN_HEARTS_PER_SEC
    # A world boss like the Overworld Golem comes back on its own timer, the respawn column
    return max(total, mob.get("respawn") or SPAWN_SECONDS)


def per_kill(state, mob_id, resource):
    """How many of a resource one kill gives. Looting counts past the mob's density like
    Fortune past a block's: nothing below it, then the Fortune curve, rare drops (under
    100%) by its cube root (Looting.java)."""
    mob = sim.MOBS[mob_id]
    effective = level(state, "sword", "looting") - mob["density"]
    if effective < 0:
        return 0.0
    common, rare = prices.fortune_multiplier(effective, 0), prices.rare_multiplier(effective, 0)
    return sum(chance * avg * (common if chance >= 1 else rare)
               for drop, chance, avg in mob["drops"] if drop == resource)


def mob_income(state, mob_id, resource):
    """Resource per second from killing one kind of mob."""
    seconds = kill_seconds(state, mob_id)
    if seconds == math.inf:
        return 0.0
    return prices.mob_income(per_kill(state, mob_id, resource), seconds)


def hostile(mob_id):
    return sim.MOBS[mob_id]["atk"] > 0


class KillBudget:
    """
    The mob drops of one zone, priced from the kills a player really makes there. A
    step's fighting time (its mob shares times mob_scale) goes to the mob that drops
    each resource fastest; while mining in a zone with hostile mobs AMBIENT_FIGHT of
    the time is spent killing whatever comes at you. Every kill pays all of the mob's
    drops, so a resource farmed for one step also pays towards the next.
    """

    def __init__(self, zone, where, scale):
        self.mobs = [m for m in (zone["mob"] or "").split("|") if m]
        self.resources = {r for m in self.mobs for r, _, _ in sim.MOBS[m]["drops"]
                          if r in sim.RESOURCES and where.get(r, ("", None))[0] == "mob"}
        self.where, self.scale = where, scale

    def produce(self, state, seconds, cost):
        fight, mining = {}, 0.0
        for resource, share in cost.items():
            if resource in self.resources:
                best = max(self.where[resource][1], key=lambda m: mob_income(state, m, resource))
                fight[best] = fight.get(best, 0.0) + seconds * share * self.scale(resource)
            elif self.where.get(resource, ("", None))[0] == "mine":
                mining += seconds * share * self.scale(resource)
        angry = [m for m in self.mobs if hostile(m)]
        for mob in angry:
            fight[mob] = fight.get(mob, 0.0) + mining * AMBIENT_FIGHT / len(angry)

        made = {}
        for mob, time in fight.items():
            kill = kill_seconds(state, mob)
            if kill == math.inf:
                continue
            for resource in {r for r, _, _ in sim.MOBS[mob]["drops"]} & self.resources:
                made[resource] = made.get(resource, 0.0) + time / kill * per_kill(state, mob, resource)
        return made


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


def pace(zones):
    """Model seconds per real second, from the zones that have both a target and the
    minutes they really take. A zone with only minutes gets minutes x 60 x this."""
    known = [z for z in zones if z.get("target_seconds") and z.get("minutes")]
    model = sum(int(z["target_seconds"]) for z in known)
    real = sum(float(z["minutes"]) * 60 for z in known)
    return model / real if real else 1.0


def target_seconds(zone, zones):
    if zone.get("target_seconds"):
        return int(zone["target_seconds"])
    return round(float(zone["minutes"]) * 60 * pace(zones))


def read_steps():
    """zone id -> its steps in purchase order, as upgrades.csv lists them."""
    steps_by_zone = {}
    for row in sorted(rows("upgrades"), key=lambda r: int(r["order"])):
        parts = row["cost"].split("|")
        cost = {k: float(v) for k, v in (part.split(":") for part in parts if "=" not in part)}
        fixed = {k: int(v) for k, v in (part.split("=") for part in parts if "=" in part)}
        step = {"tool": row["tool"], "stat": row["stat"], "cost": cost, "fixed": fixed}
        # The level a step reaches is in the data, never counted up
        step["sold"] = row["level"] if row["stat"] == "material" else int(row["level"])
        steps_by_zone.setdefault(row["zone"], []).append(step)
    return steps_by_zone


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


def zone_states(zones=None, steps_by_zone=None):
    """
    zone id -> (entry, state): what the player holds from every other region, and that
    plus what the zone's own region sold before it.

    Regions of one stage are played side by side and their levels add up. A zone is
    priced for a player who has finished every released region of every earlier stage,
    and has gone PARALLEL_SHARE as far through the other released regions of its own
    stage as through its own. A region that is not released counts only when it leads
    to the zone, so nothing is priced on levels no player can have yet.
    """
    zones = zones or sorted(rows("zones"), key=lambda z: int(z["order"]))
    steps_by_zone = steps_by_zone or read_steps()
    regions = {r["id"]: r for r in rows("regions")}
    chain = {}
    for zone in zones:
        chain.setdefault(zone["region"], []).append(zone["id"])
    by_id = {z["id"]: z for z in zones}

    def stage(zone):
        return int(zone.get("stage") or regions[zone["region"]]["stage"])

    def released(region):
        return (regions.get(region, {}).get("released") or "").strip().lower() in ("yes", "true", "1")

    def leading(region):
        """The regions a region follows, by after or by the portal leading in."""
        found, row = set(), regions.get(region)
        while row:
            parent = (row.get("after") or "").strip() or (row.get("portal") or "").strip()
            if not parent or parent == region or parent in found:
                break
            found.add(parent)
            row = regions.get(parent)
        return found

    def own(region, depth):
        """What the first `depth` zones of a region sell, in its realm's own levels."""
        levels = {}
        for zone_id in chain[region][:depth]:
            for step in steps_by_zone.get(zone_id, []):
                levels[(step["tool"], step["stat"])] = step["sold"]
        return levels

    states = {}
    for zone in zones:
        region, here = zone["region"], stage(zone)
        index = chain[region].index(zone["id"])
        progress = index / len(chain[region])
        entry = dict(START)
        leads = leading(region)
        for other, ids in chain.items():
            if other == region or not (released(other) or other in leads):
                continue
            other_stage = min(stage(by_id[i]) for i in ids)
            if other_stage < here:
                entry = add(entry, own(other, len(ids)))
            elif other_stage == here:
                entry = add(entry, own(other, int(progress * PARALLEL_SHARE * len(ids))))
        states[zone["id"]] = (entry, add(entry, own(region, index)))
    return states


def price_all():
    """[(zone row, priced steps)] for every zone in order, tools carried between them."""
    zones = sorted(rows("zones"), key=lambda z: int(z["order"]))
    where = sources(zones)
    steps_by_zone = read_steps()
    states = zone_states(zones, steps_by_zone)

    def income(state, resource):
        kind, source = where[resource]
        if kind == "mine":
            return mine_income(state, source)
        # Whichever mob gives it fastest, the way a player would farm it
        return max(mob_income(state, mob, resource) for mob in source)

    result = []
    for zone in zones:
        entry, state = states[zone["id"]]
        steps = [dict(step) for step in steps_by_zone.get(zone["id"], [])]
        for step in steps:
            if step["stat"] == "material":
                step["level"] = step["sold"]
            else:
                step["level"] = step["sold"] + entry.get((step["tool"], step["stat"]), 0)

        def scale(resource, zone=zone):
            return float(zone.get("mine_scale" if where[resource][0] == "mine" else "mob_scale") or 1)

        pool = KillBudget(zone, where, scale) if zone["mob"] else None
        # Time spent fighting what comes at you is time not mining
        busy = AMBIENT_FIGHT if pool and any(hostile(m) for m in pool.mobs) else 0.0

        def zone_income(state, resource, busy=busy):
            rate = income(state, resource)
            return rate * (1 - busy) if where[resource][0] == "mine" else rate

        priced = prices.zone_prices(target_seconds(zone, zones), steps, zone_income, state, scale=scale, pool=pool)
        # The state holds what the tool has in all, the shop sells the realm's own level
        for row, step in zip(priced, steps):
            row["level"] = step["sold"]
        result.append((zone, priced))
    return result


def show(zone, priced, zones):
    # A zone with only minutes gets its target from them, see target_seconds
    print(f"\n{zone['name']}  (target {target_seconds(zone, zones) / 60:.1f} min, mined x{zone.get('mine_scale') or 1},"
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
    zones = rows("zones")
    for zone, priced in price_all():
        if only in (None, zone["id"]):
            show(zone, priced, zones)
