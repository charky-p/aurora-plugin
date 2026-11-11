package org.charky.aurora.features.player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class BackpackListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof BackpackData) {
            event.setCancelled(true);

            // TODO: Get or put items into backpack
        }
    }

    // A drag is its own event, without this an item dragged in is gone when it closes
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BackpackData) {
            event.setCancelled(true);
        }
    }
}
