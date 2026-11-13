package org.charky.aurora.features.mine.commands;

import org.bukkit.command.CommandSender;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.mine.Mine;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.system.WandManager;
import org.charky.aurora.features.system.commands.AuroraSubCommand;
import org.charky.aurora.features.mine.MineManager;

public abstract class MineSub implements AuroraSubCommand {
    protected final MineManager mineManager;
    protected final WandManager wandManager;
    protected final DataRegistry registry;

    public MineSub(Aurora plugin) {
        this.mineManager = plugin.getMineManager();
        this.wandManager = plugin.getWandManager();
        this.registry = plugin.getRegistry();
    }

    @Override
    public String getFullUsage() {
        return "/aurora mine " + getName() + " " + getUsage();
    }

    // Null after telling the sender it does not exist
    protected Mine find(CommandSender sender, String name) {
        Mine mine = mineManager.getMine(name);
        if (mine == null) TextUtils.error(sender, "Mine '" + name + "' does not exist");
        return mine;
    }
}
