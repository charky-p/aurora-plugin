package org.charky.aurora.features.gauntlet;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// The Gauntlet's PvP zone holds whoever walks in: there is no walking or pearling
// out. /spawn, /gauntlet leave or the menu's Leave take them out after a countdown,
// and logging out inside counts as dying, to whoever hit them last
public class GauntletPvp implements Listener {
    static final int COUNTDOWN_SECONDS = 5;
    // A hit this recent makes its player the killer of someone who logs out
    private static final long LAST_HIT_MS = 15_000;
    private static final Set<String> WAY_OUT = Set.of("/spawn", "/gauntlet leave", "/gauntlet");

    private final Aurora plugin;
    private final GauntletManager manager;
    private final Map<UUID, BukkitTask> leaving = new HashMap<>();
    private final Map<UUID, UUID> lastHitBy = new HashMap<>();
    private final Map<UUID, Long> lastHitAt = new HashMap<>();

    public GauntletPvp(Aurora plugin, GauntletManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public boolean inZone(Player player) {
        return manager.isHere(player) && !manager.inRun(player) && manager.arena().inPvp(player.getLocation());
    }

    // Out of the zone after the countdown: to the lobby, or out of the Gauntlet with
    // leaveWorld. Outside the zone it happens at once. Null if it started
    public String leave(Player player, boolean leaveWorld) {
        if (!inZone(player)) {
            go(player, leaveWorld);
            return null;
        }
        if (leaving.containsKey(player.getUniqueId())) return "You are already on your way out";
        TextUtils.send(player, "<gray>Leaving the PvP zone in <white>" + COUNTDOWN_SECONDS + " seconds<gray>...");
        int[] left = {COUNTDOWN_SECONDS};
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !inZone(player)) {
                stop(player);
                return;
            }
            if (left[0] <= 0) {
                stop(player);
                go(player, leaveWorld);
                return;
            }
            player.sendActionBar(TextUtils.parse("<red>Leaving the PvP zone in <white>" + left[0] + "s"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 1.4f);
            left[0]--;
        }, 0L, 20L);
        leaving.put(player.getUniqueId(), task);
        return null;
    }

    private void stop(Player player) {
        BukkitTask task = leaving.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    private void go(Player player, boolean leaveWorld) {
        if (leaveWorld) {
            manager.leave(player);
            return;
        }
        Location lobby = manager.arena().lobby();
        if (lobby != null) player.teleport(lobby);
    }

    // Walking out is refused: back to where they stood
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom(), to = event.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;
        Player player = event.getPlayer();
        if (!inZone(player) || manager.arena().inPvp(to)) return;
        event.setTo(from.clone().setDirection(to.getDirection()));
        player.sendActionBar(TextUtils.parse("<red>You can't walk out of the PvP zone. <white>/spawn</white> leaves in "
                + COUNTDOWN_SECONDS + " seconds"));
    }

    // Ender pearls and chorus fruit can't carry anyone out either
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPearl(PlayerTeleportEvent event) {
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause != PlayerTeleportEvent.TeleportCause.ENDER_PEARL && cause != PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT
                && cause != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) return;
        if (inZone(event.getPlayer()) && !manager.arena().inPvp(event.getTo())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(TextUtils.parse("<red>Nothing carries you out of the PvP zone"));
        }
    }

    // /spawn anywhere in the Gauntlet's world goes to its lobby: at once from the lobby,
    // ending the run from a run, after the countdown from the PvP zone. In the zone
    // /gauntlet leave counts down too, and /gauntlet's menu waits
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!manager.isHere(player)) return;
        String command = event.getMessage().toLowerCase().trim().replaceAll("\\s+", " ");
        boolean spawn = command.equals("/spawn") || command.startsWith("/spawn ");
        if (spawn && !inZone(player)) {
            event.setCancelled(true);
            if (manager.inRun(player)) manager.endRun(player);
            else go(player, false);
            return;
        }
        if (!inZone(player)) return;
        if (!WAY_OUT.contains(command) && !spawn) return;
        event.setCancelled(true);
        if (command.equals("/gauntlet")) {
            TextUtils.error(player, "Not in the PvP zone. /spawn leaves it in " + COUNTDOWN_SECONDS + " seconds");
            return;
        }
        String problem = leave(player, command.equals("/gauntlet leave"));
        if (problem != null) TextUtils.error(player, problem);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !inZone(victim)) return;
        Player attacker = event.getDamager() instanceof Player p ? p
                : event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter ? shooter : null;
        if (attacker == null || attacker.equals(victim)) return;
        lastHitBy.put(victim.getUniqueId(), attacker.getUniqueId());
        lastHitAt.put(victim.getUniqueId(), System.currentTimeMillis());
    }

    // Logging out inside dies, the kill going to whoever hit them in the last 15 seconds.
    // They come back in the lobby with their setup, like any death here
    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        stop(player);
        UUID by = lastHitBy.remove(uuid);
        Long at = lastHitAt.remove(uuid);
        if (!inZone(player)) return;
        Player killer = by != null && at != null && System.currentTimeMillis() - at < LAST_HIT_MS ? plugin.getServer().getPlayer(by) : null;
        for (Player other : player.getWorld().getPlayers()) {
            if (!other.equals(player)) TextUtils.send(other, "<red>" + player.getName() + " logged out in the PvP zone and died");
        }
        if (killer != null) player.damage(10_000, killer);
        // No killer to credit, or the hit was refused (the killer stepped out), or a totem saved them
        if (!player.isDead()) player.setHealth(0);
    }
}
