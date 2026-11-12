package org.charky.aurora.features.system;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.charky.aurora.core.Aurora;

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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World mineWorld = Bukkit.getWorld(Aurora.MAIN_WORLD);

        if (mineWorld != null) {
            Location spawnLoc = plugin.getSpawn(mineWorld);

            if (spawnLoc != null) {
                // Teleport them immediately to the aurora-mines spawn
                player.teleport(spawnLoc);
            } else {
                // Fallback: if no custom spawn is set, go to world spawn
                player.teleport(mineWorld.getSpawnLocation());
            }
        }
    }
}
