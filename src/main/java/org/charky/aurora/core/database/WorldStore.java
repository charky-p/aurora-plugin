package org.charky.aurora.core.database;

import java.util.ArrayList;
import java.util.List;

// The worlds table, the extra void worlds loaded on every startup
public class WorldStore {
    private final Database db;

    public WorldStore(Database db) {
        this.db = db;
    }

    public void add(String name) {
        db.update("INSERT OR IGNORE INTO worlds (name) VALUES (?)", pst -> pst.setString(1, name));
    }

    // Only stops it loading, the world's files stay on disk
    public void remove(String name) {
        db.update("DELETE FROM worlds WHERE name = ?", pst -> pst.setString(1, name));
    }

    public List<String> loadAll() {
        List<String> names = new ArrayList<>();
        db.query("SELECT name FROM worlds ORDER BY name", pst -> {}, rs -> names.add(rs.getString("name")));
        return names;
    }
}
