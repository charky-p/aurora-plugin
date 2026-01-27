package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.managers.TraderManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SpawnCommand implements AuroraSubCommand {
    public TraderManager manager;

    public SpawnCommand(Aurora plugin) {
        this.manager = plugin.getTraderManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /trader <traderId> <x> <y> <z>"));
            return;
        }

        String traderId = args[0];
        double x = Double.parseDouble(args[1]);
        double y = Double.parseDouble(args[2]);
        double z = Double.parseDouble(args[3]);

        if (!manager.traderExists(traderId)) {
            sender.sendMessage(Component.text("Trader " + traderId + " does not exist!")
                    .color(NamedTextColor.RED));
            return;
        }

        String worldName = "world";
        if (sender instanceof Player player) worldName = player.getWorld().getName();

        manager.spawnTrader(traderId, Bukkit.getWorld(worldName), x, y, z);
        sender.sendMessage(Component.text("Successfully spawned trader!")
                .color(NamedTextColor.GREEN));
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return List.of();
    }
}
