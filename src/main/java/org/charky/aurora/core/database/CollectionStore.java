package org.charky.aurora.core.database;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// The collections table, one row per player per resource: how many of it they have
// ever gathered. Unlike the backpack it never goes down
public class CollectionStore {
    private final Database db;

    public CollectionStore(Database db) {
        this.db = db;
    }

    public Map<UUID, Map<String, Long>> loadAll() {
        Map<UUID, Map<String, Long>> results = new HashMap<>();
        db.query("SELECT uuid, item_id, amount FROM collections", pst -> {}, rs -> results
                .computeIfAbsent(UUID.fromString(rs.getString("uuid")), k -> new HashMap<>())
                .put(rs.getString("item_id"), rs.getLong("amount")));
        return results;
    }

    // Only the players given, with their totals as they are now. Takes a copy made
    // on the main thread, so it is safe to save from the database thread
    public void saveAll(Map<UUID, Map<String, Long>> collections) {
        if (collections.isEmpty()) return;

        db.transaction(conn -> {
            try (PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO collections (uuid, item_id, amount) VALUES (?, ?, ?) "
                            + "ON CONFLICT(uuid, item_id) DO UPDATE SET amount = excluded.amount")) {
                for (Map.Entry<UUID, Map<String, Long>> entry : collections.entrySet()) {
                    for (Map.Entry<String, Long> item : entry.getValue().entrySet()) {
                        pst.setString(1, entry.getKey().toString());
                        pst.setString(2, item.getKey());
                        pst.setLong(3, item.getValue());
                        pst.addBatch();
                    }
                }
                pst.executeBatch();
            }
        });
    }
}
