#!/usr/bin/env python3
"""
Aurora price formula.

An upgrade costs what the player gathers in the time that upgrade should take:

    price = seconds for this step * how fast the resource comes in right now

  seconds for a step   a zone has a time target, shared out over its upgrade steps
                       in the order they are bought. The times rise in a straight
                       line, price_calculator.ramp: with SPREAD 0.5 the first step
                       takes half the average and the last one and a half times it
  income, mined        blocks per second * drop per block * fortune multiplier
                       fortune multiplier = 1 + 0.75 L + 0.25 L^2, L = fortune - density,
                       nothing below the density (price_calculator.fortune)
  income, mob drop     drop per kill / seconds per kill

Then the prices of one resource in one zone are made to read well, fit():
  - rounded down to a price worth showing, price_calculator.nice_round
  - never the same twice in a row: steps bought together, the four armour pieces
    at one Protection level, share a price, and each group costs more than the
    one before it
  - scaled so they still add up to what the time target allows
  When a zone's whole budget of something is too small for that, ten feathers over
  eight steps, the prices are only rounded and may repeat.

Income is worked out from what the player has BEFORE buying the step, so every
Fortune level bought makes the next price bigger by itself. Nothing is priced by
hand unless a cost is written with = in upgrades.csv: change a time target, a drop
or a density and every price follows.

The formula does not care what is being upgraded. A step is a tool (pick, axe,
shovel, sword, armour piece), a stat on it and what it costs in, so every tool and
every zone, sub world or event world uses the same code.

  python scripts/balance/prices.py            the worked example below
  python scripts/balance/prices.py --check    run the self checks
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import price_calculator as calc   # noqa: E402

# How far the step times of a zone ramp: 0 is every step the same, 0.5 is from half
# the average to one and a half times it
SPREAD = 0.5
# Distinct prices may cost this much more than the time target allows before the
# zone falls back to prices that repeat
OVER_BUDGET = 1.25


def step_seconds(target_seconds, steps, spread=SPREAD):
    """The zone's time target shared over its steps, longest last."""
    return calc.ramp(target_seconds, steps, spread)


def fortune_multiplier(fortune, density):
    """How many times over a block drops, DataRegistry.getBlockDrops. Nothing below
    the block's density, then 1, 2, 3.5, 5.5, 8 ... for every level past it."""
    return calc.fortune(fortune, density)


def block_income(blocks_per_second, drop_per_block, fortune, density):
    return blocks_per_second * drop_per_block * fortune_multiplier(fortune, density)


def mob_income(drop_per_kill, seconds_per_kill):
    """Mob drop per second, seconds_per_kill includes finding the next mob."""
    return drop_per_kill / seconds_per_kill


nice = calc.nice_round


def fit(raw, sizes):
    """
    Prices for the groups of one resource in one zone, in purchase order.

    raw     what the formula gives each group, unrounded
    sizes   how many steps share each group's price
    """
    budget = sum(amount * size for amount, size in zip(raw, sizes))

    def build(scale):
        return calc.distinct([nice(amount * scale) for amount in raw])

    def total(prices):
        return sum(price * size for price, size in zip(prices, sizes))

    # Even the cheapest prices that never repeat cost too much. Whole numbers then,
    # never dropping, with what rounding left over carried on to the next group so
    # the later ones rise as soon as the budget lets them
    if len(raw) > 1 and total(build(0)) > budget * OVER_BUDGET:
        prices, carried = [], 0.0
        for amount, size in zip(raw, sizes):
            carried += amount * size
            price = max(1, prices[-1] if prices else 1, int(carried // size))
            carried -= price * size
            prices.append(price)
        return prices

    # The biggest scale whose prices still fit the budget
    low, high = 0.0, 4.0
    for _ in range(40):
        middle = (low + high) / 2
        if total(build(middle)) <= budget:
            low = middle
        else:
            high = middle
    under, over = build(low), build(high)
    return under if budget - total(under) <= total(over) - budget else over


def zone_prices(target_seconds, steps, income, start, spread=SPREAD, scale=None):
    """
    Prices for every step of a zone, in purchase order.

    steps    list of {"tool", "stat", "cost": {resource: share}}. The shares of a
             step's cost add up to 1 and split its seconds between its resources.
             "fixed": {resource: amount} is charged as written, no formula.
    income   function(state, resource) -> that resource per second
    start    {(tool, stat): level} the player arrives with. Buying a step adds 1,
             or sets the step's own "level", which a material change uses
    scale    function(resource) -> what that resource's prices are multiplied by,
             so mined and looted costs can be made cheaper or dearer apart
    """
    state = dict(start)
    # A step whose whole cost is written out by hand takes none of the zone's time
    seconds = iter(step_seconds(target_seconds, sum(1 for step in steps if step["cost"]), spread))
    priced = []
    for step in steps:
        time = next(seconds) if step["cost"] else 0.0
        raw = {resource: time * share * (scale(resource) if scale else 1) * income(state, resource)
               for resource, share in step["cost"].items()}
        key = (step["tool"], step["stat"])
        state[key] = step["level"] if "level" in step else state.get(key, 0) + 1
        priced.append({"tool": step["tool"], "stat": step["stat"], "level": state[key],
                       "seconds": time, "raw": raw, "cost": {}, "state": dict(state)})

    # Each resource's prices are fitted together. Steps that follow each other with
    # the same stat and level, the four armour pieces, are one group with one price
    for resource in dict.fromkeys(r for row in priced for r in row["raw"]):
        rows = [row for row in priced if resource in row["raw"]]
        groups = []
        for row in rows:
            if groups and (groups[-1][0]["stat"], groups[-1][0]["level"]) == (row["stat"], row["level"]):
                groups[-1].append(row)
            else:
                groups.append([row])
        amounts = [sum(row["raw"][resource] for row in group) / len(group) for group in groups]
        for group, price in zip(groups, fit(amounts, [len(group) for group in groups])):
            for row in group:
                row["cost"][resource] = price

    for row, step in zip(priced, steps):
        # In the order the step lists them, then what was written by hand
        row["cost"] = {resource: row["cost"][resource] for resource in step["cost"]}
        row["cost"].update(step.get("fixed") or {})
        del row["raw"]
    return priced


# ------------------------------------------------------------------ example
# The Coal Seam with made up numbers, to show the shape of the output. Real zones
# will read their steps, drops and densities from scripts/data once that exists.
def example():
    density = 0
    drops = {"coal": 3.0, "wool": 2.0}     # per block, per kill

    def income(state, resource):
        if resource == "coal":
            # Every Efficiency level bought digs a little faster
            blocks = 1.5 + 0.25 * state.get(("pick", "efficiency"), 0)
            return block_income(blocks, drops["coal"], state.get(("pick", "fortune"), 0), density)
        # Every Sharpness level bought kills a little faster
        kill = 8.0 - 0.5 * state.get(("sword", "sharpness"), 0)
        return mob_income(drops["wool"], kill)

    steps = [
        {"tool": "pick", "stat": "fortune", "cost": {"coal": 1.0}},
        {"tool": "pick", "stat": "efficiency", "cost": {"coal": 1.0}},
        {"tool": "sword", "stat": "sharpness", "cost": {"coal": 0.5, "wool": 0.5}},
        {"tool": "pick", "stat": "fortune", "cost": {"coal": 1.0}},
        {"tool": "chestplate", "stat": "protection", "cost": {"coal": 0.4, "wool": 0.6}},
        {"tool": "pick", "stat": "fortune", "cost": {"coal": 0.8, "wool": 0.2}},
    ]
    start = {("pick", "fortune"): 1, ("pick", "efficiency"): 1, ("sword", "sharpness"): 1}
    return zone_prices(120, steps, income, start)


def show(priced):
    print(f"{'step':<28}{'seconds':>8}  cost")
    for row in priced:
        name = f"{row['tool']} {row['stat']} {row['level']}"
        cost = ", ".join(f"{amount} {resource}" for resource, amount in row["cost"].items())
        print(f"{name:<28}{row['seconds']:>8.1f}  {cost}")
    print(f"{'total':<28}{sum(row['seconds'] for row in priced):>8.1f}")


def check():
    times = step_seconds(120, 6)
    assert abs(sum(times) - 120) < 1e-9, "steps add up to the target"
    assert all(b > a for a, b in zip(times, times[1:])), "every step is longer than the last"
    assert abs(times[-1] / times[0] - 3) < 1e-9, "the last step is three times the first"
    assert step_seconds(60, 1) == [60], "one step takes the whole zone"
    assert step_seconds(60, 0) == []

    assert block_income(2, 3, 0, 0) == 6
    assert block_income(2, 3, 2, 0) == 21, "fortune 2 is 3.5 times a density 0 block"
    assert block_income(2, 3, 2, 3) == 0, "below the density nothing drops"
    assert mob_income(2, 8) == 0.25

    assert [nice(n) for n in (0.2, 1, 19, 29, 2050)] == [1, 1, 16, 24, 2000], "rounded down to a price worth showing"
    assert [calc.next_step(n) for n in (4, 20, 80, 2000)] == [8, 24, 100, 2500]
    assert calc.distinct([8, 8, 8, 40]) == [8, 12, 16, 40], "never the same twice in a row"

    prices = fit([10, 14, 18, 22, 26], [1, 1, 4, 1, 1])
    assert all(b > a for a, b in zip(prices, prices[1:])), "every group costs more than the last"
    spent = sum(p * s for p, s in zip(prices, [1, 1, 4, 1, 1]))
    assert abs(spent - 144) <= 144 * 0.2, "and they still add up to about the budget"
    assert fit([0.6, 0.8, 1.0, 1.6], [1, 1, 4, 2]) == [1, 1, 1, 1], "too small a budget to tell prices apart"
    tight = fit([1, 1.5, 2.2, 3.4], [1, 4, 1, 1])
    assert tight == sorted(tight) and tight[-1] > tight[0], "but they rise once there is enough left over"

    priced = example()
    coal = [row["cost"]["coal"] for row in priced]
    assert all(b > a for a, b in zip(coal, coal[1:])), "each step costs more coal than the last"
    assert abs(sum(row["seconds"] for row in priced) - 120) < 1e-9
    print("prices.py: all checks passed")


if __name__ == "__main__":
    if "--check" in sys.argv:
        check()
    else:
        show(example())
