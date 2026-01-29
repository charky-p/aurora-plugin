package org.charky.aurora.features.mob;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.features.loot.MenuItem;
import org.charky.aurora.features.loot.IndependentLootPool;
import org.charky.aurora.features.loot.ItemDrop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobPresets {
    private static final Map<String, MobData> PRESETS = new HashMap<>();

    static {
        register("spear_zombie", EntityType.ZOMBIE, Component.text("Spear Zombie"))
                .equip(EquipmentSlot.HEAD, Material.DIAMOND_HELMET)
                .equip(EquipmentSlot.CHEST, Material.GOLDEN_CHESTPLATE)
                .equip(EquipmentSlot.HAND, Material.DIAMOND_SPEAR)
                .drop(Material.BEDROCK, 20, 64)
                .build();

        register("wraith", EntityType.HUSK, Component.text("Hollowed Wraith").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                .equip(EquipmentSlot.HEAD, Material.GOLDEN_HELMET)
                .equip(EquipmentSlot.CHEST, Material.IRON_CHESTPLATE)
                .equip(EquipmentSlot.LEGS, Material.GOLDEN_LEGGINGS)
                .equip(EquipmentSlot.FEET, Material.IRON_BOOTS)
                .equip(EquipmentSlot.HAND, Material.GOLDEN_SWORD, "Ancient Blade", Map.of(Enchantment.SHARPNESS, 4))
                .attribute(Attribute.MAX_HEALTH, 60.0)
                .attribute(Attribute.MOVEMENT_SPEED, 0.4)
                .attribute(Attribute.KNOCKBACK_RESISTANCE, 0.8)
                .drop(Material.DIAMOND, 100, 1, 5)
                .drop(Material.TOTEM_OF_UNDYING, 20, 1, 1)
                .build();

//        IndependentLootPool pool1 = new IndependentLootPool();
//        pool1.add(new ItemDrop(new ItemStack(Material.BEDROCK), 20, 64));
//        PRESETS.put("spear_zombie", new MobData("spear_zombie",
//                EntityType.ZOMBIE,
//                Component.text("Spear Zombie"),
//                Map.of(
//                        EquipmentSlot.HEAD, new ItemStack(Material.DIAMOND_HELMET),
//                        EquipmentSlot.CHEST, new ItemStack(Material.GOLDEN_CHESTPLATE),
//                        EquipmentSlot.FEET, new ItemStack(Material.LEATHER_BOOTS),
//                        EquipmentSlot.HAND, new ItemStack(Material.DIAMOND_SPEAR),
//                        EquipmentSlot.OFF_HAND, new ItemStack(Material.SHIELD)
//                ),
//                new HashMap<>(),
//                pool1
//        ));
//
//        // --- WRAITH PRESET ---
//        Map<EquipmentSlot, ItemStack> wraithEquip = new HashMap<>();
//        wraithEquip.put(EquipmentSlot.HEAD, new ItemStack(Material.GOLDEN_HELMET));
//        wraithEquip.put(EquipmentSlot.CHEST, new ItemStack(Material.IRON_CHESTPLATE));
//        wraithEquip.put(EquipmentSlot.LEGS, new ItemStack(Material.GOLDEN_LEGGINGS));
//        wraithEquip.put(EquipmentSlot.FEET, new ItemStack(Material.IRON_BOOTS));
//        wraithEquip.put(EquipmentSlot.OFF_HAND, new ItemStack(Material.TOTEM_OF_UNDYING));
//
//        // Create and Enchant the Sword
//        ItemStack sword = new ItemStack(Material.GOLDEN_SWORD);
//        sword.editMeta(meta -> {
//            meta.addEnchant(Enchantment.SHARPNESS, 4, true);
//            meta.displayName(Component.text("Ancient Blade").decoration(TextDecoration.ITALIC, false));
//        });
//        wraithEquip.put(EquipmentSlot.HAND, sword);
//
//        // Attributes
//        Map<Attribute, Double> wraithAttrs = new HashMap<>();
//        wraithAttrs.put(Attribute.MAX_HEALTH, 60.0);
//        wraithAttrs.put(Attribute.MOVEMENT_SPEED, 0.4);
//        wraithAttrs.put(Attribute.KNOCKBACK_RESISTANCE, 0.8);
//        wraithAttrs.put(Attribute.FOLLOW_RANGE, 32.0);
//
//        // Drops
//        IndependentLootPool pool = new IndependentLootPool();
//        pool.add(new ItemDrop(new ItemStack(Material.DIAMOND), 1, 5));
//        pool.add(new ItemDrop(new ItemStack(Material.TOTEM_OF_UNDYING), 20, 1, 1));
//
//        // Build the Record
//        MobData wraith = new MobData(
//                "wraith",
//                EntityType.HUSK,
//                Component.text("Hollowed Wraith")
//                        .color(NamedTextColor.AQUA)
//                        .decorate(TextDecoration.BOLD),
//                wraithEquip,
//                wraithAttrs,
//                pool
//        );

//        PRESETS.put(wraith.id(), wraith);
    }

    public static MobData get(String id) {
        return PRESETS.get(id.toLowerCase());
    }

    public static boolean hasMob(String id) {
        return PRESETS.containsKey(id.toLowerCase());
    }

    public static List<MenuItem> getDrops(String id) {
        if (!hasMob(id)) return new ArrayList<>();
        return new ArrayList<>(get(id).drops().roll());
    }

    private static MobBuilder register(String id, EntityType type, Component name) {
        return new MobBuilder(id, type, name);
    }

    private static class MobBuilder {
        private final String id;
        private final EntityType type;
        private final Component name;
        private final Map<EquipmentSlot, ItemStack> equip = new HashMap<>();
        private final Map<Attribute, Double> attrs = new HashMap<>();
        private final IndependentLootPool pool = new IndependentLootPool();

        public MobBuilder(String id, EntityType type, Component name) {
            this.id = id; this.type = type; this.name = name;
        }

        public MobBuilder equip(EquipmentSlot slot, Material mat) {
            equip.put(slot, new ItemStack(mat));
            return this;
        }

        public MobBuilder equip(EquipmentSlot slot, Material mat, String customName, Map<Enchantment, Integer> enchants) {
            ItemStack item = new ItemStack(mat);
            item.editMeta(meta -> {
                meta.displayName(Component.text(customName).decoration(TextDecoration.ITALIC, false));
                enchants.forEach((ench, lvl) -> meta.addEnchant(ench, lvl, true));
            });
            equip.put(slot, item);
            return this;
        }

        public MobBuilder attribute(Attribute attr, double value) {
            attrs.put(attr, value);
            return this;
        }

        public MobBuilder drop(Material mat, int minAmt, int maxAmt) {
            pool.add(new ItemDrop(new ItemStack(mat), minAmt, maxAmt), 100);
            return this;
        }

        public MobBuilder drop(Material mat, double chance, int minAmt, int maxAmt) {
            pool.add(new ItemDrop(new ItemStack(mat), minAmt, maxAmt), chance);
            return this;
        }

        public void build() {
            PRESETS.put(id, new MobData(id, type, name, equip, attrs, pool));
        }
    }
}
