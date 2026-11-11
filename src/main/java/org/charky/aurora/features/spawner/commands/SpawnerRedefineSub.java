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
        // Just the name moves it to where you stand, otherwise all three coordinates
        if (args.length != 1 && args.length < 4) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String spawnerName = args[0];
        if (!spawnerManager.spawnerExists(spawnerName)) {
            TextUtils.error(sender, "Spawner '" + spawnerName + "' does not exist");
            return;
        }

        if (args.length == 1 && !(sender instanceof Player)) {
            TextUtils.error(sender, "Console must give coordinates");
            return;
        }

        try {
            String[] coords = (args.length == 1) ? new String[]{"~", "~", "~"} : new String[]{args[1], args[2], args[3]};
            // Floor, not a cast, or negative coordinates land one block off
            int x = (int) Math.floor(LocationUtils.parseCoordinate(sender, coords[0], Axis.X));
            int y = (int) Math.floor(LocationUtils.parseCoordinate(sender, coords[1], Axis.Y));
            int z = (int) Math.floor(LocationUtils.parseCoordinate(sender, coords[2], Axis.Z));

            spawnerManager.redefine(spawnerName, x, y, z);
            TextUtils.success(sender, "Spawner '" + spawnerName + "' has been redefined successfully");
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Invalid coordinates: " + e.getMessage());
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return TextUtils.getCompletions(args[0], spawnerManager.getSpawners());
        }

        if (args.length >= 2 && args.length <= 4) {
            return LocationUtils.relativeCoordinates(player, args.length - 2);
        }

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<spawner> [<x> <y> <z>]";
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
