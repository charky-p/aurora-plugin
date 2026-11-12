#!/usr/bin/env python3
"""
Builds a playable skeleton of every region in a fresh void world, aurora_test, with
nothing but console commands. Plain 1 block thick platforms, one square per zone with
its mines, spawners and trader, and portal gates linking the regions both ways.
Every name starts with t_ so nothing clashes with the mines and spawners you built.

  python scripts/build_test_world.py              print the commands
  python scripts/build_test_world.py --send       send them to the running local server

Ids are checked against scripts/data/*.csv first. Go there with /aurora world tp aurora_test.
Run it once: a second run fails on names that already exist.
"""
import argparse
import csv
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "scripts" / "data"
CONSOLE = ROOT / "LocalServer" / "console.in"

WORLD = "aurora_test"
DIM = f"minecraft:{WORLD}"
FLOOR_Y = 64            # the 1 block thick floor, players stand at 65
ZONE = 32               # each zone is a 28 x 28 square, 32 apart along x
PAD = 28
FLOOR = "smooth_stone"
FRAME = "obsidian"

# zone: trader, [(mine, {block: %})], [(mob, limit)]
ORIGIN = [
    ("starter", "starter_trader", [], []),
    ("cobble", "cobble_trader", [("cobble", {"cobblestone": 60, "stone": 40})], []),
    ("coal", "coal_trader", [("coal", {"coal_ore": 60, "coal_block": 40})], []),
    ("copper", "copper_trader", [("copper", {"copper_ore": 60, "copper_block": 40})], [("chicken", 6)]),
    ("iron", "iron_trader", [("iron", {"iron_ore": 60, "iron_block": 40})], [("sheep", 5)]),
    ("gold", "gold_trader", [("gold", {"gold_ore": 60, "gold_block": 40})], [("cow", 5)]),
]
GORMAK = [
    ("city", "city_trader", [("brick", {"carved_brick": 90, "cracked_brick": 10})],
     [("hollowed_serf", 3), ("scavenger_husk", 3)]),
    ("dunes", "dunes_trader", [("sun", {"sunbaked_sand": 80, "cut_sunbaked_sand": 20})],
     [("dune_scout", 3), ("sand_stalker", 3)]),
    ("canyons", "canyons_trader", [("crimson", {"crimson_sand": 80, "cut_crimson_sand": 20})],
     [("scarab_zealot", 3), ("hollowed_legionnaire", 3)]),
    ("temple", "temple_trader", [("raw_gold", {"raw_gold_block": 20, "gilded_stone": 80})],
     [("tomb_guard", 3), ("vizier_mage", 3)]),
    ("tomb", "tomb_trader", [], [("royal_myrmidon", 3), ("pharaoh", 3), ("gormak_eternal", 1)]),
]


def grove(zone, log, metal, mob):
    return (zone, f"{zone}_trader",
            [(f"{zone}_log", {log: 60, f"{zone}_wood": 25, f"stripped_{log}": 15}),
             (f"{zone}_metal", {f"{metal}_ore": 60, f"{metal}_rich": 40})],
            [(mob, 4)])


VERDANT = [
    grove("oak", "oak_log", "tin", "grove_horse"),
    grove("birch", "birch_log", "bronze", "hollow_fox"),
    grove("spruce", "spruce_log", "lead", "ridge_wolf"),
    grove("darkoak", "dark_oak_log", "silver", "thicket_sniffer"),
    ("treehouse", "treehouse_trader", [], [("tree_zombee", 4), ("treant", 1)]),
]

# Gates that need a tag, with the trader beside them that sells its key
LOCKED = {("origin", "gormak"): ("gormak", "gate_trader")}

# region name, its zones, z of the strip
REGIONS = [("origin", ORIGIN, 0), ("gormak", GORMAK, 200), ("verdant", VERDANT, 400)]


def ids(name):
    with open(DATA / f"{name}.csv", newline="", encoding="utf-8") as f:
        return {row["id"] for row in csv.DictReader(f)}


def check():
    traders, blocks = ids("traders"), ids("blocks")
    keys = {}
    with open(DATA / "resources_special.csv", newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            keys[row["unlocks"]] = row["id"]
    mobs = ids("mobs_origin") | ids("mobs_gormak") | ids("mobs_verdant")
    missing = []
    for _, zones, _ in REGIONS:
        for zone, trader, mines, spawners in zones:
            missing += [f"trader {trader}" for _ in [0] if trader not in traders]
            missing += [f"block {b}" for _, mix in mines for b in mix if b not in blocks]
            missing += [f"mob {m}" for m, _ in spawners if m not in mobs]
    for tag, trader in LOCKED.values():
        missing += [f"trader {trader}" for _ in [0] if trader not in traders]
        missing += [f"key for tag {tag}" for _ in [0] if tag not in keys]
    if missing:
        sys.exit("Not in the CSVs: " + ", ".join(missing))


def run(cmd):
    return f"execute in {DIM} run {cmd}"


def commands():
    out = [
        f"aurora world create {WORLD}",
        # Time stands still and nothing spawns by itself, so only spawner mobs are
        # about. Not /time set: in 26.x it moves the overworld clock other worlds share
        run("gamerule advance_time false"), run("gamerule spawn_mobs false"),
    ]

    strip = {}
    for name, zones, z in REGIONS:
        x1, x2 = -10, len(zones) * ZONE + 2
        strip[name] = (x1, x2, z)
        out.append(run(f"forceload add {x1} {z - 4} {x2} {z + PAD + 4}"))
        out.append(run(f"fill {x1} {FLOOR_Y} {z - 4} {x2} {FLOOR_Y} {z + PAD + 3} {FLOOR}"))

        for i, (zone, trader, mines, spawners) in enumerate(zones):
            x0 = i * ZONE
            # A coloured stripe across the strip marks where each zone starts
            out.append(run(f"fill {x0 - 2} {FLOOR_Y} {z - 4} {x0 - 2} {FLOOR_Y} {z + PAD + 3} black_concrete"))
            for m, (mine, mix) in enumerate(mines):
                mx = x0 + 2 + m * 10
                mz = z + 2
                out.append(f"aurora mine create t_{mine} {mx} {FLOOR_Y + 1} {mz} {mx + 6} {FLOOR_Y + 4} {mz + 6} 101 1200 -w {WORLD}")
                out.append(f"aurora mine setblocks t_{mine} " + " ".join(f"{b} {p}" for b, p in mix.items()))
                out.append(f"aurora mine settp t_{mine} {mx + 3.5} {FLOOR_Y + 1} {mz + 8.5}")
            for s, (mob, limit) in enumerate(spawners):
                sx = x0 + 5 + s * 9
                out.append(f"aurora spawner create t_{mob} {sx} {FLOOR_Y + 1} {z + 20} {mob} {limit} 3 100 -w {WORLD}")
            out.append(f"aurora trader create {trader} {x0 + 24.5} {FLOOR_Y + 1} {z + 4.5} -w {WORLD}")

    # Gates: the end of each strip leads to the start of the next, and back
    # A 5 wide, 4 high opening standing on the floor, framed on the sides and top
    def gate(x, z):
        out.append(run(f"fill {x} {FLOOR_Y + 1} {z + 11} {x} {FLOOR_Y + 5} {z + 17} {FRAME}"))
        out.append(run(f"fill {x} {FLOOR_Y + 1} {z + 12} {x} {FLOOR_Y + 4} {z + 16} air"))
        return f"{x} {FLOOR_Y + 1} {z + 12} {x} {FLOOR_Y + 4} {z + 16}"

    for (a, b) in [("origin", "gormak"), ("gormak", "verdant")]:
        _, ax2, az = strip[a]
        bx1, _, bz = strip[b]
        forward = gate(ax2 - 2, az)
        back = gate(bx1 + 2, bz)
        out.append(f"aurora portal create t_{a}_to_{b} {forward} -w {WORLD}")
        out.append(f"aurora portal setdest t_{a}_to_{b} {WORLD} {bx1 + 5.5} {FLOOR_Y + 1} {bz + 14.5} -90 0")
        if (a, b) in LOCKED:
            tag, trader = LOCKED[(a, b)]
            out.append(f"aurora portal settag t_{a}_to_{b} {tag}")
            out.append(f"aurora trader create {trader} {ax2 - 5.5} {FLOOR_Y + 1} {az + 19.5} -w {WORLD}")
        out.append(f"aurora portal create t_{b}_to_{a} {back} -w {WORLD}")
        out.append(f"aurora portal setdest t_{b}_to_{a} {WORLD} {ax2 - 5.5} {FLOOR_Y + 1} {az + 14.5} 90 0")

    ox1, _, oz = strip["origin"]
    out.append(f"aurora setspawn {ox1 + 4.5} {FLOOR_Y + 1} {oz + 14.5} -90 -w {WORLD}")
    # Mines are filled and every placement saved, chunks can unload again
    for name, (x1, x2, z) in strip.items():
        out.append(run(f"forceload remove {x1} {z - 4} {x2} {z + PAD + 4}"))
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__.strip().split("\n")[0])
    ap.add_argument("--send", action="store_true", help="send to the server started by scripts/server.sh")
    args = ap.parse_args()

    check()
    cmds = commands()
    if not args.send:
        print("\n".join(cmds))
        return

    if not CONSOLE.exists():
        sys.exit("No LocalServer/console.in, start the server with scripts/server.sh start")
    with open(CONSOLE, "a", encoding="utf-8") as console:
        for cmd in cmds:
            console.write(cmd + "\n")
            console.flush()
            # World creation and forceloading take a moment, the rest are quick
            time.sleep(4 if cmd.startswith(("aurora world", "execute in " + DIM + " run forceload add")) else 0.3)
    print(f"Sent {len(cmds)} commands")


if __name__ == "__main__":
    main()
