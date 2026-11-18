package org.charky.aurora.features.movement;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.features.perk.Perk;
import org.charky.aurora.features.perk.PerkManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Dash, an Essence shop perk: shift twice quickly to dash the way you face, once a
// second. /dash, or clicking it in the shop, turns it on or off
public class DashListener implements Listener, CommandExecutor {
    // Two presses of shift closer together than this are a double tap
    private static final long DOUBLE_TAP_MS = 300;
    private static final long COOLDOWN_MS = 1000;
    private static final double SPEED = 1.4;
    private static final double LIFT = 0.25;

    private final PerkManager perks;
    private final Map<UUID, Long> lastTap = new HashMap<>();
    private final Map<UUID, Long> lastDash = new HashMap<>();

    public DashListener(PerkManager perks) {
        this.perks = perks;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        // Shift also gets off a mount and steers gliding, neither should dash
        if (player.isInsideVehicle() || player.isGliding() || player.isFlying()) return;
        if (!perks.dashOn(player)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long tap = lastTap.put(uuid, now);
        if (tap == null || now - tap > DOUBLE_TAP_MS) return;
        lastTap.remove(uuid);

        Long dashed = lastDash.get(uuid);
        if (dashed != null && now - dashed < COOLDOWN_MS) return;
        lastDash.put(uuid, now);

        // Along the ground, whatever the pitch, with a small hop so it clears a step
        Vector direction = player.getLocation().getDirection().setY(0);
        if (direction.lengthSquared() < 1e-6) return;
        player.setVelocity(direction.normalize().multiply(SPEED).setY(LIFT));
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.2, 0), 12, 0.3, 0.1, 0.3, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 0.8f, 1.4f);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastTap.remove(event.getPlayer().getUniqueId());
        lastDash.remove(event.getPlayer().getUniqueId());
    }

    // /dash
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return false;
        if (!perks.owns(player, Perk.DASH)) {
            player.sendActionBar(Component.text("Buy Dash in the Essence shop, /shop", NamedTextColor.RED));
            return true;
        }
        TextUtils.success(player, "Dash " + (perks.toggleDash(player) ? "on: shift twice quickly to dash" : "off"));
        return true;
    }
}
