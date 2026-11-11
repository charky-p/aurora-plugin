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
 * Mine sub command to redefine the boundaries of a mine, either with coordinates
 * or, with only the mine name, from the player's wand selection
 */
public class MineRedefineSub extends MineSub {
    public MineRedefineSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 1 && args.length < 7) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String mineName = args[0];
        if (!mineManager.exists(mineName)) {
            TextUtils.error(sender, "Mine '" + mineName + "' does not exist");
            return;
        }

        Location location1;
        Location location2;

        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                TextUtils.error(sender, "Console must give coordinates, only players have a wand selection");
                return;
            }

            if (!wandManager.hasBothPositions(player.getUniqueId())) {
                TextUtils.error(sender, "Select two new points with your wand first, or give coordinates");
                return;
            }

            location1 = wandManager.getPos1(player.getUniqueId());
            location2 = wandManager.getPos2(player.getUniqueId());
        } else {
            try {
                location1 = LocationUtils.parseLocation(sender, args[1], args[2], args[3]);
                location2 = LocationUtils.parseLocation(sender, args[4], args[5], args[6]);
            } catch (NumberFormatException e) {
                TextUtils.error(sender, "Coordinates must be valid numbers");
                return;
            }
        }

        if (!location1.getWorld().equals(location2.getWorld())) {
            TextUtils.error(sender, "Positions must be in the same world");
            return;
        }

        try {
            mineManager.redefineMine(mineName, location1, location2);
        } catch (MineException e) {
            TextUtils.error(sender, e.getMessage());
            return;
        }
        mineManager.resetMine(mineName);
        TextUtils.success(sender, "Mine '" + mineName + "' redefined successfully");
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
        return "<mine> [<x1> <y1> <z1> <x2> <y2> <z2>]";
    }

    @Override
    public String getName() {
        return "redefine";
    }

    @Override
    public String getDescription() {
        return "Redefine the area of a mine, from coordinates or your wand selection";
    }
}
