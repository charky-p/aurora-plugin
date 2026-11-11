package org.charky.aurora.core.database;

import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.spawner.MobData;
import org.charky.aurora.features.spawner.MobSpawner;

import java.util.ArrayList;
import java.util.List;

// The spawners table
public class SpawnerStore {
    private final Database db;
    private final DataRegistry registry;

    public SpawnerStore(Database db, DataRegistry registry) {
        this.db = db;
        this.registry = registry;
    }

    public void save(MobSpawner spawner) {
        db.update("INSERT INTO spawners (name, world, x, y, z, mob, radius, mob_limit, delay) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", pst -> {
            pst.setString(1, spawner.getName());
            pst.setString(2, spawner.getWorld());
            pst.setInt(3, spawner.getX());
            pst.setInt(4, spawner.getY());
            pst.setInt(5, spawner.getZ());
            pst.setString(6, spawner.getMobName());
            pst.setDouble(7, spawner.getRadius());
            pst.setInt(8, spawner.getLimit());
            pst.setInt(9, spawner.getDelay());
        });
    }

    public void remove(String spawnerName) {
        db.update("DELETE FROM spawners WHERE name = ?", pst -> pst.setString(1, spawnerName));
    }

    // Spawners whose mob is no longer in the registry are skipped
    public List<MobSpawner> loadAll(Aurora plugin) {
        List<MobSpawner> spawners = new ArrayList<>();
        db.query("SELECT * FROM spawners", pst -> {}, rs -> {
            MobData data = registry.getMob(rs.getString("mob"));
            if (data == null) return;

            spawners.add(new MobSpawner(
                    plugin,
                    rs.getString("name"),
                    rs.getString("world"),
                    rs.getInt("x"),
                    rs.getInt("y"),
                    rs.getInt("z"),
                    data,
                    rs.getDouble("radius"),
                    rs.getInt("mob_limit"),
                    rs.getInt("delay"),
                    rs.getInt("active") != 0));
        });
        return spawners;
    }

    public void setMob(String spawnerName, String mobName) {
        updateColumn("mob", spawnerName, pst -> pst.setString(1, mobName));
    }

    public void setRadius(String spawnerName, double radius) {
        updateColumn("radius", spawnerName, pst -> pst.setDouble(1, radius));
    }

    public void setLimit(String spawnerName, int limit) {
        updateColumn("mob_limit", spawnerName, pst -> pst.setInt(1, limit));
    }

    public void setActive(String spawnerName, boolean active) {
        updateColumn("active", spawnerName, pst -> pst.setInt(1, active ? 1 : 0));
    }

    public void setDelay(String spawnerName, int delay) {
        updateColumn("delay", spawnerName, pst -> pst.setInt(1, delay));
    }

    public void redefine(String spawnerName, int x, int y, int z) {
        db.update("UPDATE spawners SET x = ?, y = ?, z = ? WHERE name = ?", pst -> {
            pst.setInt(1, x);
            pst.setInt(2, y);
            pst.setInt(3, z);
            pst.setString(4, spawnerName);
        });
    }

    public void rename(String oldName, String newName) {
        updateColumn("name", oldName, pst -> pst.setString(1, newName));
    }

    // Column names are only ever our own constants, never player input
    private void updateColumn(String column, String spawnerName, Database.SqlBinder value) {
        db.update("UPDATE spawners SET " + column + " = ? WHERE name = ?", pst -> {
            value.bind(pst);
            pst.setString(2, spawnerName);
        });
    }
}
