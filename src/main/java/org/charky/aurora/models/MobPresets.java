package org.charky.aurora.models;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobPresets {
    private static final Map<String, MobData> PRESETS = new HashMap<>();

    static {
        IndependentLootPool pool1 = new IndependentLootPool();
        pool1.add(new ItemDrop(new ItemStack(Material.BEDROCK), 20, 64));
        PRESETS.put("spear_zombie", new MobData("spear_zombie",
                EntityType.ZOMBIE,
                Component.text("Spear Zombie"),
                Map.of(
                        EquipmentSlot.HEAD, new ItemStack(Material.DIAMOND_HELMET),
                        EquipmentSlot.CHEST, new ItemStack(Material.GOLDEN_CHESTPLATE),
                        EquipmentSlot.FEET, new ItemStack(Material.LEATHER_BOOTS),
                        EquipmentSlot.HAND, new ItemStack(Material.DIAMOND_SPEAR),
                        EquipmentSlot.OFF_HAND, new ItemStack(Material.SHIELD)
                ),
                new HashMap<>(),
                pool1
        ));

        // --- WRAITH PRESET ---
        Map<EquipmentSlot, ItemStack> wraithEquip = new HashMap<>();
        wraithEquip.put(EquipmentSlot.HEAD, new ItemStack(Material.GOLDEN_HELMET));
        wraithEquip.put(EquipmentSlot.CHEST, new ItemStack(Material.IRON_CHESTPLATE));
        wraithEquip.put(EquipmentSlot.LEGS, new ItemStack(Material.GOLDEN_LEGGINGS));
        wraithEquip.put(EquipmentSlot.FEET, new ItemStack(Material.IRON_BOOTS));
        wraithEquip.put(EquipmentSlot.OFF_HAND, new ItemStack(Material.TOTEM_OF_UNDYING));

        // Create and Enchant the Sword
        ItemStack sword = new ItemStack(Material.GOLDEN_SWORD);
        sword.editMeta(meta -> {
            meta.addEnchant(Enchantment.SHARPNESS, 4, true);
            meta.displayName(Component.text("Ancient Blade").decoration(TextDecoration.ITALIC, false));
        });
        wraithEquip.put(EquipmentSlot.HAND, sword);

        // Attributes
        Map<Attribute, Double> wraithAttrs = new HashMap<>();
        wraithAttrs.put(Attribute.MAX_HEALTH, 60.0);
        wraithAttrs.put(Attribute.MOVEMENT_SPEED, 0.4);
        wraithAttrs.put(Attribute.KNOCKBACK_RESISTANCE, 0.8);
        wraithAttrs.put(Attribute.FOLLOW_RANGE, 32.0);

        // Drops
        IndependentLootPool pool = new IndependentLootPool();
        pool.add(new ItemDrop(new ItemStack(Material.DIAMOND), 1, 5));
        pool.add(new ItemDrop(new ItemStack(Material.TOTEM_OF_UNDYING), 20, 1, 1));

        // Build the Record
        MobData wraith = new MobData(
                "wraith",
                EntityType.HUSK,
                Component.text("Hollowed Wraith")
                        .color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.BOLD),
                wraithEquip,
                wraithAttrs,
                pool
        );

        PRESETS.put(wraith.id(), wraith);
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
}
