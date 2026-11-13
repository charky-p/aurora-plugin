package org.charky.aurora.features.spawner.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.Collections;
import java.util.List;

public class SpawnerToggleSub extends SpawnerSub {
    public SpawnerToggleSub(Aurora plugin) {
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

        boolean toggle = spawnerManager.spawnerToggle(spawnerName);
        if (toggle) {
            TextUtils.success(sender, "Spawner '" + spawnerName + "' has been toggled on");
        } else {
            TextUtils.success(sender, "Spawner '" + spawnerName + "' has been toggled off");
        }
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
        return "toggle";
    }

    @Override
    public String getDescription() {
        return "Toggle a spawner";
    }
}
