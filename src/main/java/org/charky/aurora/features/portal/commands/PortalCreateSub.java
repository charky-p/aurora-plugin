package org.charky.aurora.features.portal.commands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.LocationUtils;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.Collections;
import java.util.List;

/**
 * Portal sub command to create a portal from the wand selection, or from two
 * corners. It leads nowhere until setdest is used
 */
public class PortalCreateSub extends PortalSub {
    public PortalCreateSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 1 && args.length != 7) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String name = args[0];
        if (portalManager.exists(name)) {
            TextUtils.error(sender, "Portal '" + name + "' already exists");
            return;
        }

        Location a, b;
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                TextUtils.error(sender, "Console must give both corners");
                return;
            }
            if (!wandManager.hasBothPositions(player.getUniqueId())) {
                TextUtils.error(sender, "Select two corners with the wand first, or give them as coordinates");
                return;
            }
            a = wandManager.getPos1(player.getUniqueId());
            b = wandManager.getPos2(player.getUniqueId());
        } else {
            try {
                a = LocationUtils.parseLocation(sender, args[1], args[2], args[3]);
                b = LocationUtils.parseLocation(sender, args[4], args[5], args[6]);
            } catch (NumberFormatException e) {
                TextUtils.error(sender, "Invalid coordinates: " + e.getMessage());
                return;
            }
        }

        String problem = portalManager.create(name, a, b);
        if (problem != null) {
            TextUtils.error(sender, problem);
            return;
        }
        TextUtils.success(sender, "Portal '" + name + "' created, stand where it should lead and run /aurora portal setdest " + name);
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length >= 2 && args.length <= 7) {
            return LocationUtils.relativeCoordinates(player, (args.length - 2) % 3);
        }
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<name> [<x1> <y1> <z1> <x2> <y2> <z2>]";
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Create a portal from the wand selection or two corners";
    }
}
