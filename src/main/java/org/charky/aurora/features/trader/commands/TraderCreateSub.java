package org.charky.aurora.features.trader.commands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.LocationUtils;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.trader.TraderSpot;

import java.util.Collections;
import java.util.List;

/**
 * Trader sub command to place a trader at a location, or where the player stands
 * facing them. The spot is saved, so the trader respawns there
 */
public class TraderCreateSub extends TraderSub {
    public TraderCreateSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 1 && args.length < 4) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String traderId = args[0];
        if (!traderManager.traderExists(traderId)) {
            TextUtils.error(sender, "Trader '" + traderId + "' does not exist");
            return;
        }

        Location location;
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                TextUtils.error(sender, "Console must give coordinates");
                return;
            }

            // Where you stand, turned round to face you
            location = player.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
            location.setYaw(player.getLocation().getYaw() + 180);
        } else {
            try {
                location = LocationUtils.parseLocation(sender, args[1], args[2], args[3]);
            } catch (NumberFormatException e) {
                TextUtils.error(sender, "Invalid Coordinates: " + e.getMessage());
                return;
            }
        }

        TraderSpot spot = traderManager.placeTrader(traderId, location);
        if (spot == null) {
            TextUtils.error(sender, "Could not save trader '" + traderId + "', check the console");
            return;
        }
        TextUtils.success(sender, "Trader '" + traderId + "' placed as spot #" + spot.id()
                + ", remove it with /aurora trader delete " + spot.id() + " or the wand");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], traderManager.getTraders());
        if (args.length >= 2 && args.length <= 4) {
            return LocationUtils.relativeCoordinates(player, args.length - 2);
        }
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<trader> [<x> <y> <z>]";
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Places a trader that respawns at its spot";
    }
}
