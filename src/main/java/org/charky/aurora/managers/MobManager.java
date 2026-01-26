package org.charky.aurora.managers;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.Aurora;
import org.charky.aurora.Database;
import org.charky.aurora.models.MobSpawner;
import org.charky.aurora.models.MobData;

import java.util.HashMap;
import java.util.Map;

public class MobManager {
    private Aurora plugin;
    private Database db;
    private final Map<String, MobSpawner> spawnerMap = new HashMap<>();
    private final Map<String, MobData> mobMap = new HashMap<>();

    public MobManager(Aurora plugin, Database db) {
        this.plugin = plugin;
        this.db = db;

        // Add all spawners and mobs from db (not yet, hardcoded)
        MobData mob = new MobData("Test",
                EntityType.ZOMBIE,
                "Test Custom",
                new ItemStack[]{
                        new ItemStack(Material.DIAMOND_HELMET),
                        new ItemStack(Material.GOLDEN_CHESTPLATE),
                        null,
                        new ItemStack(Material.LEATHER_BOOTS),
                        new ItemStack(Material.DIAMOND_SPEAR),
                        new ItemStack(Material.SHIELD)
                },
                new HashMap<>()
        );

        mobMap.put("Test", mob);

        ItemStack[] equipment = new ItemStack[6];
        equipment[0] = new ItemStack(Material.GOLDEN_HELMET);
        equipment[1] = new ItemStack(Material.IRON_CHESTPLATE);
        equipment[2] = new ItemStack(Material.GOLDEN_LEGGINGS);
        equipment[3] = new ItemStack(Material.IRON_BOOTS);
        equipment[4] = new ItemStack(Material.GOLDEN_SWORD);
        equipment[5] = new ItemStack(Material.TOTEM_OF_UNDYING);

        ItemMeta meta = equipment[4].getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 4, true);
            equipment[4].setItemMeta(meta);
        }

        // Attributes for an "Ancient" feel
        Map<Attribute, Double> attributes = new HashMap<>();
        attributes.put(Attribute.MAX_HEALTH, 60.0);
        attributes.put(Attribute.MOVEMENT_SPEED, 0.4);
        attributes.put(Attribute.KNOCKBACK_RESISTANCE, 0.8);
        attributes.put(Attribute.FOLLOW_RANGE, 32.0);

        mob = new MobData("wraith", EntityType.HUSK, "§b§lHollowed Wraith", equipment, attributes);
        mobMap.put(mob.name(), mob);

        for (MobSpawner spawner : db.getSpawners(mobMap, plugin)) {
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
        return this.mobMap.containsKey(name);
    }

    public void addSpawner(String name, String world, int x, int y, int z, String mob, double radius, int limit) {
        if (this.spawnerExists(name)) return;
        if (!this.mobExists(mob)) return;

        MobData data = mobMap.get(mob);
        MobSpawner spawner = new MobSpawner(this.plugin, name, world, x, y, z, data, radius, limit);
        db.saveSpawner(spawner);
        spawnerMap.put(name, spawner);
    }

    public void spawnMob(String mob, Location spawnLocation, String spawner) {
        if (!this.spawnerExists(spawner)) return;
        if (!this.mobExists(mob)) return;

        MobData data = mobMap.get(mob);

        spawnLocation.getWorld().spawn(spawnLocation, data.type().getEntityClass(), entity -> {
            // Spawner Id
            entity.getPersistentDataContainer().set(
                    Aurora.SPAWNER_ID_KEY,
                    PersistentDataType.STRING,
                    spawner
            );

            entity.customName(Component.text(data.name()));
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
                    inv.setHelmet(data.equipment()[0]);
                    inv.setChestplate(data.equipment()[1]);
                    inv.setLeggings(data.equipment()[2]);
                    inv.setBoots(data.equipment()[3]);
                    inv.setItemInMainHand(data.equipment()[4]);
                    inv.setItemInOffHand(data.equipment()[5]);
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
