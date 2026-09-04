package org.charky.aurora.features.spawner;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.Database;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.features.combat.CustomHealth;

import java.util.*;

public class SpawnerManager {
    private final Aurora plugin;
    private final DataRegistry registry;
    private final Database db;
    private final Map<String, MobSpawner> spawnerMap = new HashMap<>();
    private Map<String, Integer> cachedCounts = new HashMap<>();
    private long cacheExpiry = 0;

    public SpawnerManager(Aurora plugin, Database db) {
        this.plugin = plugin;
        this.db = db;
        this.registry = plugin.getRegistry();

        // Add all spawners, (mobs are hardcoded with presets)
        for (MobSpawner spawner : db.getSpawners(plugin)) {
            spawnerMap.put(spawner.getName(), spawner);
        }
    }

    public boolean spawnerExists(String name) {
        return this.spawnerMap.containsKey(name);
    }

    public boolean mobExists(String name) {
        return registry.hasMob(name);
    }

    public void addSpawner(String name, String world, int x, int y, int z, String mob, int limit, double radius, int delay) {
        if (this.spawnerExists(name)) return;
        if (!this.mobExists(mob)) return;

        MobData data = registry.getMob(mob);
        MobSpawner spawner = new MobSpawner(this.plugin, name, world, x, y, z, data, radius, limit, delay);
        db.saveSpawner(spawner);
        spawnerMap.put(name, spawner);
    }

    public void removeSpawner(String spawnerName) {
        if (!this.spawnerExists(spawnerName)) return;

        db.removeSpawner(spawnerName);
        spawnerMap.remove(spawnerName);
    }

    public List<String> getSpawners() {
        return new ArrayList<>(spawnerMap.keySet());
    }

    public void tpSpawner(Player player, String spawnerName) {
        MobSpawner spawner = spawnerMap.get(spawnerName);
        if (spawner == null) return;

        Location location = spawner.getLocation();
        if (location != null && location.getWorld() != null) {
            player.teleport(location.add(0, 1, 0));
        }
        // TODO: deal with error, maybe nothing, but it is done differently in MineManager tpMine
    }

    public List<String> getSpawnerInfo(String spawnerName) {
        MobSpawner spawner = spawnerMap.get(spawnerName);
        if (spawner == null) return new ArrayList<>();

        return spawner.getInfo();
    }

    public void spawnMob(String mob, Location spawnLocation, String spawner) {
        if (!this.spawnerExists(spawner)) return;
        if (!this.mobExists(mob)) return;

        MobData data = registry.getMob(mob);

        spawnLocation.getWorld().spawn(spawnLocation, data.type().getEntityClass(), entity -> {
            // Remove jockeys
            entity.getPassengers().forEach(entity::removePassenger);

            // Spawner Id
            entity.getPersistentDataContainer().set(DataKeys.SPAWNER_KEY, PersistentDataType.STRING, spawner);
            entity.getPersistentDataContainer().set(DataKeys.MOB_KEY, PersistentDataType.STRING, data.id());

            entity.customName(data.displayName());
            entity.setCustomNameVisible(true);
            // TODO: add glowing support
            if (!(entity instanceof LivingEntity living)) return;

            // Prevent babies TODO: Add baby support
            if (living instanceof Zombie zombie) {
                zombie.setBaby(false);
            }

            if (living instanceof Ageable ageable) {
                ageable.setAdult();
            }

            data.attributes().forEach((attr, value) -> {
                AttributeInstance instance = living.getAttribute(attr);
                if (instance != null) instance.setBaseValue(value);
            });

            CustomHealth.initialise(living, data);

            if (data.equipment() != null) {
                EntityEquipment inv = living.getEquipment();
                if (inv != null) {
                    inv.setHelmet(data.equipment().get(EquipmentSlot.HEAD));
                    inv.setChestplate(data.equipment().get(EquipmentSlot.CHEST));
                    inv.setLeggings(data.equipment().get(EquipmentSlot.LEGS));
                    inv.setBoots(data.equipment().get(EquipmentSlot.FEET));
                    inv.setItemInMainHand(data.equipment().get(EquipmentSlot.HAND));
                    inv.setItemInOffHand(data.equipment().get(EquipmentSlot.OFF_HAND));
                }
            }

            living.setRemoveWhenFarAway(false);
        });

        countSpawned(spawner);
    }

    public void setMob(String spawnerName, String mobName) {
        if (!this.spawnerExists(spawnerName)) return;
        MobSpawner spawner = spawnerMap.get(spawnerName);
        if (spawner == null) return;

        MobData mob = registry.getMob(mobName);
        db.setMob(spawnerName, mobName);
        spawner.setMob(mob);
    }

    public void setRadius(String spawnerName, double radius) {
        if (!this.spawnerExists(spawnerName)) return;
        MobSpawner spawner = spawnerMap.get(spawnerName);
        if (spawner == null) return;

        db.setSpawnerRadius(spawnerName, radius);
        spawner.setRadius(radius);
    }

    public void setLimit(String spawnerName, int limit) {
        if (!this.spawnerExists(spawnerName)) return;
        MobSpawner spawner = spawnerMap.get(spawnerName);
        if (spawner == null) return;

        db.setSpawnerLimit(spawnerName, limit);
        spawner.setLimit(limit);
    }

    public void setDelay(String spawnerName, int delay) {
        if (!this.spawnerExists(spawnerName)) return;
        MobSpawner spawner = spawnerMap.get(spawnerName);
        if (spawner == null) return;

        db.setSpawnerDelay(spawnerName, delay);
        spawner.setDelay(delay);
    }

    public void redefine(String spawnerName, int x, int y, int z) {
        if (!this.spawnerExists(spawnerName)) return;
        MobSpawner spawner = spawnerMap.get(spawnerName);
        if (spawner == null) return;

        db.spawnerRedefine(spawnerName, x, y, z);
        spawner.redefine(x, y, z);
    }

    public void rename(String oldName, String newName) {
        if (!this.spawnerExists(oldName)) return;
        MobSpawner spawner = spawnerMap.get(oldName);
        if (spawner == null) return;

        db.spawnerRename(oldName, newName);
        spawner.rename(newName);
        spawnerMap.remove(oldName);
        spawnerMap.put(newName, spawner);
    }

    public boolean spawnerToggle(String spawnerName) {
        if (!this.spawnerExists(spawnerName)) return false;
        MobSpawner spawner = spawnerMap.get(spawnerName);
        if (spawner == null) return false;

        // TODO: toggle in database too
        return spawner.toggle();
    }

    // TODO: make cache most consistent with server restarts
    private Map<String, Integer> countAll() {
        Map<String, Integer> counts = new HashMap<>();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.isDead() || !(entity instanceof LivingEntity)) continue;
                String owner = entity.getPersistentDataContainer()
                        .get(DataKeys.SPAWNER_KEY, PersistentDataType.STRING);
                if (owner != null) counts.merge(owner, 1, Integer::sum);
            }
        }
        return counts;
    }

    public int countFor(String spawnerName) {
        long now = System.currentTimeMillis();
        if (now >= cacheExpiry) {
            cachedCounts = countAll();
            cacheExpiry = now + 1000;
        }
        return cachedCounts.getOrDefault(spawnerName, 0);
    }

    private void countSpawned(String spawnerName) {
        cachedCounts.merge(spawnerName, 1, Integer::sum);
    }

    public void shutdown() {
        for (MobSpawner spawner : spawnerMap.values()) {
            spawner.stop();
        }
    }
}
