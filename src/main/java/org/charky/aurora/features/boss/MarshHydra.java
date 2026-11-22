package org.charky.aurora.features.boss;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.combat.MobDamage;
import org.charky.aurora.features.spawner.MobData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// The Marsh Hydra's fight: a zoglin with a Bogged Rider on its back that shoots.
//  - Rider: rides an invisible seat on the zoglin, so the zoglin keeps walking at
//    players by itself instead of being steered. Killed, it stays dead for the fight
//  - Charge: every few seconds it roars and paws the ground for a second, then
//    lunges at a player, hitting whoever it runs into for twice a normal hit and
//    throwing them back
//  - Sludge: it lobs sludge where a player stands. After a second's warning the
//    puddle slows anyone in it and stings them every second for a few seconds
// All its damage comes from its own row in mobs_marsh.csv (attack and damage), so
// it is tuned there like any other mob
public class MarshHydra implements BossAbilities {
    public static final String BOSS_ID = "marsh_hydra";
    public static final String RIDER_ID = "hydra_rider";

    private static final double TARGET_RANGE = 48.0;

    // Charge: seconds between, the wind-up, how long the lunge can hit, and its push
    private static final int CHARGE_EVERY_TICKS = 160;
    private static final int CHARGE_JITTER_TICKS = 40;
    private static final int CHARGE_WINDUP_TICKS = 20;
    private static final int CHARGE_HIT_TICKS = 15;
    private static final double CHARGE_RANGE = 24.0;
    private static final double CHARGE_SPEED = 1.6;
    private static final double CHARGE_REACH = 2.2;
    private static final double CHARGE_HIT_SCALE = 2.0;

    // Sludge: seconds between, the warning, how long a puddle lasts, its size and sting
    private static final int SLUDGE_EVERY_TICKS = 240;
    private static final int SLUDGE_JITTER_TICKS = 40;
    private static final int SLUDGE_WARN_TICKS = 20;
    private static final int SLUDGE_LASTS_TICKS = 80;
    private static final int SLUDGE_STING_TICKS = 20;
    private static final double SLUDGE_RADIUS = 3.0;
    private static final double SLUDGE_HIT_SCALE = 0.4;

    private final Aurora plugin;
    private ArmorStand seat;
    private UUID rider;
    private boolean mounted = false;
    private int age = 0;

    private int nextCharge = CHARGE_EVERY_TICKS;
    private int windupEnds = -1;
    private int lungeEnds = -1;
    private UUID chargeTarget;
    private Vector lunge;
    private final Set<UUID> hitByLunge = new HashSet<>();

    private int nextSludge = SLUDGE_EVERY_TICKS / 2;
    private final List<Puddle> puddles = new ArrayList<>();

    private record Puddle(Location centre, int lands, int dries) {
    }

    public MarshHydra(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void tick(BossFight fight) {
        age += BossManager.TICKS;
        LivingEntity boss = fight.boss();

        // Not in the constructor: the body is still being added to the world then
        if (!mounted) {
            mounted = true;
            mountRider(fight);
        }
        keepRiderSeated(boss);

        charge(fight);
        sludge(fight);
    }

    private void mountRider(BossFight fight) {
        String spawner = fight.spawnerName();
        if (spawner == null) return;

        LivingEntity boss = fight.boss();
        LivingEntity archer = plugin.getSpawnerManager().spawnMob(RIDER_ID, boss.getLocation().add(0, boss.getHeight(), 0), spawner);
        if (archer == null) {
            plugin.getLogger().warning("The Marsh Hydra has no '" + RIDER_ID + "' mob in the registry to carry");
            return;
        }
        rider = archer.getUniqueId();
        fight.aim(archer, TARGET_RANGE);
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ZOGLIN_ANGRY, 1.5f, 0.5f);
    }

    // Vanilla lets the first rider steer, so the archer sits on a marker on the
    // zoglin's back instead of on the zoglin itself
    private void keepRiderSeated(LivingEntity boss) {
        Entity archer = rider == null ? null : plugin.getServer().getEntity(rider);
        if (archer == null || !archer.isValid() || archer.isDead()) {
            rider = null;
            return;
        }
        if (seat == null || !seat.isValid()) {
            seat = boss.getWorld().spawn(boss.getLocation(), ArmorStand.class, stand -> {
                stand.setInvisible(true);
                stand.setMarker(true);
                stand.setInvulnerable(true);
                stand.setGravity(false);
                stand.setPersistent(false);
            });
        }
        mount(boss, seat);
        mount(seat, archer);
    }

    private static void mount(Entity vehicle, Entity passenger) {
        if (passenger.getVehicle() == vehicle) return;

        passenger.leaveVehicle();
        vehicle.addPassenger(passenger);
    }

    private void charge(BossFight fight) {
        LivingEntity boss = fight.boss();

        if (windupEnds < 0 && lungeEnds < 0 && age >= nextCharge) {
            Player target = nearest(fight, CHARGE_RANGE);
            if (target == null) {
                nextCharge = age + CHARGE_JITTER_TICKS;
                return;
            }
            chargeTarget = target.getUniqueId();
            windupEnds = age + CHARGE_WINDUP_TICKS;
            boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ZOGLIN_ANGRY, 2f, 0.6f);
            target.sendActionBar(TextUtils.parse("<red>The Hydra is about to charge you!"));
        }

        if (windupEnds >= 0) {
            boss.getWorld().spawnParticle(Particle.BLOCK, boss.getLocation(), 12, 0.6, 0.1, 0.6,
                    boss.getLocation().subtract(0, 1, 0).getBlock().getBlockData());
            if (age < windupEnds) return;

            windupEnds = -1;
            Player target = chargeTarget == null ? null : plugin.getServer().getPlayer(chargeTarget);
            if (target == null || !target.getWorld().equals(boss.getWorld())) {
                nextCharge = age + CHARGE_EVERY_TICKS;
                return;
            }
            Vector direction = target.getLocation().toVector().subtract(boss.getLocation().toVector()).setY(0);
            if (direction.lengthSquared() < 0.01) direction = boss.getLocation().getDirection().setY(0);
            lunge = direction.normalize().multiply(CHARGE_SPEED);
            boss.setVelocity(lunge.clone().setY(0.25));
            boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.5f, 1.2f);
            hitByLunge.clear();
            lungeEnds = age + CHARGE_HIT_TICKS;
        }

        if (lungeEnds >= 0) {
            boss.getWorld().spawnParticle(Particle.CLOUD, boss.getLocation(), 4, 0.4, 0.2, 0.4, 0.01);
            for (Player player : fight.playersWithin(CHARGE_REACH)) {
                if (!hitByLunge.add(player.getUniqueId())) continue;

                hurt(boss, player, CHARGE_HIT_SCALE);
                player.setVelocity(lunge.clone().multiply(0.8).setY(0.5));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 0.7f);
            }
            if (age >= lungeEnds) {
                lungeEnds = -1;
                nextCharge = age + CHARGE_EVERY_TICKS + ThreadLocalRandom.current().nextInt(-CHARGE_JITTER_TICKS, CHARGE_JITTER_TICKS + 1);
            }
        }
    }

    private void sludge(BossFight fight) {
        LivingEntity boss = fight.boss();

        if (age >= nextSludge) {
            nextSludge = age + SLUDGE_EVERY_TICKS + ThreadLocalRandom.current().nextInt(-SLUDGE_JITTER_TICKS, SLUDGE_JITTER_TICKS + 1);
            Player target = fight.randomPlayer(TARGET_RANGE);
            if (target != null) {
                Location centre = target.getLocation().clone();
                puddles.add(new Puddle(centre, age + SLUDGE_WARN_TICKS, age + SLUDGE_WARN_TICKS + SLUDGE_LASTS_TICKS));
                boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_SLIME_JUMP, 1.5f, 0.5f);
                target.sendActionBar(TextUtils.parse("<dark_green>Sludge is coming down on you, move!"));
            }
        }

        for (Iterator<Puddle> it = puddles.iterator(); it.hasNext(); ) {
            Puddle puddle = it.next();
            if (age >= puddle.dries()) {
                it.remove();
                continue;
            }
            Location centre = puddle.centre();
            if (age < puddle.lands()) {
                // The warning: a ring where it will land
                ring(centre, Particle.ITEM_SLIME, 16);
                continue;
            }
            if (age - puddle.lands() < BossManager.TICKS) {
                centre.getWorld().playSound(centre, Sound.BLOCK_SLIME_BLOCK_BREAK, 1.5f, 0.6f);
            }
            centre.getWorld().spawnParticle(Particle.ITEM_SLIME, centre, 25, SLUDGE_RADIUS * 0.6, 0.1, SLUDGE_RADIUS * 0.6, 0.02);
            ring(centre, Particle.SNEEZE, 10);

            boolean sting = (age - puddle.lands()) % SLUDGE_STING_TICKS < BossManager.TICKS;
            for (Player player : centre.getWorld().getPlayers()) {
                if (!fight.playersWithin(TARGET_RANGE).contains(player)) continue;
                Location at = player.getLocation();
                if (Math.abs(at.getY() - centre.getY()) > 2 || horizontalDistanceSquared(at, centre) > SLUDGE_RADIUS * SLUDGE_RADIUS) continue;

                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true, true));
                if (sting) hurt(boss, player, SLUDGE_HIT_SCALE);
            }
        }
    }

    private static void ring(Location centre, Particle particle, int points) {
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            centre.getWorld().spawnParticle(particle, centre.clone().add(Math.cos(angle) * SLUDGE_RADIUS, 0.1, Math.sin(angle) * SLUDGE_RADIUS),
                    1, 0, 0, 0, 0);
        }
    }

    private static double horizontalDistanceSquared(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    // A share of the Hydra's own hit, from its attack and damage in the data. Dealt
    // without a source so CombatListener does not price it again as a swing
    private void hurt(LivingEntity boss, Player player, double scale) {
        MobData mob = plugin.getRegistry().mobOf(boss);
        if (mob == null) return;
        player.damage(MobDamage.dealt(mob, player, false) * scale);
    }

    private Player nearest(BossFight fight, double range) {
        Location at = fight.boss().getLocation();
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player player : fight.playersWithin(range)) {
            double distance = player.getLocation().distanceSquared(at);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }

    @Override
    public void end(BossFight fight) {
        Entity archer = rider == null ? null : plugin.getServer().getEntity(rider);
        if (archer != null && archer.isValid()) {
            archer.getWorld().spawnParticle(Particle.ITEM_SLIME, archer.getLocation(), 15, 0.3, 0.3, 0.3, 0.05);
            archer.remove();
        }
        rider = null;
        if (seat != null) seat.remove();
        seat = null;
        puddles.clear();
        hitByLunge.clear();
    }
}
