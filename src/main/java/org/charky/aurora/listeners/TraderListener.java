package org.charky.aurora.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.managers.TraderManager;

public class TraderListener implements Listener {
    private final TraderManager manager;

    public TraderListener(TraderManager manager) {
        this.manager = manager;
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

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;


        String id = getMenuId(meta);
        if (id == null) return;

        event.setCancelled(true);
        manager.handleMenu(player, id);
    }

    private String getTraderId(Entity entity) {
        return entity.getPersistentDataContainer().get(this.manager.getTraderKey(), PersistentDataType.STRING);
    }

    private String getMenuId(ItemMeta meta) {
        return meta.getPersistentDataContainer().get(this.manager.getMenuKey(), PersistentDataType.STRING);
    }
}
