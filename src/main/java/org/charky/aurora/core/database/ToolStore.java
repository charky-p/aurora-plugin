package org.charky.aurora.core.database;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// The player_tools table: the level of every stat of every tool a player has, one
// set per realm. This is where a tool lives, the item in the inventory is only
// drawn from it
public class ToolStore {
    private final Database db;

    public ToolStore(Database db) {
        this.db = db;
    }

    // uuid -> realm -> tool -> stat -> level
    public Map<UUID, Map<String, Map<String, Map<String, Integer>>>> loadAll() {
        Map<UUID, Map<String, Map<String, Map<String, Integer>>>> results = new HashMap<>();
        db.query("SELECT uuid, realm, tool, stat, level FROM player_tools", pst -> {}, rs -> results
                .computeIfAbsent(UUID.fromString(rs.getString("uuid")), k -> new HashMap<>())
                .computeIfAbsent(rs.getString("realm"), k -> new HashMap<>())
                .computeIfAbsent(rs.getString("tool"), k -> new HashMap<>())
                .put(rs.getString("stat"), rs.getInt("level")));
        return results;
    }

    public void save(UUID uuid, String realm, String tool, String stat, int level) {
        db.update("INSERT INTO player_tools (uuid, realm, tool, stat, level) VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT(uuid, realm, tool, stat) DO UPDATE SET level = excluded.level", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, realm);
            pst.setString(3, tool);
            pst.setString(4, stat);
            pst.setInt(5, level);
        });
    }
}
