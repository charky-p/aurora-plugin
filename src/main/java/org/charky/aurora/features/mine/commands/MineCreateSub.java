package org.charky.aurora.features.mine.commands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.LocationUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Mine subcommand to create mines manually by coordinates.
 */
public class MineCreateSub extends MineSub {
    public MineCreateSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 7) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        try {
            String mineName = args[0];

            if (mineManager.exists(mineName)) {
                TextUtils.error(sender, "Mine '" + mineName + "' already exists");
                return;
            }

            Location location1 = LocationUtils.parseLocation(sender, args[1], args[2], args[3]);
            Location location2 = LocationUtils.parseLocation(sender, args[4], args[5], args[6]);

            // Optional parameters, by default threshold is off (101%), delay is 1 minute
            double threshold = (args.length >= 8) ? Double.parseDouble(args[7]) : 101;
            int delay = (args.length >= 9) ? Math.max(20, Integer.parseInt(args[8])) : 1200;

            try {
                mineManager.addMine(mineName, location1, location2, threshold, delay);
                TextUtils.success(sender, "Mine '" + mineName + "' created successfully");
            } catch (MineException e) {
                TextUtils.error(sender, e.getMessage());
            }
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Coordinates, threshold, and delay must be valid numbers");
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length >= 2 && args.length <= 7) {
            return LocationUtils.relativeCoordinates(player, args.length - 2);
        }

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<name> <x1> <y1> <z1> <x2> <y2> <z2> [threshold] [delay]";
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Manually create a mine using coordinates";
    }
}
