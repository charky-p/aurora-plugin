package org.charky.aurora.features.trader;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TraderIcon implements TraderNode {
    private final ItemStack icon;

    public TraderIcon(ItemStack icon) {
        this.icon = icon;
    }

    @Override
    public ItemStack getDisplayItem(Player player) {
        return icon == null ? new ItemStack(Material.AIR) : icon.clone();
    }

    @Override
    public void onClick(Player player) {
        // Display only
    }
}