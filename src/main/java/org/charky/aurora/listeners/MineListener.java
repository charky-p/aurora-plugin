package org.charky.aurora.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.charky.aurora.Mine;
import org.charky.aurora.MineManager;

public class MineListener implements Listener {
    private final MineManager manager;

    public MineListener(MineManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMine(BlockBreakEvent event) {
        // TODO: O(N) time complexity, however mines is small so maybe not an issue
        manager.handleBlockBreak(event.getPlayer(), event.getBlock().getLocation());
    }
}
