package org.charky.aurora.managers;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.Aurora;
import org.charky.aurora.models.MenuItem;
import org.charky.aurora.models.MenuNode;
import org.charky.aurora.models.Trader;
import org.charky.aurora.models.TraderMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraderManager {
    public static final String TRADER_KEY_NAME = "trader_id";
    public static final String MENU_KEY_NAME = "menu_id";
    private final Map<String, Trader> traderMap = new HashMap<>();
    private final Map<String, MenuNode> menuMap = new HashMap<>();
    private final NamespacedKey traderKey;
    private final NamespacedKey menuKey;

    public TraderManager(Aurora plugin) {
        this.traderKey = new NamespacedKey(plugin, TRADER_KEY_NAME);
        this.menuKey = new NamespacedKey(plugin, MENU_KEY_NAME);

        // Test
        ItemStack product = new ItemStack(Material.IRON_CHESTPLATE);
        List<ItemStack> costs = new ArrayList<>();
        costs.add(new ItemStack(Material.COBBLESTONE, 99));
        Map<Integer, MenuNode> contents = new HashMap<>();
        MenuItem menuItem = new MenuItem(this, "test_purchase", product, costs);
        contents.put(6, menuItem);
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        TraderMenu menu = new TraderMenu("test_menu", Component.text("TITLE TEST"), 5, contents, item);
        Trader trader = new Trader(this, "test_trader", Component.text("TEST"), EntityType.VILLAGER, menu);
        menuMap.put("test_menu", menu);
        menuMap.put("test_purchase", menuItem);
        traderMap.put("test_trader", trader);
    }

    public NamespacedKey getTraderKey() {
        return this.traderKey;
    }

    public NamespacedKey getMenuKey() {
        return this.menuKey;
    }

    public void spawnTrader(String traderId, World world, double x, double y, double z) {
        Trader trader = traderMap.get(traderId);
        if (trader == null) return;

        trader.spawn(new Location(world, x, y, z));
    }

    public void handleTrade(Player player, String traderId) {
        Trader trader = this.traderMap.get(traderId);
        if (trader == null) return;

        this.handleMenu(player, trader.getMenuId());
    }

    public void handleMenu(Player player, String menuId) {
        MenuNode menu = this.menuMap.get(menuId);
        if (menu == null) return;
        menu.onClick(player);
    }
}
