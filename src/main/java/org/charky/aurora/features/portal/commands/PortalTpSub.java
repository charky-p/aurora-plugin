package org.charky.aurora.features.portal.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.portal.Portal;

import java.util.Collections;
import java.util.List;

/**
 * Portal sub command to teleport into a portal. Arriving inside does not trigger it,
 * step out and back in to use it
 */
public class PortalTpSub extends PortalSub {
    public PortalTpSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        Portal portal = find(sender, args[0]);
        if (portal == null) return;

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                TextUtils.error(sender, "Player '" + args[1] + "' is not online");
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            TextUtils.error(sender, "Console must specify a player to teleport");
            return;
        }

        Location centre = portal.getCentre();
        if (centre == null) {
            TextUtils.error(sender, "Portal '" + portal.getName() + "' is in a world that is not loaded");
            return;
        }

        target.teleport(centre);
        TextUtils.success(sender, "Teleported " + target.getName() + " to portal '" + portal.getName() + "'");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], portalManager.getNames());
        if (args.length == 2) {
            return TextUtils.playerCompletions(args[1]);
        }
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<portal> [player]";
    }

    @Override
    public String getName() {
        return "tp";
    }

    @Override
    public String getDescription() {
        return "Teleport into a portal";
    }
}
