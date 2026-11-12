package org.charky.aurora.features.spawner.commands;

import org.bukkit.Bukkit;
import org.bukkit.Axis;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.LocationUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.common.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Spawner sub command to create a spawner at a given location, mob limit and radius can be provided.
 */
public class SpawnerCreateSub extends SpawnerSub {
    public SpawnerCreateSub(Aurora plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 5) {
            TextUtils.usage(sender, getFullUsage());
            return;
        }

        String spawnerName = args[0];
        if (spawnerManager.spawnerExists(spawnerName)) {
            TextUtils.error(sender, "Spawner '" + spawnerName + "' already exists");
            return;
        }

        try {
            int x = (int) Math.floor(LocationUtils.parseCoordinate(sender, args[1], Axis.X));
            int y = (int) Math.floor(LocationUtils.parseCoordinate(sender, args[2], Axis.Y));
            int z = (int) Math.floor(LocationUtils.parseCoordinate(sender, args[3], Axis.Z));
            String mobName = args[4];

            if (!spawnerManager.mobExists(mobName)) {
                TextUtils.error(sender, "Mob '" + mobName + "' does not exist");
                return;
            }

            // The console has no world of its own, it places spawners in the main world
            String world = (sender instanceof Player player)
                    ? player.getWorld().getName() : Bukkit.getWorlds().getFirst().getName();

            try {
                // Default limit = 3, radius = 5
                int limit = (args.length < 6) ? 3 : Integer.parseInt(args[5]);
                double radius = (args.length < 7) ? 5 : Double.parseDouble(args[6]);
                int delay = (args.length < 8) ? 200 : Integer.parseInt(args[7]);

                // Same limits as the set commands, a delay of 0 would spawn every tick
                if (limit < 1 || radius <= 0 || delay < 1) {
                    TextUtils.error(sender, "Limit and delay must be at least 1, radius above 0");
                    return;
                }

                spawnerManager.addSpawner(spawnerName, world, x, y, z, mobName, limit, radius, delay);
                TextUtils.success(sender, "Spawner '" + spawnerName + "' has been successfully added");
            } catch (NumberFormatException e) {
                TextUtils.error(sender, "Limit, radius and delay must be valid numbers");
            }
        } catch (NumberFormatException e) {
            TextUtils.error(sender, "Invalid coordinates: " + e.getMessage());
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length >= 2 && args.length <= 4) {
            return List.of("~");
        } else if (args.length == 5) {
            return registry.getMobs()
                    .stream()
                    .filter(name -> name.toLowerCase().startsWith(args[4].toLowerCase()))
                    .sorted()
                    .toList();
        }

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<spawner> <x> <y> <z> <mob> [limit] [radius] [delay]";
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Create a new mob spawner";
    }
}
