package org.charky.aurora.features.boss;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.charky.aurora.core.Aurora;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// Arachne wakes with her rider on her back, a skeleton that shoots from it, the way a
// vanilla spider jockey does. The rider steers her, as in vanilla. Killed, it stays
// dead for the rest of the fight; it goes with her
public class Arachne implements BossAbilities {
    public static final String BOSS_ID = "arachne";
    public static final String RIDER_ID = "arachne_rider";

    private static final double TARGET_RANGE = 48.0;

    private final Aurora plugin;
    private UUID rider;
    private boolean mounted = false;

    public Arachne(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void tick(BossFight fight) {
        // Not in the constructor: she is still being added to the world then
        if (mounted) return;
        mounted = true;

        String spawner = fight.spawnerName();
        if (spawner == null) return;

        LivingEntity boss = fight.boss();
        LivingEntity skeleton = plugin.getSpawnerManager().spawnMob(RIDER_ID, boss.getLocation().add(0, boss.getHeight(), 0), spawner);
        if (skeleton == null) {
            plugin.getLogger().warning("Arachne has no '" + RIDER_ID + "' mob in the registry to carry");
            return;
        }

        boss.addPassenger(skeleton);
        rider = skeleton.getUniqueId();
        List<Player> targets = fight.playersWithin(TARGET_RANGE);
        if (skeleton instanceof Mob mob && !targets.isEmpty()) {
            mob.setTarget(targets.get(ThreadLocalRandom.current().nextInt(targets.size())));
        }
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1.5f, 0.5f);
    }

    @Override
    public void end(BossFight fight) {
        if (rider == null) return;

        Entity skeleton = plugin.getServer().getEntity(rider);
        if (skeleton != null && skeleton.isValid()) {
            skeleton.getWorld().spawnParticle(Particle.POOF, skeleton.getLocation().add(0, 1, 0), 10, 0.3, 0.6, 0.3, 0.02);
            skeleton.remove();
        }
        rider = null;
    }
}
