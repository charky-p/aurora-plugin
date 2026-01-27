package org.charky.aurora.models;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockPresets {
    private record PresetData(Material material, LootPool pool) {}
    private static final Map<String, PresetData> PRESETS = new HashMap<>();

    static {
        register("minecraft:cobblestone",
                Material.COBBLESTONE,
                "COBBLESTONE",
                NamedTextColor.YELLOW,
                List.of("Good ol cobble"),
                2, 4);
        register("aurora:peat",
                Material.GRAY_TERRACOTTA,
                "Peat",
                NamedTextColor.GRAY,
                List.of("Disgusting filthy peat", "my lord this stinks"),
                1, 8);
    }

    public static boolean hasBlock(String id) {
        return PRESETS.containsKey(id);
    }

    public static List<MenuItem> getDrops(String id) {
        if (!hasBlock(id)) return new ArrayList<>();
        return new ArrayList<>(PRESETS.get(id).pool().roll());
    }

    public static Material getMaterial(String id) {
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
}
