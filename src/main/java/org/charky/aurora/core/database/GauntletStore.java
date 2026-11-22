package org.charky.aurora.core.database;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.charky.aurora.features.gauntlet.GauntletProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

// The gauntlet tables: each player's Shards, Cores, best floor, runs and care package
// level, their gear levels, the milestones paid, their starting kit and its layout,
// and the inventory they brought into the gauntlet's world. Its places (lobby, arena, spawns, PvP zone) are in the
// locations table, every name starting with "gauntlet_"
public class GauntletStore {
    public static final String PREFIX = "gauntlet_";

    private final Database db;

    public GauntletStore(Database db) {
        this.db = db;
    }

    // Every player who has played, so the top floors can be shown without a query
    public Map<UUID, GauntletProfile> loadAll() {
        Map<UUID, GauntletProfile> profiles = new HashMap<>();
        db.query("SELECT uuid, shards, cores, best, runs, care FROM gauntlet_players", pst -> {}, rs -> {
            GauntletProfile profile = new GauntletProfile();
            profile.shards = rs.getInt("shards");
            profile.cores = rs.getInt("cores");
            profile.best = rs.getInt("best");
            profile.runs = rs.getInt("runs");
            profile.care = Math.max(1, rs.getInt("care"));
            profiles.put(UUID.fromString(rs.getString("uuid")), profile);
        });
        db.query("SELECT uuid, gear, level FROM gauntlet_gear", pst -> {}, rs -> {
            GauntletProfile profile = profiles.get(UUID.fromString(rs.getString("uuid")));
            if (profile != null) profile.gear.put(rs.getString("gear"), rs.getInt("level"));
        });
        db.query("SELECT uuid, floor FROM gauntlet_milestones", pst -> {}, rs -> {
            GauntletProfile profile = profiles.get(UUID.fromString(rs.getString("uuid")));
            if (profile != null) profile.milestones.add(rs.getInt("floor"));
        });
        db.query("SELECT uuid, item, level FROM gauntlet_setup", pst -> {}, rs -> {
            GauntletProfile profile = profiles.get(UUID.fromString(rs.getString("uuid")));
            if (profile != null) profile.setup.put(rs.getString("item"), rs.getInt("level"));
        });
        db.query("SELECT uuid, layout FROM gauntlet_layouts", pst -> {}, rs -> {
            GauntletProfile profile = profiles.get(UUID.fromString(rs.getString("uuid")));
            if (profile == null || rs.getString("layout") == null) return;
            for (String entry : rs.getString("layout").split(";")) {
                String[] parts = entry.split("=", 2);
                if (parts.length != 2) continue;
                try {
                    profile.layout.put(Integer.parseInt(parts[0]), parts[1]);
                } catch (NumberFormatException ignored) {
                    // A broken entry is left out, the item goes to a free slot
                }
            }
        });
        return profiles;
    }

    public void save(UUID uuid, GauntletProfile profile) {
        int shards = profile.shards, cores = profile.cores, best = profile.best, runs = profile.runs, care = profile.care;
        db.update("INSERT INTO gauntlet_players (uuid, shards, cores, best, runs, care) VALUES (?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(uuid) DO UPDATE SET shards = excluded.shards, cores = excluded.cores, best = excluded.best, "
                + "runs = excluded.runs, care = excluded.care", pst -> {
            pst.setString(1, uuid.toString());
            pst.setInt(2, shards);
            pst.setInt(3, cores);
            pst.setInt(4, best);
            pst.setInt(5, runs);
            pst.setInt(6, care);
        });
    }

    public void saveGear(UUID uuid, String gear, int level) {
        db.update("INSERT INTO gauntlet_gear (uuid, gear, level) VALUES (?, ?, ?) "
                + "ON CONFLICT(uuid, gear) DO UPDATE SET level = excluded.level", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, gear);
            pst.setInt(3, level);
        });
    }

    public void saveSetup(UUID uuid, String item, int level) {
        db.update("INSERT INTO gauntlet_setup (uuid, item, level) VALUES (?, ?, ?) "
                + "ON CONFLICT(uuid, item) DO UPDATE SET level = excluded.level", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, item);
            pst.setInt(3, level);
        });
    }

    public void saveLayout(UUID uuid, Map<Integer, String> layout) {
        StringBuilder text = new StringBuilder();
        new TreeMap<>(layout).forEach((slot, key) -> text.append(slot).append('=').append(key).append(';'));
        db.update("INSERT OR REPLACE INTO gauntlet_layouts (uuid, layout) VALUES (?, ?)", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, text.toString());
        });
    }

    public void saveMilestone(UUID uuid, int floor) {
        db.update("INSERT OR IGNORE INTO gauntlet_milestones (uuid, floor) VALUES (?, ?)", pst -> {
            pst.setString(1, uuid.toString());
            pst.setInt(2, floor);
        });
    }

    // Null when nothing is put away
    public byte[] loadInventory(UUID uuid) {
        byte[][] items = {null};
        db.query("SELECT items FROM gauntlet_inventories WHERE uuid = ?", pst -> pst.setString(1, uuid.toString()),
                rs -> items[0] = rs.getBytes("items"));
        return items[0];
    }

    public void saveInventory(UUID uuid, byte[] items) {
        db.update("INSERT OR REPLACE INTO gauntlet_inventories (uuid, items) VALUES (?, ?)", pst -> {
            pst.setString(1, uuid.toString());
            pst.setBytes(2, items);
        });
    }

    public void removeInventory(UUID uuid) {
        db.update("DELETE FROM gauntlet_inventories WHERE uuid = ?", pst -> pst.setString(1, uuid.toString()));
    }

    // Every gauntlet place by its name less the prefix. Ones in worlds not loaded are left out
    public Map<String, Location> loadPlaces() {
        Map<String, Location> places = new HashMap<>();
        db.query("SELECT * FROM locations WHERE name LIKE ?", pst -> pst.setString(1, PREFIX + "%"), rs -> {
            World world = Bukkit.getWorld(rs.getString("world"));
            if (world == null) return;
            places.put(rs.getString("name").substring(PREFIX.length()), new Location(world, rs.getDouble("x"),
                    rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch")));
        });
        return places;
    }

    public void savePlace(String name, Location location) {
        db.update("INSERT INTO locations (name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(name) DO UPDATE SET world=excluded.world, x=excluded.x, y=excluded.y, z=excluded.z, " +
                "yaw=excluded.yaw, pitch=excluded.pitch", pst -> {
            pst.setString(1, PREFIX + name);
            pst.setString(2, location.getWorld().getName());
            pst.setDouble(3, location.getX());
            pst.setDouble(4, location.getY());
            pst.setDouble(5, location.getZ());
            pst.setFloat(6, location.getYaw());
            pst.setFloat(7, location.getPitch());
        });
    }

    // Every place whose name starts with this, like all the mob spawns
    public void removePlaces(String namePrefix) {
        db.update("DELETE FROM locations WHERE name LIKE ?", pst -> pst.setString(1, PREFIX + namePrefix + "%"));
    }
}
