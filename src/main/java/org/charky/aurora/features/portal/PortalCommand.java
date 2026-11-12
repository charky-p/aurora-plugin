package org.charky.aurora.features.portal;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.commands.AuroraSubCommand;
import org.charky.aurora.features.system.commands.HelpCommand;
import org.charky.aurora.features.portal.commands.*;

import java.util.*;

/**
 * Portal commands. A portal is a box like a mine, walking into it sends you to its
 * destination, which can be in another world
 */
public class PortalCommand implements AuroraSubCommand {
    private final Map<String, AuroraSubCommand> subCommands = new HashMap<>();

    public PortalCommand(Aurora plugin) {
        register(new PortalCreateSub(plugin));
        register(new PortalSetDestSub(plugin));
        register(new PortalDeleteSub(plugin));
        register(new PortalRenameSub(plugin));
        register(new PortalListSub(plugin));
        register(new PortalInfoSub(plugin));
        register(new PortalTpSub(plugin));
    }

    private void register(AuroraSubCommand command) {
        subCommands.put(command.getName().toLowerCase(), command);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            HelpCommand.showList(sender, "PORTAL COMMANDS", "/aurora portal", subCommands.values());
            return;
        }

        String subCommandName = args[0].toLowerCase();
        AuroraSubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            TextUtils.error(sender, "Unknown portal command: " + subCommandName);
            HelpCommand.showList(sender, "PORTAL COMMANDS", "/aurora portal", subCommands.values());
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
        return "portal";
    }

    @Override
    public String getDescription() {
        return "Create portals that take you between worlds";
    }
}
