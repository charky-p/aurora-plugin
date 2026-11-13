package org.charky.aurora.features.spawner;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.database.SpawnerStore;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.features.combat.CustomHealth;

import java.util.*;

public class SpawnerManager {
    private final Aurora plugin;
    private final DataRegistry registry;
    private final SpawnerStore store;
    // Case-insensitive, so commands find a spawner however its name is typed.
    // The store keys on the saved casing, so store calls use the spawner's own name
    private final Map<String, MobSpawner> spawnerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, Integer> cachedCounts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private long cacheExpiry = 0;

    public SpawnerManager(Aurora plugin, SpawnerStore store) {
        this.plugin = plugin;
        this.store = store;
        this.registry = plugin.getRegistry();

        // Add all spawners, (mobs are hardcoded with presets)
        for (MobSpawner spawner : store.loadAll(plugin)) {
            spawnerMap.put(spawner.getName(), spawner);
        }
    }

    // After a registry reload, so spawners use the new mob stats instead of the old copy
    public void reloadMobs() {
        for (MobSpawner spawner : spawnerMap.values()) {
            MobData data = registry.getMob(spawner.getMobName());
            if (data == null) {
                plugin.getLogger().warning("Spawner '" + spawner.getName() + "': mob '"
                        + spawner.getMobName() + "' is gone after reload, it keeps the old one");
                continue;
            }
            spawner.setMob(data);
        }
    }

    public boolean spawnerExists(String name) {
        return this.spawnerMap.containsKey(name);
    }

    public boolean mobExists(String name) {
        return registry.hasMob(name);
    }

    public void addSpawner(String name, String world, int x, int y, int z, String mob, int limit, double radius, int delay) {
        MobData data = registry.getMob(mob);
        if (data == null || this.spawnerExists(name)) return;

        MobSpawner spawner = new MobSpawner(this.plugin, name, world, x, y, z, data, radius, limit, delay, true);
        store.save(spawner);
        spawnerMap.put(name, spawner);
    }

    public void removeSpawner(String spawnerName) {
        // Its spawn task would otherwise keep spawning until the next restart
        MobSpawner spawner = spawnerMap.remove(spawnerName);
        if (spawner == null) return;
        spawner.stop();
        store.remove(spawner.getName());
    }

    public List<String> getSpawners() {
        return new ArrayList<>(spawnerMap.keySet());
    }

    // False when the spawner's world is not loaded
    public boolean tpSpawner(Player player, String spawnerName) {
        MobSpawner spawner = spawnerMap.get(spawnerName);
        if (spawner == null) return false;

        Location location = spawner.getLocation();
        if (location.getWorld() == null) return false;

        player.teleport(location.add(0, 1, 0));
        return true;
    }

    public MobSpawner getSpawner(String spawnerName) {
        return spawnerMap.get(spawnerName);
    }

    public List<String> getSpawnerInfo(String spawnerName) {
        MobSpawner spawner = spawnerMap.get(spawnerName);
        if (spawner == null) return new ArrayList<>();

        return spawner.getInfo();
    }

    // The mob it spawned, null if the mob or the spawner does not exist
    public LivingEntity spawnMob(String mob, Location spawnLocation, String spawner) {
        MobData data = registry.getMob(mob);
        if (data == null || !this.spawnerExists(spawner)) return null;

        Entity spawned = spawnLocation.getWorld().spawn(spawnLocation, data.type().getEntityClass(), entity -> {
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
            // Never saved with the chunk, so a crash can't leave mobs behind to count
            // towards limits, the spawner refills whenever a player is near
            living.setPersistent(false);
        });

        countSpawned(spawner);
        return spawned instanceof LivingEntity living ? living : null;
    }

    public void setMob(String spawnerName, String mobName) {
        MobSpawner spawner = spawnerMap.get(spawnerName);
        if (spawner == null) return;

        MobData mob = registry.getMob(mobName);
        if (mob == null) return;
        store.setMob(spawner.getName(), mobName);
        spawner.setMob(mob);
    }

    public void setRadius(String spawnerName, double radius) {
        MobSpawner spawner = spawnerMap.get(spawnerName);
        if (spawner == null) return;

        store.setRadius(spawner.getName(), radius);
        spawner.setRadius(radius);
    }

    public void setLimit(String spawnerName, int limit) {
        MobSpawner spawner = spawnerMap.get(spawnerName);
        if (spawner == null) return;

        store.setLimit(spawner.getName(), limit);
        spawner.setLimit(limit);
    }

    public void setDelay(String spawnerName, int delay) {
        MobSpawner spawner = spawnerMap.get(spawnerName);
        if (spawner == null) return;

        store.setDelay(spawner.getName(), delay);
        spawner.setDelay(delay);
    }

    public void redefine(String spawnerName, int x, int y, int z) {
        MobSpawner spawner = spawnerMap.get(spawnerName);
        if (spawner == null) return;

        store.redefine(spawner.getName(), x, y, z);
        spawner.redefine(x, y, z);
    }

    public void rename(String oldName, String newName) {
        MobSpawner spawner = spawnerMap.get(oldName);
        if (spawner == null) return;

        store.rename(spawner.getName(), newName);
        spawner.rename(newName);
        spawnerMap.remove(oldName);
        spawnerMap.put(newName, spawner);
    }

    public boolean spawnerToggle(String spawnerName) {
        MobSpawner spawner = spawnerMap.get(spawnerName);
        if (spawner == null) return false;

        boolean active = spawner.toggle();
        store.setActive(spawner.getName(), active);
        return active;
    }

    // Counted from the world, which is the truth since spawner mobs are never saved.
    // A mob knocked off its island is removed here instead of counted, or it holds
    // its spawner's slot for as long as it keeps falling
    private Map<String, Integer> countAll() {
        Map<String, Integer> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.isDead() || !(entity instanceof LivingEntity)) continue;
                String owner = entity.getPersistentDataContainer()
                        .get(DataKeys.SPAWNER_KEY, PersistentDataType.STRING);
                if (owner == null) continue;

                if (entity.getLocation().getY() < world.getMinHeight()) entity.remove();
                else counts.merge(owner, 1, Integer::sum);
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
