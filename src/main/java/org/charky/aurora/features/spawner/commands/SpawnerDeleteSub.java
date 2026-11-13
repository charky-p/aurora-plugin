package org.charky.aurora.features.spawner.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Spawner sub command to delete a spawner
 */
public class SpawnerDeleteSub extends SpawnerSub {
    public SpawnerDeleteSub(Aurora plugin) {
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

        spawnerManager.removeSpawner(spawnerName);
        TextUtils.success(sender, "Spawner '" + spawnerName + "' has been successfully removed");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return TextUtils.getCompletions(args[0], spawnerManager.getSpawners());
        }

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<spawner>";
    }

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Delete a spawner";
    }
}
