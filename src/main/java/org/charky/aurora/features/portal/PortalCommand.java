package org.charky.aurora.features.portal;

import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.portal.commands.*;
import org.charky.aurora.features.system.commands.SubCommandRouter;

/**
 * Portal commands. A portal is a box like a mine, walking into it sends you to its
 * destination, which can be in another world
 */
public class PortalCommand extends SubCommandRouter {
    public PortalCommand(Aurora plugin) {
        super("portal", "Create portals that take you between worlds");
        register(new PortalCreateSub(plugin));
        register(new PortalSetDestSub(plugin));
        register(new PortalSetTagSub(plugin));
        register(new PortalDeleteSub(plugin));
        register(new PortalRenameSub(plugin));
        register(new PortalListSub(plugin));
        register(new PortalInfoSub(plugin));
        register(new PortalTpSub(plugin));
    }
}
