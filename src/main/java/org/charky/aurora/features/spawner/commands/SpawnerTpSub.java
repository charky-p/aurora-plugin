package org.charky.aurora.features.spawner.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Spawner sub command to teleport to a spawner
 */
public class SpawnerTpSub extends SpawnerSub {
    public SpawnerTpSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String spawnerName = args[0];
        if (!spawnerManager.spawnerExists(spawnerName)) {
            TextUtils.error(sender, "Spawner '" + spawnerName + "' does not exist!");
            return;
        }

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

        if (!spawnerManager.tpSpawner(target, spawnerName)) {
            TextUtils.error(sender, "Spawner '" + spawnerName + "' is in a world that is not loaded");
            return;
        }
        TextUtils.success(target, "Teleported to " + spawnerName + " successfully");
        if (target != sender) {
            TextUtils.success(sender, "Teleported player '" + target.getName() + "' to '" + spawnerName + "' successfully");
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return TextUtils.getCompletions(args[0], spawnerManager.getSpawners());
        }

        if (args.length == 2) {
            return TextUtils.getCompletions(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<spawner> [player]";
    }

    @Override
    public String getName() {
        return "tp";
    }

    @Override
    public String getDescription() {
        return "Teleports to a spawner";
    }
}
