package org.charky.aurora.core.database;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// The build_worlds, builders and build_inventories tables
public class BuildStore {
    private final Database db;

    public BuildStore(Database db) {
        this.db = db;
    }

    public Set<String> loadWorlds() {
        Set<String> worlds = new HashSet<>();
        db.query("SELECT world FROM build_worlds", pst -> {}, rs -> worlds.add(rs.getString("world")));
        return worlds;
    }

    public void addWorld(String world) {
        db.update("INSERT OR IGNORE INTO build_worlds (world) VALUES (?)", pst -> pst.setString(1, world));
    }

    public void removeWorld(String world) {
        db.update("DELETE FROM build_worlds WHERE world = ?", pst -> pst.setString(1, world));
    }

    // Each builder's uuid with the name they had when added
    public Map<UUID, String> loadBuilders() {
        Map<UUID, String> builders = new HashMap<>();
        db.query("SELECT uuid, name FROM builders", pst -> {},
                rs -> builders.put(UUID.fromString(rs.getString("uuid")), rs.getString("name")));
        return builders;
    }

    public void addBuilder(UUID uuid, String name) {
        db.update("INSERT OR REPLACE INTO builders (uuid, name) VALUES (?, ?)", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, name);
        });
    }

    public void removeBuilder(UUID uuid) {
        db.update("DELETE FROM builders WHERE uuid = ?", pst -> pst.setString(1, uuid.toString()));
    }

    // Keyed by uuid then kind
    public Map<UUID, Map<String, byte[]>> loadInventories() {
        Map<UUID, Map<String, byte[]>> inventories = new HashMap<>();
        db.query("SELECT uuid, kind, items FROM build_inventories", pst -> {}, rs -> inventories
                .computeIfAbsent(UUID.fromString(rs.getString("uuid")), k -> new HashMap<>())
                .put(rs.getString("kind"), rs.getBytes("items")));
        return inventories;
    }

    public void saveInventory(UUID uuid, String kind, byte[] items) {
        db.update("INSERT OR REPLACE INTO build_inventories (uuid, kind, items) VALUES (?, ?, ?)", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, kind);
            pst.setBytes(3, items);
        });
    }
}
