package org.charky.aurora.core.database;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// The player_perks table, one row per player per perk with the level they bought
public class PerkStore {
    private final Database db;

    public PerkStore(Database db) {
        this.db = db;
    }

    public Map<UUID, Map<String, Integer>> loadAll() {
        Map<UUID, Map<String, Integer>> results = new HashMap<>();
        db.query("SELECT uuid, perk_id, level FROM player_perks", pst -> {}, rs -> results
                .computeIfAbsent(UUID.fromString(rs.getString("uuid")), k -> new HashMap<>())
                .put(rs.getString("perk_id"), rs.getInt("level")));
        return results;
    }

    // A level is paid for when it is bought, so it is written straight away
    public void save(UUID uuid, String perkId, int level) {
        db.update("INSERT INTO player_perks (uuid, perk_id, level) VALUES (?, ?, ?) " +
                "ON CONFLICT(uuid, perk_id) DO UPDATE SET level = excluded.level", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, perkId);
            pst.setInt(3, level);
        });
    }

    // Everything the player has here, for a full reset
    public void deleteAll(UUID uuid) {
        db.update("DELETE FROM player_perks WHERE uuid = ?", pst -> pst.setString(1, uuid.toString()));
    }
}
