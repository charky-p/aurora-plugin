package org.charky.aurora.features.spawner.commands;

import org.bukkit.command.CommandSender;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.spawner.MobSpawner;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.system.commands.AuroraSubCommand;
import org.charky.aurora.features.spawner.SpawnerManager;

public abstract class SpawnerSub implements AuroraSubCommand {
    protected final DataRegistry registry;
    protected final SpawnerManager spawnerManager;

    public SpawnerSub(Aurora plugin) {
        this.spawnerManager = plugin.getSpawnerManager();
        this.registry = plugin.getRegistry();
    }

    @Override
    public String getFullUsage() {
        return "/aurora spawner " + getName() + " " + getUsage();
    }

    // Null after telling the sender it does not exist
    protected MobSpawner find(CommandSender sender, String name) {
        MobSpawner spawner = spawnerManager.getSpawner(name);
        if (spawner == null) TextUtils.error(sender, "Spawner '" + name + "' does not exist");
        return spawner;
    }
}
