#!/usr/bin/env python3
"""
Builds a playable skeleton of every region with nothing but console commands, each
region in its own fresh void world like the real server, so dying respawns you at that
region's spawn. Plain 1 block thick platforms, one square per zone with its mines,
spawners and trader, and portal gates linking the regions both ways.
Every name starts with t_ so nothing clashes with the mines and spawners you built.

  python scripts/build_test_world.py                       print the commands
  python scripts/build_test_world.py --send                send them to the running local server
  python scripts/build_test_world.py --regions gormak      only build these, portals to them included
  python scripts/build_test_world.py --teardown --send     delete what it built, to build again

Ids are checked against scripts/data/*.csv first. Go there with /aurora world tp aurora_test.
Run it once per region: a second run fails on names that already exist.
"""
import argparse
import csv
import sqlite3
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "scripts" / "data"
CONSOLE = ROOT / "LocalServer" / "console.in"
DB = ROOT / "LocalServer" / "plugins" / "Aurora" / "mines.db"

FLOOR_Y = 64            # the 1 block thick floor, players stand at 65
ZONE = 32               # each zone is a 28 x 28 square, 32 apart along x
PAD = 28
FLOOR = "smooth_stone"
FRAME = "obsidian"
FENCE = "dark_oak_fence"
GATE = "dark_oak_fence_gate"
CARPET = "red_carpet"
# Zones with only these mobs stay open, the rest are fenced in so their mobs stay put
PASSIVE = {"CHICKEN", "SHEEP", "COW", "PIG", "RABBIT", "HORSE", "FOX", "WOLF", "SNIFFER", "PANDA", "OCELOT"}

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

# region name, its zones, its world
REGIONS = [("origin", ORIGIN, "aurora_test"), ("gormak", GORMAK, "aurora_test_gormak"),
           ("verdant", VERDANT, "aurora_test_verdant")]
LINKS = [("origin", "gormak"), ("gormak", "verdant")]
Z = 0  # every strip runs along x from z 0


def ids(name):
    with open(DATA / f"{name}.csv", newline="", encoding="utf-8") as f:
        return {row["id"] for row in csv.DictReader(f)}


def entities():
    found = {}
    for name in ("mobs_origin", "mobs_gormak", "mobs_verdant"):
        with open(DATA / f"{name}.csv", newline="", encoding="utf-8") as f:
            found.update({row["id"]: row["entity"].upper() for row in csv.DictReader(f)})
    return found


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


def run(world, cmd):
    return f"execute in minecraft:{world} run {cmd}"


# Fence posts with carpet on top, too high to jump, across the strip on both sides of
# every zone with a hostile mob. A fence gate on the walking line lets players through
def fences(world, zones):
    kinds = entities()
    edges = set()
    for i, (_, _, _, spawners) in enumerate(zones):
        if any(kinds.get(mob) not in PASSIVE for mob, _ in spawners):
            edges |= {i * ZONE - 2, (i + 1) * ZONE - 2}
    out = []
    for x in sorted(edges):
        out += [
            run(world, f"fill {x} {FLOOR_Y + 1} {Z - 4} {x} {FLOOR_Y + 1} {Z + PAD + 3} {FENCE}[north=true,south=true]"),
            run(world, f"setblock {x} {FLOOR_Y + 1} {Z + 14} {GATE}[facing=east]"),
            run(world, f"fill {x} {FLOOR_Y + 2} {Z - 4} {x} {FLOOR_Y + 2} {Z + PAD + 3} {CARPET}"),
            run(world, f"setblock {x} {FLOOR_Y + 2} {Z + 14} air"),
        ]
    return out


# Both ends of a region's strip along x
def strip(zones):
    return -10, len(zones) * ZONE + 2


def commands(only):
    out = []
    worlds = {name: world for name, _, world in REGIONS}
    ends = {name: strip(zones) for name, zones, _ in REGIONS}

    for name, zones, world in REGIONS:
        if name not in only:
            continue
        x1, x2 = ends[name]
        out += [
            f"aurora world create {world}",
            # Time stands still and nothing spawns by itself, so only spawner mobs are
            # about. Not /time set: in 26.x it moves the overworld clock other worlds share
            run(world, "gamerule advance_time false"), run(world, "gamerule spawn_mobs false"),
            # Gamerules are per world in 26.x, dying must not drop your gear here either
            run(world, "gamerule keep_inventory true"),
            run(world, f"forceload add {x1} {Z - 4} {x2} {Z + PAD + 4}"),
            run(world, f"fill {x1} {FLOOR_Y} {Z - 4} {x2} {FLOOR_Y} {Z + PAD + 3} {FLOOR}"),
        ]

        for i, (zone, trader, mines, spawners) in enumerate(zones):
            x0 = i * ZONE
            # A coloured stripe across the strip marks where each zone starts
            out.append(run(world, f"fill {x0 - 2} {FLOOR_Y} {Z - 4} {x0 - 2} {FLOOR_Y} {Z + PAD + 3} black_concrete"))
            for m, (mine, mix) in enumerate(mines):
                mx = x0 + 2 + m * 10
                mz = Z + 2
                out.append(f"aurora mine create t_{mine} {mx} {FLOOR_Y + 1} {mz} {mx + 6} {FLOOR_Y + 4} {mz + 6} 101 1200 -w {world}")
                out.append(f"aurora mine setblocks t_{mine} " + " ".join(f"{b} {p}" for b, p in mix.items()))
                out.append(f"aurora mine settp t_{mine} {mx + 3.5} {FLOOR_Y + 1} {mz + 8.5}")
            for s, (mob, limit) in enumerate(spawners):
                sx = x0 + 5 + s * 9
                out.append(f"aurora spawner create t_{mob} {sx} {FLOOR_Y + 1} {Z + 20} {mob} {limit} 3 100 -w {world}")
            # Right beside the zone's last mine, or near the stripe for a zone without one
            tx = x0 + 10 * len(mines) + 0.5 if mines else x0 + 4.5
            out.append(f"aurora trader create {trader} {tx} {FLOOR_Y + 1} {Z + 5.5} -w {world}")

        out += fences(world, zones)

        # Arrive and respawn at the start of the strip, facing along it
        out.append(f"aurora setspawn {x1 + 5.5} {FLOOR_Y + 1} {Z + 14.5} -90 -w {world}")

    # Gates: the end of each strip leads to the start of the next region's, and back.
    # A 5 wide, 4 high opening standing on the floor, framed on the sides and top
    touched = set()

    def gate(world, x):
        touched.add(world)
        out.append(run(world, f"forceload add {x} {Z + 11} {x} {Z + 17}"))
        out.append(run(world, f"fill {x} {FLOOR_Y + 1} {Z + 11} {x} {FLOOR_Y + 5} {Z + 17} {FRAME}"))
        out.append(run(world, f"fill {x} {FLOOR_Y + 1} {Z + 12} {x} {FLOOR_Y + 4} {Z + 16} air"))
        return f"{x} {FLOOR_Y + 1} {Z + 12} {x} {FLOOR_Y + 4} {Z + 16}"

    for a, b in LINKS:
        if a not in only and b not in only:
            continue
        aw, bw = worlds[a], worlds[b]
        _, ax2 = ends[a]
        bx1, _ = ends[b]
        forward = gate(aw, ax2 - 2)
        back = gate(bw, bx1 + 2)
        out.append(f"aurora portal create t_{a}_to_{b} {forward} -w {aw}")
        out.append(f"aurora portal setdest t_{a}_to_{b} {bw} {bx1 + 5.5} {FLOOR_Y + 1} {Z + 14.5} -90 0")
        if (a, b) in LOCKED:
            tag, trader = LOCKED[(a, b)]
            out.append(f"aurora portal settag t_{a}_to_{b} {tag}")
            if a in only:
                out.append(f"aurora trader create {trader} {ax2 - 5.5} {FLOOR_Y + 1} {Z + 19.5} -w {aw}")
        out.append(f"aurora portal create t_{b}_to_{a} {back} -w {bw}")
        out.append(f"aurora portal setdest t_{b}_to_{a} {aw} {ax2 - 5.5} {FLOOR_Y + 1} {Z + 14.5} 90 0")

    # Mines are filled and every placement saved, chunks can unload again
    for name, _, world in REGIONS:
        if name in only or world in touched:
            out.append(run(world, "forceload remove all"))
    return out


# Deletes everything commands() makes for these regions. Trader spots are numbered, so
# they are read from the local database, every spot in a region's world goes
def teardown(only):
    worlds = {world for name, _, world in REGIONS if name in only}
    out = []
    for name, zones, _ in REGIONS:
        if name not in only:
            continue
        for _, _, mines, spawners in zones:
            out += [f"aurora mine delete t_{mine}" for mine, _ in mines]
            out += [f"aurora spawner delete t_{mob}" for mob, _ in spawners]
    for a, b in LINKS:
        if a in only or b in only:
            out += [f"aurora portal delete t_{a}_to_{b}", f"aurora portal delete t_{b}_to_{a}"]
    if DB.exists():
        with sqlite3.connect(DB) as db:
            for world in sorted(worlds):
                spots = db.execute("SELECT id FROM traders WHERE world = ? ORDER BY id", (world,)).fetchall()
                out += [f"aurora trader delete {spot}" for (spot,) in spots]
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__.strip().split("\n")[0])
    ap.add_argument("--send", action="store_true", help="send to the server started by scripts/server.sh")
    ap.add_argument("--teardown", action="store_true", help="delete what the build makes instead")
    ap.add_argument("--regions", default=",".join(name for name, _, _ in REGIONS),
                    help="comma separated, default all: " + ", ".join(name for name, _, _ in REGIONS))
    args = ap.parse_args()

    only = {r.strip() for r in args.regions.split(",") if r.strip()}
    unknown = only - {name for name, _, _ in REGIONS}
    if unknown:
        sys.exit("Unknown region: " + ", ".join(sorted(unknown)))

    check()
    cmds = teardown(only) if args.teardown else commands(only)
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
            time.sleep(4 if cmd.startswith("aurora world") or " run forceload add " in cmd else 0.3)
    print(f"Sent {len(cmds)} commands")


if __name__ == "__main__":
    main()
