package org.charky.aurora.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.TextUtils;
import org.charky.aurora.models.MessageType;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand implements CommandExecutor {
    private Aurora plugin;

    public SpawnCommand(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String s, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtils.format("Only players can run /spawn"));
            return true;
        }

        Location location =  plugin.getSpawn(player.getWorld());
        if (location == null) {
            sender.sendMessage(TextUtils.format("Spawn is not set", MessageType.ERROR));
            return true;
        }

        sender.sendMessage(TextUtils.format("Teleporting to spawn...", MessageType.SUCCESS));
        player.teleport(location);
        return true;
    }
}
