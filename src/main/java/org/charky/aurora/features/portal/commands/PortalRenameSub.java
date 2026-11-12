package org.charky.aurora.features.portal.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.portal.Portal;

import java.util.Collections;
import java.util.List;

/**
 * Portal sub command to rename a portal
 */
public class PortalRenameSub extends PortalSub {
    public PortalRenameSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        Portal portal = find(sender, args[0]);
        if (portal == null) return;

        String newName = args[1];
        // A case change of its own name is a valid rename
        if (!newName.equalsIgnoreCase(args[0]) && portalManager.exists(newName)) {
            TextUtils.error(sender, "Portal '" + newName + "' already exists");
            return;
        }

        String oldName = portal.getName();
        portalManager.rename(oldName, newName);
        TextUtils.success(sender, "Portal '" + oldName + "' renamed to '" + newName + "' successfully");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], portalManager.getNames());
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<portal> <new name>";
    }

    @Override
    public String getName() {
        return "rename";
    }

    @Override
    public String getDescription() {
        return "Rename a portal";
    }
}
