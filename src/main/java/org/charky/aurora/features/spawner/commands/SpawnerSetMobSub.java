package org.charky.aurora.features.spawner.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.Collections;
import java.util.List;

public class SpawnerSetMobSub extends SpawnerSub {
    public SpawnerSetMobSub(Aurora plugin) {
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

        String mobName = args[1];
        if (!registry.hasMob(mobName)) {
            TextUtils.error(sender, "Mob '" + mobName + "' does not exist");
            return;
        }

        spawnerManager.setMob(spawnerName, mobName);
        TextUtils.success(sender, "Spawner '" + spawnerName + "' mob has been set to " + mobName + " successfully");
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return TextUtils.getCompletions(args[0], spawnerManager.getSpawners());
        }

        if (args.length == 2) {
            return registry.getMobs()
                    .stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .sorted()
                    .toList();
        }

        return Collections.emptyList();
    }

    @Override
    public String getUsage() {
        return "<spawner> <mob>";
    }

    @Override
    public String getName() {
        return "setmob";
    }

    @Override
    public String getDescription() {
        return "Sets the mob of a spawner";
    }
}
