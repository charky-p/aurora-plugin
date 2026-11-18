package org.charky.aurora.features.movement;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.charky.aurora.core.DataRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// The grapple, a fishing rod sold at the end of Verdant Groves. Right click, on the
// ground or in the air, throws you the way you look: a burst, a second push a tick
// later so it feels like being pulled, and a trail of wind behind you. Every 3
// seconds, the rod shows the cooldown. The landing after a launch does no fall damage
public class GrappleListener implements Listener {
    public static final String ITEM_ID = "grapple";
    private static final double SPEED = 1.5;
    private static final double LIFT = 0.3;
    // The second push, a tick after the first, on top of what is left of it
    private static final double PULL = 0.45;
    private static final int COOLDOWN_TICKS = 60;
    private static final int TRAIL_TICKS = 12;
    // How long after a launch a fall still counts as the grapple's
    private static final long SAFE_LANDING_MS = 6000;

    private final Plugin plugin;
    private final Map<UUID, Long> launched = new HashMap<>();

    public GrappleListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!ITEM_ID.equals(DataRegistry.itemId(event.getItem()))) return;
        // No bobber, the rod is only the look of it
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (player.hasCooldown(Material.FISHING_ROD)) return;
        player.setCooldown(Material.FISHING_ROD, COOLDOWN_TICKS);

        Vector direction = player.getLocation().getDirection().normalize();
        player.setVelocity(direction.clone().multiply(SPEED).add(new Vector(0, LIFT, 0)));
        player.setFallDistance(0);
        launched.put(player.getUniqueId(), System.currentTimeMillis());

        player.playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.9f, 1.1f);
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1f, 0.6f);
        player.getWorld().spawnParticle(Particle.GUST, player.getLocation().add(0, 0.5, 0), 1);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks++ >= TRAIL_TICKS) {
                    cancel();
                    return;
                }
                if (ticks == 1) player.setVelocity(player.getVelocity().add(direction.clone().multiply(PULL)));
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.8, 0), 3, 0.15, 0.15, 0.15, 0.01);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;
        Long last = launched.get(player.getUniqueId());
        if (last != null && System.currentTimeMillis() - last < SAFE_LANDING_MS) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        launched.remove(event.getPlayer().getUniqueId());
    }
}
