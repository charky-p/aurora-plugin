package org.charky.aurora.features.trader;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.interfaces.TraderNode;

public class TraderInfo implements TraderNode {
    @Override
    public ItemStack getDisplayItem() {
        return new ItemStack(Material.BEDROCK);
    }

    @Override
    public void onClick(Player player) {
        return;
    }
}
