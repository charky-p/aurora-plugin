package org.charky.aurora.features.system.commands;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.LocationUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.TextUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Aurora admin command to set world spawn of the current world, different worlds have different spawns
 */
public class SetSpawnCommand implements AuroraSubCommand {
    private final Aurora plugin;

    public SetSpawnCommand(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // main first: this world's spawn also becomes where every player joins
        boolean main = args.length > 0 && args[0].equalsIgnoreCase("main");
        if (main) args = Arrays.copyOfRange(args, 1, args.length);

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                TextUtils.error(sender, "Console must give coordinates");
                return;
            }
            set(sender, player.getLocation(), main);
            return;
        }

        if (args.length < 3) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        try {
            double x = LocationUtils.parseCoordinate(sender, args[0], Axis.X);
            double y = LocationUtils.parseCoordinate(sender, args[1], Axis.Y);
            double z = LocationUtils.parseCoordinate(sender, args[2], Axis.Z);

            // Given, or the way the player faces snapped to a side, south for the console
            float yaw;
            if (args.length >= 4) {
                yaw = Float.parseFloat(args[3]);
            } else if (sender instanceof Player player) {
                yaw = switch (player.getFacing()) {
                    case NORTH -> 180;
                    case EAST -> 270;
                    case WEST -> 90;
                    default -> 0;
                };
            } else {
                yaw = 0;
            }

            World world = LocationUtils.commandWorld(sender);
            set(sender, new Location(world, x, y, z, yaw, 0), main);
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Invalid coordinates: " + e.getMessage());
        }
    }

    private void set(CommandSender sender, Location location, boolean main) {
        String where = location.getWorld().getName() + " at " + Math.round(location.getX()) + ", "
                + Math.round(location.getY()) + ", " + Math.round(location.getZ());
        if (main) {
            plugin.setMainSpawn(location);
            TextUtils.success(sender, "Main spawn set in " + where + ", every player joins here");
        } else {
            plugin.setSpawn(location);
            TextUtils.success(sender, "Spawn set for " + where);
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return List.of("main", "~");
        int coordinate = args[0].equalsIgnoreCase("main") ? args.length - 1 : args.length;
        if (coordinate >= 1 && coordinate <= 3) return List.of("~");
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "[main] [<x> <y> <z> [yaw]]";
    }

    @Override
    public String getName() {
        return "setspawn";
    }

    @Override
    public String getDescription() {
        return "Set the spawn of this world, with main also where every player joins";
    }
}