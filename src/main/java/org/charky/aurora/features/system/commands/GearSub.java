package org.charky.aurora.features.system.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.tool.UpgradeZone;

import java.util.List;

/**
 * Sets every tool and armour piece of a player to what they have on reaching a zone,
 * or on finishing it with end: the zones played before it bought out in full, for a
 * player who plays one region at a time. Every other level goes
 */
public class GearSub implements AuroraSubCommand {
    private static final List<String> WHEN = List.of("start", "end");

    private final Aurora plugin;

    public GearSub(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            TextUtils.error(sender, "Player '" + args[0] + "' is not online");
            return;
        }
        UpgradeZone zone = plugin.getRegistry().getUpgradeZone(args[1].toLowerCase());
        if (zone == null) {
            TextUtils.error(sender, "No zone called " + args[1]);
            return;
        }
        String when = args.length > 2 ? args[2].toLowerCase() : "start";
        if (!WHEN.contains(when)) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        boolean finished = when.equals("end");
        plugin.getToolManager().setToZone(target, zone, finished);
        TextUtils.success(sender, target.getName() + "'s gear is set to " + (finished ? "the end of " : "the start of ")
                + zone.name());
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return switch (args.length) {
            case 1 -> TextUtils.getCompletions(args[0], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            case 2 -> TextUtils.getCompletions(args[1], plugin.getRegistry().getUpgradeZoneIds());
            case 3 -> TextUtils.getCompletions(args[2], WHEN);
            default -> List.of();
        };
    }

    @Override
    public String getUsage() {
        return "<player> <zone> [start|end]";
    }

    @Override
    public String getName() {
        return "gear";
    }

    @Override
    public String getDescription() {
        return "Set a player's tools and armour to what they have at the start or end of a zone";
    }
}
