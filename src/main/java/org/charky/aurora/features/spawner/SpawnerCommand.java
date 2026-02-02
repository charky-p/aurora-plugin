package org.charky.aurora.features.spawner;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.commands.AuroraSubCommand;
import org.charky.aurora.features.spawner.commands.*;
import org.charky.aurora.common.util.TextUtils;

import java.util.*;

public class SpawnerCommand implements AuroraSubCommand {
    private final Map<String, AuroraSubCommand> subCommands = new HashMap<>();

    public SpawnerCommand(Aurora plugin) {
        // TODO: Separate commands into types, such as query, update, utility
        register(new SpawnerCreateSub(plugin));
        register(new SpawnerDeleteSub(plugin));
        register(new SpawnerListSub(plugin));
        register(new SpawnerTpSub(plugin));
        register(new SpawnerInfoSub(plugin));
    }

    private void register(AuroraSubCommand command) {
        subCommands.put(command.getName().toLowerCase(), command);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // create, delete, list, tp, info, TODO: setmob, setradius, setlimit, setdelay, rename, redefine, toggle
        // TODO: add activation radius hidden by player, stop mob lag
        // TODO: no mob or trade regions, where mobs will be deleted (not traders)
        if (args.length == 0) {
            // TODO: Full usage is not set
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String subCommandName = args[0].toLowerCase();
        AuroraSubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            TextUtils.error(sender, "Unknown subcommand: " + subCommandName);
            return;
        }

        subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return subCommands.keySet().stream()
                    .filter(name -> name.startsWith(input))
                    .sorted()
                    .toList();
        }

        AuroraSubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand != null) {
            return subCommand.tabComplete(player, Arrays.copyOfRange(args, 1, args.length));
        }

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<subcommand>";
    }

    @Override
    public String getName() {
        return "spawner";
    }

    @Override
    public String getDescription() {
        return "Create custom spawners with custom entities";
    }
}
