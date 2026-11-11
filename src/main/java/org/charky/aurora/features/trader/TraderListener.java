package org.charky.aurora.features.trader;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.DataKeys;

import java.util.*;

public class TraderListener implements Listener {
    private final TraderManager manager;
    private final Aurora plugin;
    private final Set<UUID> processing = new HashSet<>();
    private final Map<UUID, Long> clickCooldowns = new HashMap<>();

    public TraderListener(Aurora plugin) {
        this.manager = plugin.getTraderManager();
        this.plugin = plugin;
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

        UUID uuid = player.getUniqueId();
        if (processing.contains(uuid)) return;

        // TODO: try and fix the spam buy packet queue
//        UUID uuid = player.getUniqueId();
//        long currentTick = Bukkit.getCurrentTick();
//        long previousTick = clickCooldowns.getOrDefault(uuid, 0L);
//
//        if (currentTick - previousTick < 3) return;
//
//        clickCooldowns.put(uuid, currentTick);

        TraderNode node = menu.getNode(event.getSlot());
        if (node == null) return;

        processing.add(uuid);
        node.onClick(player);
        Bukkit.getScheduler().runTaskLater(plugin, () -> processing.remove(uuid), 2L);
    }

    private String getTraderId(Entity entity) {
        return entity.getPersistentDataContainer().get(DataKeys.TRADER_KEY, PersistentDataType.STRING);
    }

    private String getMenuId(ItemMeta meta) {
        return meta.getPersistentDataContainer().get(DataKeys.MENU_KEY, PersistentDataType.STRING);
    }
}
