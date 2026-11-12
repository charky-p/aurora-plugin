package org.charky.aurora.features.system.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A command that only hands the work to its own subcommands, like /aurora mine.
 * With no subcommand, or an unknown one, it lists what it has
 */
public abstract class SubCommandRouter implements AuroraSubCommand {
    private final Map<String, AuroraSubCommand> subCommands = new HashMap<>();
    private final String name;
    private final String description;

    protected SubCommandRouter(String name, String description) {
        this.name = name;
        this.description = description;
    }

    protected void register(AuroraSubCommand command) {
        subCommands.put(command.getName().toLowerCase(), command);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showList(sender);
            return;
        }

        String subName = args[0].toLowerCase();
        AuroraSubCommand sub = subCommands.get(subName);
        if (sub == null) {
            TextUtils.error(sender, "Unknown " + name + " command: " + subName);
            showList(sender);
            return;
        }

        sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], subCommands.keySet());

        AuroraSubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub == null) return Collections.emptyList();
        return sub.tabComplete(player, Arrays.copyOfRange(args, 1, args.length));
    }

    private void showList(CommandSender sender) {
        HelpCommand.showList(sender, name.toUpperCase() + " COMMANDS", "/aurora " + name, subCommands.values());
    }

    @Override
    public String getUsage() {
        return "<subcommand>";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
