package org.charky.aurora.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public interface AuroraSubCommand {
    void execute(CommandSender sender, String[] args);
    List<String> tabComplete(Player player, String[] args);
}
