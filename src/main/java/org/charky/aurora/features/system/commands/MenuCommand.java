package org.charky.aurora.features.system.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.trader.TraderManager;
import org.jetbrains.annotations.NotNull;

public class MenuCommand implements CommandExecutor {
    private static final String HUB_ID = "aurora_hub";

    private final TraderManager manager;
    private final DataRegistry registry;

    public MenuCommand(Aurora plugin) {
        this.manager = plugin.getTraderManager();
        this.registry = plugin.getRegistry();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            TextUtils.error(sender, "Players only.");
            return true;
        }

        if (registry.getGui(HUB_ID) == null) {
            TextUtils.error(sender, "Menu is not loaded.");
            return true;
        }

        manager.handleMenu(player, HUB_ID);
        return true;
    }
}