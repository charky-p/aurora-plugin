package org.charky.aurora.features.spawner.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.commands.AuroraSubCommand;
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
        if (find(sender, spawnerName) == null) return;

        Player target = AuroraSubCommand.targetPlayer(sender, args, 1);
        if (target == null) return;

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
            return TextUtils.playerCompletions(args[1]);
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
