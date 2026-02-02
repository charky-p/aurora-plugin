package org.charky.aurora.features.mine.commands;

import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.system.commands.AuroraSubCommand;
import org.charky.aurora.features.mine.MineManager;

public abstract class MineSub implements AuroraSubCommand {
    protected static final int MAX_MINE_VOLUME = 100000;

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
