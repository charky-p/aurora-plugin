package org.charky.aurora.features.spawner.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.Collections;
import java.util.List;

public class SpawnerSetDelaySub extends SpawnerSub {
    public SpawnerSetDelaySub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String spawnerName = args[0];
        if (find(sender, spawnerName) == null) return;

        try {
            int delay = Integer.parseInt(args[1]);
            if (delay < 1) {
                TextUtils.error(sender, "Delay must be at least 1 tick");
                return;
            }

            spawnerManager.setDelay(spawnerName, delay);
            TextUtils.success(sender, "Spawner '" + spawnerName + "' delay has been set to " + delay + " successfully");
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Invalid delay '" + args[1] + "'");
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return TextUtils.getCompletions(args[0], spawnerManager.getSpawners());
        }

        if (args.length == 2) {
            return List.of("200", "600", "1200");
        }

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<spawner> <delay>";
    }

    @Override
    public String getName() {
        return "setdelay";
    }

    @Override
    public String getDescription() {
        return "Sets the spawn delay of a spawner in ticks, where 20 is one second";
    }
}
