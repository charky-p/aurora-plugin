#!/usr/bin/env python3
"""
Aurora deploy helpers, plain Python so they run on the live server's host too.

  python scripts/deploy.py bundle
      target/aurora-deploy.zip: the plugin jar and every generated YAML, laid out
      like a server folder. Unzip it over the server folder with the server stopped.
      The plugin only writes its YAML when a file is missing, so copying the jar
      alone keeps the old game data.

  python scripts/deploy.py backup <server folder> [--keep 14]
      <server folder>/aurora-backups/aurora-<time>.zip with the Aurora database and
      every world. The database copy is safe while the server runs. Worlds can be
      mid-save, so on a running server run `save-off` then `save-all flush` first and
      `save-on` after (scripts/server.sh backup does that for LocalServer).
      Keeps the newest --keep backups.

Restore: stop the server, unzip a backup over the server folder, start it.
"""
import argparse
import os
import sqlite3
import sys
import tempfile
import time
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src" / "main" / "resources"
DB_PATH = Path("plugins") / "Aurora" / "mines.db"
BACKUP_DIR = "aurora-backups"


def bundle():
    jars = [j for j in (ROOT / "target").glob("Aurora-*.jar") if "shaded" not in j.name]
    if not jars:
        sys.exit("No plugin jar in target/, build first: scripts/server.sh build")
    jar = max(jars, key=lambda j: j.stat().st_mtime)

    out = ROOT / "target" / "aurora-deploy.zip"
    count = 0
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.write(jar, f"plugins/{jar.name}")
        for yml in sorted(RESOURCES.rglob("*.yml")):
            if yml.name == "plugin.yml":
                continue
            z.write(yml, f"plugins/Aurora/{yml.relative_to(RESOURCES).as_posix()}")
            count += 1
    print(f"{out.relative_to(ROOT)}: {jar.name} and {count} data files")


def world_folders(server):
    # A world is any top level folder with a level.dat, dimensions live inside it
    return sorted(p for p in server.iterdir() if p.is_dir() and (p / "level.dat").exists())


def backup(server, keep):
    server = Path(server).resolve()
    db = server / DB_PATH
    worlds = world_folders(server)
    if not db.exists() and not worlds:
        sys.exit(f"{server} has no {DB_PATH} and no worlds, is it a server folder?")

    target_dir = server / BACKUP_DIR
    target_dir.mkdir(exist_ok=True)
    target = target_dir / f"aurora-{time.strftime('%Y%m%d-%H%M%S')}.zip"

    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as z:
        if db.exists():
            # SQLite's own backup reads a consistent copy even mid-write
            with tempfile.TemporaryDirectory() as tmp:
                copy = Path(tmp) / "mines.db"
                src, dst = sqlite3.connect(db), sqlite3.connect(copy)
                with dst:
                    src.backup(dst)
                src.close()
                dst.close()
                z.write(copy, DB_PATH.as_posix())

        for world in worlds:
            for file in world.rglob("*"):
                # The lock is held by the running server and means nothing in a copy
                if file.is_file() and file.name != "session.lock":
                    z.write(file, file.relative_to(server).as_posix())

    size = target.stat().st_size / (1024 * 1024)
    print(f"{target}: database and {len(worlds)} world(s), {size:.1f} MB")

    backups = sorted(target_dir.glob("aurora-*.zip"))
    for old in backups[:-keep] if keep > 0 else []:
        old.unlink()
        print(f"  removed old backup {old.name}")


def main():
    parser = argparse.ArgumentParser(description=__doc__.strip().split("\n")[0])
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("bundle")
    b = sub.add_parser("backup")
    b.add_argument("server")
    b.add_argument("--keep", type=int, default=14)
    args = parser.parse_args()

    if args.command == "bundle":
        bundle()
    else:
        backup(args.server, args.keep)


if __name__ == "__main__":
    main()
