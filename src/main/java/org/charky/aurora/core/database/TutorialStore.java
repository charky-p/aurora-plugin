package org.charky.aurora.core.database;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// The tutorial table, the step each player is on, and where the tutorial starts,
// kept in the locations table as "tutorial"
public class TutorialStore {
    private static final String START = "tutorial";

    private final Database db;

    public TutorialStore(Database db) {
        this.db = db;
    }

    // Players still in the tutorial and their step, finished players are not in it
    public Map<UUID, String> loadAll() {
        Map<UUID, String> steps = new HashMap<>();
        db.query("SELECT uuid, step FROM tutorial", pst -> {},
                rs -> steps.put(UUID.fromString(rs.getString("uuid")), rs.getString("step")));
        return steps;
    }

    public void save(UUID uuid, String step) {
        db.update("INSERT INTO tutorial (uuid, step) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET step = excluded.step",
                pst -> {
                    pst.setString(1, uuid.toString());
                    pst.setString(2, step);
                });
    }

    public void remove(UUID uuid) {
        db.update("DELETE FROM tutorial WHERE uuid = ?", pst -> pst.setString(1, uuid.toString()));
    }

    // Null until /aurora tutorial setstart, or if its world is not loaded
    public Location loadStart() {
        Location[] start = {null};
        db.query("SELECT * FROM locations WHERE name = ?", pst -> pst.setString(1, START), rs -> {
            World world = Bukkit.getWorld(rs.getString("world"));
            if (world == null) return;
            start[0] = new Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                    rs.getFloat("yaw"), rs.getFloat("pitch"));
        });
        return start[0];
    }

    public void saveStart(Location location) {
        db.update("INSERT INTO locations (name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(name) DO UPDATE SET world=excluded.world, x=excluded.x, y=excluded.y, z=excluded.z, " +
                "yaw=excluded.yaw, pitch=excluded.pitch", pst -> {
            pst.setString(1, START);
            pst.setString(2, location.getWorld().getName());
            pst.setDouble(3, location.getX());
            pst.setDouble(4, location.getY());
            pst.setDouble(5, location.getZ());
            pst.setFloat(6, location.getYaw());
            pst.setFloat(7, location.getPitch());
        });
    }
}
