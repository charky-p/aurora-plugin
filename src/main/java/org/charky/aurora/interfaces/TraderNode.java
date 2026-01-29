package org.charky.aurora.interfaces;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface TraderNode {
    ItemStack getDisplayItem();

    void onClick(Player player);
}
