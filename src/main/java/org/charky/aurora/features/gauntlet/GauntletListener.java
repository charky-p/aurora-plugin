package org.charky.aurora.features.gauntlet;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GauntletListener implements Listener {
    private final GauntletManager gauntletManager;

    public GauntletListener(GauntletManager gauntletManager) {
        this.gauntletManager = gauntletManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (gauntletManager.getState() != GauntletManager.GameState.ACTIVE) return;
        gauntletManager.checkWaveComplete();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        gauntletManager.handlePlayerDeath(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        gauntletManager.handlePlayerDeath(event.getPlayer());
    }
}
