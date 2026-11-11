package org.charky.aurora.features.spawner.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.Collections;
import java.util.List;

public class SpawnerRenameSub extends SpawnerSub {
    public SpawnerRenameSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String oldName = args[0];
        if (!spawnerManager.spawnerExists(oldName)) {
            TextUtils.error(sender, "Spawner '" + oldName + "' does not exist");
            return;
        }

        String newName = args[1];
        if (spawnerManager.spawnerExists(newName)) {
            TextUtils.error(sender, "Spawner '" + newName + "' already exists");
            return;
        }

        spawnerManager.rename(oldName, newName);
        TextUtils.success(sender, "Spawner '" + oldName + "' renamed to '" + newName + "' successfully");
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
        return "<oldName> <newName>";
    }

    @Override
    public String getName() {
        return "rename";
    }

    @Override
    public String getDescription() {
        return "Rename a spawner";
    }
}
