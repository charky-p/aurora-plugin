package org.charky.aurora.features.gauntlet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GauntletCommand implements CommandExecutor {
    private GauntletManager gauntletManager;

    public GauntletCommand(GauntletManager gauntletManager) {
        this.gauntletManager = gauntletManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String s, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        player.teleport(new Location(Bukkit.getWorld("world"), -246, -59, 207));
        gauntletManager.joinQueue(player);
        return true;
    }
}
