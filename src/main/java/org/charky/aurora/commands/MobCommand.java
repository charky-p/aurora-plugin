package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.managers.MobManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MobCommand implements AuroraSubCommand {
    private final MobManager manager;

    public MobCommand(Aurora plugin) {
        this.manager = plugin.getMobManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) return;

        switch (args[0].toLowerCase()) {
            case "add" -> addSpawner(sender, args);
            case "save" -> saveMob(sender, args);
        };
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return List.of();
    }

    public void addSpawner(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 6) {
            sender.sendMessage("Usage: /mob add <name> <x> <y> <z> <mobName>");
            return;
        }

        String spawnerName = args[1];
        int x = Integer.parseInt(args[2]);
        int y = Integer.parseInt(args[3]);
        int z = Integer.parseInt(args[4]);
        String mobName = args[5];

        if (manager.spawnerExists(spawnerName)) {
            sender.sendMessage(Component.text("Spawner " + spawnerName + " already exists")
                    .color(NamedTextColor.RED));
            return;
        }

        if (!manager.mobExists(mobName)) {
            sender.sendMessage(Component.text("Mob " + mobName + " does not exist")
                    .color(NamedTextColor.RED));
            return;
        }

        // Default radius=5, limit=3 TODO
        String world = "world";
        if (sender instanceof Player player) world = player.getWorld().getName();

        manager.addSpawner(spawnerName, world, x, y, z, mobName, 5, 3);
        sender.sendMessage(Component.text("Successfully added " + spawnerName + " spawner!")
                .color(NamedTextColor.GREEN));
    }

    public void saveMob(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        return;
    }

    @Override
    public String getUsage() {
        return "IM HITLER";
    }

    @Override
    public String getName() {
        return "mob";
    }

    @Override
    public String getDescription() {
        return "Create custom spawners with custom entities";
    }
}
