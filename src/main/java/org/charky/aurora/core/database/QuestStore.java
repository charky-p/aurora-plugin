package org.charky.aurora.core.database;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// The quest table, where each player is in the Origin Isles questline. Kept once it
// is done, as "done", so it never starts again
public class QuestStore {
    private final Database db;

    public QuestStore(Database db) {
        this.db = db;
    }

    public Map<UUID, String> loadAll() {
        Map<UUID, String> steps = new HashMap<>();
        db.query("SELECT uuid, step FROM quest", pst -> {},
                rs -> steps.put(UUID.fromString(rs.getString("uuid")), rs.getString("step")));
        return steps;
    }

    public void save(UUID uuid, String step) {
        db.update("INSERT INTO quest (uuid, step) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET step = excluded.step",
                pst -> {
                    pst.setString(1, uuid.toString());
                    pst.setString(2, step);
                });
    }

    public void remove(UUID uuid) {
        db.update("DELETE FROM quest WHERE uuid = ?", pst -> pst.setString(1, uuid.toString()));
    }
}
