package org.charky.aurora.features.system.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Interface for aurora commands that are admin only used with /aurora
 */
public interface AuroraSubCommand {
    void execute(CommandSender sender, String[] args);

    List<String> tabComplete(Player player, String[] args);

    String getUsage();

    String getName();

    String getDescription();

    default String getFullUsage() {
        return "/aurora " + getName() + " " + getUsage();
    }

    // Detailed info for /aurora info, most commands need no more than the description
    default List<String> getInfo() {
        return List.of(getDescription());
    }
}
