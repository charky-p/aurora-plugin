package org.charky.aurora.core.database;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// The portal_access table, one row per player per place a pass has opened for them
public class AccessStore {
    private final Database db;

    public AccessStore(Database db) {
        this.db = db;
    }

    public Map<UUID, Set<String>> loadAll() {
        Map<UUID, Set<String>> results = new HashMap<>();
        db.query("SELECT uuid, access FROM portal_access", pst -> {}, rs -> results
                .computeIfAbsent(UUID.fromString(rs.getString("uuid")), k -> new HashSet<>())
                .add(rs.getString("access")));
        return results;
    }

    public void add(UUID uuid, String access) {
        db.update("INSERT OR IGNORE INTO portal_access (uuid, access) VALUES (?, ?)", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, access);
        });
    }

    public void remove(UUID uuid, String access) {
        db.update("DELETE FROM portal_access WHERE uuid = ? AND access = ?", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, access);
        });
    }

    public void deleteAll(UUID uuid) {
        db.update("DELETE FROM portal_access WHERE uuid = ?", pst -> pst.setString(1, uuid.toString()));
    }
}
