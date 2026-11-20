#!/usr/bin/env python3
"""
Builds a playable skeleton of every region with nothing but console commands. A
region is a strip of zones in a void world. Regions of the same world follow each
other along the strip in regions.csv order, and dying respawns you at the world's
spawn. Plain 1 block thick platforms, one square per zone with its mines, spawners
and trader, and portal gates linking the worlds both ways. A boss, any mob a key
summons, gets a walled arena of its own off the right side of its zone.

A region with a rise starts that many blocks higher than the one before it, a cliff
only the grapple climbs (Khorvath) or only the elytra reaches (Elderwood Heart). A
region with a gap starts that many blocks of void further on, crossed with the
elytra (the Duat, the Ironbound Quarry).

Everything is read from scripts/data: regions.csv for the strips, zones.csv for the
zones, mines.csv for the mines, traders.csv for who stands where. Every name starts
with t_ so nothing clashes with the mines and spawners you built. Build regions of
one world in order: --regions verdant alone needs origin_isles built.

  python scripts/build_test_world.py                       print the commands
  python scripts/build_test_world.py --send                send them to the running local server
  python scripts/build_test_world.py --regions gormak      only build these, portals to them included
  python scripts/build_test_world.py --teardown --send     delete what it built, to build again

Go there with /aurora world tp aurora_test. Run it once per region: a second run
fails on names that already exist.
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

FLOOR_Y = 64            # the 1 block thick floor of a region with no rise, players stand at 65
ZONE = 32               # each zone is a 28 x 28 square, 32 apart along x
PAD = 28
MINE_W, MINE_H, MINE_GAP = 9, 5, 2   # every mine 9 x 9 and 5 deep, 2 apart
FLOOR = "smooth_stone"
CLIFF = "stone"
FRAME = "obsidian"
FENCE = "dark_oak_fence"
GATE = "dark_oak_fence_gate"
CARPET = "red_carpet"
WALL = "cut_sandstone"
WALL_H = 4              # too high to be thrown over by a boss
ARENA_DEPTH = 28        # how far a boss arena reaches out from the strip
# Zones with only these mobs stay open, the rest are fenced in so their mobs stay put
PASSIVE = {"CHICKEN", "SHEEP", "COW", "PIG", "RABBIT", "HORSE", "FOX", "WOLF", "SNIFFER", "PANDA", "OCELOT",
           "FROG", "GOAT", "SLIME", "ALLAY"}
# The test world each world of regions.csv is built in
TEST_WORLD = {"overworld": "aurora_test", "duneworld": "aurora_test_gormak",
              "elderworld": "aurora_test_elderwood", "skyworld": "aurora_test_sky"}
# Bosses a key summons, and the zone whose arena they rise in
BOSS_ZONE = {"gormak_eternal": "tomb", "khorvath": "sanctuary", "arachne": "arachne_cave",
             "sol": "throne", "elder_treant": "the_crown", "sky_king": "throne_room"}
# Traders with no zone of their own, by the zone they stand in
EXTRA_TRADERS = {"cobble": ["food_trader"], "golems_rest": ["core_trader", "essence_trader"]}
Z = 0  # every strip runs along x from z 0
SAFE_Z = Z - 12  # far edge of the safe platforms beside hostile zones


# The CSVs are sorted into folders under data/, a file is found by its name alone
def data_file(name):
    return next(DATA.rglob(f"{name}.csv"))


def rows(name):
    with open(data_file(name), newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def mob_rows():
    return {r["id"]: r for f in sorted(DATA.rglob("mobs_*.csv")) for r in rows(f.stem)}


def load():
    """[(region id, [(zone, trader, [(mine, {block: %})], [(mob, limit, delay)])], test world, region row)]."""
    mines = {r["id"]: {b.split(":")[0]: int(b.split(":")[1]) for b in r["blocks"].split("|")} for r in rows("mines")}
    every = {r["id"] for r in rows("traders")}
    traders = {r["gui"][len("upgrades:"):]: r["id"] for r in rows("traders") if r["gui"].startswith("upgrades:")}
    # A trader that opens a shop menu first, like the Royal Tomb's, goes by <zone>_trader
    traders.update({z["id"]: f"{z['id']}_trader" for z in rows("zones")
                    if z["id"] not in traders and f"{z['id']}_trader" in every})
    mobs = mob_rows()
    zones = sorted(rows("zones"), key=lambda z: int(z["order"]))
    out = []
    for region in rows("regions"):
        strip_ = []
        if region["id"] == "origin_isles":
            strip_.append(("starter", "starter_trader", [], []))
        for z in (z for z in zones if z["region"] == region["id"]):
            spawners = []
            for mob in filter(None, z["mob"].split("|")):
                entity = mobs[mob]["entity"].upper()
                respawn = float(mobs[mob].get("respawn") or 0)
                limit = 5 if entity in PASSIVE else (1 if respawn else 3)
                spawners.append((mob, limit, int(respawn * 20) if respawn else 100))
            spawners += [(boss, 1, 100) for boss, zone in BOSS_ZONE.items() if zone == z["id"]]
            strip_.append((z["id"], traders.get(z["id"]), [(m, mines[m]) for m in filter(None, z["mine"].split("|"))],
                           spawners))
        out.append((region["id"], strip_, TEST_WORLD[region["world"]], region))
    return out


REGIONS = load()
# A mob in more than one zone gets a spawner per zone, named after the zone too
SHARED = {m for m in (mob for _, zs, _, _ in REGIONS for _, _, _, sp in zs for mob, _, _ in sp)
          if sum(1 for _, zs, _, _ in REGIONS for _, _, _, sp in zs for mob, _, _ in sp if mob == m) > 1}


def spawner_name(zone, mob):
    return f"t_{zone}_{mob}" if mob in SHARED else f"t_{mob}"


def check():
    traders, blocks, mobs = {r["id"] for r in rows("traders")}, {r["id"] for r in rows("blocks")}, mob_rows()
    keys = {r["unlocks"] for r in rows("resources_special") if r.get("unlocks")}
    missing = []
    for _, zones, _, region in REGIONS:
        for zone, trader, mines, spawners in zones:
            if not trader:
                missing.append(f"trader for zone {zone}")
            elif trader not in traders:
                missing.append(f"trader {trader}")
            missing += [f"block {b}" for _, mix in mines for b in mix if b not in blocks]
            missing += [f"mob {m}" for m, _, _ in spawners if m not in mobs]
        if region["tag"] and region["tag"] not in keys:
            missing.append(f"key for tag {region['tag']}")
        if region["gate_trader"] and region["gate_trader"] not in traders:
            missing.append(f"trader {region['gate_trader']}")
    if missing:
        sys.exit("Not in the CSVs: " + ", ".join(missing))


def run(world, cmd):
    return f"execute in minecraft:{world} run {cmd}"


def hostile(spawners, kinds):
    return any(kinds.get(mob) not in PASSIVE for mob, _, _ in spawners)


def entities():
    return {mid: r["entity"].upper() for mid, r in mob_rows().items()}


# Fence posts with carpet on top, too high to jump, across the strip on both sides of
# every zone with a hostile mob, running on past the safe platform. A fence gate on
# the walking line lets players through
def fences(world, zones, offset, y):
    kinds = entities()
    edges = set()
    for i, (_, _, _, spawners) in enumerate(zones):
        if hostile(spawners, kinds):
            edges |= {offset + i * ZONE - 2, offset + (i + 1) * ZONE - 2}
    out = []
    for x in sorted(edges):
        out += [
            run(world, f"fill {x} {y + 1} {SAFE_Z} {x} {y + 1} {Z + PAD + 3} {FENCE}[north=true,south=true]"),
            run(world, f"setblock {x} {y + 1} {Z + 14} {GATE}[facing=east]"),
            run(world, f"fill {x} {y + 2} {SAFE_Z} {x} {y + 2} {Z + PAD + 3} {CARPET}"),
            run(world, f"setblock {x} {y + 2} {Z + 14} air"),
        ]
    return out


# A platform off the left side of a hostile zone, walking along the strip, fenced off
# from the zone. Its trader stands back from the fence, out of reach of mobs at it
def safe_platform(world, x0, y):
    front = Z - 5
    return [
        run(world, f"fill {x0 - 2} {y} {SAFE_Z} {x0 + ZONE - 2} {y} {front} {FLOOR}"),
        run(world, f"fill {x0 - 1} {y + 1} {front} {x0 + ZONE - 3} {y + 1} {front} {FENCE}[east=true,west=true]"),
        run(world, f"setblock {x0 + 14} {y + 1} {front} {GATE}[facing=south]"),
        run(world, f"fill {x0 - 1} {y + 2} {front} {x0 + ZONE - 3} {y + 2} {front} {CARPET}"),
        run(world, f"setblock {x0 + 14} {y + 2} {front} air"),
    ], (x0 + 14.5, Z - 9.5)


# A walled platform off the right side of a boss's zone, walking along the strip. Cut
# off from the zone's other mobs by the wall, the only way in is a fence gate in a
# doorway on the walking side. Returns its commands and where the boss rises, its centre
def arena(world, x0, y):
    x1, x2 = x0, x0 + ZONE - 4
    z1 = Z + PAD + 4
    z2 = z1 + ARENA_DEPTH
    y1, y2 = y + 1, y + WALL_H
    door = x0 + 14
    return [
        run(world, f"forceload add {x1} {z1} {x2} {z2}"),
        run(world, f"fill {x1} {y} {z1} {x2} {y} {z2} {FLOOR}"),
        run(world, f"fill {x1} {y1} {z1} {x2} {y2} {z1} {WALL}"),
        run(world, f"fill {x1} {y1} {z2} {x2} {y2} {z2} {WALL}"),
        run(world, f"fill {x1} {y1} {z1} {x1} {y2} {z2} {WALL}"),
        run(world, f"fill {x2} {y1} {z1} {x2} {y2} {z2} {WALL}"),
        run(world, f"setblock {door} {y1} {z1} {GATE}[facing=south]"),
        run(world, f"setblock {door} {y1 + 1} {z1} air"),
    ], (door, (z1 + z2) // 2)


def layout():
    """region -> (x of its first zone, floor y). Regions of a world follow each other, a
    rise lifts a region above the one before it and a gap leaves void before it."""
    found, taken, height = {}, {}, {}
    for name, zones, world, region in REGIONS:
        rise, gap = int(region["rise"] or 0), int(region["gap"] or 0)
        start = taken[world] + gap if world in taken else 0
        y = height.get(world, FLOOR_Y) + rise
        found[name] = (start, y)
        taken[world] = start + len(zones) * ZONE + 12
        height[world] = y
    return found


# Both ends of a region's strip along x
def strip(zones, offset):
    return offset - 10, offset + len(zones) * ZONE + 2


def commands(only):
    out = []
    worlds = {name: world for name, _, world, _ in REGIONS}
    places = layout()
    ends = {name: strip(zones, places[name][0]) for name, zones, _, _ in REGIONS}
    kinds = entities()
    made = set()

    for name, zones, world, region in REGIONS:
        start, y = places[name]
        x1, x2 = ends[name]
        first = world not in made and start == 0
        made.add(world)
        if name not in only:
            continue
        if first:
            out += [
                f"aurora world create {world}",
                # Time stands still and nothing spawns by itself, so only spawner mobs are
                # about. Not /time set: in 26.x it moves the overworld clock other worlds share
                run(world, "gamerule advance_time false"), run(world, "gamerule spawn_mobs false"),
                # Gamerules are per world in 26.x, dying must not drop your gear here either
                run(world, "gamerule keep_inventory true"),
            ]
        out += [
            run(world, f"forceload add {x1} {SAFE_Z} {x2} {Z + PAD + 4}"),
            run(world, f"fill {x1} {y} {Z - 4} {x2} {y} {Z + PAD + 3} {FLOOR}"),
        ]
        # A cliff face up to a raised region, from the floor of the one before it
        if region["rise"] and not region["gap"]:
            out.append(run(world, f"fill {x1} {y - int(region['rise'])} {Z - 4} {x1} {y} {Z + PAD + 3} {CLIFF}"))

        for i, (zone, trader, mines, spawners) in enumerate(zones):
            x0 = start + i * ZONE
            # A coloured stripe across the strip marks where each zone starts
            out.append(run(world, f"fill {x0 - 2} {y} {Z - 4} {x0 - 2} {y} {Z + PAD + 3} black_concrete"))
            for m, (mine, mix) in enumerate(mines):
                mx = x0 + 2 + m * (MINE_W + MINE_GAP)
                mz = Z + 2
                # Threshold 100: refills the moment the last block is mined, and every
                # minute anyway
                out.append(f"aurora mine create t_{mine} {mx} {y + 1} {mz} {mx + MINE_W - 1} "
                           f"{y + MINE_H} {mz + MINE_W - 1} 100 1200 -w {world}")
                out.append(f"aurora mine setblocks t_{mine} " + " ".join(f"{b} {p}" for b, p in mix.items()))
                out.append(f"aurora mine settp t_{mine} {mx + MINE_W / 2} {y + 1} {mz + MINE_W + 1.5}")
            for s, (mob, limit, delay) in enumerate(m for m in spawners if m[0] not in BOSS_ZONE):
                sx = x0 + 5 + s * 9
                out.append(f"aurora spawner create {spawner_name(zone, mob)} {sx} {y + 1} {Z + 20} {mob} {limit} 3 {delay} -w {world}")
            for mob, limit, delay in (m for m in spawners if m[0] in BOSS_ZONE):
                walls, (bx, bz) = arena(world, x0, y)
                out += walls
                out.append(f"aurora spawner create t_{mob} {bx} {y + 1} {bz} {mob} {limit} 3 {delay} -w {world}")
            # On the safe platform in a hostile zone, otherwise right beside the zone's
            # last mine, or near the stripe for a zone without one
            if hostile(spawners, kinds):
                pen, (tx, tz) = safe_platform(world, x0, y)
                out += pen
            else:
                tx = x0 + 2 + len(mines) * (MINE_W + MINE_GAP) - MINE_GAP + 1.5 if mines else x0 + 4.5
                tz = Z + 2 + MINE_W / 2
            out.append(f"aurora trader create {trader} {tx} {y + 1} {tz} -w {world}")
            for k, extra in enumerate(EXTRA_TRADERS.get(zone, []), 1):
                out.append(f"aurora trader create {extra} {tx + 3 * k} {y + 1} {tz} -w {world}")

        out += fences(world, zones, start, y)

        # Arrive and respawn at the start of the strip, facing along it
        if first:
            out.append(f"aurora setspawn {x1 + 5.5} {y + 1} {Z + 14.5} -90 -w {world}")

    # Gates: a region reached by portal has one at the end of the region it comes from,
    # and one back at its own start. A 5 wide, 4 high opening standing on the floor
    touched = set()

    def gate(world, x, y):
        touched.add(world)
        out.append(run(world, f"forceload add {x} {Z + 11} {x} {Z + 17}"))
        out.append(run(world, f"fill {x} {y + 1} {Z + 11} {x} {y + 5} {Z + 17} {FRAME}"))
        out.append(run(world, f"fill {x} {y + 1} {Z + 12} {x} {y + 4} {Z + 16} air"))
        return f"{x} {y + 1} {Z + 12} {x} {y + 4} {Z + 16}"

    for name, _, world, region in REGIONS:
        a, b = region["portal"], name
        if not a or (a not in only and b not in only):
            continue
        aw, bw = worlds[a], worlds[b]
        (_, ay), (_, by) = places[a], places[b]
        _, ax2 = ends[a]
        bx1, _ = ends[b]
        forward = gate(aw, ax2 - 2, ay)
        back = gate(bw, bx1 + 2, by)
        out.append(f"aurora portal create t_{a}_to_{b} {forward} -w {aw}")
        out.append(f"aurora portal setdest t_{a}_to_{b} {bw} {bx1 + 5.5} {by + 1} {Z + 14.5} -90 0")
        if region["tag"]:
            out.append(f"aurora portal settag t_{a}_to_{b} {region['tag']}")
        if region["gate_trader"] and a in only:
            out.append(f"aurora trader create {region['gate_trader']} {ax2 - 5.5} {ay + 1} {Z + 19.5} -w {aw}")
        out.append(f"aurora portal create t_{b}_to_{a} {back} -w {bw}")
        out.append(f"aurora portal setdest t_{b}_to_{a} {aw} {ax2 - 5.5} {ay + 1} {Z + 14.5} 90 0")

    # Mines are filled and every placement saved, chunks can unload again
    for name, _, world, _ in REGIONS:
        if name in only or world in touched:
            out.append(run(world, "forceload remove all"))
    return out


# Deletes everything commands() makes for these regions. Trader spots are numbered, so
# they are read from the local database, every spot in a region's part of its world goes
def teardown(only):
    places = layout()
    out = []
    for name, zones, _, region in REGIONS:
        if name not in only:
            continue
        for zone, _, mines, spawners in zones:
            out += [f"aurora mine delete t_{mine}" for mine, _ in mines]
            out += [f"aurora spawner delete {spawner_name(zone, mob)}" for mob, _, _ in spawners]
    for name, _, _, region in REGIONS:
        a = region["portal"]
        if a and (a in only or name in only):
            out += [f"aurora portal delete t_{a}_to_{name}", f"aurora portal delete t_{name}_to_{a}"]
    if DB.exists():
        with sqlite3.connect(DB) as db:
            for name, zones, world, _ in REGIONS:
                if name not in only:
                    continue
                x1, x2 = strip(zones, places[name][0])
                spots = db.execute("SELECT id FROM traders WHERE world = ? AND x >= ? AND x <= ? ORDER BY id",
                                   (world, x1, x2)).fetchall()
                out += [f"aurora trader delete {spot}" for (spot,) in spots]
    return out


def main():
    names = [name for name, _, _, _ in REGIONS]
    ap = argparse.ArgumentParser(description=__doc__.strip().split("\n")[0])
    ap.add_argument("--send", action="store_true", help="send to the server started by scripts/server.sh")
    ap.add_argument("--teardown", action="store_true", help="delete what the build makes instead")
    ap.add_argument("--regions", default=",".join(names), help="comma separated, default all: " + ", ".join(names))
    args = ap.parse_args()

    only = {r.strip() for r in args.regions.split(",") if r.strip()}
    unknown = only - set(names)
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
