package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.managers.TraderManager;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand implements CommandExecutor {
    public TraderManager manager;

    public SpawnCommand(TraderManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String s, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return false;

//        player.teleport(new Location())
        if (args.length < 4) {
            player.sendMessage(Component.text("Usage: /trader <traderId> <x> <y> <z>"));
            return true;
        }

        String traderId = args[0];
        double x = Double.parseDouble(args[1]);
        double y = Double.parseDouble(args[2]);
        double z = Double.parseDouble(args[3]);

        if (!manager.traderExists(traderId)) {
            player.sendMessage(Component.text("Trader " + traderId + " does not exist!")
                    .color(NamedTextColor.RED));
            return true;
        }
        manager.spawnTrader(traderId, player.getWorld(), x, y, z);
        player.sendMessage(Component.text("Successfully spawned trader!")
                .color(NamedTextColor.GREEN));
        return true;
    }
}
