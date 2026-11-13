package org.charky.aurora.features.system.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;

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

    // The player a command acts on: the one named at args[index], or without it the
    // sender. Null after telling the sender why there is nobody to act on
    static Player targetPlayer(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            Player named = Bukkit.getPlayer(args[index]);
            if (named == null) TextUtils.error(sender, "Player '" + args[index] + "' is not online");
            return named;
        }
        if (sender instanceof Player player) return player;

        TextUtils.error(sender, "Console must specify a player to teleport");
        return null;
    }

    // Detailed info for /aurora info, most commands need no more than the description
    default List<String> getInfo() {
        return List.of(getDescription());
    }
}
