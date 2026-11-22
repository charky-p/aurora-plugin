package org.charky.aurora.features.spawner;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.DataKeys;

import java.util.List;

// Spawner mobs as entities in the world. They are never saved with their chunk now,
// so any that load from disk were left by a crash or by a version before that, and
// would count towards limits. Players cannot right click them, and they drop nothing
// of their own: what they give comes from the files when they die
public class LeftoverMobListener implements Listener {
    // Chunks loaded before the plugin enabled never fire the event
    public static void sweepLoaded() {
        for (World world : Bukkit.getWorlds()) removeLeftovers(world.getEntities());
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        removeLeftovers(event.getEntities());
    }

    // Right clicking one does nothing: an allay would take the item in your hand, tools
    // included, and a lead would walk a frog or a slime off its island
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (isSpawned(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    // Zombies, skeletons, bogged, strays and phantoms never catch fire in the sun. Fire,
    // lava and a burning hit (the subclasses) still set them alight
    @EventHandler(ignoreCancelled = true)
    public void onSunburn(EntityCombustEvent event) {
        if (event instanceof EntityCombustByBlockEvent || event instanceof EntityCombustByEntityEvent) return;
        if (isSpawned(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    // A cave spider's bite poisons in vanilla, on top of the hit the data prices. Players
    // have no other way to be poisoned by an attack, so none of it lands
    @EventHandler(ignoreCancelled = true)
    public void onBitePoison(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof Player && event.getCause() == EntityPotionEffectEvent.Cause.ATTACK
                && event.getModifiedType() == PotionEffectType.POISON) {
            event.setCancelled(true);
        }
    }

    // Vanilla drops while alive: a sniffer's dug up seeds and pitcher pods, a chicken's eggs
    @EventHandler(ignoreCancelled = true)
    public void onDrop(EntityDropItemEvent event) {
        if (isSpawned(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    // Spawned by one of our spawners, a boss and its minions included
    public static boolean isSpawned(Entity entity) {
        return entity.getPersistentDataContainer().has(DataKeys.SPAWNER_KEY, PersistentDataType.STRING);
    }

    private static void removeLeftovers(List<Entity> entities) {
        for (Entity entity : entities) {
            if (entity.isPersistent() && isSpawned(entity)) {
                entity.remove();
            }
        }
    }
}
