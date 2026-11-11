package org.charky.aurora.features.system.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.mine.MineCommand;
import org.charky.aurora.features.spawner.SpawnerCommand;
import org.charky.aurora.features.trader.TraderCommand;
import org.charky.aurora.common.util.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Main command handler for the Aurora plugin.
 * On its own it opens the player menu, for everyone.
 * Subcommands are admin only, not used by players.
 */
public class AuroraCommand implements CommandExecutor, TabCompleter {
    private final Map<String, AuroraSubCommand> subCommands = new HashMap<>();
    private final MenuCommand menu;

    public AuroraCommand(Aurora plugin) {
        this.menu = new MenuCommand(plugin);
        register(new WandCommand(plugin));
        register(new SpawnerCommand(plugin));
        register(new TraderCommand(plugin));
        register(new MineCommand(plugin));
        register(new SetSpawnCommand(plugin));
        register(new HelpCommand(subCommands));
        register(new InfoCommand(subCommands));
    }

    private void register(AuroraSubCommand command) {
        subCommands.put(command.getName().toLowerCase(), command);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String s, @NotNull String @NotNull [] args) {
        // Players get the menu, the console has no inventory so it gets help
        if (args.length == 0) {
            if (sender instanceof Player) menu.onCommand(sender, command, s, args);
            else subCommands.get("help").execute(sender, args);
            return true;
        }

        // TODO: TEMPORARY PERMISSION
        if (!sender.isOp()) {
            TextUtils.error(sender, "This is an administrator-only command");
            return true;
        }
        // TODO: if (!player.hasPermission("aurora.admin")) return true
        AuroraSubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null) {
            TextUtils.error(sender, "Unknown subcommand: " + args[0]);
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
        // Players only use the bare command, so they get no subcommand hints
        if (!player.isOp()) return Collections.emptyList();

        if (args.length == 1) {
            return subCommands.keySet()
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        AuroraSubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand != null) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return subCommand.tabComplete(player, subArgs);
        }

        return Collections.emptyList();
    }
}
