package org.charky.aurora.features.mine.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Mine sub command to set the threshold of a mine, 0 to 100, where higher than 100 implies no threshold
 */
public class MineSetThresholdSub extends MineSub {
    public MineSetThresholdSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String mineName = args[0];
        if (find(sender, mineName) == null) return;

        try {
            double threshold = Double.parseDouble(args[1]);
            // 0 or below resets the mine on every single block broken
            if (threshold <= 0) {
                TextUtils.error(sender, "Threshold must be above 0, or above 100 for no threshold");
                return;
            }
            mineManager.setThreshold(mineName, threshold);
            TextUtils.success(sender, "Mine '" + mineName + "' threshold set to " + threshold + " successfully");
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Invalid threshold: " + e.getMessage());
        }

    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], mineManager.getMines());

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<mine> <threshold>";
    }

    @Override
    public String getName() {
        return "setthreshold";
    }

    @Override
    public String getDescription() {
        return "Sets the threshold of a mine as a percent from 0 to 100, higher than 100 indicates no threshold";
    }
}
