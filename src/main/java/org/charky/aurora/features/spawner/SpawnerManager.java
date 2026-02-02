package org.charky.aurora.features.spawner;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.Database;
import org.charky.aurora.common.util.DataKeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpawnerManager {
    private final Aurora plugin;
    private final DataRegistry registry;
    private final Database db;
    private final Map<String, MobSpawner> spawnerMap = new HashMap<>();

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

    public void addSpawner(String name, String world, int x, int y, int z, String mob, int limit, double radius) {
        if (this.spawnerExists(name)) return;
        if (!this.mobExists(mob)) return;

        MobData data = registry.getMob(mob);
        MobSpawner spawner = new MobSpawner(this.plugin, name, world, x, y, z, data, radius, limit);
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
            // Spawner Id
            entity.getPersistentDataContainer().set(DataKeys.SPAWNER_KEY, PersistentDataType.STRING, spawner);
            entity.getPersistentDataContainer().set(DataKeys.MOB_KEY, PersistentDataType.STRING, data.id());

            entity.customName(data.displayName());
            entity.setCustomNameVisible(true);
            // TODO: add glowing support
            if (!(entity instanceof LivingEntity living)) return;

            data.attributes().forEach((attr, value) -> {
                AttributeInstance instance = living.getAttribute(attr);
                if (instance != null) instance.setBaseValue(value);
            });

            living.setHealth(living.getAttribute(Attribute.MAX_HEALTH).getValue());

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
    }

    public void handleMobDeath(String spawnerName) {
        MobSpawner spawner = spawnerMap.get(spawnerName);
        if (spawner == null) return;
        spawner.decrementCount();
    }

    public void shutdown() {
        for (MobSpawner spawner : spawnerMap.values()) {
            spawner.stop();
        }
    }
}
