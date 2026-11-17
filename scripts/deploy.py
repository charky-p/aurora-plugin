#!/usr/bin/env python3
"""
Aurora deploy helpers, plain Python so they run on the live server's host too.

  python scripts/deploy.py upload [--db] [--worlds] [--all]
      Builds the plugin and sends it to the host in scripts/deploy.ini over FTP/FTPS/SFTP: the
      jar and every generated YAML. --db also sends LocalServer's mines.db and
      --worlds the game worlds named in deploy.ini, --all is both. Stop the host's
      server first when sending the database or worlds. The host's own mines.db is
      downloaded into LocalServer/aurora-backups before it is overwritten.
      Files that have not changed since the last upload are skipped, --force sends
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
import json
import os
import shutil
import socket
import sqlite3
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


def upload(args):
    config = load_config()
    host = config["host"]
    with_db = args.db or args.all
    with_worlds = args.worlds or args.all

    if not args.no_build:
        build(config)
    jar = newest_jar()

    files = [(jar, f"plugins/{jar.name}")] + data_files()
    worlds = world_files(config) if with_worlds else []
    db = LOCAL_SERVER / DB_PATH
    if with_db and not db.exists():
        sys.exit(f"No database at {db}")

    # Only what changed since the last upload to this host
    target = f"{host.get('host')}{host.get('root', '/')}"
    history = json.loads(UPLOADED.read_text()) if UPLOADED.exists() else {}
    sent = history.setdefault(target, {})

    def stamp(file):
        stat = file.stat()
        return [stat.st_size, stat.st_mtime_ns]

    queue = [(file, path) for file, path in files + worlds if args.force or sent.get(path) != stamp(file)]
    skipped = len(files) + len(worlds) - len(queue)

    print(f"\nTo {target or 'the host'}:")
    print(f"  {jar.name}, {len(files) - 1} data files" + (f", {len(worlds)} world files" if with_worlds else "")
          + (", mines.db" if with_db else ""))
    print(f"  {len(queue)} to send ({megabytes(sum(file.stat().st_size for file, _ in queue))}), {skipped} unchanged")

    if args.dry_run:
        for file, path in queue:
            print(f"    {path}  {megabytes(file.stat().st_size)}")
        if with_db:
            print(f"    {DB_PATH.as_posix()}  {megabytes(db.stat().st_size)}")
        return

    if not host.get("host") or not host.get("user"):
        sys.exit(f"Fill in host and user in {CONFIG.relative_to(ROOT)}")

    if (with_db or with_worlds) and not args.yes:
        print("\nThis overwrites " + " and ".join(
            (["the host's mines.db, every player's progress there with it"] if with_db else [])
            + (["its game worlds"] if with_worlds else [])) + ".")
        print("The host's server has to be stopped, or it writes its own copy back over the upload.")
        if input("Type yes to go on: ").strip().lower() != "yes":
            sys.exit("Nothing was uploaded")

    password = host.get("password") or os.environ.get(PASSWORD_ENV) or getpass.getpass(f"FTP password for {host['user']}: ")
    remote = Host(config, password)
    try:
        for number, (file, path) in enumerate(queue, 1):
            print(f"  [{number}/{len(queue)}] {path}  {megabytes(file.stat().st_size)}")
            remote.upload(file, path)
            sent[path] = stamp(file)

        # A second Aurora jar would be loaded instead of the new one, or beside it
        for name in remote.names("plugins"):
            if fnmatch.fnmatch(name, JAR_PATTERN) and name != jar.name:
                remote.delete(f"plugins/{name}")
                print(f"  removed old {name}")

        if with_db:
            backups = LOCAL_SERVER / BACKUP_DIR
            backups.mkdir(exist_ok=True)
            kept = backups / f"host-mines-{time.strftime('%Y%m%d-%H%M%S')}.db"
            if remote.download(DB_PATH.as_posix(), kept):
                print(f"  the host's mines.db is kept in {kept.relative_to(ROOT)}")
            with tempfile.TemporaryDirectory() as tmp:
                remote.upload(copy_database(db, tmp), DB_PATH.as_posix())
            print(f"  {DB_PATH.as_posix()}")
    finally:
        remote.close()
        UPLOADED.parent.mkdir(exist_ok=True)
        UPLOADED.write_text(json.dumps(history))
    print("Uploaded. Start or restart the host's server to load it.")


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
    b = sub.add_parser("backup")
    b.add_argument("server")
    b.add_argument("--keep", type=int, default=14)
    args = parser.parse_args()

    if args.command == "bundle":
        bundle()
    elif args.command == "upload":
        upload(args)
    else:
        backup(args.server, args.keep)


if __name__ == "__main__":
    main()