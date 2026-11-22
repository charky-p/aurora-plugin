package org.charky.aurora.core.database;

import org.charky.aurora.features.fishing.FishingArea;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// fishing_rod, each player's rod levels, and fishing_areas, the water of each spot
public class FishingStore {
    private final Database db;

    public FishingStore(Database db) {
        this.db = db;
    }

    // Every player's rod, small enough to hold in memory from the start
    public Map<UUID, Map<String, Integer>> loadRods() {
        Map<UUID, Map<String, Integer>> rods = new HashMap<>();
        db.query("SELECT uuid, stat, level FROM fishing_rod", pst -> {}, rs -> rods
                .computeIfAbsent(UUID.fromString(rs.getString("uuid")), k -> new HashMap<>())
                .put(rs.getString("stat"), rs.getInt("level")));
        return rods;
    }

    public void saveLevel(UUID uuid, String stat, int level) {
        db.update("INSERT OR REPLACE INTO fishing_rod (uuid, stat, level) VALUES (?, ?, ?)", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, stat);
            pst.setInt(3, level);
        });
    }

    public Map<String, FishingArea> loadAreas() {
        Map<String, FishingArea> areas = new LinkedHashMap<>();
        db.query("SELECT * FROM fishing_areas ORDER BY name", pst -> {}, rs -> areas.put(rs.getString("name"),
                new FishingArea(rs.getString("name"), rs.getString("spot"), rs.getString("world"),
                        rs.getInt("x1"), rs.getInt("y1"), rs.getInt("z1"), rs.getInt("x2"), rs.getInt("y2"), rs.getInt("z2"))));
        return areas;
    }

    public void saveArea(FishingArea area) {
        db.update("INSERT OR REPLACE INTO fishing_areas (name, spot, world, x1, y1, z1, x2, y2, z2) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", pst -> {
            pst.setString(1, area.name());
            pst.setString(2, area.spot());
            pst.setString(3, area.world());
            pst.setInt(4, area.x1());
            pst.setInt(5, area.y1());
            pst.setInt(6, area.z1());
            pst.setInt(7, area.x2());
            pst.setInt(8, area.y2());
            pst.setInt(9, area.z2());
        });
    }

    public void deleteArea(String name) {
        db.update("DELETE FROM fishing_areas WHERE name = ?", pst -> pst.setString(1, name));
    }
}
