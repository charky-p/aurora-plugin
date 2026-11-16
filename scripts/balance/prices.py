#!/usr/bin/env python3
"""
Aurora price formula.

An upgrade costs what the player gathers in the time that upgrade should take:

    price = seconds for this step * how fast the resource comes in right now

  seconds for a step   a zone has a time target, shared out over its upgrade steps
                       in the order they are bought. Each step gets GROWTH times
                       the time of the one before, so a zone starts quick and its
                       last upgrades are the long ones:
                         weight(i) = GROWTH ** i
                         seconds(i) = target * weight(i) / sum of all weights
  income, mined        blocks per second * drop per block * (fortune + 1 - density)
  income, mob drop     drop per kill / seconds per kill

Income is worked out from what the player has BEFORE buying the step, so every
Fortune level bought makes the next price bigger by itself. Nothing is priced by
hand: change a time target, a drop or a density and every price follows.

The formula does not care what is being upgraded. A step is a tool (pick, axe,
shovel, sword, armour piece), a stat on it and what it costs in, so every tool and
every zone, sub world or event world uses the same code.

  python scripts/balance/prices.py            the worked example below
  python scripts/balance/prices.py --check    run the self checks
"""
import math
import sys

# Each step takes this many times as long as the one before it
GROWTH = 1.25


def step_seconds(target_seconds, steps, growth=GROWTH):
    """The zone's time target shared over its steps, longest last."""
    if steps <= 0:
        return []
    weights = [growth ** i for i in range(steps)]
    total = sum(weights)
    return [target_seconds * w / total for w in weights]


def block_income(blocks_per_second, drop_per_block, fortune, density):
    """Mined resource per second. Nothing at all below the block's density."""
    return blocks_per_second * drop_per_block * max(0, fortune + 1 - density)


def mob_income(drop_per_kill, seconds_per_kill):
    """Mob drop per second, seconds_per_kill includes finding the next mob."""
    return drop_per_kill / seconds_per_kill


def nice(amount):
    """Prices read as round numbers: two significant figures, never below 1."""
    if amount <= 1:
        return 1
    magnitude = 10 ** (math.floor(math.log10(amount)) - 1)
    return max(1, int(round(amount / magnitude) * magnitude))


def price(seconds, income_per_second):
    return nice(seconds * income_per_second)


def zone_prices(target_seconds, steps, income, start, growth=GROWTH):
    """
    Prices for every step of a zone, in purchase order.

    steps    list of {"tool", "stat", "cost": {resource: share}}. The shares of a
             step's cost add up to 1 and split its seconds between its resources.
    income   function(state, resource) -> that resource per second
    start    {(tool, stat): level} the player arrives with. Buying a step adds 1
    """
    state = dict(start)
    seconds = step_seconds(target_seconds, len(steps), growth)
    priced = []
    for step, time in zip(steps, seconds):
        cost = {resource: price(time * share, income(state, resource))
                for resource, share in step["cost"].items()}
        key = (step["tool"], step["stat"])
        state[key] = state.get(key, 0) + 1
        priced.append({"tool": step["tool"], "stat": step["stat"], "level": state[key],
                       "seconds": time, "cost": cost})
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
    assert step_seconds(60, 1) == [60], "one step takes the whole zone"
    assert step_seconds(60, 0) == []

    assert block_income(2, 3, 0, 0) == 6
    assert block_income(2, 3, 2, 0) == 18, "fortune 2 triples a density 0 block"
    assert block_income(2, 3, 2, 3) == 0, "below the density nothing drops"
    assert mob_income(2, 8) == 0.25

    assert [nice(n) for n in (0.2, 1, 7.4, 43, 187, 1234, 98765)] == [1, 1, 7, 43, 190, 1200, 99000]
    assert price(10, 6) == 60

    priced = example()
    coal = [row["cost"]["coal"] for row in priced if row["stat"] == "fortune" and row["cost"] == {"coal": row["cost"]["coal"]}]
    assert coal == sorted(coal), "each fortune level costs more than the last"
    assert abs(sum(row["seconds"] for row in priced) - 120) < 1e-9
    print("prices.py: all checks passed")


if __name__ == "__main__":
    if "--check" in sys.argv:
        check()
    else:
        show(example())
