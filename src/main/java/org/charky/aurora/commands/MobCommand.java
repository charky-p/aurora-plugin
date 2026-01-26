package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.managers.MobManager;
import org.jetbrains.annotations.NotNull;

public class MobCommand implements CommandExecutor {
    private MobManager manager;

    public MobCommand(MobManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length == 0) return false;

        return switch (args[0].toLowerCase()) {
            case "add" -> addSpawner(sender, args);
            case "save" -> saveMob(sender, args);
            default -> false;
        };
    }

    public boolean addSpawner(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 6) {
            sender.sendMessage("Usage: /mobspawner add <name> <x> <y> <z> <mobName>");
            return true;
        }

        String spawnerName = args[1];
        int x = Integer.parseInt(args[2]);
        int y = Integer.parseInt(args[3]);
        int z = Integer.parseInt(args[4]);
        String mobName = args[5];

        if (manager.spawnerExists(spawnerName)) {
            sender.sendMessage(Component.text("Spawner " + spawnerName + " already exists")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (!manager.mobExists(mobName)) {
            sender.sendMessage(Component.text("Mob " + mobName + " does not exist")
                    .color(NamedTextColor.RED));
            return true;
        }

        // Default radius=5, limit=3 TODO
        String world = "world";
        if (sender instanceof Player player) {
            world = player.getWorld().getName();
        }

        manager.addSpawner(spawnerName, world, x, y, z, mobName, 5, 3);
        sender.sendMessage(Component.text("Successfully added " + spawnerName + " spawner!")
                .color(NamedTextColor.GREEN));
        return true;
    }

    public boolean saveMob(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        return false;
    }
}
