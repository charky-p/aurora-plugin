# Aurora

![Build](https://github.com/charky-p/aurora-plugin/actions/workflows/build.yml/badge.svg)

A sky-mining RPG plugin for Paper 26.3. Mine resources, trade them up for better gear, and fight custom mobs across the islands.

## Features

- **Mines** that reset on a timer, with Fortune, Blast and Luck enchants
- **Custom mobs and spawners** with their own health, attack levels and drops
- **Traders and shops** where gear upgrades cost resources and the previous tier
- **Backpack** that holds mined resources outside the inventory
- **Armory** where weapons dropped by mobs are unlocked for good
- **Player menu** with `/aurora` or `/a`

## Requirements

- Java 25
- Maven
- Python 3 with PyYAML, for the game data

## Game data

All gear, mobs, blocks and shops live in `scripts/data/*.csv`. Edit the CSVs, then generate the YAML:

```bash
python scripts/generate.py
```

Never edit the generated YAML in `src/main/resources/`.

## Build and run

`scripts/server.sh` builds the plugin and runs the local test server in `LocalServer/`:

```bash
scripts/server.sh build     # build, copies the jar and YAML into LocalServer
scripts/server.sh start     # start in the background
scripts/server.sh stop      # stop cleanly
scripts/server.sh restart   # stop, build, start
```

With the server running, data-only changes can be loaded live with `python scripts/generate.py --live` then `/refresh` in game.

## Deploying

```bash
scripts/server.sh build
python scripts/deploy.py bundle      # target/aurora-deploy.zip
```

Stop the live server, unzip `aurora-deploy.zip` over its folder, start it. The zip holds the jar and the game data YAML: the plugin only writes its YAML when a file is missing, so copying the jar alone keeps the old data.

Back up before every deploy. On the live server's host, after `save-off` and `save-all flush` in the console (then `save-on`):

```bash
python scripts/deploy.py backup <server folder>
```

This writes the database and worlds to `<server folder>/aurora-backups/` and keeps the newest 14. For the local server, `scripts/server.sh backup` does the saving steps itself. To restore, stop the server and unzip a backup over its folder.

## Tests

```bash
mvn test
```

CI runs the data check, build and tests on every push.

## Planning

Ideas and design notes are in [PLANNING.md](PLANNING.md).
