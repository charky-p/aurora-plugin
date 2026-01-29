package org.charky.aurora.features.trader;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.interfaces.TraderNode;

public class TraderBorder implements TraderNode {
    @Override
    public ItemStack getDisplayItem() {
        return new ItemStack(Material.RED_STAINED_GLASS_PANE);
    }

    @Override
    public void onClick(Player player) {
        return;
    }
}
