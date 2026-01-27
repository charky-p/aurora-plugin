package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.managers.MineManager;
import org.charky.aurora.models.MineBlock;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MineCommand implements AuroraSubCommand {
    private final MineManager manager;

    public MineCommand(Aurora plugin) {
        this.manager = plugin.getMineManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) return;

        switch (args[0].toLowerCase()) {
            case "create" -> createMine(sender, args);
            case "delete" -> deleteMine(sender, args);
            case "list" -> listMines(sender, args);
            case "setblocks" -> setBlocks(sender, args);
            case "reset" -> resetMine(sender, args);
            case "tp" -> tpMine(sender, args);
        };
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return List.of("create", "delete", "list", "setblocks", "reset", "tp").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .toList();

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (List.of("delete", "setblocks", "reset", "tp").contains(sub)) {
                return manager.getMines().stream()
                        .filter(name -> name.startsWith(args[1].toLowerCase()))
                        .toList();
            }

            if (sub.equals("tp")) {
                List<String> suggestions = new ArrayList<>(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.startsWith(args[1].toLowerCase()))
                        .toList());
                suggestions.addAll(manager.getMines().stream()
                        .filter(name -> name.startsWith(args[1].toLowerCase()))
                        .toList());
                return suggestions;
            }
        }

        if (args[0].equalsIgnoreCase("setblocks") && args.length > 2 && args.length % 2 != 0) {
            String input = args[args.length - 1].toLowerCase();
            return List.of(Material.values()).stream()
                    .map(m -> m.name().toLowerCase())
                    .filter(m -> m.startsWith(input))
                    .limit(20)
                    .toList();
        }

        return List.of();
    }

    private void createMine(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        // TODO: Consider edge cases, like overlapping mines, or delay is too small, and what to do for default threshold
        if (args.length < 8) {
            sender.sendMessage(Component.text("Usage: /mine create <name> <x1> <y1> <z1> <x2> <y2> <z2> [threshold]")
                    .color(NamedTextColor.RED));
            return;
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
                return;
            }

            manager.addMine(name, worldName, Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                    Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2), threshold, delay);
            sender.sendMessage(Component.text(name + " mine created successfully").color(NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Error: Coordinates and thresholds must be numbers!")
                    .color(NamedTextColor.RED));
        }
    }

    private void deleteMine(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /delete mine <mineName>"));
            return;
        }

        String mineName = args[1];
        if (!manager.exists(mineName)) {
            sender.sendMessage(Component.text("Mine " + mineName + " does not exist")
                    .color(NamedTextColor.RED));
            return;
        }

        manager.removeMine(mineName);
        sender.sendMessage(Component.text("Removed mine " + mineName + " successfully!")
                .color(NamedTextColor.GREEN));
    }

    private void listMines(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        List<String> names = manager.getMines();
        if (names.isEmpty()) {
            sender.sendMessage(Component.text("There are no mines created yet."));
            return;
        }

        sender.sendMessage(Component.text("Mines:"));
        for (String name : names) {
            sender.sendMessage(Component.text(" - " + name));
        }
    }

    private void setBlocks(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        // TODO: block must actually be a block
        // Read blocks
        if (args.length < 4 || args.length % 2 != 0) {
            sender.sendMessage(Component.text("Usage: /mine setblocks <mineName> [block1] [block1_amt]...")
                    .color(NamedTextColor.RED));
            return;
        }

        String mineName = args[1];
        if (!manager.exists(mineName)) {
            sender.sendMessage(Component.text("No such mine " + mineName).color(NamedTextColor.RED));
            return;
        }

        List<MineBlock> blocks = new ArrayList<>();
        int totalPercent = 0;
        for (int i = 2; i < args.length; i += 2) {
            Material material = Material.matchMaterial(args[i]);
            if (material == null) {
                sender.sendMessage("No such material: " + args[i]);
                return;
            }

            try {
                int percent = Integer.parseInt(args[i + 1]);
                if (percent < 0) throw new NumberFormatException();

                totalPercent += percent;
                blocks.add(new MineBlock(material, percent));
            } catch (NumberFormatException e) {
                sender.sendMessage("Illegal percentage: " + args[i + 1]);
                return;
            }
        }

        if (totalPercent != 100) {
            sender.sendMessage("Percentages must add up to 100!");
            return;
        }

        manager.setBlocks(mineName, blocks);
        sender.sendMessage(Component.text("Successfully updated mine " + mineName)
                .color(NamedTextColor.GREEN));
    }

    private void resetMine(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /mine reset <mineName>").color(NamedTextColor.RED));
            return;
        }

        String name = args[1];
        if (!manager.exists(name)) {
            sender.sendMessage(Component.text("Mine '" + name + "' not found.").color(NamedTextColor.RED));
            return;
        }

        manager.resetMine(name);
        sender.sendMessage(Component.text("Mine " + name + " successfully reset!").color(NamedTextColor.GREEN));
    }

    private void tpMine(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length != 2 && args.length != 3) {
            sender.sendMessage(Component.text("Usage: /mine tp [player] <mineName>")
                    .color(NamedTextColor.RED));
            return;
        }

        if (args.length == 2 && !(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Specify a user to teleport!")
                    .color(NamedTextColor.RED));
            return;
        }

        Player target = args.length == 2 ? (Player) sender : Bukkit.getPlayer(args[1]);
        String mineName = args.length == 2 ? args[1] : args[2];
        if (target == null) {
            sender.sendMessage(Component.text("Player " + args[1] + " does not exist.")
                    .color(NamedTextColor.RED));
            return;
        } else if (!manager.exists(mineName)) {
            sender.sendMessage(Component.text("Mine " + mineName + " does not exist!")
                    .color(NamedTextColor.RED));
            return;
        }

        manager.tpMine(target, mineName);
        sender.sendMessage(Component.text("Player " + target.getName() + " successfully teleported to "
                            + mineName).color(NamedTextColor.GREEN));
    }
}
