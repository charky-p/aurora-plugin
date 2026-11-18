package org.charky.aurora.core.data;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

// What an ACTION component in a GUI file does when clicked, by id. Menus are built
// before most managers exist, so an action is looked up when it is clicked, not when
// the menu loads. Kept in step with ACTIONS in scripts/generate.py
public class MenuActions {
    private static final Set<String> KNOWN = Set.of("backpack", "armory", "spawn", "collections", "ranks", "shop", "warps", "customise", "tools", "howto", "core", "exchange", "leaderboard");

    // Actions written as name:parameter, such as upgrades:coal for a zone's upgrades
    private static final Set<String> KNOWN_WITH_PARAMETER = Set.of("upgrades");
    private static final String PARAMETER_SEP = ":";

    private final Map<String, Consumer<Player>> actions = new HashMap<>();
    private final Map<String, BiConsumer<Player, String>> withParameter = new HashMap<>();

    public MenuActions() {
        register("spawn", player -> player.performCommand("spawn"));
    }

    public void register(String id, Consumer<Player> action) {
        actions.put(id.toLowerCase(), action);
    }

    public void register(String name, BiConsumer<Player, String> action) {
        withParameter.put(name.toLowerCase(), action);
    }

    // Null for an id no action will ever have, so a typo in the data is caught on load
    public Consumer<Player> get(String id) {
        String key = id.toLowerCase();
        int split = key.indexOf(PARAMETER_SEP);
        if (split > 0) {
            String name = key.substring(0, split);
            String parameter = key.substring(split + 1);
            if (!KNOWN_WITH_PARAMETER.contains(name)) return null;
            return player -> {
                BiConsumer<Player, String> action = withParameter.get(name);
                if (action != null) action.accept(player, parameter);
            };
        }
        return KNOWN.contains(key) ? player -> run(key, player) : null;
    }

    public void run(String id, Player player) {
        Consumer<Player> action = actions.get(id.toLowerCase());
        if (action != null) action.accept(player);
    }
}
