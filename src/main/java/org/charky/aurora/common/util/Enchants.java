package org.charky.aurora.common.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

// Every enchant level lives in the item's PDC, written by DataRegistry.loadGear.
// Lore is display text only, so an anvil rename can never fake or wipe a level
public final class Enchants {
    private Enchants() {
    }

    public static int level(ItemStack item, String enchantId) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return 0;

        PersistentDataContainer enchants = item.getItemMeta().getPersistentDataContainer()
                .get(DataKeys.ENCHANT_KEY, PersistentDataType.TAG_CONTAINER);
        if (enchants == null) return 0;

        NamespacedKey key = new NamespacedKey("aurora", enchantId.toLowerCase());
        return enchants.getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    // Average over the slots, an empty slot counts as 0, so a full set reads as its level
    public static double average(ItemStack[] items, String enchantId) {
        if (items.length == 0) return 0.0;

        double total = 0;
        for (ItemStack item : items) {
            total += level(item, enchantId);
        }
        return total / items.length;
    }
}
