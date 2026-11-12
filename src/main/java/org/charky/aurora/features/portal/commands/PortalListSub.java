package org.charky.aurora.features.portal.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.portal.Portal;

import java.util.Collections;
import java.util.List;

/**
 * Portal sub command to list every portal and where it leads
 */
public class PortalListSub extends PortalSub {
    public PortalListSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<String> names = TextUtils.getCompletions("", portalManager.getNames());
        if (names.isEmpty()) {
            TextUtils.warn(sender, "There are no portals created yet");
            return;
        }

        TextUtils.display(sender, "<aqua><st>                              </st> <b>PORTALS</b> <st>                              </st>");
        for (String name : names) {
            Portal portal = portalManager.get(name);
            String leads = portal.hasDestination() ? "<gray>" + portal.getWorld() + " → " + portal.getDestWorld() : "<red>no destination";
            String infoIcon = "<click:run_command:'/aurora portal info " + name + "'><hover:show_text:'<aqua>Click for <aqua>Info'><aqua>ℹ</aqua></hover></click>";
            String tpIcon = "<click:run_command:'/aurora portal tp " + name + "'><hover:show_text:'<green>Teleport to <green>" + name + "'><green>➹</green></hover></click>";
            TextUtils.display(sender, " <dark_gray>» <gray>" + name + " " + leads + " " + infoIcon + " " + tpIcon);
        }
        TextUtils.line(sender, "<aqua>");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "List every portal and where it leads";
    }
}
