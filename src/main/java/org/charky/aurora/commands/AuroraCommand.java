package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.managers.MobManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AuroraCommand implements CommandExecutor, TabCompleter {
    private final Map<String, AuroraSubCommand> subCommands = new HashMap<>();

    public AuroraCommand(Aurora plugin) {
        subCommands.put("wand", new WandCommand(plugin));
        subCommands.put("mob", new MobCommand(plugin));
        subCommands.put("trader", new SpawnCommand(plugin));
        subCommands.put("mine", new MineCommand(plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length == 0) return false;

        AuroraSubCommand subCommand = subCommands.get(args[0].toLowerCase());

        if (subCommand == null) {
            sender.sendMessage(Component.text("Unknown sub-command: " + args[0]).color(NamedTextColor.RED));
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subArgs);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            return subCommands.keySet().stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        AuroraSubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand != null) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return subCommand.tabComplete(player, subArgs);
        }

        return List.of();
    }
}
