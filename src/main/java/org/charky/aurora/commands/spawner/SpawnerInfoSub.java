package org.charky.aurora.commands.spawner;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.util.TextUtils;

import java.util.Collections;
import java.util.List;

/**
 * Spawner sub command to get information about a spawner
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
        // TODO: spawnername and minename should be lowercase for existence
        if (!spawnerManager.spawnerExists(spawnerName)) {
            TextUtils.error(sender, "Spawner '" + spawnerName + "' does not exist");
            return;
        }

        TextUtils.display(sender, "<aqua><b><st>               </st>SPAWNER INFO<st>               </st>");
        for (String info : spawnerManager.getSpawnerInfo(spawnerName)) {
            TextUtils.display(sender, " <dark_gray>» <gray>" + info);
        }
        TextUtils.line(sender, "<aqua>");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return spawnerManager.getSpawners()
                    .stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
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
