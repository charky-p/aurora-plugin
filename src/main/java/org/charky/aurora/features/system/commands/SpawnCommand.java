package org.charky.aurora.features.system.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.TextUtils;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand implements CommandExecutor {
    private final Aurora plugin;

    public SpawnCommand(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String s, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Only players can run this command");
            return true;
        }

        Location location = plugin.getSpawn(player.getWorld());
        if (location == null) {
            TextUtils.error(sender, "Spawn location is not set");
            return true;
        }

        player.teleport(location);
        TextUtils.send(sender, "<aqua>Teleporting to spawn...");
        return true;
    }
}
