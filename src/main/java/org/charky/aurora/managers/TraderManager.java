package org.charky.aurora.managers;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.DataRegistry;
import org.charky.aurora.features.trader.Trader;
import org.charky.aurora.interfaces.TraderNode;

public class TraderManager {
    public DataRegistry registry;
    public TraderManager(Aurora plugin) {
        this.registry = plugin.getRegistry();
    }

    public boolean traderExists(String traderId) {
        return registry.hasTrader(traderId);
    }

    public void spawnTrader(String traderId, World world, double x, double y, double z) {
        Trader trader = registry.getTrader(traderId);
        if (trader == null) return;

        trader.spawn(new Location(world, x, y, z));
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
