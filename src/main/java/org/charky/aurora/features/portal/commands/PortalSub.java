package org.charky.aurora.features.portal.commands;

import org.bukkit.command.CommandSender;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.portal.Portal;
import org.charky.aurora.features.portal.PortalManager;
import org.charky.aurora.features.system.WandManager;
import org.charky.aurora.features.system.commands.AuroraSubCommand;

public abstract class PortalSub implements AuroraSubCommand {
    protected final PortalManager portalManager;
    protected final WandManager wandManager;

    public PortalSub(Aurora plugin) {
        this.portalManager = plugin.getPortalManager();
        this.wandManager = plugin.getWandManager();
    }

    @Override
    public String getFullUsage() {
        return "/aurora portal " + getName() + " " + getUsage();
    }

    // Null after telling the sender it does not exist
    protected Portal find(CommandSender sender, String name) {
        Portal portal = portalManager.get(name);
        if (portal == null) TextUtils.error(sender, "Portal '" + name + "' does not exist");
        return portal;
    }
}
