package org.charky.aurora.features.system.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Help command for admin commands, gets a brief description and usage of each command.
 * Use InfoCommand for a more technical description of a specific command.
 */
public class HelpCommand implements AuroraSubCommand {
    private final Map<String, AuroraSubCommand> subCommands;

    public HelpCommand(Map<String, AuroraSubCommand> subCommands) {
        this.subCommands = subCommands;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TextUtils.display(sender, "<aqua><st>                          </st> <b>AURORA COMMANDS</b> <st>                          </st>");
        subCommands.values()
                .stream()
                .sorted(Comparator.comparing(AuroraSubCommand::getName))
                .forEach(sub -> {
                String line = "<click:suggest_command:'/aurora " + sub.getName() + " '>" +
                        "<hover:show_text:'<gray>Click to use'><yellow>" + sub.getFullUsage() +
                        " <dark_gray>» <gray>" + sub.getDescription() + "</hover></click>";
                TextUtils.display(sender, line);
        });
        TextUtils.line(sender, "<aqua>");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public String getFullUsage() {
        return "/aurora help";
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Displays a list of available Aurora commands";
    }
}
