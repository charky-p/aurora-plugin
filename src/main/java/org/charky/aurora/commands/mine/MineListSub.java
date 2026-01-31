package org.charky.aurora.commands.mine;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.commands.AuroraSubCommand;

import java.util.List;

public class MineListSub implements AuroraSubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {

    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return List.of();
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }
}
