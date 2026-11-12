package org.charky.aurora.features.portal.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.portal.Portal;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Portal sub command to lock a portal behind a tag, players without it are knocked
 * back. Key items in resources.csv give the tag, leave it out to unlock the portal
 */
public class PortalSetTagSub extends PortalSub {
    private final DataRegistry registry;

    public PortalSetTagSub(Aurora plugin) {
        super(plugin);
        this.registry = plugin.getRegistry();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        Portal portal = find(sender, args[0]);
        if (portal == null) return;

        if (args.length == 1) {
            portalManager.setRequiredTag(portal.getName(), null);
            TextUtils.success(sender, "Portal '" + portal.getName() + "' is open to everyone");
            return;
        }

        String tag = args[1].toLowerCase();
        if (!tag.matches("[a-z0-9_.+-]+")) {
            TextUtils.error(sender, "Tags use lowercase letters, numbers and _ . + -");
            return;
        }

        portalManager.setRequiredTag(portal.getName(), tag);
        List<String> keys = registry.getKeysFor(tag);
        TextUtils.success(sender, "Portal '" + portal.getName() + "' now needs the tag '" + tag + "'");
        if (keys.isEmpty()) {
            TextUtils.display(sender, "<yellow>No item gives '" + tag + "' yet, add it to the unlocks column of resources.csv, or use /tag <player> add " + tag);
        } else {
            TextUtils.display(sender, "<gray>Given by: <white>" + String.join(", ", keys));
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], portalManager.getNames());
        if (args.length == 2) {
            Set<String> tags = new LinkedHashSet<>();
            for (var item : registry.getItems()) {
                String tag = registry.getUnlock(item.getId());
                if (tag != null) tags.add(tag);
            }
            return TextUtils.getCompletions(args[1], List.copyOf(tags));
        }
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<portal> [tag]";
    }

    @Override
    public String getName() {
        return "settag";
    }

    @Override
    public String getDescription() {
        return "Lock a portal to players with a tag, no tag unlocks it";
    }
}
