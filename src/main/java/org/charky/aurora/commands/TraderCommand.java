package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.util.TextUtils;
import org.charky.aurora.managers.TraderManager;
import org.charky.aurora.util.MessageType;

import java.util.List;

public class TraderCommand implements AuroraSubCommand {
    public TraderManager manager;

    public TraderCommand(Aurora plugin) {
        this.manager = plugin.getTraderManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /trader <traderId> <x> <y> <z>"));
            return;
        }

        String traderId = args[0];
        try {
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
        } catch (NumberFormatException e) {
            sender.sendMessage(TextUtils.format("Invalid number", MessageType.ERROR));
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return List.of();
    }

    @Override
    public String getUsage() {
        return "Usage: /aurora trader TODO";
    }

    @Override
    public String getName() {
        return "trader";
    }

    @Override
    public String getDescription() {
        return "create an npc trader to interact with for shops";
    }
}
