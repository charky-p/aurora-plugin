package org.charky.aurora.common.util;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class LocationUtils {
    public static double parseCoordinate(CommandSender sender, String input, Axis axis) throws NumberFormatException {
        if (input.startsWith("~")) {
            if (!(sender instanceof Player player)) {
                throw new NumberFormatException("Relative coordinates (~) cannot be used from console");
            }

            Location location = player.getLocation();
            double base = switch (axis) {
                case X -> location.getX();
                case Y -> location.getY();
                case Z -> location.getZ();
            };

            if (input.length() == 1) return base;
            return base + Double.parseDouble(input.substring(1));
        }
        return Double.parseDouble(input);
    }

    public static List<String> relativeCoordinates(Player player, int index) {
        Block target = player.getTargetBlockExact(5);
        int coordinateIndex = index % 3;

        if (target != null) {
            String lookValue = switch (coordinateIndex) {
                case 0 -> String.valueOf(target.getX());
                case 1 -> String.valueOf(target.getY());
                default -> String.valueOf(target.getZ());
            };
            return List.of(lookValue, "~");
        }

        return List.of("~");
    }
}
