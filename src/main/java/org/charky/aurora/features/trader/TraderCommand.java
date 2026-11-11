package org.charky.aurora.features.trader;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.LocationUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.commands.AuroraSubCommand;
import org.charky.aurora.common.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Spawns an npc trader at a location, or where the player stands facing them.
 * Traders are removed by right clicking them with the wand
 */
public class TraderCommand implements AuroraSubCommand {
    public TraderManager manager;

    public TraderCommand(Aurora plugin) {
        this.manager = plugin.getTraderManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 1 && args.length < 4) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String traderId = args[0];
        if (!manager.traderExists(traderId)) {
            TextUtils.error(sender, "Trader '" + traderId + "' does not exist!");
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

        manager.spawnTrader(traderId, location);
        TextUtils.success(sender, "Trader '" + traderId + "' successfully created, remove it by right clicking with the wand");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], manager.getTraders());
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
        return "trader";
    }

    @Override
    public String getDescription() {
        return "Spawn an npc trader for shops, where you stand or at coordinates";
    }
}
