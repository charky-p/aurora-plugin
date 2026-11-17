# price_calculator.py
# TODO: Note that it does not calculate mob yet
# This is a heuristic, eta -> 1 means full efficiency, however it can be used to scale
# to be more difficult, that is, eta -> around 10 at some point

import math

def nice_round(n: float) -> int:
    """
    Rounds DOWN aggressively to powers of 2 and stack multiples for small numbers
    (e.g., 6 -> 4, 9 -> 8, 13 -> 12, 17 -> 16, 22 -> 20, 35 -> 32, 67 -> 64),
    and clean 2-sig-fig numbers at larger scales (e.g., 2020 -> 2000).
    """
    if n < 1:
        return 1

    # Explicit Minecraft-friendly checkpoints below 100
    if n < 100:
        small_steps = [1, 2, 3, 4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 80]
        return max(s for s in small_steps if s <= n + 1e-9)

    exp = math.floor(math.log10(n))
    mag = 10 ** exp
    mantissa = n / mag

    # TODO: make previous step never equal to current step
    steps = [1.0, 1.2, 1.5, 1.6, 2.0, 2.5, 3.0, 3.2, 4.0, 4.8, 5.0, 6.0, 6.4, 7.5, 8.0]
    chosen = max(s for s in steps if s <= mantissa + 1e-9)
    return int(round(chosen * mag))

def get_effective_bps(break_ticks: float, blast_mult: float, eta: float) -> float:
    raw_bps = 20.0 if break_ticks <= 0 else 20.0 / (break_ticks + 5)
    return raw_bps * blast_mult * eta

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

    total_items = blocks_mined * avg_drop

    if prices <= 1:
        return [nice_round(total_items)]

    # Build a linear ramp whose unrounded sum equals total_items
    avg = total_items / prices
    raw_steps = [
        avg * (1.0 - spread + (2.0 * spread * i / (prices - 1)))
        for i in range(prices)
    ]

    result = [nice_round(x) for x in raw_steps[:-1]]
    remainder = max(1.0, total_items - sum(result))
    result.append(nice_round(remainder))
    return result

# Fortune formula
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