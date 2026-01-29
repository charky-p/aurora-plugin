package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.util.MessageType;
import org.charky.aurora.util.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AuroraCommand implements CommandExecutor, TabCompleter {
    private final Map<String, AuroraSubCommand> subCommands = new HashMap<>();

    public AuroraCommand(Aurora plugin) {
        register(new WandCommand(plugin));
        register(new MobCommand(plugin));
        register(new TraderCommand(plugin));
        register(new MineCommand(plugin));
        register(new HelpCommand(subCommands));
        register(new InfoCommand(subCommands));
    }

    private void register(AuroraSubCommand command) {
        subCommands.put(command.getName().toLowerCase(), command);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            subCommands.get("help").execute(sender, args);
            return true;
        }

        AuroraSubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null) {
            TextUtils.format(sender, "Unknown command: " + args[0], MessageType.ERROR);
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subArgs);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String @NotNull [] args) {
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
