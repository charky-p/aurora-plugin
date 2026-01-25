package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.MineManager;
import org.charky.aurora.models.MineBlock;
import org.jetbrains.annotations.NotNull;

import javax.naming.Name;
import java.util.ArrayList;
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
            case "delete" -> deleteMine(sender, args);
            case "list" -> listMines(sender, args);
            case "setblocks" -> setBlocks(sender, args);
            case "reset" -> resetMine(sender, args);
            default -> false;
        };
    }

    private boolean createMine(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        // TODO: Consider edge cases, like overlapping mines, or delay is too small, and what to do for default threshold
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
            // Default threshold is off, (200%)
            double threshold = (args.length >= 9) ? Double.parseDouble(args[8]) : 2;
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

    private boolean deleteMine(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /delete mine <mineName>"));
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
            sender.sendMessage(Component.text(" - " + name));
        }
        return true;
    }

    private boolean setBlocks(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        // Read blocks
        if (args.length < 4 || args.length % 2 != 0) {
            sender.sendMessage(Component.text("Usage: /mine setblocks <mineName> [block1] [block1_amt]...")
                    .color(NamedTextColor.RED));
            return true;
        }

        String mineName = args[1];
        if (!manager.exists(mineName)) {
            sender.sendMessage(Component.text("No such mine " + mineName).color(NamedTextColor.RED));
            return true;
        }

        List<MineBlock> blocks = new ArrayList<>();
        int totalPercent = 0;
        for (int i = 2; i < args.length; i += 2) {
            Material material = Material.matchMaterial(args[i]);
            if (material == null) {
                sender.sendMessage("No such material: " + args[i]);
                return true;
            }

            try {
                int percent = Integer.parseInt(args[i + 1]);
                if (percent < 0) throw new NumberFormatException();

                totalPercent += percent;
                blocks.add(new MineBlock(material, percent));
            } catch (NumberFormatException e) {
                sender.sendMessage("Illegal percentage: " + args[i + 1]);
                return true;
            }
        }

        if (totalPercent != 100) {
            sender.sendMessage("Percentages must add up to 100!");
            return true;
        }

        manager.setBlocks(mineName, blocks);
        sender.sendMessage(Component.text("Successfully updated mine " + mineName)
                .color(NamedTextColor.GREEN));
        return true;
    }

    private boolean resetMine(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /mine reset <mineName>").color(NamedTextColor.RED));
            return true;
        }

        String name = args[1];
        if (!manager.exists(name)) {
            sender.sendMessage(Component.text("Mine '" + name + "' not found.").color(NamedTextColor.RED));
            return true;
        }

        manager.resetMine(name);
        sender.sendMessage(Component.text("Mine " + name + " successfully reset!").color(NamedTextColor.GREEN));
        return true;
    }
}
