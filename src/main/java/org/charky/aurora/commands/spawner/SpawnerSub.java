package org.charky.aurora.commands.spawner;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.DataRegistry;
import org.charky.aurora.commands.AuroraSubCommand;
import org.charky.aurora.managers.SpawnerManager;

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
