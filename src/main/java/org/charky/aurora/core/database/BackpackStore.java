package org.charky.aurora.core.database;

import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.player.BackpackData;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// The backpack_data table, one row per player per resource
public class BackpackStore {
    private final Database db;

    public BackpackStore(Database db) {
        this.db = db;
    }

    public Map<UUID, BackpackData> loadAll(DataRegistry registry) {
        Map<UUID, BackpackData> results = new HashMap<>();
        db.query("SELECT uuid, item_id, amount FROM backpack_data", pst -> {}, rs -> results
                .computeIfAbsent(UUID.fromString(rs.getString("uuid")), k -> new BackpackData(registry))
                .addItem(rs.getString("item_id"), rs.getLong("amount")));
        return results;
    }

    // Takes a copy made on the main thread, so it is safe to save from the database thread
    public void saveAll(Map<UUID, Map<String, Long>> backpacks) {
        db.transaction(conn -> {
            // Wipe to refresh state
            Database.execute(conn, "DELETE FROM backpack_data", pst -> {});

            try (PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO backpack_data (uuid, item_id, amount) VALUES (?, ?, ?)")) {
                for (Map.Entry<UUID, Map<String, Long>> entry : backpacks.entrySet()) {
                    for (Map.Entry<String, Long> item : entry.getValue().entrySet()) {
                        if (item.getValue() <= 0) continue;

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
