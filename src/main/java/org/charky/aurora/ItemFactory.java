package org.charky.aurora;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemFactory {

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

}
