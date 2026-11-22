package org.charky.aurora.features.combat;

import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
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

        // An arrow from the bow is priced from the shot, anything else from the swing.
        // Vanilla decides what a crit is (falling, not sprinting, not in water), we scale it
        BowListener.Shot shot = BowListener.shotOf(event.getDamager());
        MobData mob = registry.mobOf(victim);
        double dealt = shot != null
                ? Math.max(0.0, Math.round(shot.raw() - (mob == null ? 0.0 : mob.defence())))
                : playerDamage.dealt(attacker, mob, event.isCritical());
        dealt = Math.round(dealt * CombatHooks.outgoing(attacker, victim, shot != null));
        event.setDamage(CustomHealth.NOMINAL_DAMAGE);
        CustomHealth.apply(victim, dealt, attacker, registry);
        indicator.show(victim, dealt);
        CombatHooks.afterHit(attacker, victim, dealt, shot != null);
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

        double damage = CombatHooks.incoming(player, source, MobDamage.dealt(mob, player, event.getDamager() instanceof Projectile));
        if (damage <= 0) {
            event.setCancelled(true);
            return;
        }
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

    // Our mobs only ever go for players. Iron golems hunt hostile mobs, zoglins anything,
    // vindicators and pillagers the traders, and a stray arrow turns the mob it hits on
    // the archer: none of it can hurt (mob on mob damage is cancelled above), but they
    // would chase and swing at each other instead of the player. Passive mobs never go
    // for a player either, so a wolf that is hit stays calm instead of biting for nothing
    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob) || !CustomHealth.isTracked(mob)) return;
        if (!(event.getTarget() instanceof Player)) {
            // A null target is the mob giving up its target, which is always allowed
            if (event.getTarget() != null) event.setCancelled(true);
            return;
        }

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

    // Our mobs' arrows hurt by their attack level alone: the Poison a Bogged's carries
    // and the Slowness of a Stray's would deal vanilla damage past it
    @EventHandler(ignoreCancelled = true)
    public void onMobShoot(EntityShootBowEvent event) {
        if (!CustomHealth.isTracked(event.getEntity())) return;
        if (event.getProjectile() instanceof Arrow arrow) {
            arrow.clearCustomEffects();
            arrow.setBasePotionType(null);
        }
    }

    // A witch's potion is her attack: whatever she throws (harming, poison, slowness,
    // weakness) lands on each player it splashes as one ranged hit at her attack level,
    // priced by onMobAttack like an arrow. No vanilla effect lands on anyone, or poison
    // would deal vanilla damage past it. All the players in one splash are hit, so a
    // witch's attack_cooldown is left empty: vanilla already throws every 3 seconds
    @EventHandler(ignoreCancelled = true)
    public void onMobPotion(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        if (!(potion.getShooter() instanceof LivingEntity thrower) || !CustomHealth.isTracked(thrower)) return;

        DamageSource source = DamageSource.builder(DamageType.INDIRECT_MAGIC)
                .withCausingEntity(thrower).withDirectEntity(potion).build();
        for (LivingEntity hit : event.getAffectedEntities()) {
            event.setIntensity(hit, 0);
            if (hit instanceof Player player) player.damage(1.0, source);
        }
    }

    // An arrow a mob shot, or one from the bow, is gone the moment it lands, so none
    // are left stuck in players or lying about
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobProjectileLand(ProjectileHitEvent event) {
        // A player's trident flies back to them, a mob's is gone like its arrows
        if (event.getEntity() instanceof Trident && event.getEntity().getShooter() instanceof Player) return;
        if (!(event.getEntity().getShooter() instanceof Player) || BowListener.shotOf(event.getEntity()) != null) {
            event.getEntity().remove();
        }
    }
}
