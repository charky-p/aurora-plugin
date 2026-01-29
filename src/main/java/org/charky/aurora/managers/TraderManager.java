package org.charky.aurora.managers;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.features.trader.Trader;
import org.charky.aurora.features.trader.TraderPresets;
import org.charky.aurora.interfaces.TraderNode;

public class TraderManager {
    public TraderManager(Aurora plugin) {

        TraderPresets.init(this);
    }

    public boolean traderExists(String traderId) {
        return TraderPresets.hasTrader(traderId);
    }

    public void spawnTrader(String traderId, World world, double x, double y, double z) {
        Trader trader = TraderPresets.getTrader(traderId);
        if (trader == null) return;

        trader.spawn(new Location(world, x, y, z));
    }

    public void handleTrade(Player player, String traderId) {
        Trader trader = TraderPresets.getTrader(traderId);
        if (trader == null) return;

        this.handleMenu(player, trader.getMenuId());
    }

    public void handleMenu(Player player, String menuId) {
        TraderNode menu = TraderPresets.getNode(menuId);
        if (menu == null) return;
        menu.onClick(player);
    }
}
