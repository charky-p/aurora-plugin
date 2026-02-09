package org.charky.aurora.features.mine.commands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.LocationUtils;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.Collections;
import java.util.List;

/**
 * Mine sub command to redefine the boundaries of a mine
 */
public class MineRedefineSub extends MineSub {
    public MineRedefineSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // TODO: Implement this properly, there r 2 redefines, manual and with wand
        if (args.length < 7) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

//        if (!wandManager.hasBothPositions(player.getUniqueId())) {
//            TextUtils.error(sender, "Select two new points with your wand first!");
//            return;
//        }

        try {
            String mineName = args[0];

            if (mineManager.exists(mineName)) {
                TextUtils.error(sender, "Mine '" + mineName + "' already exists");
                return;
            }

            Location location1 = LocationUtils.parseLocation(sender, args[1], args[2], args[3]);
            Location location2 = LocationUtils.parseLocation(sender, args[4], args[5], args[6]);

            mineManager.redefineMine(mineName, location1, location2);
            TextUtils.success(sender, "Mine '" + mineName + "' redefined successfully");

            mineManager.resetMine(mineName);
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Coordinates must be valid numbers");
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], mineManager.getMines());

        if (args.length >= 2 && args.length <= 7) {
            return LocationUtils.relativeCoordinates(player, args.length - 2);
        }

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<mine> <x1> <y1> <z1> <x2> <y2> <z2>";
    }

    @Override
    public String getName() {
        return "redefine";
    }

    @Override
    public String getDescription() {
        return "Redefine the coordinates of a mine";
    }
}
