package org.charky.aurora.features.spawner.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.spawner.MobSpawner;

import java.util.Collections;
import java.util.List;

/**
 * Spawner sub command to get information about a spawner, laid out like mine info
 */
public class SpawnerInfoSub extends SpawnerSub {
    public SpawnerInfoSub(Aurora plugin) {
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

        MobSpawner spawner = spawnerManager.getSpawner(spawnerName);
        String state = spawner.isActive() ? "<green>On" : "<red>Off";

        TextUtils.display(sender, "<aqua><st>                              </st> <b>SPAWNER INFO</b> <st>                              </st>");
        TextUtils.display(sender, " <dark_gray>» <aqua>Name: <white>" + spawner.getName());
        TextUtils.display(sender, " <dark_gray>» <aqua>State: " + state);
        TextUtils.display(sender, " <dark_gray>» <aqua>World: <white>" + spawner.getWorld());
        TextUtils.display(sender, " <dark_gray>» <aqua>Coordinate: <white>" + spawner.getX() + "<gray>, <white>" + spawner.getY() + "<gray>, <white>" + spawner.getZ());
        TextUtils.display(sender, " <dark_gray>» <aqua>Mob: <white>" + spawner.getMobName());
        TextUtils.display(sender, " <dark_gray>» <aqua>Alive: <white>" + spawnerManager.countFor(spawnerName) + "<gray>/<white>" + spawner.getLimit());
        TextUtils.display(sender, " <dark_gray>» <aqua>Radius: <white>" + spawner.getRadius());
        // Stored in ticks, 20 to a second
        TextUtils.display(sender, " <dark_gray>» <aqua>Delay: <white>" + spawner.getDelay() + " ticks <gray>(" + spawner.getDelay() / 20.0 + "s)");
        TextUtils.line(sender, "<aqua>");
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
        return "info";
    }

    @Override
    public String getDescription() {
        return "Display information about a spawner";
    }
}
