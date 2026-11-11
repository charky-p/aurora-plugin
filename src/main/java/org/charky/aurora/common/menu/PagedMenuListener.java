package org.charky.aurora.features.player;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class BackpackListener implements Listener {
    // View only, nothing moves in or out, see BackpackMenu
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof BackpackMenu menu)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        menu.handleClick(player, event.getRawSlot());
    }

    // A drag is its own event, without this an item dragged in is gone when it closes
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof BackpackMenu) {
            event.setCancelled(true);
        }
    }
}
