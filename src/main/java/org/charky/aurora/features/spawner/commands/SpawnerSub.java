package org.charky.aurora.features.spawner.commands;

import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.commands.AuroraSubCommand;
import org.charky.aurora.features.spawner.SpawnerManager;

public abstract class SpawnerSub implements AuroraSubCommand {
    protected final DataRegistry registry;
    protected final SpawnerManager spawnerManager;

    public SpawnerSub(Aurora plugin) {
        this.spawnerManager = plugin.getMobManager();
        this.registry = plugin.getRegistry();
    }

    @Override
    public String getFullUsage() {
        return "/aurora spawner " + getName() + " " + getUsage();
    }
}
