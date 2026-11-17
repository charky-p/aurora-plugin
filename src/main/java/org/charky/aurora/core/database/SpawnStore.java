package org.charky.aurora.core.database;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

// The spawns table, one spawn point per world
public class SpawnStore {
    private final Database db;

    public SpawnStore(Database db) {
        this.db = db;
    }

    public Map<World, Location> loadAll() {
        Map<World, Location> spawnMap = new HashMap<>();
        db.query("SELECT * FROM spawns", pst -> {}, rs -> {
            // A world that is not loaded has nowhere to put the spawn
            World world = Bukkit.getWorld(rs.getString("world"));
            if (world == null) return;

            spawnMap.put(world, new Location(
                    world,
                    rs.getDouble("x"),
                    rs.getDouble("y"),
                    rs.getDouble("z"),
                    rs.getFloat("yaw"),
                    rs.getFloat("pitch")));
        });
        return spawnMap;
    }

    // The world players join in, set with /aurora setspawn main. Null until one is set
    public String loadMain() {
        String[] main = {null};
        db.query("SELECT world FROM spawns WHERE main = 1", pst -> {}, rs -> main[0] = rs.getString("world"));
        return main[0];
    }

    // Only one world is main, the spawn row is saved first so there is one to mark
    public void setMain(World world) {
        db.update("UPDATE spawns SET main = CASE WHEN world = ? THEN 1 ELSE 0 END",
                pst -> pst.setString(1, world.getName()));
    }

    public void save(Location location) {
        db.update("INSERT INTO spawns (world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(world) DO UPDATE SET x=excluded.x, y=excluded.y, z=excluded.z, " +
                "yaw=excluded.yaw, pitch=excluded.pitch", pst -> {
            pst.setString(1, location.getWorld().getName());
            pst.setDouble(2, location.getX());
            pst.setDouble(3, location.getY());
            pst.setDouble(4, location.getZ());
            pst.setFloat(5, location.getYaw());
            pst.setFloat(6, location.getPitch());
        });
    }
}
