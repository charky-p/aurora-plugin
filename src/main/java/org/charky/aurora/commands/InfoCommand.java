package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.util.MessageType;
import org.charky.aurora.util.TextUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class InfoCommand implements AuroraSubCommand {
    private final Map<String, AuroraSubCommand> subCommands;

    public InfoCommand(Map<String, AuroraSubCommand> subCommands) {
        this.subCommands = subCommands;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(TextUtils.format(getUsage(), MessageType.ERROR));
            return;
        }

        AuroraSubCommand target = subCommands.get(args[0].toLowerCase());
        if (target == null) {
            sender.sendMessage(TextUtils.format("Unknown command: " + args[0], MessageType.ERROR));
            return;
        }

        // Info format
        sender.sendMessage(Component.text("» ", NamedTextColor.AQUA).decorate(TextDecoration.BOLD)
                .append(Component.text("Command Details: " + target.getName().toUpperCase(), NamedTextColor.WHITE)));

        sender.sendMessage(Component.text("  Description: ", NamedTextColor.YELLOW)
                .append(Component.text(target.getDescription(), NamedTextColor.GRAY)));

        if (!target.getUsage().isEmpty()) {
            sender.sendMessage(Component.text("  Usage: ", NamedTextColor.YELLOW)
                    .append(Component.text("/aurora " + target.getName() + " " + target.getUsage(), NamedTextColor.WHITE))
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand("/aurora " + target.getName() + " ")));
        }

        sender.sendMessage(Component.text("  Details:", NamedTextColor.YELLOW));
        for (Component line : target.getInfo()) {
            sender.sendMessage(Component.text("    • ", NamedTextColor.AQUA).append(line.colorIfAbsent(NamedTextColor.GRAY)));
        }

        sender.sendMessage(Component.text("---", NamedTextColor.DARK_GRAY).decoration(TextDecoration.STRIKETHROUGH, true));
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return subCommands.values().stream()
                    .map(AuroraSubCommand::getName)
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
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
