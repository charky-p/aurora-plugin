package org.charky.aurora.features.spawner.commands;

import org.bukkit.Axis;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.LocationUtils;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.Collections;
import java.util.List;

public class SpawnerRedefineSub extends SpawnerSub {
    public SpawnerRedefineSub(Aurora plugin) {
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
            int x = (int) LocationUtils.parseCoordinate(sender, args[1], Axis.X);
            int y = (int) LocationUtils.parseCoordinate(sender, args[2], Axis.Y);
            int z = (int) LocationUtils.parseCoordinate(sender, args[3], Axis.Z);

            spawnerManager.redefine(spawnerName, x, y, z);
            TextUtils.success(sender, "Spawner '" + spawnerName + "' has been redefined successfully");
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Invalid coordinates: " + e.getMessage());
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return spawnerManager.getSpawners()
                    .stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .sorted()
                    .toList();
        }

        if (args.length >= 2 && args.length <= 4) {
            return LocationUtils.relativeCoordinates(player, args.length - 2);
        }

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<spawner> <x> <y> <z>";
    }

    @Override
    public String getName() {
        return "redefine";
    }

    @Override
    public String getDescription() {
        return "Sets the coordinate of a spawner";
    }
}
