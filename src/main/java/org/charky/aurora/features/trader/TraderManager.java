package org.charky.aurora.features.trader;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;

public class TraderManager {
    public DataRegistry registry;
    public TraderManager(Aurora plugin) {
        this.registry = plugin.getRegistry();
    }

    public boolean traderExists(String traderId) {
        return registry.hasTrader(traderId);
    }

    public void spawnTrader(String traderId, World world, double x, double y, double z) {
        spawnTrader(traderId, new Location(world, x, y, z));
    }

    // Keeps the location's yaw, so a trader placed where you stand can face you
    public void spawnTrader(String traderId, Location location) {
        Trader trader = registry.getTrader(traderId);
        if (trader == null) return;

        trader.spawn(location);
    }

    public java.util.List<String> getTraders() {
        return registry.getTraders();
    }

    public void handleTrade(Player player, String traderId) {
        Trader trader = registry.getTrader(traderId);
        if (trader == null) return;

        this.handleMenu(player, trader.getMenuId());
    }

    public void handleMenu(Player player, String menuId) {
        TraderNode menu = registry.getGui(menuId);
        if (menu == null) return;
        menu.onClick(player);
    }
}
