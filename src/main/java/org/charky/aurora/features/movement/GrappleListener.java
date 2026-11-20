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
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.charky.aurora.common.util.Enchants;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.tool.ToolManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// The grapple, a tool bought at the end of Verdant Groves. Right click, on the ground
// or in the air, throws you the way you look: a burst, a second push a tick later so
// it feels like being pulled, and a trail of wind behind you. The rod shows the
// cooldown. The landing after a launch does no fall damage. It starts at half the
// throw it once had and a 10 second cooldown. Every zone after Verdant Groves sells one
// grapple upgrade, Thrust and Recharge in turn, and each level is a big step
public class GrappleListener implements Listener {
    public static final String ITEM_ID = "grapple";
    // Full power, what a grapple threw before it could be upgraded
    private static final double SPEED = 1.5;
    private static final double LIFT = 0.3;
    // The second push, a tick after the first, on top of what is left of it
    private static final double PULL = 0.45;
    // Share of full power: half to start, a twentieth more for each Thrust level
    private static final double BASE_POWER = 0.5;
    private static final double POWER_PER_THRUST = 0.05;
    // 10 seconds to start, half a second less for each Recharge level
    private static final int BASE_COOLDOWN_TICKS = 200;
    private static final int TICKS_PER_RECHARGE = 10;
    private static final int MIN_COOLDOWN_TICKS = 30;
    private static final int TRAIL_TICKS = 12;
    // How long after a launch a fall still counts as the grapple's
    private static final long SAFE_LANDING_MS = 6000;

    private final Aurora plugin;
    private final Map<UUID, Long> launched = new HashMap<>();

    public GrappleListener(Aurora plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack rod = event.getItem();
        boolean tool = ITEM_ID.equals(ToolManager.toolId(rod));
        // One bought when it was a shop item: made the tool now, the next click throws
        boolean old = !tool && ITEM_ID.equals(DataRegistry.itemId(rod));
        if (!tool && !old) return;
        // No bobber, the rod is only the look of it
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (old) {
            plugin.getToolManager().refresh(player);
            return;
        }
        if (player.hasCooldown(Material.FISHING_ROD)) return;
        int recharge = Enchants.level(rod, "recharge");
        player.setCooldown(Material.FISHING_ROD,
                Math.max(MIN_COOLDOWN_TICKS, BASE_COOLDOWN_TICKS - TICKS_PER_RECHARGE * recharge));
        double power = BASE_POWER + POWER_PER_THRUST * Enchants.level(rod, "thrust");

        Vector direction = player.getLocation().getDirection().normalize();
        player.setVelocity(direction.clone().multiply(SPEED * power).add(new Vector(0, LIFT, 0)));
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
                if (ticks == 1) player.setVelocity(player.getVelocity().add(direction.clone().multiply(PULL * power)));
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
