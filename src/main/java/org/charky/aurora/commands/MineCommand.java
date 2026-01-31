package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.DataRegistry;
import org.charky.aurora.commands.mine.MineCreateSub;
import org.charky.aurora.managers.WandManager;
import org.charky.aurora.util.TextUtils;
import org.charky.aurora.managers.MineManager;
import org.charky.aurora.util.MessageType;
import org.charky.aurora.features.mining.MineBlock;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MineCommand implements AuroraSubCommand {
    private final Map<String, AuroraSubCommand> subCommands = new HashMap<>();
    private final MineManager manager;
    private final WandManager wandManager;
    private final DataRegistry registry;

    public MineCommand(Aurora plugin) {
        register(new MineCreateSub(plugin));
        this.manager = plugin.getMineManager();
        this.wandManager = plugin.getWandManager();
        this.registry = plugin.getRegistry();
    }

    private void register(AuroraSubCommand command) {
        subCommands.put(command.getName().toLowerCase(), command);
    }

    // TODO: tmr split into subclasses like aurora command
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) return;

        switch (args[0].toLowerCase()) {
            case "create" -> createMine(sender, args);
            case "create2" -> createMine2(sender, args);
            case "delete" -> deleteMine(sender, args);
            case "list" -> listMines(sender, args);
            case "setblocks" -> setBlocks(sender, args);
            case "reset" -> resetMine(sender, args);
            case "resetall" -> resetAll(sender, args);
            case "tp" -> tpMine(sender, args);
            case "settp" -> setTp(sender, args);
            case "info" -> info(sender, args);
            case "setthreshold" -> setThreshold(sender, args);
            case "setdelay" -> setDelay(sender, args);
            case "redefine" -> redefine(sender, args);
            case "rename" -> rename(sender, args);
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

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "mine [create|... TODO]";
    }

    @Override
    public String getName() {
        return "mine";
    }

    @Override
    public String getDescription() {
        return "Create regenerating mines with custom or vanilla blocks";
    }

    private void createMine(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        // TODO: Consider edge cases, like overlapping mines, or delay is too small, and what to do for default threshold
        if (args.length < 8) {
            sender.sendMessage(
                    TextUtils.format(
                            "Usage: /aurora mine create <name> <x1> <y1> <z1> <x2> <y2> <z2> [threshold] [delay]",
                            MessageType.ERROR)
            );
            return;
        }

        sender.sendMessage(TextUtils.format("This command is outdated, please use a wand to select", MessageType.WARNING));

        try {
            String name = args[1];
            String worldName = (sender instanceof Player p) ? p.getWorld().getName() : "world";
            int x1 = Integer.parseInt(args[2]);
            int y1 = Integer.parseInt(args[3]);
            int z1 = Integer.parseInt(args[4]);
            int x2 = Integer.parseInt(args[5]);
            int y2 = Integer.parseInt(args[6]);
            int z2 = Integer.parseInt(args[7]);
            // Default threshold is off, (101%)
            double threshold = (args.length >= 9) ? Double.parseDouble(args[8]) : 101;
            int delay = (args.length >= 10) ? Integer.parseInt(args[9]) : 1200;

            if (manager.exists(name)) {
                sender.sendMessage(TextUtils.format("Mine already exists!", MessageType.ERROR));
                return;
            }

            manager.addMine(name, worldName, Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                    Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2), threshold, delay);
            sender.sendMessage(TextUtils.format(name + " mine created successfully", MessageType.SUCCESS));
        } catch (NumberFormatException e) {
            sender.sendMessage(TextUtils.format("Coordinates and thresholds must be numbers", MessageType.ERROR));
        }
    }

    private void createMine2(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return;

        if (args.length < 2) {
            TextUtils.format(player, "Usage: /aurora mine create <name>", MessageType.ERROR);
            return;
        }

        String mineName = args[1];
        if (!wandManager.hasBothPositions(player.getUniqueId())) {
            TextUtils.format(player, "You must select two positions with the Aurora Wand first!", MessageType.ERROR);
            return;
        }

        Location p1 = wandManager.getPos1(player.getUniqueId());
        Location p2 = wandManager.getPos2(player.getUniqueId());

        // We pass the locations directly to the manager
        manager.addMine(
                mineName,
                p1.getWorld().getName(),
                Math.min(p1.getBlockX(), p2.getBlockX()),
                Math.min(p1.getBlockY(), p2.getBlockY()),
                Math.min(p1.getBlockZ(), p2.getBlockZ()),
                Math.max(p1.getBlockX(), p2.getBlockX()),
                Math.max(p1.getBlockY(), p2.getBlockY()),
                Math.max(p1.getBlockZ(), p2.getBlockZ()),
                101, // default threshold
                1200 // default delay
        );

        TextUtils.format(player, "Mine '" + mineName + "' created using your wand selection!", MessageType.SUCCESS);
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
            try {
                int percent = Integer.parseInt(args[i + 1]);
                if (percent < 0) throw new NumberFormatException();

                totalPercent += percent;
                if (registry.hasBlock(args[i])) {
                    blocks.add(new MineBlock(registry.getMaterial(args[i]), percent, args[i]));
                } else {
                    Material material = Material.matchMaterial(args[i]);
                    if (material == null) {
                        sender.sendMessage("No such material: " + args[i]);
                        return;
                    } else if (!material.isBlock()) {
                        sender.sendMessage(args[i] + " is not a block");
                        return;
                    }
                    blocks.add(new MineBlock(material, percent, material.getKey().toString()));
                }
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

        // Reset mine immediately
        manager.resetMine(mineName);
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

    private void resetAll(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        manager.resetAll();
        TextUtils.format(sender, "Successfully reset all mines", MessageType.SUCCESS);
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

    private void setTp(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 5) {
            TextUtils.format(sender, "Usage: /aurora mine setTp <mineName> <x> <y> <z>", MessageType.ERROR);
            return;
        }

        String mineName = args[1];
        double x = Double.parseDouble(args[2]);
        double y = Double.parseDouble(args[3]);
        double z = Double.parseDouble(args[4]);
        if (manager.exists(mineName)) {
            TextUtils.format(sender, "Mine: " + mineName + " does not exist", MessageType.ERROR);
            return;
        }

        manager.setTp(mineName, x, y, z);
        TextUtils.format(sender, "Set tp point of " + mineName + " to " + x + " " + y + " " + z, MessageType.SUCCESS);
    }

    private void info(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        // /aurora info <mineName>
        if (args.length < 2) {
            TextUtils.send(sender, "Usage: /aurora info <mineName>");
            return;
        }

        String name = args[1];
        if (!manager.exists(name)) {
            TextUtils.error(sender, "it dont exist:" + name);
            return;
        }

        TextUtils.display(sender, "<aqua>MINE INFO <dark_gray>» <yellow>" + name);
        for (String line : manager.getMineInfo(name)) {
            TextUtils.display(sender, "<aqua>" + line);
        }
        TextUtils.line(sender, "<aqua>");
    }

    private void setThreshold(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 3) {
            TextUtils.send(sender, "Usage: /aurora setthreshold <mineName> <threshold>");
            return;
        }

        String name = args[1];
        double threshold = Double.parseDouble(args[2]);

        if (!manager.exists(name)) {
            TextUtils.send(sender, "BRUH the mine dont exist");
            return;
        }

        // set threshold
        manager.setThreshold(name, threshold);
        TextUtils.send(sender, "Successfully updated threshold to " + threshold);
    }

    private void setDelay(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 3) {
            TextUtils.send(sender, "Usage: /aurora setdelay <mineName> <delay>");
            return;
        }

        String name = args[1];
        int delay = Integer.parseInt(args[2]);

        if (!manager.exists(name)) {
            TextUtils.send(sender, "BRUH the mine dont exist");
            return;
        }

        // set delay
        manager.setDelay(name, delay);
        TextUtils.send(sender, "Successfully updated delay to " + delay);
    }

    private void redefine(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return;
        if (args.length < 2) {
            TextUtils.usage(sender, "mine redefine <name>");
            return;
        }

        String name = args[1];
        if (!manager.exists(name)) {
            TextUtils.error(sender, "Mine not found.");
            return;
        }

        if (!wandManager.hasBothPositions(player.getUniqueId())) {
            TextUtils.error(sender, "Select two new points with your wand first!");
            return;
        }

        // Update the existing object
        manager.redefineMine(name, wandManager.getPos1(player.getUniqueId()), wandManager.getPos2(player.getUniqueId()));

        TextUtils.send(sender, "Redefined boundaries for <white>" + name);
        manager.resetMine(name);
    }

    private void rename(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 3) {
            TextUtils.usage(sender, "mine rename <oldName> <newName>");
            return;
        }

        String oldName = args[1];
        String newName = args[2];

        if (!manager.exists(oldName)) {
            TextUtils.error(sender, "no such  mine");
            return;
        }

        if (manager.exists(newName)) {
            TextUtils.error(sender, "A mine with that name already exists!");
            return;
        }

        if (oldName.equals(newName)) {
            TextUtils.error(sender, "cannot be Same name");
            return;
        }
        manager.renameMine(oldName, newName);
        TextUtils.send(sender, "Mine <yellow>" + oldName + "</yellow> renamed to <white>" + newName);
    }
}
