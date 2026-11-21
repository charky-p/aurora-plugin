package org.charky.aurora.core.database;

import org.charky.aurora.features.portal.TeleportPad;

import java.util.LinkedHashMap;
import java.util.Map;

// The teleport_pads table, the blocks players step on to unlock a teleport from spawn
public class PadStore {
    private final Database db;

    public PadStore(Database db) {
        this.db = db;
    }

    public Map<String, TeleportPad> loadAll() {
        Map<String, TeleportPad> pads = new LinkedHashMap<>();
        db.query("SELECT * FROM teleport_pads ORDER BY name", pst -> {}, rs -> pads.put(rs.getString("name"),
                new TeleportPad(rs.getString("name"), rs.getString("world"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"))));
        return pads;
    }

    public void save(TeleportPad pad) {
        db.update("INSERT OR REPLACE INTO teleport_pads (name, world, x, y, z) VALUES (?, ?, ?, ?, ?)", pst -> {
            pst.setString(1, pad.name());
            pst.setString(2, pad.world());
            pst.setInt(3, pad.x());
            pst.setInt(4, pad.y());
            pst.setInt(5, pad.z());
        });
    }

    public void delete(String name) {
        db.update("DELETE FROM teleport_pads WHERE name = ?", pst -> pst.setString(1, name));
    }
}
