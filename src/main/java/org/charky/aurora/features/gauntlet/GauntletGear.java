package org.charky.aurora.features.gauntlet;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.models.GearModel;
import org.charky.aurora.features.combat.BowListener;
import org.charky.aurora.features.tool.ToolLevels;
import org.charky.aurora.features.tool.ToolType;
import org.charky.aurora.features.tool.UpgradeZone;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Gauntlet gear as items: drawn from the gear's level, with the same stats the main
// game's combat reads (Sharpness, Power, Defence, Protection, Deflection), so the
// damage rules need nothing of their own. Every piece carries its gear id, and things
// a run hands out (care package loot) carry the run mark, to be taken back after it
public final class GauntletGear {
    public static final NamespacedKey RUN_ITEM_KEY = new NamespacedKey("aurora", "gauntlet_run");
    // The main game's tools and items the starting kit hands out carry this
    private static final NamespacedKey KIT_KEY = new NamespacedKey("aurora", "gauntlet_kit");
    // The world the lore names
    private static final String WORLD = "gauntlet";
    private static final String NO_MATERIAL = "NONE";

    private GauntletGear() {
    }

    public static ItemStack build(DataRegistry registry, GauntletData.Gear gear, GauntletData.GearLevel level) {
        String material = material(gear, level);

        GearModel model = new GearModel();
        String look = material == null ? "" : Character.toUpperCase(material.charAt(0)) + material.substring(1).toLowerCase() + " ";
        if ("Golden ".equals(look) && !gear.armor()) look = "Gold ";
        model.display = "<white><b>" + look + gear.name() + " <gray>(" + level.level() + ")";
        model.material = material == null ? gear.item() : material + "_" + gear.item();
        if (Material.matchMaterial(model.material) == null) model.material = gear.item();
        model.type = gear.armor() || gear.kind().equals("shield") ? "armor" : "combat";
        model.world = WORLD;
        model.enchants = stats(gear, level);
        Map<String, Integer> vanilla = gear.enchantsAt(level.level());
        List<String> lore = new ArrayList<>();
        vanilla.forEach((enchant, amount) -> lore.add("<gray>»<aqua> " + enchantName(enchant, amount)));
        if (!lore.isEmpty()) lore.add("");
        lore.add("<gray>Gauntlet gear, upgraded in the forge");
        lore.add("<dark_gray>Only works in the Gauntlet");
        model.lore = lore;

        ItemStack item = registry.createGear(model);
        item.editMeta(meta -> {
            vanilla.forEach((enchant, amount) -> {
                Enchantment found = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchant));
                if (found != null) meta.addEnchant(found, amount, true);
            });
            meta.setUnbreakable(true);
            // Restock is the Gauntlet's own, GauntletRestock reads it from here
            Integer restock = vanilla.get(GauntletRestock.ENCHANT);
            if (restock != null) meta.getPersistentDataContainer().set(GauntletRestock.RESTOCK_KEY, PersistentDataType.INTEGER, restock);
            meta.getPersistentDataContainer().set(BowListener.GAUNTLET_KEY, PersistentDataType.STRING, gear.id());
            // A bow's, crossbow's or trident's share goes onto what it fires
            meta.getPersistentDataContainer().set(BowListener.SHARE_KEY, PersistentDataType.INTEGER, gear.damage());
            // A thrown trident flies back to whoever threw it
            if (item.getType() == Material.TRIDENT) meta.addEnchant(Enchantment.LOYALTY, 3, true);
        });
        return item;
    }

    // "Wind Burst II" for wind_burst at 2
    static String enchantName(String enchant, int amount) {
        StringBuilder name = new StringBuilder();
        for (String word : enchant.split("_")) {
            if (!name.isEmpty()) name.append(' ');
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name + " " + (amount >= 1 && amount <= ROMAN.length ? ROMAN[amount - 1] : String.valueOf(amount));
    }

    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    // The material a level shows, null for gear that comes one way (a bow)
    static String material(GauntletData.Gear gear, GauntletData.GearLevel level) {
        List<String> materials = gear.materials();
        if (materials.isEmpty() || materials.getFirst().equalsIgnoreCase(NO_MATERIAL)) return null;
        return materials.get(Math.clamp(level.material(), 0, materials.size() - 1));
    }

    static Map<String, Integer> stats(GauntletData.Gear gear, GauntletData.GearLevel level) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        switch (gear.kind()) {
            case "melee" -> stats.put("sharpness", level.melee());
            case "ranged" -> stats.put("power", level.ranged());
            case "thrown" -> {
                stats.put("sharpness", level.melee());
                stats.put("power", level.ranged());
            }
            case "armor" -> {
                stats.put("defence", level.defence());
                stats.put("protection", level.protection());
            }
            case "shield" -> {
                stats.put("deflection", level.deflection());
                stats.put("defence", level.defence());
            }
            default -> {
            }
        }
        stats.values().removeIf(value -> value <= 0);
        return stats;
    }

    // Its gear id, null for anything that is not gauntlet gear
    public static String gearId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(BowListener.GAUNTLET_KEY, PersistentDataType.STRING);
    }

    public static boolean isRunItem(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(RUN_ITEM_KEY, PersistentDataType.BYTE);
    }

    public static void markRunItem(ItemStack item) {
        item.editMeta(meta -> meta.getPersistentDataContainer().set(RUN_ITEM_KEY, PersistentDataType.BYTE, (byte) 1));
    }

    // ---- The main game's own, from the starting kit ----
    // The grapple and the Dash Feather, working as they do in the main game. Marked, so
    // the sweep of main-game things (GauntletWorld) leaves them be

    // A main-game tool with each upgrade its zones sell (the grapple's Thrust and
    // Recharge) at the level given, its lore naming the Gauntlet
    public static ItemStack mainTool(DataRegistry registry, String toolId, int upgrades) {
        ToolType tool = registry.getToolType(toolId);
        if (tool == null) return null;
        Map<String, Integer> stats = new LinkedHashMap<>();
        for (String zone : registry.getUpgradeZoneIds()) {
            for (UpgradeZone.Step step : registry.getUpgradeZone(zone).steps()) {
                if (step.tool().equals(toolId) && !step.stat().equals(ToolLevels.MATERIAL)) stats.put(step.stat(), upgrades);
            }
        }
        return markKit(registry.createTool(tool, stats, WORLD, null));
    }

    // A main-game item as it is (the Dash Feather)
    public static ItemStack mainItem(DataRegistry registry, String itemId) {
        ItemStack item = registry.getAllItems().get(itemId);
        return item == null ? null : markKit(item.clone());
    }

    public static boolean isKit(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(KIT_KEY, PersistentDataType.BYTE);
    }

    private static ItemStack markKit(ItemStack item) {
        item.editMeta(meta -> meta.getPersistentDataContainer().set(KIT_KEY, PersistentDataType.BYTE, (byte) 1));
        return item;
    }
}
