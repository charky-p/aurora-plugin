package org.charky.aurora.features.system.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.player.LeaderboardMenu;
import org.charky.aurora.features.player.PlayerManager;
import org.jetbrains.annotations.NotNull;

// /leaderboard, the top ten of each stat in a menu, see LeaderboardMenu
public class TopCommand implements CommandExecutor {
    private final DataRegistry registry;
    private final PlayerManager players;

    public TopCommand(DataRegistry registry, PlayerManager players) {
        this.registry = registry;
        this.players = players;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return false;
        LeaderboardMenu.open(registry, players, player);
        return true;
    }
}
