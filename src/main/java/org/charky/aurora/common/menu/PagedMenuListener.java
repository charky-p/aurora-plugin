package org.charky.aurora.common.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class PagedMenuListener implements Listener {
    // View only, nothing moves in or out, the menu decides what a click does
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof PagedMenu menu)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        menu.handleClick(player, event.getRawSlot(), event.isRightClick());
    }

    // A drag is its own event, without this an item dragged in is gone when it closes
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PagedMenu) {
            event.setCancelled(true);
        }
    }
}
