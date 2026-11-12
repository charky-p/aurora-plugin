package org.charky.aurora.features.trader;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.commands.AuroraSubCommand;
import org.charky.aurora.features.system.commands.HelpCommand;
import org.charky.aurora.features.trader.commands.*;

import java.util.*;

/**
 * Trader commands. Traders are saved by spot in the traders table and respawn there,
 * they can also be removed by right clicking them with the wand
 */
public class TraderCommand implements AuroraSubCommand {
    private final Map<String, AuroraSubCommand> subCommands = new HashMap<>();

    public TraderCommand(Aurora plugin) {
        register(new TraderCreateSub(plugin));
        register(new TraderDeleteSub(plugin));
        register(new TraderListSub(plugin));
        register(new TraderTpSub(plugin));
    }

    private void register(AuroraSubCommand command) {
        subCommands.put(command.getName().toLowerCase(), command);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            HelpCommand.showList(sender, "TRADER COMMANDS", "/aurora trader", subCommands.values());
            return;
        }

        String subCommandName = args[0].toLowerCase();
        AuroraSubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            TextUtils.error(sender, "Unknown trader command: " + subCommandName);
            HelpCommand.showList(sender, "TRADER COMMANDS", "/aurora trader", subCommands.values());
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
        return "trader";
    }

    @Override
    public String getDescription() {
        return "Place traders that respawn at their spot";
    }
}
