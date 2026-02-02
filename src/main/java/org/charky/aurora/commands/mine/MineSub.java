package org.charky.aurora.commands.mine;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.Aurora;
import org.charky.aurora.DataRegistry;
import org.charky.aurora.commands.AuroraSubCommand;
import org.charky.aurora.managers.MineManager;

public abstract class MineSub implements AuroraSubCommand {
    protected final MineManager mineManager;
    protected final DataRegistry registry;

    public MineSub(Aurora plugin) {
        this.mineManager = plugin.getMineManager();
        this.registry = plugin.getRegistry();
    }

    @Override
    public String getFullUsage() {
        return "/aurora mine " + getName() + " " + getUsage();
    }
}
