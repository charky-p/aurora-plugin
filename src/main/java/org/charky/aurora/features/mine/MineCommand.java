package org.charky.aurora.features.mine;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.commands.AuroraSubCommand;
import org.charky.aurora.features.system.commands.HelpCommand;
import org.charky.aurora.features.mine.commands.*;
import org.charky.aurora.common.util.TextUtils;

import java.util.*;

/**
 * Main command for all /aurora mine commands
 * Acts as a router to assign tasks to specific sub commands
 */
public class MineCommand implements AuroraSubCommand {
    private final Map<String, AuroraSubCommand> subCommands = new HashMap<>();

    public MineCommand(Aurora plugin) {
        // TODO: Separate commands into types, such as query, update, utility
        register(new MineCreateSub(plugin));
        register(new MineDeleteSub(plugin));
        register(new MineListSub(plugin));
        register(new MineSetBlocksSub(plugin));
        register(new MineResetSub(plugin));
        register(new MineResetAllSub(plugin));
        register(new MineTpSub(plugin));
        register(new MineSetTpSub(plugin));
        register(new MineInfoSub(plugin));
        register(new MineSetThresholdSub(plugin));
        register(new MineSetDelaySub(plugin));
        register(new MineRedefineSub(plugin));
        register(new MineRenameSub(plugin));
        register(new MineWandCreateSub(plugin));
    }

    private void register(AuroraSubCommand command) {
        subCommands.put(command.getName().toLowerCase(), command);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            HelpCommand.showList(sender, "MINE COMMANDS", "/aurora mine", subCommands.values());
            return;
        }

        String subName = args[0].toLowerCase();
        AuroraSubCommand sub = subCommands.get(subName);

        if (sub == null) {
            TextUtils.error(sender, "Unknown mine command: " + subName);
            HelpCommand.showList(sender, "MINE COMMANDS", "/aurora mine", subCommands.values());
            return;
        }

        sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
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

        AuroraSubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub != null) {
            return sub.tabComplete(player, Arrays.copyOfRange(args, 1, args.length));
        }

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<subcommand>";
    }

    @Override
    public String getName() {
        return "mine";
    }

    @Override
    public String getDescription() {
        return "Create regenerating mines with custom or vanilla blocks";
    }
}
