package org.charky.aurora.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.TextUtils;
import org.charky.aurora.models.MessageType;
import org.jetbrains.annotations.NotNull;

public class SetSpawnCommand implements CommandExecutor {
    private final Aurora plugin;

    public SetSpawnCommand(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return true;
        // if (!player.hasPermission("aurora.admin")) return true;

        plugin.setSpawn(player.getLocation());

        player.sendMessage(TextUtils.format("Spawn set for " + player.getWorld().getName() + "!", MessageType.SUCCESS));
        return true;
    }
}