package org.charky.aurora.features.portal.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.portal.Portal;

import java.util.Collections;
import java.util.List;

/**
 * Portal sub command to delete a portal
 */
public class PortalDeleteSub extends PortalSub {
    public PortalDeleteSub(Aurora plugin) {
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

        portalManager.remove(portal.getName());
        TextUtils.success(sender, "Portal '" + portal.getName() + "' deleted successfully");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], portalManager.getNames());
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<portal>";
    }

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Delete a portal";
    }
}
