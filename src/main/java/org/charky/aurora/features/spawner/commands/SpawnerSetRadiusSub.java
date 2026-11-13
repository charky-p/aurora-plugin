package org.charky.aurora.features.spawner.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.Collections;
import java.util.List;

public class SpawnerSetRadiusSub extends SpawnerSub {
    public SpawnerSetRadiusSub(Aurora plugin) {
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
            double radius = Double.parseDouble(args[1]);
            if (radius <= 0) {
                TextUtils.error(sender, "Radius must be above 0");
                return;
            }

            spawnerManager.setRadius(spawnerName, radius);
            TextUtils.success(sender, "Spawner '" + spawnerName + "' radius has been set to " + radius + " successfully");
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Invalid radius '" + args[1] + "'");
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return TextUtils.getCompletions(args[0], spawnerManager.getSpawners());
        }

        if (args.length == 2) {
            return List.of("5", "10", "25");
        }

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<spawner> <radius>";
    }

    @Override
    public String getName() {
        return "setradius";
    }

    @Override
    public String getDescription() {
        return "Sets the radius of a spawner";
    }
}
