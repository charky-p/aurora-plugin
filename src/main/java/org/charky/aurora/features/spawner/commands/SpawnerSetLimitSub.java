package org.charky.aurora.features.spawner.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.Collections;
import java.util.List;

public class SpawnerSetLimitSub extends SpawnerSub {
    public SpawnerSetLimitSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String spawnerName = args[0];
        if (!spawnerManager.spawnerExists(spawnerName)) {
            TextUtils.error(sender, "Spawner '" + spawnerName + "' does not exist");
            return;
        }

        try {
            int limit = Integer.parseInt(args[1]);
            if (limit < 1) {
                TextUtils.error(sender, "Limit must be at least 1, use toggle to turn a spawner off");
                return;
            }

            spawnerManager.setLimit(spawnerName, limit);
            TextUtils.success(sender, "Spawner '" + spawnerName + "' limit has been set to " + limit + " successfully");
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Invalid limit '" + args[1] + "'");
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return TextUtils.getCompletions(args[0], spawnerManager.getSpawners());
        }

        if (args.length == 2) {
            return List.of("1", "3", "5");
        }

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<spawner> <limit>";
    }

    @Override
    public String getName() {
        return "setlimit";
    }

    @Override
    public String getDescription() {
        return "Sets the mob limit of a spawner";
    }
}
