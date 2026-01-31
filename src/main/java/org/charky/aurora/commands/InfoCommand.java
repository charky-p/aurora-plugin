package org.charky.aurora.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.util.TextUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Detailed documentation command for admins.
 * Provides subcommand syntax and technical details using getInfo()
 */
public class InfoCommand implements AuroraSubCommand {
    private final Map<String, AuroraSubCommand> subCommands;

    public InfoCommand(Map<String, AuroraSubCommand> subCommands) {
        this.subCommands = subCommands;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        AuroraSubCommand target = subCommands.get(args[0].toLowerCase());
        if (target == null) {
            TextUtils.error(sender, "Command not found: <yellow> " + args[0]);
            return;
        }

        TextUtils.display(sender, "<aqua><st>                           </st> <b>COMMAND DETAILS</b> <st>                           </st>");
        TextUtils.display(sender, " <yellow>Command <dark_gray>» <gray>" + target.getName());
        TextUtils.display(sender, " <yellow>Description <dark_gray>» <gray>" + target.getDescription());
        TextUtils.display(sender, " <yellow>Usage <dark_gray>» <white>" + "<click:suggest_command:'/aurora " + target.getName() + " '>" +
                "<hover:show_text:'<gray>Click to use'><gray>" + target.getFullUsage());
        TextUtils.display(sender, "<yellow> Details <dark_gray>» <white>");
        for (String line : target.getInfo()) {
            TextUtils.display(sender, "<aqua>    • <gray>" + line);
        }
        TextUtils.line(sender, "<aqua>");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return subCommands.keySet().stream()
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .sorted()
                    .toList();
        }
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<command>";
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Displays technical documentation and syntax for a command";
    }
}
