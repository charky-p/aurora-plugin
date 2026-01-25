package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.MineManager;
import org.jetbrains.annotations.NotNull;

public class CreateMine implements CommandExecutor {
    private final MineManager manager;

    public CreateMine(MineManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length < 7) {
            sender.sendMessage(Component.text("Usage: /create-mine <name> <x1> <y1> <z1> <x2> <y2> <z2> [threshold]")
                    .color(NamedTextColor.RED));
            return true;
        }

        try {
            String name = args[0];
            String worldName = (sender instanceof Player p) ? p.getWorld().getName() : "world";
            int x1 = Integer.parseInt(args[1]);
            int y1 = Integer.parseInt(args[2]);
            int z1 = Integer.parseInt(args[3]);
            int x2 = Integer.parseInt(args[4]);
            int y2 = Integer.parseInt(args[5]);
            int z2 = Integer.parseInt(args[6]);
            double threshold = (args.length >= 8) ? Double.parseDouble(args[7]) : 0.5;
            int delay = (args.length >= 9) ? Integer.parseInt(args[8]) : 1200;

            if (manager.exists(name)) {
                sender.sendMessage(Component.text("Mine already exists!")
                        .color(NamedTextColor.RED));
                return true;
            }

            manager.addMine(name, worldName, Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                    Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2), threshold, delay);
            sender.sendMessage(Component.text(name + " mine created successfully").color(NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Error: Coordinates and thresholds must be numbers!")
                    .color(NamedTextColor.RED));
        }
        return true;
    }
}
