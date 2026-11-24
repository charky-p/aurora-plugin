package org.charky.aurora.features.pvp;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.features.moderation.Durations;

import java.util.function.Predicate;

// Applies KillStreaks: counts player kills and refuses the hits of someone who has
// killed the same player too often lately. Not where `exempt` says (the Gauntlet,
// where killing is the game)
public class KillStreakListener implements Listener {
    private final KillStreaks streaks = new KillStreaks();
    private final Predicate<Player> exempt;

    public KillStreakListener(Predicate<Player> exempt) {
        this.exempt = exempt;
    }

    private static String often() {
        return KillStreaks.KILLS + " times in " + Durations.describe(KillStreaks.WINDOW_MS);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim) || exempt.test(victim)) return;
        if (!streaks.kill(killer.getUniqueId(), victim.getUniqueId(), System.currentTimeMillis())) return;

        String wait = Durations.describe(KillStreaks.COOLDOWN_MS);
        TextUtils.error(killer, "You've killed " + victim.getName() + " " + often() + ", you can't hurt them for " + wait);
        TextUtils.send(victim, "<green>" + killer.getName() + " can't hurt you for " + wait);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || exempt.test(victim)) return;
        Player attacker = switch (event.getDamager()) {
            case Player player -> player;
            case Projectile projectile when projectile.getShooter() instanceof Player shooter -> shooter;
            default -> null;
        };
        if (attacker == null || attacker.equals(victim)) return;
        long left = streaks.cooldownLeft(attacker.getUniqueId(), victim.getUniqueId(), System.currentTimeMillis());
        if (left <= 0) return;
        event.setCancelled(true);
        attacker.sendActionBar(TextUtils.parse("<red>You can't hurt " + victim.getName() + " for "
                + Durations.describe(left) + ", you killed them " + often()));
    }
}
