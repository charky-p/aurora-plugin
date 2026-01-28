package org.charky.aurora.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.charky.aurora.Aurora;

public class SpawnListener implements Listener {
    private final Aurora plugin;

    public SpawnListener(Aurora plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Location spawnLocation = plugin.getSpawn(player.getWorld());

        if (spawnLocation != null) {
            event.setRespawnLocation(spawnLocation);
        }
    }
}
