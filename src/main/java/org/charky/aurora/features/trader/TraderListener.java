package org.charky.aurora.features.trader;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.DataKeys;

import java.util.*;

public class TraderListener implements Listener {
    private final TraderManager manager;
    // A double click lands 3-6 ticks after the first, so anything closer is not
    // a new purchase. Still allows about 4 deliberate clicks a second
    private static final int CLICK_GAP_TICKS = 5;
    private static final Set<ClickType> ACCEPTED_CLICKS =
            EnumSet.of(ClickType.LEFT, ClickType.RIGHT, ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT);

    private final Map<UUID, Integer> lastClickTick = new HashMap<>();

    public TraderListener(Aurora plugin) {
        this.manager = plugin.getTraderManager();
    }

    @EventHandler
    public void onTraderInteract(PlayerInteractEntityEvent event) {
        String id = getTraderId(event.getRightClicked());
        if (id == null) return;

        event.setCancelled(true);
        manager.handleTrade(event.getPlayer(), id);
    }

    @EventHandler
    public void onTraderHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        String id = getTraderId(event.getEntity());
        if (id == null) return;

        event.setCancelled(true);
        manager.handleTrade(player, id);
    }

    // A drag is its own event, without this an item dragged in is gone when it closes
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof TraderMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof TraderMenu menu)) return;

        event.setCancelled(true);

        if (event.getRawSlot() >= event.getInventory().getSize()) return;

        // Double click, number keys, drop and the rest would each buy again
        if (!ACCEPTED_CLICKS.contains(event.getClick())) return;

        TraderNode node = menu.getNode(event.getSlot());
        if (node == null) return;

        int now = Bukkit.getCurrentTick();
        Integer previous = lastClickTick.get(player.getUniqueId());
        if (previous != null && now - previous < CLICK_GAP_TICKS) return;
        lastClickTick.put(player.getUniqueId(), now);

        node.onClick(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastClickTick.remove(event.getPlayer().getUniqueId());
    }

    private String getTraderId(Entity entity) {
        return entity.getPersistentDataContainer().get(DataKeys.TRADER_KEY, PersistentDataType.STRING);
    }
}
