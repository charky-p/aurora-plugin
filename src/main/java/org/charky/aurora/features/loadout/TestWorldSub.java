package org.charky.aurora.features.loadout;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.system.commands.AuroraSubCommand;

import java.util.List;

/**
 * Goes to a test world keeping your main loadout, to try what you have built with the
 * progress you really have. It lasts until you are back in a main world. Going to a
 * test world any other way plays the experimental loadout as before
 */
public class TestWorldSub implements AuroraSubCommand {
    private final Aurora plugin;

    public TestWorldSub(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Only players have a loadout to take along");
            return;
        }
        String name = args.length > 0 ? args[0] : Loadout.TEST_WORLD_PREFIX;
        World world = Bukkit.getWorld(name);
        if (world == null || Loadout.of(world.getName()) != Loadout.EXPERIMENTAL) {
            TextUtils.error(sender, "'" + name + "' is not a loaded test world");
            return;
        }

        Location spawn = plugin.getSpawn(world);
        plugin.getLoadoutManager().visitWithMain(player, spawn != null ? spawn : world.getSpawnLocation());
        TextUtils.success(player, "In " + world.getName() + " with your main loadout, until you go back to a main world");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length != 1) return List.of();
        return TextUtils.getCompletions(args[0], Bukkit.getWorlds().stream().map(World::getName)
                .filter(name -> Loadout.of(name) == Loadout.EXPERIMENTAL).toList());
    }

    @Override
    public String getUsage() {
        return "[world]";
    }

    @Override
    public String getName() {
        return "testworld";
    }

    @Override
    public String getDescription() {
        return "Go to a test world keeping your main loadout, aurora_test without a world";
    }
}
