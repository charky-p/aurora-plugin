package org.charky.aurora.common.util;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class LocationUtils {
    private LocationUtils() {
    }

    // Set by /aurora -w <world> for the one command it runs, commands run on the
    // main thread one at a time so a plain field is enough
    private static World commandWorld;

    // The world a command works in: the -w world if given, else the player's own,
    // else the main world, since the console has no world of its own
    public static World commandWorld(CommandSender sender) {
        if (commandWorld != null) return commandWorld;
        if (sender instanceof Player player) return player.getWorld();
        return Bukkit.getWorlds().getFirst();
    }

    public static void runIn(World world, Runnable command) {
        World previous = commandWorld;
        commandWorld = world;
        try {
            command.run();
        } finally {
            commandWorld = previous;
        }
    }

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

    public static Location parseLocation(CommandSender sender, String xStr, String yStr, String zStr) throws NumberFormatException {
        return parseLocation(sender, commandWorld(sender).getName(), xStr, yStr, zStr);
    }

    public static Location parseLocation(CommandSender sender, String worldName, String xStr, String yStr, String zStr) throws NumberFormatException {
        World world = Bukkit.getWorld(worldName);
        if (world == null) throw new NumberFormatException("Unknown world: " + worldName);

        double x = parseCoordinate(sender, xStr, Axis.X);
        double y = Math.clamp(parseCoordinate(sender, yStr, Axis.Y), -64, 319);
        double z = parseCoordinate(sender, zStr, Axis.Z);

        return new Location(world, x, y, z);
    }
}
