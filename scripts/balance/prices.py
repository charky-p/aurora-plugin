#!/usr/bin/env python3
"""
Aurora price formula.

An upgrade costs what the player gathers in the time that upgrade should take:

    price = seconds for this step * how fast the resource comes in right now

  seconds for a step   a zone has a time target, shared out over its upgrade steps
                       in the order they are bought. A zone starts quick and its
                       last upgrade takes SPREAD times as long as its first,
                       however many steps there are in between:
                         growth = SPREAD ** (1 / (steps - 1))
                         weight(i) = growth ** i
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
import sys

# The last step of a zone takes this many times as long as its first
SPREAD = 4.0


def step_seconds(target_seconds, steps, spread=SPREAD):
    """The zone's time target shared over its steps, longest last."""
    if steps <= 0:
        return []
    growth = spread ** (1 / (steps - 1)) if steps > 1 else 1.0
    weights = [growth ** i for i in range(steps)]
    total = sum(weights)
    return [target_seconds * w / total for w in weights]


def fortune_multiplier(fortune, density):
    """How many times over a block drops, DataRegistry.getBlockDrops. Nothing below
    the block's density, then 1, 2, 3.5, 5.5, 8 ... for every level past it."""
    level = fortune - density
    if level < 0:
        return 0.0
    return 1 + 0.75 * level + 0.25 * level * level


def block_income(blocks_per_second, drop_per_block, fortune, density):
    return blocks_per_second * drop_per_block * fortune_multiplier(fortune, density)


def mob_income(drop_per_kill, seconds_per_kill):
    """Mob drop per second, seconds_per_kill includes finding the next mob."""
    return drop_per_kill / seconds_per_kill


# Prices a player recognises. Small ones are parts of a stack of 64, so 19 reads
# as 16 and 29 as 32. From a thousand up they are round numbers, so 2050 is 2000
SMALL_PRICES = [1, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64, 96, 128, 160, 200, 256, 320, 400, 500, 640, 750]
ROUND_DIGITS = [1, 1.25, 1.5, 2, 2.5, 3, 4, 5, 6, 7.5]


def nice(amount):
    """The nearest price worth showing, never below 1."""
    if amount <= 1:
        return 1
    ladder = list(SMALL_PRICES)
    power = 1000
    while ladder[-1] < amount * 2:
        ladder += [int(digit * power) for digit in ROUND_DIGITS]
        power *= 10
    return min(ladder, key=lambda step: (abs(step - amount), step))


def price(seconds, income_per_second):
    return nice(seconds * income_per_second)


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
        cost = {resource: price(time * share * (scale(resource) if scale else 1), income(state, resource))
                for resource, share in step["cost"].items()}
        cost.update(step.get("fixed") or {})
        key = (step["tool"], step["stat"])
        state[key] = step["level"] if "level" in step else state.get(key, 0) + 1
        priced.append({"tool": step["tool"], "stat": step["stat"], "level": state[key],
                       "seconds": time, "cost": cost, "state": dict(state)})
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
    assert abs(times[-1] / times[0] - SPREAD) < 1e-9, "the last step is SPREAD times the first"
    many = step_seconds(120, 30)
    assert abs(many[-1] / many[0] - SPREAD) < 1e-9, "however many steps there are"
    assert step_seconds(60, 1) == [60], "one step takes the whole zone"
    assert step_seconds(60, 0) == []

    assert block_income(2, 3, 0, 0) == 6
    assert block_income(2, 3, 2, 0) == 21, "fortune 2 is 3.5 times a density 0 block"
    assert block_income(2, 3, 2, 3) == 0, "below the density nothing drops"
    assert mob_income(2, 8) == 0.25

    assert [nice(n) for n in (0.2, 1, 19, 29, 2050)] == [1, 1, 16, 32, 2000], "the prices Charky asked for"
    assert [nice(n) for n in (7, 43, 187, 700, 1234, 98765)] == [6, 48, 200, 750, 1250, 100000]
    assert all(nice(n) == n for n in SMALL_PRICES), "a nice price stays as it is"
    assert price(10, 6) == 64, "60 is priced as a stack"

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
