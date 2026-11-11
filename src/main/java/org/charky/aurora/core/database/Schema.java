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

            // Mob Spawners
            "CREATE TABLE IF NOT EXISTS spawners (" +
                    "name TEXT PRIMARY KEY, " +
                    "world TEXT, " +
                    "x INTEGER, y INTEGER, z INTEGER, " +
                    "mob TEXT, " +
                    "radius DOUBLE, " +
                    "mob_limit INTEGER, " +
                    "delay INTEGER)",

            // TODO: Traders
            "CREATE TABLE IF NOT EXISTS traders (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "trader TEXT, " +
                    "world TEXT, " +
                    "x DOUBLE, y DOUBLE, z DOUBLE, " +
                    "yaw FLOAT)",

            // Spawns
            "CREATE TABLE IF NOT EXISTS spawns (" +
                    "world TEXT PRIMARY KEY, " +
                    "x DOUBLE, y DOUBLE, z DOUBLE, " +
                    "yaw FLOAT, pitch FLOAT)"
    );

    private Schema() {
    }

    public static void create(Database db) {
        for (String table : TABLES) {
            db.update(table, pst -> {});
        }
    }
}
