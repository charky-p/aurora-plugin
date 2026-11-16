package org.charky.aurora.features.system.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.features.player.PlayerManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// /leaderboard, the ten highest levels on the server
public class TopCommand implements CommandExecutor {
    private static final int SIZE = 10;

    private final PlayerManager players;

    public TopCommand(PlayerManager players) {
        this.players = players;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        players.topLevels(SIZE, top -> show(sender, top));
        return true;
    }

    private static void show(CommandSender sender, List<PlayerManager.Standing> top) {
        TextUtils.send(sender, "<yellow><b>LEVEL LEADERBOARD");
        if (top.isEmpty()) {
            TextUtils.send(sender, "<gray>Nobody has played yet.");
            return;
        }

        int place = 1;
        for (PlayerManager.Standing standing : top) {
            String name = Bukkit.getOfflinePlayer(standing.uuid()).getName();
            TextUtils.send(sender, "<green><b>" + place++ + ".</b> <gray>" + (name == null ? "Unknown" : name)
                    + " <dark_gray>| <yellow>Level <b>" + standing.level());
        }
    }
}
