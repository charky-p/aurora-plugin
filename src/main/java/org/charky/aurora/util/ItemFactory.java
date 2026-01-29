package org.charky.aurora.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.charky.aurora.Aurora;
import org.charky.aurora.features.loot.CumulativeLootPool;
import org.charky.aurora.features.loot.IndependentLootPool;
import org.charky.aurora.features.loot.ItemDrop;
import org.charky.aurora.interfaces.LootPool;
import org.charky.aurora.models.GearModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemFactory {
    private MiniMessage miniMessage = MiniMessage.miniMessage();

    // create gormak sword: createItem(
    //          Component.text("Gormak Sword").color(NamedTextColor.BROWN),
    //          30, "combat", "overworld", List.of("Sharpness 30", "Looting 6"))
    public ItemStack createItem(Component name, Material material, int tier, String world, List<String> enchantments) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(" [" + tier + "]").color(NamedTextColor.YELLOW)));

            List<Component> lore = new ArrayList<>();
            String materialName = material.name();
            if (materialName.contains("PICKAXE")) {
                lore.add(Component.text("⛏ Mining").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            } else if (materialName.contains("SWORD")) {
                lore.add(Component.text("\uD83D\uDDE1 Combat").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("\uD83D\uDC8E Resource").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }

            lore.add(Component.empty());
            for (String line : enchantments) {
                lore.add(Component.text("» " + line).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                int level = Integer.parseInt(line.replaceAll("[^0-9]", ""));
                if (line.startsWith("Sharpness")) {
                    meta.addEnchant(Enchantment.SHARPNESS, level, true);
                } else if (line.startsWith("Efficiency")) {
                    meta.addEnchant(Enchantment.EFFICIENCY, level, true);
                }
            }

            lore.add(Component.empty());
            lore.add(this.parseWorld(world));
            meta.lore(lore);
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            item.setItemMeta(meta);
        }

        return item;
    }

    public ItemStack createItem(Component name, Material material, String world) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("\uD83D\uDC8E Resource").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

            lore.add(Component.empty());
            lore.add(this.parseWorld(world));
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    public ItemStack createItem(String display, Material material, List<String> loreLines, String world) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(miniMessage.deserialize(display));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("\uD83D\uDC8E Resource").color(NamedTextColor.DARK_GRAY));
            addLore(lore, loreLines, world);
            metaFlags(meta);
            meta.lore(lore);
        });
        return item;
    }

    public ItemStack createGear(String display, Material material, int tier, String type, String world, List<String> loreLines, List<GearModel.EnchantEntry> enchantList) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            Component displayComponent = miniMessage.deserialize(display);
            if (tier == 0) meta.displayName(displayComponent);
            else meta.displayName(displayComponent.append(Component.text(" [T" + tier + "]").color(NamedTextColor.YELLOW)));

            List<Component> lore = new ArrayList<>();
            lore.add(parseType(type));
            lore.add(Component.empty());

            // Enchants
            for (GearModel.EnchantEntry entry : enchantList) {
                lore.add(miniMessage.deserialize(entry.display + " " + entry.level));
                if (entry.enchant != null) {
                    meta.addEnchant(Registry.ENCHANTMENT.get(NamespacedKey.minecraft(entry.enchant.toLowerCase())),
                            entry.level, true);
                }

            }

            if (!enchantList.isEmpty()) lore.add(Component.empty());
            for (String line : loreLines) lore.add(miniMessage.deserialize(line));
            if (!loreLines.isEmpty()) lore.add(Component.empty());
            lore.add(parseWorld(world));
            metaFlags(meta);
            meta.lore(lore);
        });
        return item;
    }

    public LootPool parsePool(List<Map<?, ?>> poolEntries, Map<String, ItemStack> items) {
        IndependentLootPool pool = new IndependentLootPool();
//        for (Map<?, ?> entry : poolEntries) {
//            String itemId = (String) entry.get("id");
//            double chance = ((Number) entry.getOrDefault("chance", 100.0)).doubleValue();
//            int min = (int) entry.getOrDefault("min", 1);
//            int max = (int) entry.getOrDefault("max", 1);
//
//            pool.add(new ItemDrop(items.get(itemId), chance, min, max));
//        }

        return pool;
    }

    private Component parseWorld(String world) {
        if (world.equals("overworld")) {
            return Component.text("Overworld \uD83C\uDF0E")
                            .color(NamedTextColor.GREEN)
                            .decoration(TextDecoration.BOLD, true)
                            .decoration(TextDecoration.ITALIC, false);
        }
        return Component.text("No world");
    }

    // can default tier

    private Component parseType(String type) {
        String display = switch (type) {
            case "combat" -> "\uD83D\uDDE1 Combat";
            case "mining" -> "⛏ Mining";
            default -> "";
        };

        return Component.text(display).color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private void addLore(List<Component> lore, List<String> loreLines, String world) {
        lore.add(Component.empty());
        for (String line : loreLines) lore.add(miniMessage.deserialize(line));
        if (!loreLines.isEmpty()) lore.add(Component.empty());
        lore.add(parseWorld(world));
    }

    private void metaFlags(ItemMeta meta) {
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
    }
}
