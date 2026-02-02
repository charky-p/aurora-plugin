package org.charky.aurora.features.system.commands;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.LocationUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.TextUtils;

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
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Console cannot execute this command!");
            return;
        }

        if (args.length == 0) {
            plugin.setSpawn(player.getLocation());
            TextUtils.success(sender, "Spawn set for " + player.getWorld().getName() + " at " + player.getLocation());
            return;
        }

        if (args.length < 3) {
            TextUtils.usage(player, getFullUsage());
            return;
        }

        try {
            double x = LocationUtils.parseCoordinate(player, args[0], Axis.X);
            double y = LocationUtils.parseCoordinate(player, args[1], Axis.Y);
            double z = LocationUtils.parseCoordinate(player, args[2], Axis.Z);

            // Default to south
            float yaw = switch (player.getFacing()) {
                case NORTH -> 180;
                case EAST -> 270;
                case WEST -> 90;
                default -> 0;
            };

            plugin.setSpawn(new Location(player.getWorld(), x, y, z, yaw, 0));
            TextUtils.success(sender, "Spawn set for " + player.getWorld().getName() + " at " + x + ", " + y + ", " + z);
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Invalid coordinates: " + e.getMessage());
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length >= 1 && args.length <= 3) {
            return List.of("~");
        }
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "[<x> <y> <z>]";
    }

    @Override
    public String getName() {
        return "setspawn";
    }

    @Override
    public String getDescription() {
        return "Set the spawn location of the current world";
    }
}