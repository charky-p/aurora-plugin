# Aurora

Paper 1.21 sky-mining RPG plugin (Java 21, Maven, SQLite).

## Goal: launch a working demo
- Priority is a stable, playable demo (Origin Isles), not new features.
- Prefer finishing, fixing and polishing what exists. Flag scope creep; ask before adding features.

## Game data
- `scripts/data/*.csv` is the source of truth for all game data (gear, mobs, blocks, shops).
- `scripts/generate.py` generates the YAML in `src/main/resources/` from it. Edit the CSV, never the YAML.

## Build and run
- `mvn package` builds and copies the jar to `LocalServer/plugins/`.
- `LocalServer/start.bat` runs the local test server (gitignored). Check its logs to verify changes; I test in-game.

## Rules
- I own design and features: ask before changing how the game plays (mechanics, balance targets, new features).
- Claude implements the code, core systems included. Match my style; explain non-obvious changes.
- Never commit, push, tag or delete without my go-ahead.
- Claude's commits: prefix the subject with `[C] `. No Co-Authored-By or other trailers.
