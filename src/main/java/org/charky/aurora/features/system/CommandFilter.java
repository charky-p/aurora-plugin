package org.charky.aurora.features.system;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.Set;

// What a player sees when they type / and press tab. Staff see everything, everyone
// else only the commands of the game itself, nothing from other plugins or vanilla.
// This hides commands, it does not block them: permissions do that
public class CommandFilter implements Listener {
    public static final String STAFF_PERMISSION = "aurora.admin";

    // Every player command in plugin.yml with its aliases, kept in step with it
    private static final Set<String> PLAYER_COMMANDS = Set.of(
            "a", "aurora", "aura",
            "backpack", "bp",
            "customise", "customize", "cosmetics",
            "ranks", "rank",
            "collections", "collection",
            "leaderboard",
            "tutorial",
            "shop",
            "dash",
            // The store plugin's own command, listed so donors can find it
            "buy",
            "spawn");

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        if (event.getPlayer().hasPermission(STAFF_PERMISSION)) return;
        event.getCommands().retainAll(PLAYER_COMMANDS);
    }
}
