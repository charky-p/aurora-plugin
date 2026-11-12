package org.charky.aurora.features.trader.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.trader.TraderSpot;

import java.util.Collections;
import java.util.List;

/**
 * Trader sub command to remove a trader spot for good
 */
public class TraderDeleteSub extends TraderSub {
    public TraderDeleteSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        TraderSpot spot = parseSpot(sender, args[0]);
        if (spot == null) return;

        traderManager.removeSpot(spot.id());
        TextUtils.success(sender, "Trader spot " + spot.describe() + " deleted successfully");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], traderManager.getSpotIds());
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<spot>";
    }

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Removes a trader and its spot";
    }
}
