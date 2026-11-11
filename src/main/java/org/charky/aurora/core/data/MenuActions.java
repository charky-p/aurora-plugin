package org.charky.aurora.core.data;

import org.bukkit.entity.Player;
import org.charky.aurora.features.armory.ArmoryManager;
import org.charky.aurora.features.player.BackpackManager;

import java.util.Map;
import java.util.function.Consumer;

// What an ACTION component in a GUI file does when clicked, by id
public class MenuActions {
    private final Map<String, Consumer<Player>> actions;

    public MenuActions(BackpackManager backpacks, ArmoryManager armory) {
        this.actions = Map.of(
                "backpack", backpacks::open,
                "armory", armory::open,
                "spawn", player -> player.performCommand("spawn"));
    }

    public Consumer<Player> get(String id) {
        return actions.get(id.toLowerCase());
    }

    public void run(String id, Player player) {
        Consumer<Player> action = get(id);
        if (action != null) action.accept(player);
    }
}
