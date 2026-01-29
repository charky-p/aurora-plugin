package org.charky.aurora.features.trader;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.features.loot.MenuItem;
import org.charky.aurora.interfaces.TraderNode;
import org.charky.aurora.managers.TraderManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraderPresets {
    private static final Map<String, Trader> TRADERS = new HashMap<>();
    private static final Map<String, TraderNode> NODES = new HashMap<>();

    public static void init(TraderManager manager) {
        // --- EXAMPLE: QUARRY MASTER (R4) ---
        register("quarry_master", manager, EntityType.VILLAGER, Component.text("Quarry Master"))
                .mainMenu("quarry_main", Component.text("Quarry Services"), 3)
                // A direct purchase node
                .item(11, "buy_titanium_pick", Material.IRON_PICKAXE, "Titanium Pickaxe [T4]")
                .cost(Material.MANGROVE_LOG, 64)
                .cost(Material.CLAY_BALL, 32)
                .add()
                // A sub-menu node
                .subMenu(15, "quarry_materials", Material.CHEST, "Raw Materials")
                .item(10, "buy_tungsten", Material.COAL_ORE, "Raw Tungsten")
                .cost(Material.MANGROVE_LOG, 16)
                .add()
                .done() // Go back to main builder
                .build();
    }

    private static TraderBuilder register(String id, TraderManager manager, EntityType type, Component name) {
        return new TraderBuilder(id, manager, type, name);
    }

    public static boolean hasTrader(String id) {
        return TRADERS.containsKey(id);
    }

    public static boolean hasNode(String id) {
        return NODES.containsKey(id);
    }

    public static Trader getTrader(String id) { return TRADERS.get(id); }
    public static TraderNode getNode(String id) { return NODES.get(id); }

    // --- The Fluent Builder ---
    public static class TraderBuilder {
        private final String id;
        private final TraderManager manager;
        private final EntityType type;
        private final Component name;
        private TraderMenu mainMenu;

        public TraderBuilder(String id, TraderManager mgr, EntityType type, Component name) {
            this.id = id; this.manager = mgr; this.type = type; this.name = name;
        }

        public MenuBuilder mainMenu(String menuId, Component title, int rows) {
            return new MenuBuilder(this, menuId, title, rows);
        }

        public void build() {
            Trader trader = new Trader(manager, id, name, type, mainMenu);
            TRADERS.put(id, trader);
        }
    }

    public static class MenuBuilder {
        private final TraderBuilder parent;
        private final String menuId;
        private final Component title;
        private final int rows;
        private final Map<Integer, TraderNode> contents = new HashMap<>();

        public MenuBuilder(TraderBuilder parent, String id, Component title, int rows) {
            this.parent = parent; this.menuId = id; this.title = title; this.rows = rows;
        }

        public TradeItemBuilder item(int slot, String id, Material result, String name) {
            return new TradeItemBuilder(this, slot, id, result, name);
        }

        // For nested menus
        public MenuBuilder subMenu(int slot, String id, Material icon, String name) {
            // This is where you'd recursively build another menu
            return this;
        }

        public TraderBuilder done() {
            TraderMenu menu = new TraderMenu(menuId, title, rows, contents, new ItemStack(Material.AIR));
            NODES.put(menuId, menu);
            parent.mainMenu = menu;
            return parent;
        }
    }

    public static class TradeItemBuilder {
        private final MenuBuilder parent;
        private final int slot;
        private final String id;
        private final ItemStack product;
        private final List<MenuItem> costs = new ArrayList<>();

        public TradeItemBuilder(MenuBuilder parent, int slot, String id, Material mat, String name) {
            this.parent = parent; this.slot = slot; this.id = id;
            this.product = new ItemStack(mat);
            product.editMeta(m -> m.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false)));
        }

        public TradeItemBuilder cost(Material mat, int amount) {
            costs.add(new MenuItem(new ItemStack(mat), amount));
            return this;
        }

        public MenuBuilder add() {
            TraderItem item = new TraderItem(parent.parent.manager, id, new MenuItem(product), costs);
            parent.contents.put(slot, item);
            NODES.put(id, item);
            return parent;
        }
    }
}
