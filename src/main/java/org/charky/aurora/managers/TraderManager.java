package org.charky.aurora.managers;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.models.*;

public class TraderManager {
    public static final String TRADER_KEY_NAME = "trader_id";
    public static final String MENU_KEY_NAME = "menu_id";
    private final NamespacedKey traderKey;
    private final NamespacedKey menuKey;

    public TraderManager(Aurora plugin) {
        this.traderKey = new NamespacedKey(plugin, TRADER_KEY_NAME);
        this.menuKey = new NamespacedKey(plugin, MENU_KEY_NAME);

        TraderPresets.init(this);
    }

    public NamespacedKey getTraderKey() {
        return this.traderKey;
    }

    public NamespacedKey getMenuKey() {
        return this.menuKey;
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
