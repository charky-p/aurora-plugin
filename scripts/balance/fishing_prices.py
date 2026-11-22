#!/usr/bin/env python3
"""
Fishing, the side quest: what the rod's upgrades cost and what fish sell for.

Fishing has its own data (scripts/data/fishing/: spots.csv, fish.csv, rod.csv) and
its own ladder, apart from the zones. Each spot is priced the way price_zones.py
prices a zone: its minutes are shared over its rod steps, rising, and a step costs
what a player with the rod as it is then catches of each fish in its share of them.

  a cast        = bite wait + REEL_SECONDS, the wait from Lure (FishingManager)
  which fish    = roll 1-100 + LUCK_SCORE x Luck, the fish with the highest score
                  not above it (fish.csv score)
  how many      = the fish's amount x the Fortune curve past its density, none
                  below it (the same curve as blocks, Fortune.java)
  essence       = each tier's share of the spot's essence_per_minute, for a player
                  with the rod maxed there

  python scripts/balance/fishing_prices.py           show the rod and the fish
  python scripts/balance/fishing_prices.py --write   write the fish's first collection
                                                     tier into resources.csv

generate.py writes fishing.yml from it, so run generate.py after changing the data.
"""
import csv
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import prices                    # noqa: E402
from price_calculator import nice_round  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "scripts" / "data"

# Kept in step with FishingManager
LUCK_SCORE = 2                  # score a Luck level adds to the roll
REEL_SECONDS = 2.0              # casting and reeling in, on top of the wait
BITE_MIN_TICKS, BITE_MIN_FLOOR, BITE_MIN_STEP = 100, 20, 6
BITE_MAX_TICKS, BITE_MAX_FLOOR, BITE_MAX_STEP = 400, 60, 25
# Each tier's share of a spot's Essence a minute
ESSENCE_SHARE = {"junk": 0.1, "common": 0.3, "uncommon": 0.3, "rare": 0.3}
JUNK_SHARE_OF_COMMON = 0.25


def rows(name):
    with open(DATA / "fishing" / f"{name}.csv", newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def bite_range(lure):
    """The wait for a bite, in ticks, at this Lure (FishingManager.biteRange)."""
    low = max(BITE_MIN_FLOOR, BITE_MIN_TICKS - BITE_MIN_STEP * lure)
    high = max(BITE_MAX_FLOOR, BITE_MAX_TICKS - BITE_MAX_STEP * lure)
    return low, high


def cast_seconds(lure):
    low, high = bite_range(lure)
    return (low + high) / 2 / 20 + REEL_SECONDS


def chances(fish, luck):
    """fish id -> chance a cast brings it up, before Fortune."""
    ordered = sorted(fish, key=lambda f: int(f["score"]))
    out = {}
    for i, f in enumerate(ordered):
        def at_least(score):
            # Rolls r in 1..100 with r + bonus >= score
            need = score - LUCK_SCORE * luck
            return min(1.0, max(0.0, (101 - max(1, need)) / 100))
        upper = at_least(int(ordered[i + 1]["score"])) if i + 1 < len(ordered) else 0.0
        out[f["id"]] = at_least(int(f["score"])) - upper
    return out


def average(amount):
    low, _, high = amount.partition("-")
    return (int(low) + int(high or low)) / 2


def per_second(fish, state):
    """fish id -> how many a second the rod brings up."""
    chance = chances(fish, state["luck"])
    cast = cast_seconds(state["lure"])
    out = {}
    for f in fish:
        past = state["fortune"] - int(f["density"])
        mult = prices.fortune_multiplier(past, 0) if past >= 0 else 0.0
        out[f["id"]] = chance[f["id"]] * average(f["amount"]) * mult / cast
    return out


def build():
    """[(spot row, its fish, priced rod steps, fish id -> essence)] in spot order."""
    spots = sorted(rows("spots"), key=lambda s: int(s["order"]))
    fish = rows("fish")
    rod = rows("rod")
    state = {"fortune": 0, "luck": 0, "lure": 0}
    result = []
    for spot in spots:
        here = [f for f in fish if f["spot"] == spot["id"]]
        steps = sorted((r for r in rod if r["spot"] == spot["id"]), key=lambda r: int(r["order"]))
        times = prices.step_seconds(float(spot["minutes"]) * 60, len(steps), prices.SPREAD)
        priced = []
        for step, seconds in zip(steps, times):
            rate = per_second(here, state)
            cost = {}
            for part in step["cost"].split("|"):
                resource, _, share = part.partition(":")
                amount = seconds * float(share or 1) * rate.get(resource, 0.0)
                cost[resource] = max(1, nice_round(amount))
            state[step["stat"]] = int(step["level"])
            priced.append({"stat": step["stat"], "level": int(step["level"]), "seconds": seconds, "cost": cost})
        # Essence: what the rod maxed here brings up a minute, each tier its share
        rate = per_second(here, state)
        essence = {}
        for f in here:
            per_minute = rate[f["id"]] * 60
            value = float(spot["essence_per_minute"]) * ESSENCE_SHARE[f["tier"]] / per_minute if per_minute else 0.0
            essence[f["id"]] = float(f"{value:.3g}")
        # Junk is never worth more than a quarter of the spot's common fish, however
        # rarely it bites once Luck is high
        common = next((essence[f["id"]] for f in here if f["tier"] == "common"), 0.0)
        for f in here:
            if f["tier"] == "junk":
                essence[f["id"]] = float(f"{common * JUNK_SHARE_OF_COMMON:.3g}")
        result.append((spot, here, priced, essence))
    return result


def totals(built):
    """fish id -> what its spot's rod costs of it in all, its first collection tier."""
    out = {}
    for _, _, priced, _ in built:
        for step in priced:
            for resource, amount in step["cost"].items():
                out[resource] = out.get(resource, 0) + amount
    return out


def main(write):
    built = build()
    for spot, _, priced, essence in built:
        print(f"\n{spot['id']}  ({spot['minutes']} min)")
        for step in priced:
            cost = ", ".join(f"{a} {r}" for r, a in step["cost"].items())
            print(f"  {step['stat']:<8}{step['level']:>3}  {step['seconds']:6.1f}s  {cost}")
        print("  essence: " + ", ".join(f"{k} {v}" for k, v in essence.items()))
    if not write:
        return
    tiers = totals(built)
    path = DATA / "items" / "resources.csv"
    with open(path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        fields = reader.fieldnames
        res = list(reader)
    for row in res:
        if row["id"] in tiers:
            row["collection"] = str(prices.nice(tiers[row["id"]]))
    with open(path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fields, lineterminator="\n")
        writer.writeheader()
        writer.writerows(res)
    print("\nWrote the fish's collection tiers to resources.csv, now run python scripts/generate.py")


if __name__ == "__main__":
    main("--write" in sys.argv)
