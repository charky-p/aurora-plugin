package org.charky.aurora.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public interface AuroraSubCommand {
    void execute(CommandSender sender, String[] args);
    List<String> tabComplete(Player player, String[] args);
    String getUsage();
    String getName();
    String getDescription();

    // Default detailed info to description TODO: remove default
    default List<Component> getInfo() {
        return List.of(Component.text(getDescription(), NamedTextColor.GRAY));
    }

    // TODO: default to a member permission after permissiosn are addes and add permission support
    default String getPermission() {
        return null;
    }
}
