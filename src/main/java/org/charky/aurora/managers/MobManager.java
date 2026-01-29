package org.charky.aurora.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.Aurora;
import org.charky.aurora.Database;
import org.charky.aurora.features.mob.MobPresets;
import org.charky.aurora.features.mob.MobSpawner;
import org.charky.aurora.features.mob.MobData;

import java.util.HashMap;
import java.util.Map;

public class MobManager {
    private final Aurora plugin;
    private final Database db;
    private final Map<String, MobSpawner> spawnerMap = new HashMap<>();

    public MobManager(Aurora plugin, Database db) {
        this.plugin = plugin;
        this.db = db;

        // Add all spawners, (mobs are hardcoded with presets)
        for (MobSpawner spawner : db.getSpawners(plugin)) {
            spawnerMap.put(spawner.getName(), spawner);
        }

//        for (MobData mob : db.getMobs()) {
//            mobMap.put(mob.getName(), mob);
//        }
    }

    public boolean spawnerExists(String name) {
        return this.spawnerMap.containsKey(name);
    }

    public boolean mobExists(String name) {
        return MobPresets.hasMob(name);
    }

    public void addSpawner(String name, String world, int x, int y, int z, String mob, double radius, int limit) {
        if (this.spawnerExists(name)) return;
        if (!this.mobExists(mob)) return;

        MobData data = MobPresets.get(mob);
        MobSpawner spawner = new MobSpawner(this.plugin, name, world, x, y, z, data, radius, limit);
        db.saveSpawner(spawner);
        spawnerMap.put(name, spawner);
    }

    public void spawnMob(String mob, Location spawnLocation, String spawner) {
        if (!this.spawnerExists(spawner)) return;
        if (!this.mobExists(mob)) return;

        MobData data = MobPresets.get(mob);

        spawnLocation.getWorld().spawn(spawnLocation, data.type().getEntityClass(), entity -> {
            // Spawner Id
            entity.getPersistentDataContainer().set(Aurora.SPAWNER_ID_KEY, PersistentDataType.STRING, spawner);
            entity.getPersistentDataContainer().set(Aurora.MOB_ID_KEY, PersistentDataType.STRING, data.id());

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

        // Remove all spawned mobs
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getPersistentDataContainer().has(Aurora.SPAWNER_ID_KEY, PersistentDataType.STRING)) {
                    entity.remove();
                }
            }
        }
    }
}
