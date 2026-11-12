package org.charky.aurora.features.system.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Admin command to reload all game data from the YAML without a restart,
 * used after scripts/generate.py --live
 */
public class RefreshCommand implements CommandExecutor {
    private final Aurora plugin;

    public RefreshCommand(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        try {
            plugin.getRegistry().loadAll();
            // Spawners keep their own copy of the mob, point them at the new one
            plugin.getSpawnerManager().reloadMobs();
            // Traders respawn with their new name and type on the next check
            plugin.getTraderManager().respawnAll();
            TextUtils.success(sender, "Registry reloaded");
        } catch (Exception e) {
            // Anything thrown by bad data. Loading starts from empty, so the data may
            // now be partly loaded, fix the YAML and refresh again
            TextUtils.error(sender, "Reload failed: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Registry reload failed", e);
        }
        return true;
    }
}
