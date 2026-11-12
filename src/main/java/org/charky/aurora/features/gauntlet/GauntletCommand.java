package org.charky.aurora.features.gauntlet;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.jetbrains.annotations.NotNull;

public class GauntletCommand implements CommandExecutor {
    private final GauntletManager gauntletManager;

    public GauntletCommand(GauntletManager gauntletManager) {
        this.gauntletManager = gauntletManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String s, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Only players can run this command");
            return true;
        }

        Location lobby = gauntletManager.lobby();
        if (lobby == null) {
            TextUtils.error(sender, "The gauntlet arena's world is not loaded");
            return true;
        }

        player.teleport(lobby);
        gauntletManager.joinQueue(player);

        return true;
    }
}
