package org.charky.aurora.features.movement;

import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;
import org.charky.aurora.features.perk.Perk;
import org.charky.aurora.features.perk.PerkManager;

// Double Jump, an Essence shop perk: jump again in mid-air for a second jump, once until
// you land. Vanilla's own double tap to fly is the trigger: an owner standing on the
// ground may fly, and the moment they try the flight is cancelled and turned into the
// jump. Creative and spectator players fly as normal
public class DoubleJumpListener implements Listener {
    private static final double UP = 0.9;
    private static final double FORWARD = 0.5;

    private final PerkManager perks;

    public DoubleJumpListener(PerkManager perks) {
        this.perks = perks;
    }

    private static boolean playing(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE;
    }

    // Landing gives the second jump back
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getAllowFlight() || !playing(player) || !player.isOnGround()) return;
        if (perks.owns(player, Perk.DOUBLE_JUMP)) player.setAllowFlight(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Left over from before a reset, or an admin's creative, nobody plays with flight on
        if (playing(player) && player.getAllowFlight() && !perks.owns(player, Perk.DOUBLE_JUMP)) player.setAllowFlight(false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFly(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!event.isFlying() || !playing(player)) return;
        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);
        if (!perks.owns(player, Perk.DOUBLE_JUMP) || player.isInsideVehicle() || player.isGliding()) return;

        Vector forward = player.getLocation().getDirection().setY(0);
        if (forward.lengthSquared() > 1e-6) forward.normalize().multiply(FORWARD);
        player.setVelocity(forward.setY(UP));
        player.setFallDistance(0);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 10, 0.3, 0.05, 0.3, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 0.7f, 1.1f);
    }
}
