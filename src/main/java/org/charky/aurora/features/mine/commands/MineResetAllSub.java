package org.charky.aurora.features.mine.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Mine sub command to trigger mine regeneration for all mines
 */
public class MineResetAllSub extends MineSub {
    public MineResetAllSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<String> mines = mineManager.getMines();
        if (mines.isEmpty()) {
            TextUtils.warn(sender, "There are no mines to reset");
            return;
        }

        mineManager.resetAll();
        TextUtils.success(sender, "Reset all " + mines.size() + " mines successfully");
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
        return "resetall";
    }

    @Override
    public String getDescription() {
        return "Immediately resets all mines";
    }
}
