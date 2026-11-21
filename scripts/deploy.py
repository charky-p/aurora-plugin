#!/usr/bin/env python3
"""
Aurora deploy helpers, plain Python so they run on the live server's host too.

  python scripts/deploy.py stage [--db] [--worlds] [--all]
  python scripts/deploy.py swap
      The safe way to deploy, with the host's server down for seconds. stage builds the
      plugin and sends what changed to a staging folder on the host (scripts/deploy.ini,
      SFTP) while its server keeps running: the jar, every generated YAML and, with
      --worlds, the game world files that differ from the host's. Nothing live is
      touched, so a server started or restarted meanwhile breaks nothing, and a drop is
      resumed by running stage again. Then stop the host's server and run swap: it moves
      the staged files in, removes world files LocalServer does not have and every world
      not named in deploy.ini (except keep and vanilla's overworld, nether and end), and
      with --db merges LocalServer's game data (mines, spawners, traders, portals,
      spawns, worlds, safe zones) into the host's mines.db, keeping its player data and
      the game data of the worlds under keep. Start the server again after.

  python scripts/deploy.py upload [--db] [--worlds] [--all]
      stage and swap in one go, for a host whose server is already stopped.
      The first --db with player loadouts makes everything players did on the host so
      far their experimental loadout, so the main one starts fresh on their next join.
      The host's own mines.db is downloaded into LocalServer/aurora-backups first.
      A file is sent when it changed since the last deploy (a hash kept in
      target/deploy-uploaded.json) or the host's copy is another size, --force sends
      everything. --dry-run lists what would be sent without connecting,
      --no-build sends the jar already in target/, --yes skips the question.
      The first run writes scripts/deploy.ini (gitignored) for you to fill in.

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
import configparser
import fnmatch
import ftplib
import getpass
import hashlib
import json
import os
import shutil
import socket
import sqlite3
import stat
import subprocess
import sys
import tempfile
import time
import zipfile
from pathlib import Path, PurePosixPath

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src" / "main" / "resources"
DB_PATH = Path("plugins") / "Aurora" / "mines.db"
BACKUP_DIR = "aurora-backups"

LOCAL_SERVER = ROOT / "LocalServer"
CONFIG = ROOT / "scripts" / "deploy.ini"
CONFIG_EXAMPLE = ROOT / "scripts" / "deploy.example.ini"
# What was last sent to each host, so an upload only sends what changed
UPLOADED = ROOT / "target" / "deploy-uploaded.json"
PASSWORD_ENV = "AURORA_DEPLOY_PASSWORD"
JAR_PATTERN = "Aurora-*.jar"
# Never deleted by --worlds, the server can't run without them
VANILLA_WORLDS = {"overworld", "the_nether", "the_end"}


def newest_jar():
    jars = [j for j in (ROOT / "target").glob(JAR_PATTERN) if "shaded" not in j.name]
    if not jars:
        sys.exit("No plugin jar in target/, build first: scripts/server.sh build")
    return max(jars, key=lambda j: j.stat().st_mtime)


# Every generated YAML and where it goes in a server folder
def data_files():
    return [(yml, f"plugins/Aurora/{yml.relative_to(RESOURCES).as_posix()}")
            for yml in sorted(RESOURCES.rglob("*.yml")) if yml.name != "plugin.yml"]


def bundle():
    jar = newest_jar()
    out = ROOT / "target" / "aurora-deploy.zip"
    files = data_files()
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.write(jar, f"plugins/{jar.name}")
        for yml, path in files:
            z.write(yml, path)
    print(f"{out.relative_to(ROOT)}: {jar.name} and {len(files)} data files")


def world_folders(server):
    # A world is any top level folder with a level.dat, dimensions live inside it
    return sorted(p for p in server.iterdir() if p.is_dir() and (p / "level.dat").exists())


# SQLite's own backup reads a consistent copy even mid-write
def copy_database(db, folder):
    copy = Path(folder) / "mines.db"
    src, dst = sqlite3.connect(db), sqlite3.connect(copy)
    with dst:
        src.backup(dst)
    src.close()
    dst.close()
    return copy


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
            with tempfile.TemporaryDirectory() as tmp:
                z.write(copy_database(db, tmp), DB_PATH.as_posix())

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


# Tables of the game itself, LocalServer's copy wins: each row belongs to the world
# in its column. blocks follow their mine, worlds and build_worlds name the world
GAME_TABLES = {"mines": "world", "spawners": "world", "traders": "world", "portals": "world",
               "spawns": "world", "locations": "world", "worlds": "name", "build_worlds": "world",
               "safe_zones": "world", "teleport_pads": "world"}
# Numbered by the database, the host numbers LocalServer's rows again
AUTO_IDS = {"traders": "id"}
# Every per-player table that belongs to a loadout, kept in step with LoadoutStore.TABLES
LOADOUT_TABLES = ("playerData", "player_ranks", "player_cosmetics", "player_equipped", "player_tools",
                  "player_perks", "collections", "backpack_data", "unlocked_gear", "portal_access", "tutorial")


def progress_to_experimental(db):
    """The first deploy with loadouts, the host has no loadouts table yet: everything its
    players did so far was testing. Each one is marked as playing the experimental loadout,
    nothing is moved, so their next join in a main world puts it all away as experimental
    and starts the main one fresh (LoadoutManager). How many players that is."""
    if db.execute("SELECT 1 FROM main.sqlite_master WHERE type = 'table' AND name = 'loadouts'").fetchone():
        return 0
    # The same table Schema.java makes, so the plugin finds it as it expects
    db.execute("CREATE TABLE main.loadouts (uuid TEXT PRIMARY KEY, shadow TEXT, active TEXT)")
    tables = [t for t in LOADOUT_TABLES
              if db.execute("SELECT 1 FROM main.sqlite_master WHERE type = 'table' AND name = ?", (t,)).fetchone()]
    if not tables:
        return 0
    players = " UNION ".join(f"SELECT uuid FROM main.{t}" for t in tables)
    return db.execute(f"INSERT INTO main.loadouts (uuid, active) SELECT uuid, 'experimental' "
                      f"FROM ({players}) WHERE uuid IS NOT NULL").rowcount


# The host's database with LocalServer's game data in it. Player tables stay the host's,
# and so does all game data of the kept worlds, like a build world only the host has
def merge_database(host_db, local_db, out, keep):
    shutil.copy(host_db, out)
    db = sqlite3.connect(out)
    db.execute("ATTACH DATABASE ? AS l", (str(local_db),))
    kept = ",".join("?" * len(keep)) or "''"
    with db:
        for table, column in GAME_TABLES.items():
            sql = db.execute("SELECT sql FROM l.sqlite_master WHERE type = 'table' AND name = ?", (table,)).fetchone()
            if not sql:
                continue
            # A table newer than the host's plugin is made the way LocalServer made it
            if not db.execute("SELECT 1 FROM main.sqlite_master WHERE type = 'table' AND name = ?", (table,)).fetchone():
                db.execute(sql[0])
            host_cols = {r[1] for r in db.execute(f"PRAGMA main.table_info({table})")}
            cols = [r[1] for r in db.execute(f"PRAGMA l.table_info({table})")
                    if r[1] in host_cols and r[1] != AUTO_IDS.get(table)]
            names = ", ".join(cols)

            if table == "mines":
                db.execute(f"DELETE FROM main.blocks WHERE mine NOT IN "
                           f"(SELECT name FROM main.mines WHERE world IN ({kept}))", keep)
            db.execute(f"DELETE FROM main.{table} WHERE {column} NOT IN ({kept})", keep)
            db.execute(f"INSERT OR REPLACE INTO main.{table} ({names}) "
                       f"SELECT {names} FROM l.{table} WHERE {column} NOT IN ({kept})", keep)
            if table == "mines":
                block_cols = ", ".join(r[1] for r in db.execute("PRAGMA l.table_info(blocks)"))
                db.execute(f"INSERT INTO main.blocks ({block_cols}) SELECT {block_cols} FROM l.blocks "
                           f"WHERE mine IN (SELECT name FROM l.mines WHERE world NOT IN ({kept}))", keep)

        # One main spawn: LocalServer's, when it has one
        if db.execute("SELECT 1 FROM l.spawns WHERE main = 1").fetchone():
            db.execute(f"UPDATE main.spawns SET main = 0 WHERE world IN ({kept})", keep)

        moved = progress_to_experimental(db)
        if moved:
            print(f"  {moved} players' progress is now their experimental loadout, main starts fresh")
    db.execute("DETACH DATABASE l")
    problem = db.execute("PRAGMA integrity_check").fetchone()[0]
    db.close()
    if problem != "ok":
        sys.exit(f"The merged database failed its check ({problem}), nothing was uploaded")


def load_config():
    if not CONFIG.exists():
        shutil.copy(CONFIG_EXAMPLE, CONFIG)
        sys.exit(f"Wrote {CONFIG.relative_to(ROOT)}, fill in the host's FTP details and run this again.\n"
                 "It is gitignored, so the login never reaches the repository.")
    config = configparser.ConfigParser(interpolation=None)
    config.read([CONFIG_EXAMPLE, CONFIG], encoding="utf-8")
    return config


def listed(value):
    return [name.strip() for name in value.replace("\n", ",").split(",") if name.strip()]


# The generated YAML and the jar, tests included, without touching LocalServer, so
# this works while the local server runs
def build(config):
    print("Generating game data")
    subprocess.run([sys.executable, str(ROOT / "scripts" / "generate.py")], cwd=ROOT, check=True,
                   stdout=subprocess.DEVNULL)

    print("Building the plugin")
    env = dict(os.environ)
    if config["build"].get("jdk"):
        env["JAVA_HOME"] = config["build"]["jdk"]
    mvn = config["build"].get("mvn") or "mvn"
    if not Path(mvn).exists():
        mvn = shutil.which("mvn") or shutil.which("mvn.cmd") or sys.exit(f"No Maven at '{mvn}', set mvn in deploy.ini")
    result = subprocess.run([mvn, "-q", "package", "-Dmaven.antrun.skip=true"], cwd=ROOT, env=env)
    if result.returncode != 0:
        sys.exit("The build failed, nothing was uploaded")


def kept_worlds(config):
    return listed(config["worlds"].get("keep", ""))


# Every file of the game worlds and where it goes on the host
def world_files(config):
    folder = PurePosixPath(config["worlds"]["folder"])
    files, missing = [], []
    for name in listed(config["worlds"]["names"]):
        world = LOCAL_SERVER / folder / name
        if not world.is_dir():
            missing.append(str(world))
            continue
        for file in sorted(world.rglob("*")):
            # The lock is held by the running server and means nothing in a copy
            if file.is_file() and file.name != "session.lock":
                files.append((file, (folder / name / file.relative_to(world).as_posix()).as_posix()))
    if missing:
        sys.exit("No such world, check [worlds] in deploy.ini:\n  " + "\n  ".join(missing))
    return files


class ImplicitFTP_TLS(ftplib.FTP_TLS):
    """FTPS where TLS starts immediately on connect (FileZilla's 'Require implicit FTP over TLS')."""
    def connect(self, host="", port=0, timeout=-999, source_address=None):
        if host != "":
            self.host = host
        if port > 0:
            self.port = port
        if timeout != -999:
            self.timeout = timeout
        if source_address is not None:
            self.source_address = source_address

        raw_sock = socket.create_connection((self.host, self.port), self.timeout, self.source_address)
        self.sock = self.context.wrap_socket(raw_sock, server_hostname=self.host)
        self.af = self.sock.family
        self.file = self.sock.makefile("r", encoding=self.encoding)
        self.welcome = self.getresp()
        return self.welcome


class Host:
    def __init__(self, config, password):
        host = config["host"]
        self.proto = host.get("protocol", "sftp").lower()
        self.root = host.get("root", "/").rstrip("/")
        self.made = set()

        if self.proto == "sftp":
            try:
                import paramiko
            except ImportError:
                sys.exit("SFTP requires paramiko. Run `pip install paramiko`.")

            self.ssh = paramiko.SSHClient()
            self.ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
            try:
                self.ssh.connect(
                    hostname=host["host"],
                    port=host.getint("port", 22),
                    username=host["user"],
                    password=password,
                    timeout=10,
                    banner_timeout=10,
                    auth_timeout=10,
                    look_for_keys=False,
                    allow_agent=False,
                )
            except paramiko.AuthenticationException:
                # Fallback to keyboard-interactive (same method FileZilla's psftp uses)
                transport = self.ssh.get_transport()
                transport.auth_interactive(
                    host["user"],
                    lambda title, instructions, prompt_list: [password for _ in prompt_list],
                )
            self.sftp = self.ssh.open_sftp()
        else:
            self.ftp = ImplicitFTP_TLS() if self.proto == "ftps" else ftplib.FTP()
            self.ftp.connect(host["host"], host.getint("port", 21), timeout=15)
            self.ftp.login(host["user"], password)
            if isinstance(self.ftp, ftplib.FTP_TLS):
                self.ftp.prot_p()

    def path(self, relative):
        return f"{self.root}/{relative}"

    def make_folders(self, relative):
        folder = ""
        for part in PurePosixPath(relative).parent.parts:
            folder = f"{folder}/{part}" if folder else part
            if folder in self.made:
                continue
            if self.proto == "sftp":
                try:
                    self.sftp.stat(self.path(folder))
                except IOError:
                    self.sftp.mkdir(self.path(folder))
            else:
                try:
                    self.ftp.mkd(self.path(folder))
                except ftplib.error_perm:
                    pass  # It is already there
            self.made.add(folder)

    def upload(self, local, relative):
        self.make_folders(relative)
        if self.proto == "sftp":
            self.sftp.put(str(local), self.path(relative))
        else:
            with open(local, "rb") as file:
                self.ftp.storbinary(f"STOR {self.path(relative)}", file, blocksize=262144)

    # False if the host has no such file
    def download(self, relative, local):
        if self.proto == "sftp":
            try:
                self.sftp.get(self.path(relative), str(local))
                return True
            except IOError:
                Path(local).unlink(missing_ok=True)
                return False
        try:
            with open(local, "wb") as file:
                self.ftp.retrbinary(f"RETR {self.path(relative)}", file.write)
            return True
        except ftplib.error_perm:
            Path(local).unlink(missing_ok=True)
            return False

    def names(self, relative):
        if self.proto == "sftp":
            try:
                return self.sftp.listdir(self.path(relative))
            except IOError:
                return []
        try:
            return [PurePosixPath(name).name for name in self.ftp.nlst(self.path(relative))]
        except ftplib.error_perm:
            return []

    def delete(self, relative):
        if self.proto == "sftp":
            self.sftp.remove(self.path(relative))
        else:
            self.ftp.delete(self.path(relative))

    # A folder and everything in it, nothing happens if it is not there
    def delete_tree(self, relative):
        if self.proto == "sftp":
            import stat
            try:
                entries = self.sftp.listdir_attr(self.path(relative))
            except IOError:
                return
            for entry in entries:
                child = f"{relative}/{entry.filename}"
                if stat.S_ISDIR(entry.st_mode):
                    self.delete_tree(child)
                else:
                    self.sftp.remove(self.path(child))
            self.sftp.rmdir(self.path(relative))
            return
        for name in self.names(relative):
            child = f"{relative}/{name}"
            try:
                self.ftp.delete(self.path(child))
            except ftplib.error_perm:
                self.delete_tree(child)
        try:
            self.ftp.rmd(self.path(relative))
        except ftplib.error_perm:
            pass

    def close(self):
        if self.proto == "sftp":
            self.sftp.close()
            self.ssh.close()
        else:
            try:
                self.ftp.quit()
            except ftplib.all_errors:
                self.ftp.close()


def megabytes(size):
    return f"{size / (1024 * 1024):.1f} MB"


# Where stage puts the files on the host until swap moves them in: outside world/ and
# plugins/, so the running server never loads or overwrites them
STAGING = ".aurora-staging"
PLAN = f"{STAGING}/plan.json"
# Files this small are cheap, and with no record of them the size alone proves nothing
SMALL = 65536


def file_hash(file):
    digest = hashlib.sha1()
    with open(file, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def remote_sizes(remote, folder):
    """path -> size of every file under a folder on the host, empty if there is none."""
    sizes = {}
    if remote.proto != "sftp":
        return sizes
    try:
        entries = remote.sftp.listdir_attr(remote.path(folder))
    except IOError:
        return sizes
    for entry in entries:
        child = f"{folder}/{entry.filename}"
        if stat.S_ISDIR(entry.st_mode):
            sizes.update(remote_sizes(remote, child))
        else:
            sizes[child] = entry.st_size
    return sizes


def history_for(config):
    host = config["host"]
    target = f"{host.get('host')}{host.get('root', '/')}"
    history = json.loads(UPLOADED.read_text()) if UPLOADED.exists() else {}
    return target, history, history.setdefault(target, {})


def save_history(history):
    UPLOADED.parent.mkdir(exist_ok=True)
    UPLOADED.write_text(json.dumps(history))


def connect(config):
    host = config["host"]
    if not host.get("host") or not host.get("user"):
        sys.exit(f"Fill in host and user in {CONFIG.relative_to(ROOT)}")
    password = host.get("password") or os.environ.get(PASSWORD_ENV) or getpass.getpass(f"FTP password for {host['user']}: ")
    remote = Host(config, password)
    if remote.proto != "sftp":
        remote.close()
        sys.exit("stage, swap and upload need protocol = sftp in deploy.ini")
    # A stalled connection fails after this long instead of waiting forever; stage resumes
    remote.sftp.get_channel().settimeout(120)
    return remote


def wanted_files(config, args):
    """[(local file, host path)] the deploy is made of: the jar, the YAML, the worlds."""
    if not args.no_build:
        build(config)
    jar = newest_jar()
    files = [(jar, f"plugins/{jar.name}")] + data_files()
    if args.worlds or args.all:
        files += world_files(config)
    return jar, files


def changed(files, sent, live, force):
    """The files the host needs: changed since the last deploy, a different size there,
    or small with no record. Each with its hash, the record swap keeps once it is live."""
    out = []
    print(f"  comparing {len(files)} files", flush=True)
    for file, path in files:
        size, digest = file.stat().st_size, file_hash(file)
        record = sent.get(path)
        record = record if isinstance(record, str) else None
        if force or live.get(path) != size or (record and record != digest) or (not record and size <= SMALL):
            out.append((file, path, digest))
        else:
            sent[path] = digest
    return out


def stage(args, config=None, remote=None):
    """Sends what changed to the host's staging folder. Safe while its server runs: the
    live files are not touched. A second run resumes, files already staged are skipped."""
    config = config or load_config()
    jar, files = wanted_files(config, args)
    with_db = args.db or args.all
    with_worlds = args.worlds or args.all
    if with_db and not (LOCAL_SERVER / DB_PATH).exists():
        sys.exit(f"No database at {LOCAL_SERVER / DB_PATH}")
    target, history, sent = history_for(config)

    if args.dry_run:
        print(f"\nTo {target}: {len(files)} files" + (", and mines.db at swap" if with_db else ""))
        print("  The host is not asked what it has, so this lists what changed here since the last deploy")
        for file, path in files:
            record = sent.get(path)
            if args.force or not isinstance(record, str) or record != file_hash(file):
                print(f"    {path}  {megabytes(file.stat().st_size)}")
        return None

    own = remote is None
    print(f"Connecting to {target}", flush=True)
    remote = remote or connect(config)
    try:
        # The jar and YAML one by one: the host's plugins folder holds other plugins' data too
        live = {}
        for _, path in files:
            if path.startswith("plugins/"):
                try:
                    live[path] = remote.sftp.stat(remote.path(path)).st_size
                except IOError:
                    pass
        if with_worlds:
            for name in listed(config["worlds"]["names"]):
                print(f"  checking {name} on the host", flush=True)
                live.update(remote_sizes(remote, f"{config['worlds']['folder']}/{name}"))
        queue = changed(files, sent, live, args.force)
        staged = remote_sizes(remote, STAGING)
        # Sizes taken once: a running local server can save a world file mid-upload
        sizes = {path: file.stat().st_size for file, path, _ in queue}
        total = sum(sizes.values())
        print(f"\nTo {target}: {len(queue)} of {len(files)} files changed ({megabytes(total)})"
              + (", and mines.db at swap" if with_db else ""))
        done = 0
        for number, (file, path, _) in enumerate(queue, 1):
            size = sizes[path]
            done += size
            already = staged.get(f"{STAGING}/{path}") == size and size > SMALL
            if not already:
                remote.upload(file, f"{STAGING}/{path}")
            print(f"  [{number}/{len(queue)}] {'staged' if not already else 'ready '} {path}  "
                  f"{megabytes(size)}  {done / total:.0%}" if total else f"  [{number}/{len(queue)}] {path}", flush=True)

        plan = {"jar": jar.name, "db": with_db, "worlds": with_worlds,
                "files": {path: digest for _, path, digest in queue}}
        with tempfile.TemporaryDirectory() as tmp:
            local_plan = Path(tmp) / "plan.json"
            local_plan.write_text(json.dumps(plan))
            remote.upload(local_plan, PLAN)
        save_history(history)
        print(f"Staged. Stop the host's server, run `python scripts/deploy.py swap`, then start it.")
        return plan
    finally:
        if own:
            remote.close()


def swap(args, config=None, remote=None):
    """Moves the staged files into place, tidies the worlds and merges the database. The
    host's server has to be stopped; this takes seconds."""
    config = config or load_config()
    target, history, sent = history_for(config)
    keep = kept_worlds(config)
    folder = config["worlds"]["folder"]

    if not args.yes:
        print("The host's server has to be stopped now, or it writes its own copy back over these.")
        if input("Is it stopped? Type yes to go on: ").strip().lower() != "yes":
            sys.exit("Nothing was changed")

    own = remote is None
    remote = remote or connect(config)
    try:
        with tempfile.TemporaryDirectory() as tmp:
            local_plan = Path(tmp) / "plan.json"
            if not remote.download(PLAN, local_plan):
                sys.exit("Nothing is staged on the host, run `python scripts/deploy.py stage` first")
            plan = json.loads(local_plan.read_text())

        # Staged files over the live ones, a rename each. One no longer staged was moved in
        # by a swap that dropped, so a second swap carries on where it stopped
        staged = remote_sizes(remote, STAGING)
        for path, digest in plan["files"].items():
            if f"{STAGING}/{path}" not in staged:
                sent[path] = digest
                continue
            remote.make_folders(path)
            try:
                remote.sftp.remove(remote.path(path))
            except IOError:
                pass
            remote.sftp.rename(remote.path(f"{STAGING}/{path}"), remote.path(path))
            sent[path] = digest
        print(f"  {len(plan['files'])} files moved in")

        # A second Aurora jar would be loaded instead of the new one, or beside it
        for name in remote.names("plugins"):
            if fnmatch.fnmatch(name, JAR_PATTERN) and name != plan["jar"]:
                remote.delete(f"plugins/{name}")
                print(f"  removed old {name}")

        if plan["worlds"]:
            names = set(listed(config["worlds"]["names"]))
            wanted = {path for _, path in world_files(config)}
            for name in remote.names(folder):
                if name in VANILLA_WORLDS or name in keep:
                    continue
                if name not in names:
                    remote.delete_tree(f"{folder}/{name}")
                    print(f"  deleted world {name}")
                    continue
                # What the host has in a game world and LocalServer does not, like chunks it made
                for path in remote_sizes(remote, f"{folder}/{name}"):
                    if path not in wanted and not path.endswith("session.lock"):
                        remote.delete(path)
                        sent.pop(path, None)
                        print(f"  removed {path}")

        if plan["db"]:
            merge_upload(remote, keep)
        remote.delete_tree(STAGING)
    finally:
        save_history(history)
        if own:
            remote.close()
    print("Swapped. Start the host's server to load it.")


def merge_upload(remote, keep):
    backups = LOCAL_SERVER / BACKUP_DIR
    backups.mkdir(exist_ok=True)
    kept = backups / f"host-mines-{time.strftime('%Y%m%d-%H%M%S')}.db"
    if remote.download(DB_PATH.as_posix(), kept):
        print(f"  the host's mines.db is kept in {kept.relative_to(ROOT)}")
    with tempfile.TemporaryDirectory() as tmp:
        local_copy = copy_database(LOCAL_SERVER / DB_PATH, tmp)
        if kept.exists():
            merged = Path(tmp) / "merged.db"
            merge_database(kept, local_copy, merged, keep)
            remote.upload(merged, DB_PATH.as_posix())
            print(f"  {DB_PATH.as_posix()}: LocalServer's game data, the host's players and "
                  f"{', '.join(keep) or 'no kept worlds'}")
        else:
            remote.upload(local_copy, DB_PATH.as_posix())
            print(f"  {DB_PATH.as_posix()}: the host had none, LocalServer's as it is")


def upload(args):
    """stage then swap in one go, for a host whose server is already stopped."""
    config = load_config()
    if args.dry_run:
        stage(args, config)
        return
    if (args.db or args.worlds or args.all) and not args.yes:
        print("The host's server has to be stopped for the whole upload. To keep it running while"
              " files go up, use stage now and swap later.")
        if input("Is it stopped? Type yes to go on: ").strip().lower() != "yes":
            sys.exit("Nothing was uploaded")
    remote = connect(config)
    try:
        stage(args, config, remote)
        args.yes = True
        swap(args, config, remote)
    finally:
        remote.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__.strip().split("\n")[0])
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("bundle")
    u = sub.add_parser("upload")
    u.add_argument("--db", action="store_true", help="also send LocalServer's mines.db")
    u.add_argument("--worlds", action="store_true", help="also send the game worlds")
    u.add_argument("--all", action="store_true", help="--db and --worlds")
    u.add_argument("--no-build", action="store_true", help="send the jar already in target/")
    u.add_argument("--force", action="store_true", help="send files that have not changed too")
    u.add_argument("--dry-run", action="store_true", help="list what would be sent, connect to nothing")
    u.add_argument("--yes", action="store_true", help="do not ask before overwriting the database or worlds")
    s = sub.add_parser("stage")
    for flag, text in (("--db", "merge LocalServer's mines.db in at swap"), ("--worlds", "also stage the game worlds"),
                       ("--all", "--db and --worlds"), ("--no-build", "stage the jar already in target/"),
                       ("--force", "stage files that have not changed too"),
                       ("--dry-run", "list what changed here, connect to nothing")):
        s.add_argument(flag, action="store_true", help=text)
    w = sub.add_parser("swap")
    w.add_argument("--yes", action="store_true", help="do not ask whether the host's server is stopped")
    b = sub.add_parser("backup")
    b.add_argument("server")
    b.add_argument("--keep", type=int, default=14)
    args = parser.parse_args()

    if args.command == "bundle":
        bundle()
    elif args.command == "upload":
        upload(args)
    elif args.command == "stage":
        stage(args)
    elif args.command == "swap":
        swap(args)
    else:
        backup(args.server, args.keep)


if __name__ == "__main__":
    main()