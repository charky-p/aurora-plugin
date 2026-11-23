#!/usr/bin/env python3
"""
The gauntlet's numbers: how each floor scales and what a gear level costs.

The gauntlet has its own data (scripts/data/gauntlet/) and its own ladder, apart from
the zones. One idea holds it together: a player who has kept up has their gear at
about half the floor they are on (EXPECTED_LEVEL), and the floor is tuned for that.

  weapon        = STAT_PER_LEVEL Sharpness (or Power) a level, so every upgrade is a
                  hit you feel: 10 + the Sharpness brackets (CombatFormulas.sharpnessBonus)
  armour        = its level in Defence a piece and STAT_PER_LEVEL Protection a level;
                  the shield's Deflection keeps up with the floors' attack
  floor health  = every mob's health times the hit of the expected weapon over 10,
                  so a mob takes as many hits on floor 40 as on floor 1
  floor attack  = ATTACK_PER_FLOOR levels a floor, a little ahead of the expected
                  armour's Defence (the set adds up to twice the floor), so hits creep
                  up unless armour is ahead
  kill          = Shards every mob killed pays, KILL_BASE + KILL_PER_FLOOR x floor, at
                  least 1; a boss (the one mob of a boss floor) pays BOSS_KILLS times it
  shards        = the bonus for clearing a floor, SHARDS_BASE + SHARDS_PER_FLOOR x floor
  cores         = paid on each boss floor (every 10th), every clear: its boss's
                  number, floor / 10
  gear level L  = costs SHARD_BASE x SHARD_GROWTH^L shards, rounded; every 5th level
                  is a new material; from the 10th on it also costs cores (CORES)

Past the last floor in floors.csv the plugin repeats it, health times
ENDLESS_HEALTH and attack + ATTACK_PER_FLOOR a floor (GauntletData).

  python scripts/balance/gauntlet_balance.py           show floors and gear levels
  python scripts/balance/gauntlet_balance.py --write   fill floors.csv's numbers and
                                                       write gear_levels.csv

The mobs column of floors.csv is left as it is. generate.py writes gauntlet.yml.
"""
import csv
import math
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "scripts" / "data" / "gauntlet"

EXPECTED_LEVEL = 0.5            # gear level per floor reached
STAT_PER_LEVEL = 2              # Sharpness, Power and Protection a gear level
ATTACK_PER_FLOOR = 2.2          # mob attack levels added a floor (GauntletData)
SHARDS_BASE, SHARDS_PER_FLOOR = 1.0, 0.6
KILL_BASE, KILL_PER_FLOOR = 1.0, 0.08   # KILL_PER_FLOOR is also in GauntletData
BOSS_KILLS = 5                  # GauntletData.BOSS_KILLS
SHARD_BASE, SHARD_GROWTH = 10, 1.17
MAX_LEVEL = 50
LEVELS_PER_MATERIAL = 5
CORES = {10: 1, 15: 1, 20: 2, 25: 2, 30: 3, 35: 3, 40: 4, 45: 4, 50: 5}

# CombatFormulas.sharpnessBonus
BRACKET_SIZE = 5
BRACKET_BONUS = [1, 3, 5, 10, 15, 20, 25, 30, 35, 40]


def sharpness_bonus(level):
    total, remaining, bracket = 0.0, level, 0
    while remaining > 0:
        levels = min(remaining, BRACKET_SIZE)
        bonus = BRACKET_BONUS[bracket] if bracket < len(BRACKET_BONUS) else BRACKET_BONUS[-1] + (bracket - len(BRACKET_BONUS) + 1) * 5
        total += levels * bonus
        remaining -= levels
        bracket += 1
    return total


def hit(level):
    """A weapon's hit at a gear level, interpolated between whole levels."""
    low = math.floor(level)
    a, b = 10 + sharpness_bonus(low), 10 + sharpness_bonus(low + 1)
    return a + (b - a) * (level - low)


def floor_numbers(floor):
    health = round(max(1.0, hit(floor * EXPECTED_LEVEL * STAT_PER_LEVEL) / 10), 2)
    attack = round(floor * ATTACK_PER_FLOOR)
    shards = round(SHARDS_BASE + SHARDS_PER_FLOOR * floor)
    kill = max(1, round(KILL_BASE + KILL_PER_FLOOR * floor))
    cores = floor // 10 if floor % 10 == 0 else 0
    return health, attack, shards, kill, cores


def floor_pay(row):
    """Everything a floor pays when cleared: its kills, a boss's at BOSS_KILLS, and the bonus."""
    counts = [int(part.split(":")[1]) for part in row["mobs"].split("|")]
    boss = int(row["cores"]) > 0 and 1 in counts
    kills = sum(counts) - (1 if boss else 0) + (BOSS_KILLS if boss else 0)
    return kills * int(row["kill"]) + int(row["shards"])


def nice(value):
    if value < 100:
        return int(5 * round(value / 5)) or 5
    if value < 1000:
        return int(10 * round(value / 10))
    return int(50 * round(value / 50))


def level_rows():
    rows = []
    for level in range(0, MAX_LEVEL + 1):
        shards = 0 if level == 0 else nice(SHARD_BASE * SHARD_GROWTH ** level)
        rows.append({
            "level": level,
            "shards": shards,
            "cores": CORES.get(level, 0),
            "material": level // LEVELS_PER_MATERIAL,
            "melee": level * STAT_PER_LEVEL,
            "ranged": level * STAT_PER_LEVEL,
            # Per piece, the set of four adds up to four times this
            "defence": level,
            "protection": level * STAT_PER_LEVEL,
            # A raised shield covers 11 + 4/3 attack a level (CombatFormulas), kept up
            # with the attack of the floor where the shield is expected at this level
            "deflection": max(0, round((ATTACK_PER_FLOOR * level / EXPECTED_LEVEL - 11) * 0.75 + 2)),
        })
    return rows


def read(name):
    with open(DATA / name, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def write(name, fields, rows):
    with open(DATA / name, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fields, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def main():
    floors = read("floors.csv")
    for row in floors:
        health, attack, shards, kill, cores = floor_numbers(int(row["floor"]))
        row.update(health=health, attack=attack, shards=shards, kill=kill, cores=cores)
    levels = level_rows()

    print(f"{'floor':>5} {'health x':>8} {'attack':>6} {'kill':>4} {'bonus':>5} {'pays':>5} {'run':>6} {'cores':>5}  mobs")
    run = 0
    for row in floors:
        pay = floor_pay(row)
        run += pay
        print(f"{row['floor']:>5} {row['health']:>8} {row['attack']:>6} {row['kill']:>4} {row['shards']:>5} {pay:>5} {run:>6} {row['cores']:>5}  {row['mobs']}")
    print()
    total = 0
    print(f"{'level':>5} {'shards':>6} {'cores':>5} {'total':>7}")
    for row in levels:
        total += row["shards"]
        print(f"{row['level']:>5} {row['shards']:>6} {row['cores']:>5} {total:>7}")

    if "--write" in sys.argv:
        write("floors.csv", ["floor", "mobs", "health", "attack", "kill", "shards", "cores"], floors)
        write("gear_levels.csv", list(levels[0].keys()), levels)
        print("\nWrote floors.csv and gear_levels.csv")


if __name__ == "__main__":
    main()
