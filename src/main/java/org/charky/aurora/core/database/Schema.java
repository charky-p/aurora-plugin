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
                    "essence INTEGER DEFAULT 0)",

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
                    "yaw FLOAT, pitch FLOAT)"
    );

    // Columns added after a table first shipped, CREATE TABLE IF NOT EXISTS skips
    // tables that already exist so older databases get them here
    private static final List<String[]> ADDED_COLUMNS = List.<String[]>of(
            new String[]{"spawners", "active", "INTEGER DEFAULT 1"}
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
