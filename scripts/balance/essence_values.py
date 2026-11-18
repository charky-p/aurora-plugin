#!/usr/bin/env python3
"""
Sets what the Essence Trader pays, the essence column of resources.csv. Essence is
only farmed at the very end: the trader buys the last released zone's resources
alone (the released zone furthest along in zones.csv, its region marked released in
regions.csv), from players who have maxed every released zone, priced from how fast
such a player gets them. Run it again when a region is released, and move the
trader to the new last zone:

  essence for one = PER_MINUTE / how many a minute an endgame player farms

so farming the last zone pays about PER_MINUTE Essence a minute. Every other
resource is 0, and the trader does not buy it.

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
# Essence a minute of farming the last zone
PER_MINUTE = 100.0


def endgame_state():
    """What a player holds once every zone is bought: the last zone's final state."""
    *_, (_, priced) = price_zones.price_all()
    return priced[-1]["state"]


def last_released(zones):
    """The released zone furthest along. Every zone when nothing is marked released."""
    released = {r["id"] for r in price_zones.rows("regions")
                if (r.get("released") or "").strip().lower() in ("yes", "true", "1")}
    out = [z for z in zones if z["region"] in released] or zones
    return out[-1]


def values():
    zones = sorted(price_zones.rows("zones"), key=lambda z: int(z["order"]))
    last = last_released(zones)
    print(f"Last released zone: {last['id']} ({last['region']})")
    # Only what the last released zone gives
    where = price_zones.sources([last])
    state = endgame_state()
    found = {}
    for resource, (kind, source) in where.items():
        if kind == "mine":
            per_second = price_zones.mine_income(state, source)
        else:
            per_second = max(price_zones.mob_income(state, m, resource) for m in source)
        if per_second <= 0 or math.isinf(per_second):
            continue
        value = PER_MINUTE / (per_second * 60)
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
        # Plain decimals, a YAML loader may not read 3e-05
        new = f"{value:.10f}".rstrip("0").rstrip(".") if value else ""
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
