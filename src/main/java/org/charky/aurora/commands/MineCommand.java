package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.MineManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MineCommand implements CommandExecutor {
    private final MineManager manager;

    public MineCommand(MineManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length == 0) return false;

        return switch (args[0].toLowerCase()) {
            case "create" -> createMine(sender, args);
            case "remove" -> removeMine(sender, args);
            case "list" -> listMines(sender, args);
            case "setblocks" -> setBlocks(sender, args);
            default -> false;
        };
    }

    private boolean createMine(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 8) {
            sender.sendMessage(Component.text("Usage: /mine create <name> <x1> <y1> <z1> <x2> <y2> <z2> [threshold]")
                    .color(NamedTextColor.RED));
            return true;
        }

        try {
            String name = args[1];
            String worldName = (sender instanceof Player p) ? p.getWorld().getName() : "world";
            int x1 = Integer.parseInt(args[2]);
            int y1 = Integer.parseInt(args[3]);
            int z1 = Integer.parseInt(args[4]);
            int x2 = Integer.parseInt(args[5]);
            int y2 = Integer.parseInt(args[6]);
            int z2 = Integer.parseInt(args[7]);
            double threshold = (args.length >= 9) ? Double.parseDouble(args[8]) : 0.5;
            int delay = (args.length >= 10) ? Integer.parseInt(args[9]) : 1200;

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

    private boolean removeMine(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /remove mine <mineName>"));
            return true;
        }

        String mineName = args[1];
        if (!manager.exists(mineName)) {
            sender.sendMessage(Component.text("Mine " + mineName + " does not exist")
                    .color(NamedTextColor.RED));
            return true;
        }

        manager.removeMine(mineName);
        sender.sendMessage(Component.text("Removed mine " + mineName + " successfully!")
                .color(NamedTextColor.GREEN));
        return true;
    }

    private boolean listMines(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        List<String> names = manager.getMines();
        if (names.isEmpty()) {
            sender.sendMessage(Component.text("There are no mines created yet."));
            return true;
        }

        sender.sendMessage(Component.text("Mines:"));
        for (String name : names) {
            sender.sendMessage(Component.text(name));
        }
        return true;
    }

    private boolean setBlocks(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        return false;
    }
}
