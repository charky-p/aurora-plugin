package org.charky.aurora.commands;

import org.bukkit.entity.Player;

import java.util.List;

public interface AuroraSubCommand {
    void execute(Player player, String[] args);
    List<String> tabComplete(Player player, String[] args);
}
