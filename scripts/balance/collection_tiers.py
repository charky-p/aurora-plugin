#!/usr/bin/env python3
"""
Sets the first collection tier of every resource the upgrade shops use, so blocks
and mob drops are measured the same way:

  tier I = what the zone's upgrades cost of that resource in total

A player who maxes a zone out has just reached tier I of everything it uses,
whether it is mined by the thousand or looted by the dozen. The later tiers are
multiples of the first (CollectionTiers.java). A resource no upgrade costs keeps
the number it has in resources.csv.

  python scripts/balance/collection_tiers.py           show what would change
  python scripts/balance/collection_tiers.py --write   write resources.csv

Run it again after prices change, then python scripts/generate.py.
"""
import csv
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import price_zones   # noqa: E402
import prices        # noqa: E402

RESOURCES = price_zones.sim.data_file("resources")


def zone_totals():
    """resource -> the most any one zone's upgrades cost of it."""
    totals = {}
    for _, priced in price_zones.price_all():
        zone = {}
        for row in priced:
            for resource, amount in row["cost"].items():
                zone[resource] = zone.get(resource, 0) + amount
        for resource, amount in zone.items():
            totals[resource] = max(totals.get(resource, 0), amount)
    return totals


def main(write):
    totals = zone_totals()
    with open(RESOURCES, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        fields = reader.fieldnames
        rows = list(reader)

    for row in rows:
        if row["id"] not in totals:
            continue
        base = prices.nice(totals[row["id"]])
        if str(base) != row["collection"]:
            print(f"  {row['id']:<18}{row['collection']:>6} -> {base}")
            row["collection"] = str(base)

    if write:
        with open(RESOURCES, "w", newline="", encoding="utf-8") as f:
            writer = csv.DictWriter(f, fieldnames=fields, lineterminator="\n")
            writer.writeheader()
            writer.writerows(rows)
        print("Wrote resources.csv, now run python scripts/generate.py")


if __name__ == "__main__":
    main("--write" in sys.argv)
