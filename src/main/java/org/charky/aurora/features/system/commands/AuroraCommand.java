package org.charky.aurora.features.system.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.mine.MineCommand;
import org.charky.aurora.features.spawner.SpawnerCommand;
import org.charky.aurora.features.trader.TraderCommand;
import org.charky.aurora.features.portal.PortalCommand;
import org.charky.aurora.features.world.WorldCommand;
import org.charky.aurora.common.util.LocationUtils;
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
    private static final String WORLD_FLAG = "-w";

    private final Map<String, AuroraSubCommand> subCommands = new HashMap<>();
    private final MenuCommand menu;

    public AuroraCommand(Aurora plugin) {
        this.menu = new MenuCommand(plugin);
        register(new WandCommand(plugin));
        register(new SpawnerCommand(plugin));
        register(new TraderCommand(plugin));
        register(new PortalCommand(plugin));
        register(new WorldCommand(plugin));
        register(new MineCommand(plugin));
        register(new SetSpawnCommand(plugin));
        register(new HealSub());
        register(new GiveSub(plugin));
        register(new GrantSub(plugin));
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

        if (!sender.hasPermission("aurora.admin")) {
            TextUtils.error(sender, "This is an administrator-only command");
            return true;
        }

        // -w <world> anywhere picks the world the command works in, so the console
        // and players elsewhere can place mines, spawners, traders and portals in it
        World world = null;
        List<String> rest = new ArrayList<>(Arrays.asList(args));
        int flag = rest.indexOf(WORLD_FLAG);
        if (flag >= 0) {
            if (flag + 1 >= rest.size()) {
                TextUtils.error(sender, "-w needs a world name");
                return true;
            }
            world = Bukkit.getWorld(rest.get(flag + 1));
            if (world == null) {
                TextUtils.error(sender, "World '" + rest.get(flag + 1) + "' is not loaded");
                return true;
            }
            rest.remove(flag + 1);
            rest.remove(flag);
        }
        if (rest.isEmpty()) {
            TextUtils.usage(sender, "/aurora <command> [-w <world>]");
            return true;
        }

        AuroraSubCommand subCommand = subCommands.get(rest.getFirst().toLowerCase());
        if (subCommand == null) {
            TextUtils.error(sender, "Unknown subcommand: " + rest.getFirst());
            return true;
        }

        String[] subArgs = rest.subList(1, rest.size()).toArray(new String[0]);
        if (world == null) subCommand.execute(sender, subArgs);
        else LocationUtils.runIn(world, () -> subCommand.execute(sender, subArgs));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();
        // Players only use the bare command, so they get no subcommand hints
        if (!player.hasPermission("aurora.admin")) return Collections.emptyList();

        // The world after -w, then the rest completes as if -w were not there
        if (args.length >= 2 && args[args.length - 2].equals(WORLD_FLAG)) {
            return TextUtils.getCompletions(args[args.length - 1],
                    Bukkit.getWorlds().stream().map(World::getName).toList());
        }
        List<String> kept = new ArrayList<>(Arrays.asList(args));
        int flag = kept.indexOf(WORLD_FLAG);
        if (flag >= 0 && flag + 1 < kept.size() - 1) {
            kept.remove(flag + 1);
            kept.remove(flag);
            return onTabComplete(sender, command, alias, kept.toArray(new String[0]));
        }

        if (args.length == 1) {
            return TextUtils.getCompletions(args[0], subCommands.keySet());
        }

        AuroraSubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand != null) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return subCommand.tabComplete(player, subArgs);
        }

        return Collections.emptyList();
    }
}
