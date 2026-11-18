package org.charky.aurora.core.database;

import org.charky.aurora.common.util.Box;

import java.util.LinkedHashMap;
import java.util.Map;

// The safe_zones table, boxes where players can't hurt each other
public class SafeZoneStore {
    private final Database db;

    public SafeZoneStore(Database db) {
        this.db = db;
    }

    public Map<String, Box> loadAll() {
        Map<String, Box> zones = new LinkedHashMap<>();
        db.query("SELECT * FROM safe_zones ORDER BY name", pst -> {}, rs -> zones.put(rs.getString("name"),
                new Box(rs.getString("world"), rs.getInt("x1"), rs.getInt("y1"), rs.getInt("z1"),
                        rs.getInt("x2"), rs.getInt("y2"), rs.getInt("z2"))));
        return zones;
    }

    public void save(String name, Box box) {
        db.update("INSERT OR REPLACE INTO safe_zones (name, world, x1, y1, z1, x2, y2, z2) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", pst -> {
            pst.setString(1, name);
            pst.setString(2, box.world());
            pst.setInt(3, box.x1());
            pst.setInt(4, box.y1());
            pst.setInt(5, box.z1());
            pst.setInt(6, box.x2());
            pst.setInt(7, box.y2());
            pst.setInt(8, box.z2());
        });
    }

    public void delete(String name) {
        db.update("DELETE FROM safe_zones WHERE name = ?", pst -> pst.setString(1, name));
    }
}
