package org.charky.aurora.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.charky.aurora.Aurora;
import org.charky.aurora.managers.MobManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class AuroraCommand implements CommandExecutor {
    private final Map<String, AuroraSubCommand> subCommands = new HashMap<>();

    public AuroraCommand(Aurora plugin, MobManager mobManager) {
//        subCommands.put("wand", new WandCommand(plugin));
//        subCommands.put("spawner", new MobCommand(plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        return false;
    }
}
