package org.charky.aurora.core.data;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.core.models.GuiModel;
import org.charky.aurora.features.player.BackpackManager;
import org.charky.aurora.features.trader.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

// Builds the shop and hub menus from their layouts. A layout is rows of characters,
// B border, I info, X close, space empty, anything else a component
public class GuiLoader {
    private final Logger logger;
    private final Map<String, ItemStack> items;
    private final Map<String, ItemStack> icons;
    private final MenuActions actions;
    private final BackpackManager backpacks;

    public GuiLoader(Logger logger, Map<String, ItemStack> items, Map<String, ItemStack> icons,
                     MenuActions actions, BackpackManager backpacks) {
        this.logger = logger;
        this.items = items;
        this.icons = icons;
        this.actions = actions;
        this.backpacks = backpacks;
    }

    public Map<String, TraderMenu> load(Map<String, GuiModel> data) {
        // Every menu exists before any is filled, so menus can link to each other
        Map<String, TraderMenu> guis = new HashMap<>();
        data.forEach((id, model) -> guis.put(id, new TraderMenu(id,
                MiniMessage.miniMessage().deserialize(model.title), model.rows, new HashMap<>(), icons.get(model.icon))));

        data.forEach((id, model) -> guis.get(id).setContents(contents(id, model, guis)));
        return guis;
    }

    private Map<Integer, TraderNode> contents(String id, GuiModel model, Map<String, TraderMenu> guis) {
        Map<Integer, TraderNode> contents = new HashMap<>();
        for (int row = 0; row < model.layout.size(); row++) {
            String line = model.layout.get(row);
            for (int col = 0; col < line.length(); col++) {
                TraderNode node = node(id, model, line.charAt(col), guis);
                if (node != null) contents.put(row * 9 + col, node);
            }
        }
        return contents;
    }

    // Anything broken in the data becomes a border and is logged, never a crash
    private TraderNode node(String gui, GuiModel model, char key, Map<String, TraderMenu> guis) {
        switch (key) {
            case ' ': return null;
            case 'B': return new TraderBorder();
            case 'I': return new TraderInfo();
            case 'X': return new TraderClose();
        }

        GuiModel.ComponentModel component = model.components.get(key);
        if (component == null) return broken(gui, "character '" + key + "' has no component");

        String id = component.id.toLowerCase();
        return switch (component.type) {
            case "ICON" -> new TraderIcon(icons.get(id));
            case "ACTION" -> {
                Consumer<Player> action = actions.get(id);
                yield action == null ? broken(gui, "unknown action '" + id + "'")
                        : new TraderAction(icons.get(component.icon), action);
            }
            case "GUI" -> guis.containsKey(id) ? guis.get(id) : broken(gui, "unknown menu '" + id + "'");
            default -> shopItem(gui, id, component);
        };
    }

    private TraderNode shopItem(String gui, String id, GuiModel.ComponentModel component) {
        if (!items.containsKey(id)) return broken(gui, "unknown item '" + id + "'");

        List<MenuItem> costs = new ArrayList<>();
        for (GuiModel.CostEntry entry : component.cost) {
            String costId = entry.id.toLowerCase();
            // A skipped cost makes the item cheaper than designed, so say so
            if (!items.containsKey(costId)) {
                logger.severe("GUI ERROR: '" + gui + "' item '" + id + "' costs unknown item '" + costId + "'");
                continue;
            }
            costs.add(new MenuItem(costId, items.get(costId), entry.amount));
        }

        return new TraderItem(backpacks, id, new MenuItem(id, items.get(id), component.amount), costs);
    }

    private TraderNode broken(String gui, String problem) {
        logger.severe("GUI ERROR: '" + gui + "' " + problem);
        return new TraderBorder();
    }
}
