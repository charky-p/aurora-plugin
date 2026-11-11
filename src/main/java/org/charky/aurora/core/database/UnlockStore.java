package org.charky.aurora.core.database;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// The unlocked_gear table, one row per player per unlocked item
public class UnlockStore {
    private final Database db;

    public UnlockStore(Database db) {
        this.db = db;
    }

    public Map<UUID, Set<String>> loadAll() {
        Map<UUID, Set<String>> results = new HashMap<>();
        db.query("SELECT uuid, item_id FROM unlocked_gear", pst -> {}, rs -> results
                .computeIfAbsent(UUID.fromString(rs.getString("uuid")), k -> new HashSet<>())
                .add(rs.getString("item_id")));
        return results;
    }

    // Unlocks never go away, so each one is written once, straight away
    public void add(UUID uuid, String itemId) {
        db.update("INSERT OR IGNORE INTO unlocked_gear (uuid, item_id) VALUES (?, ?)", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, itemId);
        });
    }
}
