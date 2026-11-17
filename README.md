# Aurora

![Build](https://github.com/charky-p/aurora-plugin/actions/workflows/build.yml/badge.svg)

A sky-mining RPG plugin for Paper 26.3. Mine resources, spend them on upgrades for tools that stay with you, and fight custom mobs across the islands.

## Features

- **Mines** that reset on a timer, gated by Fortune against each block's density
- **Custom mobs and spawners** with their own health, attack levels and drops
- **Upgrades, not items**: each zone's trader sells levels for the pick, axe, sword, shield and armour, priced by a formula from a time target per zone
- **Three regions**: the Origin Isles, then two branches that add up, Gormak for fighting and Verdant Groves for gathering
- **Backpack** that holds mined resources outside the inventory
- **Armory** where weapons dropped by mobs are unlocked for good
- **Collections, levels and Essence**, playtime ranks and cosmetics in `/customise`
- **Bosses** summoned with a key, with phases and adds
- **Player menu** with `/aurora` or `/a`

## Requirements

- Java 25
- Maven
- Python 3 with PyYAML, for the game data

## Commands

Every command, and the order to build a region in, is in [docs/COMMANDS.md](docs/COMMANDS.md). How the game progresses and how it is tuned is in [docs/PROGRESSION.md](docs/PROGRESSION.md), and selling donor ranks through a store in [docs/STORE.md](docs/STORE.md).

## Game data

All game data lives in CSVs under `scripts/data/`, sorted into four folders. The scripts find a file by its name, so a file can move between folders freely.

| Folder | Holds |
|---|---|
| `world/` | Where the game happens: `zones.csv` (each zone's time target, mines, mobs and price scales), `realms.csv` (which worlds share tool levels), `warps.csv`, `blocks.csv` (what every block drops and its density) |
| `items/` | Everything a player can hold: `resources.csv` and `resources_special.csv` (keys), `tools.csv` (the tools that level up), `enchants.csv`, and the `gear_*.csv` files for what mobs wear and drop, food, rods and the backpack |
| `mobs/` | One file per region: health, defence, attack, equipment and drops |
| `shops/` | What traders offer: `upgrades.csv` (every upgrade step and what it costs in), `traders.csv`, the menu layouts in `guis_shops*.csv` and their `icons.csv` |

Edit the CSVs, then generate the YAML:

```bash
python scripts/generate.py
```

Never edit the generated YAML in `src/main/resources/`.

## Architecture

The code is laid out by feature: `core/` wires everything together and holds the data layer, `common/` holds what features share, and each folder under `features/` is one part of the game with its own manager, listeners and menus.

```mermaid
flowchart TD
    Aurora["Aurora<br/>the plugin: builds everything on enable"]

    subgraph Events["Events"]
        Commands["Commands<br/>/a and its subcommands,<br/>/backpack, /customise, /ranks ..."]
        Listeners["Listeners<br/>mining, combat, traders,<br/>menus, joins, chat"]
    end

    subgraph Managers["Managers, one per feature"]
        M1["Mine, Spawner,<br/>Trader, Portal, World"]
        M2["Tool, Backpack,<br/>Armory, Collection"]
        M3["Player, Level, Rank,<br/>Perk, Cosmetic"]
    end

    subgraph Objects["Objects"]
        Trader["Trader<br/>Trader, TraderMenu,<br/>UpgradeZone"]
        Mining["Mining<br/>Mine, blocks,<br/>LootPool"]
        Spawners["Spawners<br/>Spawner, mobs,<br/>bosses"]
        PlayerData["Player Data<br/>PlayerData, BackpackData,<br/>tool levels"]
    end

    subgraph Data["Data"]
        Registry["Registry<br/>DataRegistry: every item, block,<br/>mob, menu and upgrade"]
        Models["Models<br/>one class per YAML shape"]
        YAML["YAML<br/>src/main/resources,<br/>generated from scripts/data"]
        Database["Database<br/>SQLite, one Store per table"]
    end

    Aurora --> Events
    Aurora --> Managers
    Aurora --> Data
    Commands --> Managers
    Listeners --> Managers
    Managers --> Objects
    Managers --> Registry
    Managers --> Database
    Registry --> Models
    Models --> YAML
    CSV["CSV files in scripts/data"] -.->|"generate.py"| YAML
```

| Group | Where | What it is |
|---|---|---|
| Aurora | `core/Aurora.java` | Creates the database, the registry and every manager, then registers commands and listeners |
| Commands | `features/system/commands/`, and beside each feature | Parse what was typed and call a manager |
| Listeners | beside each feature | Turn Bukkit events into manager calls |
| Managers | `features/<feature>/*Manager.java` | The rules of one feature and its state in memory |
| Objects | `features/trader`, `features/mine`, `features/spawner`, `features/player` | What managers hold: a mine, a spawner, a trader, a player's data |
| Registry | `core/DataRegistry.java`, `core/data/` | Loads the YAML once and answers every lookup |
| Models | `core/models/` | The shape of each YAML file |
| YAML | `src/main/resources/` | Generated from the CSVs, never edited by hand |
| Database | `core/database/` | SQLite: one Store class per table, everything players and admins change |

Logic with no Bukkit in it (level curve, price of perks, collection tiers, tool levels, loot rolls) sits in small classes of its own so it can be unit tested.

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

Or send it straight to the host over FTP, FTPS or SFTP, with the login in `scripts/deploy.ini` (gitignored, written on the first run):

```
python scripts/deploy.py upload              # build, then the jar and the game data YAML
python scripts/deploy.py upload --all        # and LocalServer's mines.db and the game worlds
python scripts/deploy.py upload --dry-run    # list what would be sent
```

`--db` and `--worlds` pick one of the two. Only the worlds named in `deploy.ini` are sent, never the vanilla overworld, nether or end. Stop the host's server before sending the database or worlds: `--db` replaces every player's progress on the host with the local one, after saving the host's copy to `LocalServer/aurora-backups/`. Files unchanged since the last upload are skipped.

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

Ideas and design notes for later regions are in [DESIGN.md](DESIGN.md).
