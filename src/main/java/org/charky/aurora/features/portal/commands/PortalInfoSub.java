package org.charky.aurora.features.portal.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.portal.Portal;

import java.util.Collections;
import java.util.List;

/**
 * Portal sub command to show a portal's box and destination, laid out like mine info
 */
public class PortalInfoSub extends PortalSub {
    public PortalInfoSub(Aurora plugin) {
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

        String destination = portal.hasDestination()
                ? "<white>" + portal.getDestWorld() + " " + (int) Math.floor(portal.getDestX()) + "<gray>, <white>"
                + (int) Math.floor(portal.getDestY()) + "<gray>, <white>" + (int) Math.floor(portal.getDestZ())
                + (portal.getDestination() == null ? " <red>(world not loaded)" : "")
                : "<red>Not set";

        TextUtils.display(sender, "<aqua><st>                              </st> <b>PORTAL INFO</b> <st>                              </st>");
        TextUtils.display(sender, " <dark_gray>» <aqua>Name: <white>" + portal.getName());
        TextUtils.display(sender, " <dark_gray>» <aqua>World: <white>" + portal.getWorld());
        TextUtils.display(sender, " <dark_gray>» <aqua>From: <white>" + portal.getX1() + "<gray>, <white>" + portal.getY1() + "<gray>, <white>" + portal.getZ1());
        TextUtils.display(sender, " <dark_gray>» <aqua>To: <white>" + portal.getX2() + "<gray>, <white>" + portal.getY2() + "<gray>, <white>" + portal.getZ2());
        TextUtils.display(sender, " <dark_gray>» <aqua>Leads to: " + destination);
        TextUtils.display(sender, " <dark_gray>» <aqua>Needs tag: " + (portal.getRequiredTag() == null
                ? "<gray>None, anyone can enter" : "<white>" + portal.getRequiredTag()));
        TextUtils.line(sender, "<aqua>");
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
        return "info";
    }

    @Override
    public String getDescription() {
        return "Display information about a portal";
    }
}
