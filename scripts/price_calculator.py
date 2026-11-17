# price_calculator.py
# The pieces every price is made of. scripts/balance/prices.py uses nice_round,
# next_step, ramp and fortune from here to price every upgrade in upgrades.csv, so
# a change in this file changes the prices in the game after python scripts/generate.py.
# calc_price and calc_mob_price work a price out by hand from numbers you give them.
#
# This is a heuristic, eta -> 1 means full efficiency, however it can be used to scale
# to be more difficult, that is, eta -> around 10 at some point

import math

# Explicit Minecraft-friendly checkpoints below 100
SMALL_STEPS = [1, 2, 3, 4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 80]
# From 100 up, the first digits a price may have
MANTISSA_STEPS = [1.0, 1.2, 1.5, 1.6, 2.0, 2.5, 3.0, 3.2, 4.0, 4.8, 5.0, 6.0, 6.4, 7.5, 8.0]


def nice_round(n: float) -> int:
    """
    Rounds DOWN aggressively to powers of 2 and stack multiples for small numbers
    (e.g., 6 -> 4, 9 -> 8, 13 -> 12, 17 -> 16, 22 -> 20, 35 -> 32, 67 -> 64),
    and clean 2-sig-fig numbers at larger scales (e.g., 2020 -> 2000).
    """
    if n < 1:
        return 1

    if n < 100:
        return max(s for s in SMALL_STEPS if s <= n + 1e-9)

    exp = math.floor(math.log10(n))
    mag = 10 ** exp
    mantissa = n / mag

    chosen = max(s for s in MANTISSA_STEPS if s <= mantissa + 1e-9)
    return int(round(chosen * mag))


def next_step(n: int) -> int:
    """The next price up from n: 4 -> 8, 20 -> 24, 80 -> 100, 2000 -> 2500."""
    if n < SMALL_STEPS[-1]:
        return min(s for s in SMALL_STEPS if s > n)

    exp = max(2, math.floor(math.log10(n)))
    higher = [int(round(s * 10 ** e)) for e in (exp, exp + 1) for s in MANTISSA_STEPS]
    return min(p for p in higher if p > n)


def distinct(prices: list[int]) -> list[int]:
    """Never the same price twice in a row: a price that would repeat or drop
    takes the next step up from the one before it."""
    result = []
    for price in prices:
        if result and price <= result[-1]:
            price = next_step(result[-1])
        result.append(price)
    return result


def ramp(total: float, prices: int, spread: float = 0.5) -> list[float]:
    """total shared over a number of prices that rise in a straight line.
    spread 0.0 = flat [50, 50...], 0.5 = ramps from 0.5x the average to 1.5x."""
    if prices <= 0:
        return []
    if prices == 1:
        return [total]
    avg = total / prices
    return [avg * (1.0 - spread + (2.0 * spread * i / (prices - 1))) for i in range(prices)]


def get_effective_bps(break_ticks: float, blast_mult: float, eta: float) -> float:
    raw_bps = 20.0 if break_ticks <= 0 else 20.0 / (break_ticks + 5)
    return raw_bps * blast_mult * eta


def split(total_items: float, prices: int, spread: float) -> list[int]:
    """Rounded, rising prices that add up to about total_items."""
    if prices <= 1:
        return [nice_round(total_items)]

    raw_steps = ramp(total_items, prices, spread)
    result = [nice_round(x) for x in raw_steps[:-1]]
    remainder = max(1.0, total_items - sum(result))
    result.append(nice_round(remainder))
    return distinct(result)


def calc_price(
        minutes: float,
        break_ticks: float,
        blast_mult: float,
        eta: float,
        base_chance: float,
        drop_range: tuple[int, int],
        fortune_mult: float = 1.0,
        prices: int = 1,
        spread: float = 0.5,  # 0.0 = flat [50, 50...], 0.5 = ramps from 0.5x avg to 1.5x avg
) -> list[int]:
    eff_bps = get_effective_bps(break_ticks, blast_mult, eta)
    blocks_mined = eff_bps * 60 * minutes
    avg_drop = base_chance * (sum(drop_range) / 2) * fortune_mult

    return split(blocks_mined * avg_drop, prices, spread)


def calc_mob_price(
        minutes: float,
        kill_seconds: float,   # one fight, from the first hit to the last
        walk_seconds: float,   # finding and reaching the next mob
        eta: float,
        base_chance: float,
        drop_range: tuple[int, int],
        looting_bonus: float = 0.0,  # Looting shifts the whole range up by this much
        prices: int = 1,
        spread: float = 0.5,
) -> list[int]:
    """The mob version of calc_price: what a player loots in these minutes."""
    kills = minutes * 60 / (kill_seconds + walk_seconds) * eta
    avg_drop = base_chance * (sum(drop_range) / 2 + looting_bonus)

    return split(kills * avg_drop, prices, spread)


# Fortune formula, DataRegistry.getBlockDrops is the same in Java
def fortune(level: int, density: int) -> float:
    effective_level = level - density
    if effective_level < 0:
        return 0.0
    return 1 + 0.75 * effective_level + 0.25 * effective_level ** 2


# --- Examples ---
if __name__ == "__main__":
    # Cobble Prices - Goes from Wooden pick to Stone pick w fortune 1 + eff 2
    alpha = 0.8  # Percent chance they mine a rich block, tends to be more than average (50%)
    effective_drop_avg = lambda rich, normal: alpha * rich + (1 - alpha) * normal

    eta = 0.8
    weight = 0.7
    avg_ticks = weight * 6 + (1 - weight) * 23  # Weighted average break_ticks
    avg_fortune = weight * fortune(1, 0) + (1 - weight) * 1   # Weighted average of fortune

    cobble_steps = calc_price(
        minutes=0.5,
        break_ticks=avg_ticks,
        blast_mult=1.0,
        eta=eta,
        base_chance=1.0,
        drop_range=(effective_drop_avg(1, 1), effective_drop_avg(3, 1)),
        fortune_mult=avg_fortune,
        prices=5,
    )

    print("Cobble Prices: ", cobble_steps) # We should expect around [4, 8, 12, 16, 20]

    # Coal Prices - Eff 2 , Fort 1 -> Eff 4, Fort 2
    eta = 0.9
    weight = 0.8
    avg_ticks = weight * 7.4 + (1 - weight) * 15.6
    avg_fortune = weight * fortune(2, 1) + (1 - weight) * fortune(1, 1)

    coal_steps = calc_price(
        minutes=0.8,
        break_ticks=avg_ticks,
        blast_mult=1.0,
        eta=eta,
        base_chance=1.0,
        drop_range=(effective_drop_avg(5, 1), effective_drop_avg(9, 1)),
        fortune_mult=avg_fortune,
        prices=5,
    )

    print("Coal Prices: ", coal_steps)

    # Feather Prices - chickens in the Copper Vein, 4 s a fight, 3 s to the next one
    feather_steps = calc_mob_price(
        minutes=0.6,
        kill_seconds=4,
        walk_seconds=3,
        eta=0.8,
        base_chance=1.0,
        drop_range=(1, 3),
        prices=4,
    )

    print("Feather Prices: ", feather_steps)
