package org.charky.aurora.features.trader;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
        player.closeInventory();
    }
}
