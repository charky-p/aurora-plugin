package org.charky.aurora.features.mine.commands;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
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

            World world = (sender instanceof Player p) ? p.getWorld() : Bukkit.getWorld("world");

            double x1 = LocationUtils.parseCoordinate(sender, args[1], Axis.X);
            double y1 = Math.max(-64, Math.min(LocationUtils.parseCoordinate(sender, args[2], Axis.Y), 319));
            double z1 = LocationUtils.parseCoordinate(sender, args[3], Axis.Z);
            double x2 = LocationUtils.parseCoordinate(sender, args[4], Axis.X);
            double y2 = Math.max(-64, Math.min(LocationUtils.parseCoordinate(sender, args[5], Axis.Y), 319));
            double z2 = LocationUtils.parseCoordinate(sender, args[6], Axis.Z);

            int minX = (int) Math.floor(Math.min(x1, x2));
            int minY = (int) Math.floor(Math.min(y1, y2));
            int minZ = (int) Math.floor(Math.min(z1, z2));
            int maxX = (int) Math.floor(Math.max(x1, x2));
            int maxY = (int) Math.floor(Math.max(y1, y2));
            int maxZ = (int) Math.floor(Math.max(z1, z2));

            if ((maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1) > MAX_MINE_VOLUME) {
                TextUtils.error(sender, "Mine volume exceeds limit of " + MAX_MINE_VOLUME);
                return;
            }

            Location location1 = new Location(world, minX, minY, minZ);
            Location location2 = new Location(world, maxX, maxY, maxZ);
            mineManager.redefineMine(mineName, location1, location2);
            TextUtils.success(sender, "Mine '" + mineName + "' redefined successfully");

            mineManager.resetMine(mineName);
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Coordinates must be valid numbers");
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return mineManager.getMines()
                    .stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .sorted()
                    .toList();
        }

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
