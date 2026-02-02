package org.charky.aurora.features.trader;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TraderClose implements TraderNode {
    @Override
    public ItemStack getDisplayItem() {
        return new ItemStack(Material.BARRIER);
    }

    @Override
    public void onClick(Player player) {
        return;
    }
}
