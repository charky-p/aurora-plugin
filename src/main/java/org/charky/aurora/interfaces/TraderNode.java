package org.charky.aurora.interfaces;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.charky.aurora.Aurora;

public interface TraderNode {
    NamespacedKey MENU_KEY = new NamespacedKey(JavaPlugin.getPlugin(Aurora.class), "menu_id");

    ItemStack getDisplayItem();

    void onClick(Player player);
}
