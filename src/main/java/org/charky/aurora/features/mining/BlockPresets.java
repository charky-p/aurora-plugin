package org.charky.aurora.features.mining;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.Aurora;
import org.charky.aurora.features.loot.MenuItem;
import org.charky.aurora.features.loot.IndependentLootPool;
import org.charky.aurora.features.loot.ItemDrop;
import org.charky.aurora.interfaces.LootPool;
import org.charky.aurora.util.ItemFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockPresets {
    private record PresetData(Material material, LootPool pool) {}
    private static final Map<String, PresetData> PRESETS = new HashMap<>();

    public BlockPresets(ItemFactory itemFactory) {
        ItemStack item = itemFactory.createItem(Component.text("Cobblestone").color(NamedTextColor.GRAY),
                Material.COBBLESTONE, "overworld");
        register("aurora:cobblestone", Material.COBBLESTONE, item, 1, 1);
        register("aurora:stone", Material.STONE, item, 2, 4);
        item = itemFactory.createItem(Component.text("Coal").color(NamedTextColor.GRAY), Material.COAL, "overworld");
        register("aurora:coal_ore", Material.COAL_ORE, item, 1, 1);
        register("aurora:coal_block", Material.COAL_BLOCK, item, 6, 9);
        item = itemFactory.createItem(Component.text("Copper").color(NamedTextColor.GRAY), Material.COPPER_INGOT, "overworld");
        register("aurora:copper_ore", Material.COPPER_ORE, item, 1, 1);
        register("aurora:copper_block", Material.COPPER_BLOCK, item, 6, 9);
        item = itemFactory.createItem(Component.text("Iron").color(NamedTextColor.GRAY), Material.IRON_INGOT, "overworld");
        register("aurora:iron_ore", Material.IRON_ORE, item, 1, 1);
        register("aurora:iron_block", Material.IRON_BLOCK, item, 6, 9);
        item = itemFactory.createItem(Component.text("Gold").color(NamedTextColor.GRAY), Material.GOLD_INGOT, "overworld");
        register("aurora:gold_ore", Material.GOLD_ORE, item, 1, 1);
        register("aurora:gold_block", Material.GOLD_BLOCK, item, 6, 9);
    }
//    static {
//        register("minecraft:cobblestone",
//                Material.COBBLESTONE,
//                "COBBLESTONE",
//                NamedTextColor.YELLOW,
//                List.of("Good ol cobble"),
//                2, 4);
//        register("aurora:peat",
//                Material.GRAY_TERRACOTTA,
//                "Peat",
//                NamedTextColor.GRAY,
//                List.of("Disgusting filthy peat", "my lord this stinks"),
//                1, 8);
//    }

    public boolean hasBlock(String id) {
        return PRESETS.containsKey(id);
    }

    public List<MenuItem> getDrops(String id) {
        if (!hasBlock(id)) return new ArrayList<>();
        return new ArrayList<>(PRESETS.get(id).pool().roll());
    }

    public Material getMaterial(String id) {
        if (!hasBlock(id)) return Material.AIR;
        return PRESETS.get(id).material();
    }

    private static void register(String id, Material material, String displayName,
                                 NamedTextColor color, List<String> loreStrings, int min, int max) {
        ItemStack item = new ItemStack(material);

        item.editMeta(meta -> {
            meta.displayName(Component.text(displayName, color).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = loreStrings.stream()
                    .map(text -> (Component) Component.text(text, NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false))
                    .toList();
            meta.lore(lore);
        });

        IndependentLootPool pool = new IndependentLootPool(new ItemDrop(item, min, max));
        PRESETS.put(id, new PresetData(material, pool));
    }

    private static void register(String id, Material material, ItemStack item, int min, int max) {
        IndependentLootPool pool = new IndependentLootPool(new ItemDrop(item, min, max));
        PRESETS.put(id, new PresetData(material, pool));
    }
}
