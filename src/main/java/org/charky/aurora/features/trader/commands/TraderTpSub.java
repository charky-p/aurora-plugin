package org.charky.aurora.features.trader.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.trader.TraderSpot;

import java.util.Collections;
import java.util.List;

/**
 * Trader sub command to teleport to a trader spot
 */
public class TraderTpSub extends TraderSub {
    public TraderTpSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        TraderSpot spot = parseSpot(sender, args[0]);
        if (spot == null) return;

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                TextUtils.error(sender, "Player '" + args[1] + "' is not online");
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                TextUtils.error(sender, "Console must specify a player to teleport");
                return;
            }
            target = player;
        }

        Location location = spot.toLocation();
        if (location == null) {
            TextUtils.error(sender, "Trader spot #" + spot.id() + " is in a world that is not loaded");
            return;
        }

        // Two blocks in front of the trader, facing it
        Location front = location.clone().add(location.getDirection().multiply(2));
        front.setYaw(location.getYaw() + 180);
        target.teleport(front);

        TextUtils.success(target, "Teleported to trader spot #" + spot.id() + " successfully");
        if (target != sender) {
            TextUtils.success(sender, "Teleported player '" + target.getName() + "' to trader spot #" + spot.id() + " successfully");
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) return TextUtils.getCompletions(args[0], traderManager.getSpotIds());
        if (args.length == 2) {
            return TextUtils.getCompletions(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<spot> [player]";
    }

    @Override
    public String getName() {
        return "tp";
    }

    @Override
    public String getDescription() {
        return "Teleports to a trader spot";
    }
}
