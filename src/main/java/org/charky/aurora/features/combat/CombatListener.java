package org.charky.aurora.features.combat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.charky.aurora.common.util.Enchants;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.spawner.MobData;

// Routes combat events to the damage rules, the rules themselves live in
// PlayerDamage, MobDamage, Thorns and Bloodlust
public class CombatListener implements Listener {
    private final DataRegistry registry;
    private final SwingTimer swings = new SwingTimer();
    private final PlayerDamage playerDamage = new PlayerDamage(swings);
    private final DamageIndicator indicator;
    private final Thorns thorns;

    public CombatListener(Aurora plugin) {
        this.registry = plugin.getRegistry();
        this.indicator = new DamageIndicator(plugin);
        this.thorns = new Thorns(registry, indicator);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (!CustomHealth.isTracked(victim)) return;

        // Our own killing blow, let vanilla apply it so getKiller() gets set
        if (CustomHealth.isFinishing(victim)) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null) {
            // Mob on mob, or a projectile with no player behind it
            event.setCancelled(true);
            return;
        }

        // Vanilla decides what a crit is (falling, not sprinting, not in water), we scale it
        double dealt = playerDamage.dealt(attacker, registry.mobOf(victim), event.isCritical());
        event.setDamage(CustomHealth.NOMINAL_DAMAGE);
        CustomHealth.apply(victim, dealt, attacker, registry);
        indicator.show(victim, dealt);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        LivingEntity source = resolveMobAttacker(event);
        if (source == null || !CustomHealth.isTracked(source)) return;

        MobData mob = registry.mobOf(source);
        if (mob == null || mob.attack() <= 0) {
            // Passive mobs never hurt or knock anyone back, returning here would let
            // vanilla damage through
            event.setCancelled(true);
            return;
        }

        if (MobDamage.onCooldown(source, mob.attackCooldown())) {
            event.setCancelled(true);
            return;
        }

        double damage = MobDamage.dealt(mob, player, event.getDamager() instanceof Projectile);
        thorns.reflect(player, source);

        // Vanilla would stop a blocked hit fully and knock the player back, so the
        // hit is cancelled and ShieldBlock deals what gets through itself
        if (ShieldBlock.blocks(player, event.getDamager())) {
            event.setCancelled(true);
            ShieldBlock.take(player, mob, damage);
            return;
        }

        event.setDamage(damage);
    }

    // Passive mobs never go for a player, so a wolf that is hit stays calm instead of
    // chasing you down to bite for nothing
    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity mob) || !CustomHealth.isTracked(mob)) return;

        MobData data = registry.mobOf(mob);
        if (data == null || data.attack() <= 0) event.setCancelled(true);
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || !CustomHealth.isTracked(victim)) return;

        if (Enchants.level(killer.getInventory().getItemInMainHand(), "bloodlust") > 0) Bloodlust.addStack(killer);
    }

    // Swing timings are per player, drop them when the player leaves
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        swings.forget(event.getPlayer());
    }

    // Terrain damage would move vanilla health without moving the nameplate, so
    // it is refused. KILL and CUSTOM stay through or the mobs survive /kill and
    // there is no way to clear them. A mob in the void is removed outright: with
    // vanilla health decoupled the void would take two minutes to kill it, and it
    // holds its spawner's slot the whole time.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnvironmental(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (!CustomHealth.isTracked(victim)) return;
        if (CustomHealth.isFinishing(victim)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID) {
            victim.remove();
            return;
        }
        if (cause == EntityDamageEvent.DamageCause.KILL
                || cause == EntityDamageEvent.DamageCause.CUSTOM) return;

        event.setCancelled(true);
    }

    private static Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) return shooter;
        return null;
    }

    private static LivingEntity resolveMobAttacker(EntityDamageByEntityEvent event) {
        return switch (event.getDamager()) {
            case Player ignored -> null;
            case LivingEntity living -> living;
            case Projectile projectile when projectile.getShooter() instanceof LivingEntity shooter -> shooter;
            default -> null;
        };
    }
}
