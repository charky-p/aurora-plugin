package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HelpCommand implements AuroraSubCommand {
    private final Map<String, AuroraSubCommand> subCommands;

    public HelpCommand(Map<String, AuroraSubCommand> subCommands) {
        this.subCommands = subCommands;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(Component.text("--- Aurora Commands ---", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        subCommands.values().forEach(sub -> sender.sendMessage(
                Component.text("/aurora " + sub.getName(), NamedTextColor.YELLOW)
                .append(Component.text(" - " + sub.getDescription(), NamedTextColor.GRAY)))
        );
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
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Displays a list of available Aurora commands";
    }
}
