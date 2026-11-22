package org.charky.aurora.features.movement;

import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import org.charky.aurora.features.perk.Perk;
import org.charky.aurora.features.perk.PerkManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Double Jump, an Essence shop perk (and a Gauntlet talent, PerkManager.has): press jump
// again in mid-air for a second jump, as high as a normal one, once until you land. The
// press is read from the player's keys, so one press is enough. Creative and spectator
// players fly as normal
public class DoubleJumpListener implements Listener {
    // Vanilla's own jump
    private static final double JUMP = 0.42;

    private final PerkManager perks;
    // Whether jump was held at the last input, so only a fresh press counts
    private final Map<UUID, Boolean> held = new HashMap<>();
    // Used the second jump and not landed since
    private final Set<UUID> used = new HashSet<>();

    public DoubleJumpListener(PerkManager perks) {
        this.perks = perks;
    }

    private static boolean playing(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE;
    }

    @EventHandler
    public void onInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        boolean jump = event.getInput().isJump();
        boolean pressed = jump && !Boolean.TRUE.equals(held.put(uuid, jump));
        // The press that leaves the ground is the first jump, not this one
        if (!pressed || player.isOnGround() || used.contains(uuid) || !playing(player)) return;
        if (player.isInsideVehicle() || player.isGliding() || player.isFlying() || player.isInWater() || player.isClimbing()) return;
        if (!perks.has(player, Perk.DOUBLE_JUMP)) return;

        used.add(uuid);
        Vector velocity = player.getVelocity();
        player.setVelocity(velocity.setY(JUMP));
        player.setFallDistance(0);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 8, 0.3, 0.05, 0.3, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 0.6f, 1.2f);
    }

    // Landing gives the second jump back
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getPlayer().isOnGround()) used.remove(event.getPlayer().getUniqueId());
    }

    // The first version of this perk let owners fly to catch the double tap, a player
    // who still has that left over is put back on the ground
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (playing(player) && player.getAllowFlight() && !player.hasPermission("aurora.admin")) player.setAllowFlight(false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        held.remove(event.getPlayer().getUniqueId());
        used.remove(event.getPlayer().getUniqueId());
    }
}
