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
import org.charky.aurora.features.trader.Trader;
import org.charky.aurora.features.trader.TraderItem;
import org.charky.aurora.managers.TraderManager;
import org.charky.aurora.features.trader.TraderMenu;

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

    // TODO: Add drag stop, as u can drag items into inventory
    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getView().getTopInventory().getHolder() instanceof TraderMenu) {

            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                if (event.getClick().isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }

            event.setCancelled(true);

            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType().isAir()) return;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            String id = getMenuId(meta);
            if (id == null) return;

            manager.handleMenu(player, id);
        }
    }

    private String getTraderId(Entity entity) {
        return entity.getPersistentDataContainer().get(Trader.TRADER_KEY, PersistentDataType.STRING);
    }

    private String getMenuId(ItemMeta meta) {
        return meta.getPersistentDataContainer().get(TraderItem.MENU_KEY, PersistentDataType.STRING);
    }
}
