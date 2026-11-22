package org.charky.aurora.core.database;

import java.util.List;

// Every table, created on startup if it does not exist yet
public final class Schema {
    private static final List<String> TABLES = List.of(
            "CREATE TABLE IF NOT EXISTS mines (" +
                    "name TEXT PRIMARY KEY, " +
                    "world TEXT, " +
                    "x1 INTEGER, y1 INTEGER, z1 INTEGER, " +
                    "x2 INTEGER, y2 INTEGER, z2 INTEGER, " +
                    "reset_threshold DOUBLE, " +
                    "delay INTEGER, " +
                    "tpX DOUBLE, tpY DOUBLE, tpZ DOUBLE)",

            // Blocks that map to mines
            "CREATE TABLE IF NOT EXISTS blocks (" +
                    "mine TEXT, " +
                    "material TEXT, " +
                    "percent DOUBLE, " +
                    "id TEXT)",

            // Player data for scoreboard
            "CREATE TABLE IF NOT EXISTS playerData (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "rank TEXT DEFAULT 'MEMBER', " +
                    "level INTEGER DEFAULT 0, " +
                    "kills INTEGER DEFAULT 0, " +
                    "mined INTEGER DEFAULT 0, " +
                    "essence INTEGER DEFAULT 0, " +
                    "xp INTEGER DEFAULT 0, " +
                    "playtime INTEGER DEFAULT 0)",

            // Shop ranks each player has bought
            "CREATE TABLE IF NOT EXISTS player_ranks (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "rank_id TEXT NOT NULL, " +
                    "PRIMARY KEY (uuid, rank_id))",

            // Cosmetics each player has bought or been given, and the one worn in each slot
            "CREATE TABLE IF NOT EXISTS player_cosmetics (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "cosmetic_id TEXT NOT NULL, " +
                    "PRIMARY KEY (uuid, cosmetic_id))",

            "CREATE TABLE IF NOT EXISTS player_equipped (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "slot TEXT NOT NULL, " +
                    "cosmetic_id TEXT NOT NULL, " +
                    "PRIMARY KEY (uuid, slot))",

            // Tools live here, not on the item: every stat of every tool, per realm
            "CREATE TABLE IF NOT EXISTS player_tools (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "realm TEXT NOT NULL, " +
                    "tool TEXT NOT NULL, " +
                    "stat TEXT NOT NULL, " +
                    "level INTEGER NOT NULL, " +
                    "PRIMARY KEY (uuid, realm, tool, stat))",

            // The level of each perk a player has bought
            "CREATE TABLE IF NOT EXISTS player_perks (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "perk_id TEXT NOT NULL, " +
                    "level INTEGER NOT NULL, " +
                    "PRIMARY KEY (uuid, perk_id))",

            // How many of each resource a player has ever gathered
            "CREATE TABLE IF NOT EXISTS collections (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "item_id TEXT NOT NULL, " +
                    "amount BIGINT NOT NULL, " +
                    "PRIMARY KEY (uuid, item_id))",

            // Player backpacks
            "CREATE TABLE IF NOT EXISTS backpack_data (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "item_id TEXT NOT NULL, " +
                    "amount BIGINT NOT NULL, " +
                    "PRIMARY KEY (uuid, item_id))",

            // Gear each player has unlocked in the Armory
            "CREATE TABLE IF NOT EXISTS unlocked_gear (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "item_id TEXT NOT NULL, " +
                    "PRIMARY KEY (uuid, item_id))",

            // Mob Spawners
            "CREATE TABLE IF NOT EXISTS spawners (" +
                    "name TEXT PRIMARY KEY, " +
                    "world TEXT, " +
                    "x INTEGER, y INTEGER, z INTEGER, " +
                    "mob TEXT, " +
                    "radius DOUBLE, " +
                    "mob_limit INTEGER, " +
                    "delay INTEGER, " +
                    "active INTEGER DEFAULT 1)",

            // Trader placements, so traders can be respawned at their spots instead
            // of placed by hand on every server
            "CREATE TABLE IF NOT EXISTS traders (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "trader TEXT, " +
                    "world TEXT, " +
                    "x DOUBLE, y DOUBLE, z DOUBLE, " +
                    "yaw FLOAT)",

            // Extra void worlds, created by command and loaded on every startup
            "CREATE TABLE IF NOT EXISTS worlds (" +
                    "name TEXT PRIMARY KEY)",

            // Portals, a box that sends whoever walks in to its destination
            "CREATE TABLE IF NOT EXISTS portals (" +
                    "name TEXT PRIMARY KEY, " +
                    "world TEXT, " +
                    "x1 INTEGER, y1 INTEGER, z1 INTEGER, " +
                    "x2 INTEGER, y2 INTEGER, z2 INTEGER, " +
                    "dest_world TEXT, " +
                    "dest_x DOUBLE, dest_y DOUBLE, dest_z DOUBLE, " +
                    "dest_yaw FLOAT, dest_pitch FLOAT)",

            // Spawns
            "CREATE TABLE IF NOT EXISTS spawns (" +
                    "world TEXT PRIMARY KEY, " +
                    "x DOUBLE, y DOUBLE, z DOUBLE, " +
                    "yaw FLOAT, pitch FLOAT)",

            // Where a pass has let each player go, the ids locked portals and warps ask for
            "CREATE TABLE IF NOT EXISTS portal_access (" +
                    "uuid TEXT, " +
                    "access TEXT, " +
                    "PRIMARY KEY (uuid, access))",

            // The step each player is on in the first-join tutorial, deleted once done
            "CREATE TABLE IF NOT EXISTS tutorial (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "step TEXT)",

            // Where each player is in the Origin Isles questline, "done" once finished
            "CREATE TABLE IF NOT EXISTS quest (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "step TEXT)",

            // The gauntlet, kept for the person whatever loadout is in play: Shards and
            // Cores to spend, the best floor cleared, runs played and the care package level
            "CREATE TABLE IF NOT EXISTS gauntlet_players (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "shards INTEGER DEFAULT 0, " +
                    "cores INTEGER DEFAULT 0, " +
                    "best INTEGER DEFAULT 0, " +
                    "runs INTEGER DEFAULT 0, " +
                    "care INTEGER DEFAULT 1)",

            // Each piece of gauntlet gear a player has unlocked and its level
            "CREATE TABLE IF NOT EXISTS gauntlet_gear (" +
                    "uuid TEXT, " +
                    "gear TEXT, " +
                    "level INTEGER, " +
                    "PRIMARY KEY (uuid, gear))",

            // Gauntlet milestones paid out, by floor
            "CREATE TABLE IF NOT EXISTS gauntlet_milestones (" +
                    "uuid TEXT, " +
                    "floor INTEGER, " +
                    "PRIMARY KEY (uuid, floor))",

            // Each starting kit item a player has bought and its level
            "CREATE TABLE IF NOT EXISTS gauntlet_setup (" +
                    "uuid TEXT, " +
                    "item TEXT, " +
                    "level INTEGER, " +
                    "PRIMARY KEY (uuid, item))",

            // Where each player keeps their gauntlet setup in the inventory, slot=item;...
            "CREATE TABLE IF NOT EXISTS gauntlet_layouts (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "layout TEXT)",

            // The inventory a player brought into the gauntlet's world, handed back on the way out
            "CREATE TABLE IF NOT EXISTS gauntlet_inventories (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "items BLOB)",

            // Named spots that belong to no world's spawn, like where the tutorial starts
            "CREATE TABLE IF NOT EXISTS locations (" +
                    "name TEXT PRIMARY KEY, " +
                    "world TEXT, " +
                    "x DOUBLE, y DOUBLE, z DOUBLE, " +
                    "yaw FLOAT, pitch FLOAT)",

            // Worlds where builders build in creative, set with /aurora world build
            "CREATE TABLE IF NOT EXISTS build_worlds (" +
                    "world TEXT PRIMARY KEY)",

            // Players who may go to build worlds and build there without OP
            "CREATE TABLE IF NOT EXISTS builders (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "name TEXT)",

            // A builder's two inventories: the survival one kept while they build, and
            // the creative one kept while they play. kind is survival or build
            "CREATE TABLE IF NOT EXISTS build_inventories (" +
                    "uuid TEXT, " +
                    "kind TEXT, " +
                    "items BLOB, " +
                    "PRIMARY KEY (uuid, kind))",

            // Boxes where players can't hurt each other, PvP is on everywhere else
            "CREATE TABLE IF NOT EXISTS safe_zones (" +
                    "name TEXT PRIMARY KEY, " +
                    "world TEXT, " +
                    "x1 INTEGER, y1 INTEGER, z1 INTEGER, " +
                    "x2 INTEGER, y2 INTEGER, z2 INTEGER)",

            // What players sent with /suggest, read with /aurora suggestions
            "CREATE TABLE IF NOT EXISTS suggestions (" +
                    "id INTEGER PRIMARY KEY, " +
                    "uuid TEXT, " +
                    "name TEXT, " +
                    "text TEXT, " +
                    "time INTEGER)",

            // Which loadout each player has in play, and the id main is kept under while put away
            "CREATE TABLE IF NOT EXISTS loadouts (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "shadow TEXT, " +
                    "active TEXT)",

            // Each loadout's inventory and ender chest from when it was last put away
            "CREATE TABLE IF NOT EXISTS loadout_inventories (" +
                    "uuid TEXT, " +
                    "loadout TEXT, " +
                    "items BLOB, " +
                    "ender BLOB, " +
                    "PRIMARY KEY (uuid, loadout))",

            // Every id a loadout is put away under, whose it is and which one, so the
            // leaderboards tell a put-away loadout from a player
            "CREATE TABLE IF NOT EXISTS loadout_homes (" +
                    "home TEXT PRIMARY KEY, " +
                    "uuid TEXT, " +
                    "loadout TEXT)",

            // Teleport pads, the block a player steps on to unlock it from spawn
            "CREATE TABLE IF NOT EXISTS teleport_pads (" +
                    "name TEXT PRIMARY KEY, " +
                    "world TEXT, " +
                    "x INTEGER, y INTEGER, z INTEGER)",

            // Each player's fishing rod: its level in each stat
            "CREATE TABLE IF NOT EXISTS fishing_rod (" +
                    "uuid TEXT, " +
                    "stat TEXT, " +
                    "level INTEGER, " +
                    "PRIMARY KEY (uuid, stat))",

            // Every mute, ban and kick, kept after it ends as the player's history.
            // until is 0 for one with no end
            "CREATE TABLE IF NOT EXISTS punishments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid TEXT, " +
                    "name TEXT, " +
                    "type TEXT, " +
                    "reason TEXT, " +
                    "by_name TEXT, " +
                    "created INTEGER, " +
                    "until INTEGER, " +
                    "lifted INTEGER DEFAULT 0)",

            // The water of a fishing spot, a box the hook has to land in
            "CREATE TABLE IF NOT EXISTS fishing_areas (" +
                    "name TEXT PRIMARY KEY, " +
                    "spot TEXT, " +
                    "world TEXT, " +
                    "x1 INTEGER, y1 INTEGER, z1 INTEGER, " +
                    "x2 INTEGER, y2 INTEGER, z2 INTEGER)"
    );

    // Columns added after a table first shipped, CREATE TABLE IF NOT EXISTS skips
    // tables that already exist so older databases get them here
    private static final List<String[]> ADDED_COLUMNS = List.<String[]>of(
            new String[]{"spawners", "active", "INTEGER DEFAULT 1"},
            new String[]{"portals", "required_tag", "TEXT"},
            new String[]{"playerData", "xp", "INTEGER DEFAULT 0"},
            new String[]{"playerData", "playtime", "INTEGER DEFAULT 0"},
            new String[]{"spawns", "main", "INTEGER DEFAULT 0"}
    );

    private Schema() {
    }

    public static void create(Database db) {
        for (String table : TABLES) {
            db.update(table, pst -> {});
        }

        for (String[] column : ADDED_COLUMNS) {
            if (!hasColumn(db, column[0], column[1])) {
                db.update("ALTER TABLE " + column[0] + " ADD COLUMN " + column[1] + " " + column[2], pst -> {});
            }
        }
    }

    private static boolean hasColumn(Database db, String table, String column) {
        boolean[] found = {false};
        db.query("PRAGMA table_info(" + table + ")", pst -> {}, rs -> {
            if (rs.getString("name").equalsIgnoreCase(column)) found[0] = true;
        });
        return found[0];
    }
}
