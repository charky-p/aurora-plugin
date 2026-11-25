package org.charky.aurora.features.boss;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.combat.CustomHealth;
import org.charky.aurora.features.spawner.MobData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Every mob that is summoned with a key is a boss: it gets a boss bar for as long
// as it lives, and its own abilities if it has any
public class BossManager implements Listener {
    // How often a fight is updated, a quarter of a second
    static final int TICKS = 5;

    private final Aurora plugin;
    private final DataRegistry registry;
    private final Map<UUID, BossFight> fights = new HashMap<>();
    private final BukkitTask task;

    public BossManager(Aurora plugin) {
        this.plugin = plugin;
        this.registry = plugin.getRegistry();
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, TICKS, TICKS);
        CustomHealth.setLastStand(this::lastStand);
    }

    // The mob is already tagged by the time it is added to the world
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        MobData mob = registry.mobOf(entity);
        if (mob == null || !registry.getKeys().isSummoned(mob.id())) return;

        fights.put(entity.getUniqueId(), new BossFight(entity, mob, abilitiesFor(mob.id())));
    }

    private BossAbilities abilitiesFor(String mobId) {
        if (GormakEternal.BOSS_ID.equalsIgnoreCase(mobId)) return new GormakEternal(plugin);
        if (MarshHydra.BOSS_ID.equalsIgnoreCase(mobId)) return new MarshHydra(plugin);
        if (Arachne.BOSS_ID.equalsIgnoreCase(mobId)) return new Arachne(plugin);
        return BossAbilities.NONE;
    }

    // Before CombatListener, so a refused hit never reaches the boss's health
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBossHit(EntityDamageByEntityEvent event) {
        BossFight fight = fights.get(event.getEntity().getUniqueId());
        if (fight == null) return;

        Player attacker = switch (event.getDamager()) {
            case Player player -> player;
            case Projectile projectile when projectile.getShooter() instanceof Player shooter -> shooter;
            default -> null;
        };
        if (attacker != null && fight.shields(attacker)) event.setCancelled(true);
    }

    // After CombatListener, so a hit a shield stopped is already cancelled
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof LivingEntity attacker) || attacker instanceof Projectile) return;

        BossFight fight = fights.get(attacker.getUniqueId());
        if (fight != null) fight.onHit(player);

        MobData mob = registry.mobOf(attacker);
        if (mob != null && GormakEternal.GHOUL_ID.equalsIgnoreCase(mob.id())) GormakEternal.reap(player);
    }

    private void tick() {
        fights.values().removeIf(fight -> {
            if (fight.tick()) return false;

            fight.end();
            return true;
        });
    }

    private boolean lastStand(LivingEntity entity) {
        BossFight fight = fights.get(entity.getUniqueId());
        return fight != null && fight.lastStand();
    }

    public void shutdown() {
        task.cancel();
        CustomHealth.setLastStand(null);
        fights.values().forEach(BossFight::end);
        fights.clear();
    }
}
