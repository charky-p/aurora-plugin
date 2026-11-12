package org.charky.aurora.features.trader.commands;

import org.bukkit.command.CommandSender;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.commands.AuroraSubCommand;
import org.charky.aurora.features.trader.TraderManager;
import org.charky.aurora.features.trader.TraderSpot;

public abstract class TraderSub implements AuroraSubCommand {
    protected final TraderManager traderManager;

    public TraderSub(Aurora plugin) {
        this.traderManager = plugin.getTraderManager();
    }

    @Override
    public String getFullUsage() {
        return "/aurora trader " + getName() + " " + getUsage();
    }

    // Spots are named by their id, "#3" or "3". Null after telling the sender why
    protected TraderSpot parseSpot(CommandSender sender, String input) {
        String number = input.startsWith("#") ? input.substring(1) : input;
        try {
            TraderSpot spot = traderManager.getSpot(Integer.parseInt(number));
            if (spot == null) TextUtils.error(sender, "Trader spot '" + input + "' does not exist");
            return spot;
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Invalid trader spot: " + input);
            return null;
        }
    }
}
