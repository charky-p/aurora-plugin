package org.charky.aurora.features.movement;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import org.charky.aurora.core.DataRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// The grapple, sold at the end of Verdant Groves: right click launches you the way you
// look: about 12 blocks up, enough for Khorvath's 10 block cliff, and about 20 across, short
// of the 40 block gaps only the elytra crosses. A 5 second cooldown stops it being
// chained into flight, and the landing after a launch does no fall damage
public class GrappleListener implements Listener {
    public static final String ITEM_ID = "grapple";
    private static final double SPEED = 1.3;
    private static final double LIFT = 0.25;
    private static final long COOLDOWN_MS = 5000;
    // How long after a launch a fall still counts as the grapple's
    private static final long SAFE_LANDING_MS = 5000;

    private final Map<UUID, Long> launched = new HashMap<>();

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!ITEM_ID.equals(DataRegistry.itemId(event.getItem()))) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        // From the ground only, or launches chained in the air would cross the elytra gaps
        if (!player.isOnGround()) {
            player.sendActionBar(Component.text("The grapple needs solid ground", NamedTextColor.GRAY));
            return;
        }
        long now = System.currentTimeMillis();
        Long last = launched.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MS) {
            player.sendActionBar(Component.text("Grapple recharging", NamedTextColor.GRAY));
            return;
        }

        Vector direction = player.getLocation().getDirection().normalize();
        player.setVelocity(direction.multiply(SPEED).add(new Vector(0, LIFT, 0)));
        player.setFallDistance(0);
        launched.put(player.getUniqueId(), now);
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 0.8f);
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
