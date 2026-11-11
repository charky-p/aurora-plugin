package org.charky.aurora.features.spawner;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.DataKeys;

import java.util.List;

// Spawner mobs are never saved with their chunk now, so any that load from disk
// were left by a crash or by a version before that, and would count towards limits
public class LeftoverMobListener implements Listener {
    // Chunks loaded before the plugin enabled never fire the event
    public static void sweepLoaded() {
        for (World world : Bukkit.getWorlds()) removeLeftovers(world.getEntities());
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        removeLeftovers(event.getEntities());
    }

    private static void removeLeftovers(List<Entity> entities) {
        for (Entity entity : entities) {
            if (entity.isPersistent() && entity.getPersistentDataContainer()
                    .has(DataKeys.SPAWNER_KEY, PersistentDataType.STRING)) {
                entity.remove();
            }
        }
    }
}
