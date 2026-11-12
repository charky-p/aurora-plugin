package org.charky.aurora.features.portal.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.portal.Portal;

import java.util.Collections;
import java.util.List;

/**
 * Portal sub command to set where a portal leads, where you stand and the way you
 * face, or a world and coordinates
 */
public class PortalSetDestSub extends PortalSub {
    public PortalSetDestSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 1 && args.length < 5) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        Portal portal = find(sender, args[0]);
        if (portal == null) return;

        Location destination;
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                TextUtils.error(sender, "Console must give a world and coordinates");
                return;
            }
            destination = player.getLocation();
        } else {
            World world = Bukkit.getWorld(args[1]);
            if (world == null) {
                TextUtils.error(sender, "World '" + args[1] + "' is not loaded");
                return;
            }
            try {
                destination = new Location(world, Double.parseDouble(args[2]), Double.parseDouble(args[3]),
                        Double.parseDouble(args[4]),
                        args.length > 5 ? Float.parseFloat(args[5]) : 0f,
                        args.length > 6 ? Float.parseFloat(args[6]) : 0f);
            } catch (NumberFormatException e) {
                TextUtils.error(sender, "Invalid coordinates: " + e.getMessage());
                return;
            }
        }

        if (portal.contains(destination)) {
            TextUtils.error(sender, "The destination can't be inside the portal itself");
            return;
        }

        portalManager.setDestination(portal.getName(), destination);
        TextUtils.success(sender, "Portal '" + portal.getName() + "' now leads to " + destination.getWorld().getName()
                + " " + destination.getBlockX() + ", " + destination.getBlockY() + ", " + destination.getBlockZ());
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], portalManager.getNames());
        if (args.length == 2) return TextUtils.getCompletions(args[1], Bukkit.getWorlds().stream().map(World::getName).toList());
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<portal> [<world> <x> <y> <z> [yaw] [pitch]]";
    }

    @Override
    public String getName() {
        return "setdest";
    }

    @Override
    public String getDescription() {
        return "Set where a portal leads, defaults to where you stand";
    }
}
