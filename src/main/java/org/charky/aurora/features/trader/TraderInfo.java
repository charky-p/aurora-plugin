package org.charky.aurora.features.trader;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TraderInfo implements TraderNode {
    @Override
    public ItemStack getDisplayItem(Player player) {
        return new ItemStack(Material.BEDROCK);
    }

    @Override
    public void onClick(Player player) {
        return;
    }
}
