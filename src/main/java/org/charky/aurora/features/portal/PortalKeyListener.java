package org.charky.aurora.features.portal;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;

// Right clicking a key item held in the hand, the backpack handles its own
public class PortalKeyListener implements Listener {
    private final DataRegistry registry;

    public PortalKeyListener(Aurora plugin) {
        this.registry = plugin.getRegistry();
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        String itemId = DataRegistry.itemId(item);
        if (registry.getUnlock(itemId) == null) return;

        // A tripwire hook or the like must not be placed
        event.setCancelled(true);
        if (PortalKeys.use(event.getPlayer(), registry, itemId)) item.setAmount(item.getAmount() - 1);
    }
}
