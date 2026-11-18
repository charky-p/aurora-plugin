#!/usr/bin/env python3
"""
Sets what the Essence Trader pays for one of every resource, the essence column of
resources.csv. The trader is for players who have maxed every zone, so a price is
worked out from how fast such a player gets the resource:

  essence for one = essence a minute / how many a minute an endgame player farms

  essence a minute = PER_MINUTE x (1 + DENSITY_BONUS x density)

so every resource pays about the same for the time it takes, and the denser the
blocks or mobs it comes from, the more. A resource nothing farms keeps 0, and the
trader does not buy it.

  python scripts/balance/essence_values.py           show what would change
  python scripts/balance/essence_values.py --write   write resources.csv

Run it again after prices or densities change, then python scripts/generate.py.
"""
import csv
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import balance_sim as sim   # noqa: E402
import price_zones         # noqa: E402

RESOURCES = sim.data_file("resources")
# Essence a minute of farming at density 0, and how much each level of density adds
PER_MINUTE = 20.0
DENSITY_BONUS = 0.1


def endgame_state():
    """What a player holds once every zone is bought: the last zone's final state."""
    *_, (_, priced) = price_zones.price_all()
    return priced[-1]["state"]


def values():
    zones = sorted(price_zones.rows("zones"), key=lambda z: int(z["order"]))
    where = price_zones.sources(zones)
    state = endgame_state()
    found = {}
    for resource, (kind, source) in where.items():
        if kind == "mine":
            per_second = price_zones.mine_income(state, source)
            density = max(sim.BLOCKS[b]["density"] for b in sim.MINES[source])
        else:
            best = max(source, key=lambda m: price_zones.mob_income(state, m, resource))
            per_second = price_zones.mob_income(state, best, resource)
            density = sim.MOBS[best]["density"]
        if per_second <= 0 or math.isinf(per_second):
            continue
        per_minute = PER_MINUTE * (1 + DENSITY_BONUS * density)
        value = per_minute / (per_second * 60)
        found[resource] = float(f"{value:.4g}")
    return found


def main(write):
    found = values()
    with open(RESOURCES, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        header = reader.fieldnames
        rows = list(reader)
    if "essence" not in header:
        header = header + ["essence"]
    changed = 0
    for row in rows:
        value = found.get(row["id"], 0)
        new = f"{value:g}" if value else ""
        if (row.get("essence") or "") != new:
            print(f"  {row['id']:<24} {row.get('essence') or '-':>10} -> {new or '-'}")
            row["essence"] = new
            changed += 1
    if not write:
        print(f"{changed} would change, --write to write them")
        return
    with open(RESOURCES, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=header, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)
    print("Wrote resources.csv, now run python scripts/generate.py")


if __name__ == "__main__":
    main("--write" in sys.argv)
