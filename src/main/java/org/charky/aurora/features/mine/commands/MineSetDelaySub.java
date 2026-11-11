package org.charky.aurora.features.mine.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Mine sub command to set the delay in ticks of a mine
 */
public class MineSetDelaySub extends MineSub {
    public MineSetDelaySub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String mineName = args[0];
        if (!mineManager.exists(mineName)) {
            TextUtils.error(sender, "Mine '" + mineName + "' does not exist");
            return;
        }

        try {
            // Same floor as /aurora mine create, anything faster resets every tick
            int delay = Math.max(20, Integer.parseInt(args[1]));
            mineManager.setDelay(mineName, delay);
            TextUtils.success(sender, "Mine '" + mineName + "' delay set to " + delay + " successfully");
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Invalid delay: " + e.getMessage());
        }

    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], mineManager.getMines());

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<mine> <delay>";
    }

    @Override
    public String getName() {
        return "setdelay";
    }

    @Override
    public String getDescription() {
        return "Sets the delay of a mine in ticks, where 1200 is one minute";
    }
}
