package org.charky.aurora.core.commands;

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

    // Default detailed info to description TODO: remove default
    default List<String> getInfo() {
        return List.of(getDescription());
    }

    // TODO: default to a member permission after permissiosn are addes and add permission support
    default String getPermission() {
        return null;
    }
}
