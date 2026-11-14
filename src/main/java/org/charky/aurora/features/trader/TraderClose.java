package org.charky.aurora.features.trader;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.charky.aurora.common.util.TextUtils;

// The X of a layout, named and behaving like the close button of a PagedMenu
public class TraderClose implements TraderNode {
    @Override
    public ItemStack getDisplayItem(Player player) {
        ItemStack item = new ItemStack(Material.BARRIER);
        item.editMeta(meta -> meta.displayName(TextUtils.parse("<!italic><red>Close")));
        return item;
    }

    @Override
    public void onClick(Player player) {
        close(player);
    }

    // A tick later: closing a menu from inside its own click can leave the
    // client showing it, or the clicked item on the cursor
    public static void close(Player player) {
        Bukkit.getScheduler().runTask(JavaPlugin.getProvidingPlugin(TraderClose.class), () -> {
            if (player.isOnline()) player.closeInventory();
        });
    }
}
