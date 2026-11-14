package org.charky.aurora.core.data;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

// What an ACTION component in a GUI file does when clicked, by id. Menus are built
// before most managers exist, so an action is looked up when it is clicked, not when
// the menu loads. Kept in step with ACTIONS in scripts/generate.py
public class MenuActions {
    private static final Set<String> KNOWN = Set.of("backpack", "armory", "spawn", "collections", "ranks", "shop", "warps");

    private final Map<String, Consumer<Player>> actions = new HashMap<>();

    public MenuActions() {
        register("spawn", player -> player.performCommand("spawn"));
    }

    public void register(String id, Consumer<Player> action) {
        actions.put(id.toLowerCase(), action);
    }

    // Null for an id no action will ever have, so a typo in the data is caught on load
    public Consumer<Player> get(String id) {
        String key = id.toLowerCase();
        return KNOWN.contains(key) ? player -> run(key, player) : null;
    }

    public void run(String id, Player player) {
        Consumer<Player> action = actions.get(id.toLowerCase());
        if (action != null) action.accept(player);
    }
}
