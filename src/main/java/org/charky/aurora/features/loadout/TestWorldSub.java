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
 * Goes to a test world on the experimental loadout. The first time, when there is no
 * experimental loadout yet, it starts as a copy of your main one instead of from
 * nothing, to try what you have built with the progress you really have. After that
 * the two go their own ways, and this goes to the experimental loadout as it is
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
        // In the Gauntlet the inventory is its gear, the player's own is put away
        if (plugin.getGauntletManager() != null && plugin.getGauntletManager().isHere(player)) {
            TextUtils.error(player, "Leave the Gauntlet first, its gear is not your inventory");
            return;
        }
        String name = args.length > 0 ? args[0] : Loadout.TEST_WORLD_PREFIX;
        World world = Bukkit.getWorld(name);
        if (world == null || Loadout.of(world.getName()) != Loadout.EXPERIMENTAL) {
            TextUtils.error(sender, "'" + name + "' is not a loaded test world");
            return;
        }

        Location spawn = plugin.getSpawn(world);
        boolean copied = plugin.getLoadoutManager().visit(player, spawn != null ? spawn : world.getSpawnLocation());
        if (copied) TextUtils.success(player, "Your experimental loadout starts as a copy of your main one");
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
        return "Go to a test world, the first time with your experimental loadout copied from your main one";
    }
}
