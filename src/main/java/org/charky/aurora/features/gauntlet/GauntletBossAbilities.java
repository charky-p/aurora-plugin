package org.charky.aurora.features.gauntlet;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.combat.MobDamage;
import org.charky.aurora.features.spawner.MobData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// What the Gauntlet's bosses do besides hitting (bosses.csv): each ability on its own
// cooldown, the first one half a cooldown into the fight, one at a time. Every one is
// shown before it lands, a ring of red dust or a roar, so it can be dodged:
//
//   slam    a ring on the ground, then a blow to everyone within SLAM_RADIUS, 1.5x a hit,
//           thrown back and up
//   volley  five arrows fanned at the player, each an arrow hit of the boss
//   charge  a roar, then a rush at the player; caught within CHARGE_REACH, 2x a hit
//   leap    a jump at the player, landing a blow within LEAP_RADIUS, 1.2x a hit
//   summon  the floor's other mobs called to its side
//   fireball three fireballs at the player, each a ranged hit (they set nothing alight)
//   blink   a swirl of portal dust, then it steps behind the player; still close, 1.5x a hit
//   sonic   a charge that lights the line to the player, then a blast down it, 2.5x a
//           hit if the boss can still see them within SONIC_RANGE
//
// A hit is the boss's own melee hit on the player's armour (MobDamage), through the
// run's Evasion and Iron Skin
public class GauntletBossAbilities {
    private static final double SLAM_RADIUS = 5;
    private static final double LEAP_RADIUS = 4;
    private static final double CHARGE_REACH = 3;
    private static final double SONIC_RANGE = 15;
    private static final Particle.DustOptions WARNING = new Particle.DustOptions(Color.fromRGB(0xFF3030), 1.6f);

    // The run a boss fights in: whether it is still on, calling help, and the run's
    // own say on a hit (Evasion, Iron Skin)
    public interface Fight {
        boolean on();

        void summon(int count);

        double incoming(Player player, double damage);
    }

    private final Plugin plugin;
    private final DataRegistry registry;
    // When each boss may use each ability next
    private final Map<UUID, Map<String, Long>> ready = new HashMap<>();

    public GauntletBossAbilities(Plugin plugin, DataRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    // Every second while a boss lives
    public void tick(Player player, LivingEntity boss, List<GauntletData.BossAbility> abilities, int floor, Fight fight) {
        if (abilities.isEmpty()) return;
        long now = System.currentTimeMillis();
        Map<String, Long> next = ready.computeIfAbsent(boss.getUniqueId(), k -> new HashMap<>());
        for (GauntletData.BossAbility ability : abilities) {
            Long at = next.get(ability.type());
            if (at == null) {
                next.put(ability.type(), now + ability.cooldown() * 500L);
                continue;
            }
            if (now < at) continue;
            next.put(ability.type(), now + ability.cooldown() * 1000L);
            use(ability.type(), player, boss, floor, fight);
            return;
        }
    }

    public void forget(UUID boss) {
        ready.remove(boss);
    }

    private void use(String type, Player player, LivingEntity boss, int floor, Fight fight) {
        switch (type) {
            case "slam" -> slam(player, boss, fight);
            case "volley" -> volley(player, boss);
            case "charge" -> charge(player, boss, fight);
            case "leap" -> leap(player, boss, fight);
            case "fireball" -> fireball(player, boss);
            case "blink" -> blink(player, boss, fight);
            case "sonic" -> sonic(player, boss, fight);
            case "summon" -> {
                boss.getWorld().spawnParticle(Particle.SOUL, boss.getLocation().add(0, 1, 0), 30, 1, 1, 1, 0.02);
                fight.summon(2 + floor / 20);
            }
            default -> {
            }
        }
    }

    private void slam(Player player, LivingEntity boss, Fight fight) {
        ring(boss.getLocation(), SLAM_RADIUS);
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, 1f, 0.6f);
        later(20, () -> {
            if (!fight.on() || !boss.isValid()) return;
            Location at = boss.getLocation();
            at.getWorld().spawnParticle(Particle.EXPLOSION, at, 6, SLAM_RADIUS / 2, 0.2, SLAM_RADIUS / 2, 0);
            at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);
            blowAround(player, boss, at, SLAM_RADIUS, 1.5, 1.2, fight);
        });
    }

    private void volley(Player player, LivingEntity boss) {
        Location eyes = boss.getEyeLocation();
        Vector aim = player.getEyeLocation().toVector().subtract(eyes.toVector()).normalize();
        boss.getWorld().playSound(eyes, Sound.ENTITY_SKELETON_SHOOT, 1f, 0.7f);
        for (int i = -2; i <= 2; i++) {
            Vector direction = aim.clone().rotateAroundY(Math.toRadians(i * 8)).multiply(1.7);
            Arrow arrow = boss.launchProjectile(Arrow.class, direction);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        }
    }

    private void charge(Player player, LivingEntity boss, Fight fight) {
        boss.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, boss.getLocation().add(0, boss.getHeight(), 0), 8, 0.5, 0.3, 0.5, 0);
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1f, 1.2f);
        later(15, () -> {
            if (!fight.on() || !boss.isValid() || !player.isOnline()) return;
            Vector rush = player.getLocation().toVector().subtract(boss.getLocation().toVector()).setY(0);
            if (rush.lengthSquared() < 0.01) return;
            boss.setVelocity(rush.normalize().multiply(1.8).setY(0.3));
            later(10, () -> {
                if (!fight.on() || !boss.isValid() || !player.isOnline() || !player.getWorld().equals(boss.getWorld())) return;
                if (player.getLocation().distance(boss.getLocation()) <= CHARGE_REACH) {
                    hit(player, boss, 2.0, fight);
                    throwBack(player, boss.getLocation(), 1.4);
                }
            });
        });
    }

    private void leap(Player player, LivingEntity boss, Fight fight) {
        Vector jump = player.getLocation().toVector().subtract(boss.getLocation().toVector());
        double distance = Math.min(12, jump.length());
        if (distance < 0.5) return;
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_GOAT_LONG_JUMP, 1f, 0.6f);
        boss.setVelocity(jump.setY(0).normalize().multiply(distance * 0.12).setY(1.0));
        later(25, () -> {
            if (!fight.on() || !boss.isValid()) return;
            Location at = boss.getLocation();
            ring(at, LEAP_RADIUS);
            at.getWorld().spawnParticle(Particle.EXPLOSION, at, 3, 1, 0.2, 1, 0);
            at.getWorld().playSound(at, Sound.ENTITY_IRON_GOLEM_DAMAGE, 1f, 0.5f);
            blowAround(player, boss, at, LEAP_RADIUS, 1.2, 0.9, fight);
        });
    }

    private void fireball(Player player, LivingEntity boss) {
        Location eyes = boss.getEyeLocation();
        Vector aim = player.getEyeLocation().toVector().subtract(eyes.toVector()).normalize();
        boss.getWorld().playSound(eyes, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.8f);
        for (int i = -1; i <= 1; i++) {
            SmallFireball ball = boss.launchProjectile(SmallFireball.class, aim.clone().rotateAroundY(Math.toRadians(i * 10)).multiply(1.2));
            ball.setIsIncendiary(false);
        }
    }

    private void blink(Player player, LivingEntity boss, Fight fight) {
        boss.getWorld().spawnParticle(Particle.PORTAL, boss.getLocation().add(0, 1, 0), 60, 0.5, 1, 0.5, 0.5);
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.8f, 1.4f);
        later(12, () -> {
            if (!fight.on() || !boss.isValid() || !player.isOnline() || !player.getWorld().equals(boss.getWorld())) return;
            Vector behind = player.getLocation().getDirection().setY(0);
            if (behind.lengthSquared() < 0.01) behind = new Vector(0, 0, 1);
            Location to = player.getLocation().clone().subtract(behind.normalize().multiply(2));
            to.setDirection(player.getLocation().toVector().subtract(to.toVector()));
            boss.teleport(to);
            boss.getWorld().playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
            later(10, () -> {
                if (!fight.on() || !boss.isValid() || !player.isOnline() || !player.getWorld().equals(boss.getWorld())) return;
                if (player.getLocation().distance(boss.getLocation()) <= CHARGE_REACH) hit(player, boss, 1.5, fight);
            });
        });
    }

    private void sonic(Player player, LivingEntity boss, Fight fight) {
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.5f, 1f);
        for (int tick = 0; tick < 30; tick += 6) {
            later(tick, () -> {
                if (fight.on() && boss.isValid() && player.isOnline() && player.getWorld().equals(boss.getWorld())) line(boss, player, Particle.CRIT);
            });
        }
        later(34, () -> {
            if (!fight.on() || !boss.isValid() || !player.isOnline() || !player.getWorld().equals(boss.getWorld())) return;
            boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2f, 1f);
            line(boss, player, Particle.SONIC_BOOM);
            if (player.getLocation().distance(boss.getLocation()) <= SONIC_RANGE && boss.hasLineOfSight(player)) {
                hit(player, boss, 2.5, fight);
                throwBack(player, boss.getLocation(), 1.0);
            }
        });
    }

    // Dots from the boss's eyes to the player's
    private static void line(LivingEntity boss, Player player, Particle particle) {
        Location from = boss.getEyeLocation();
        Vector step = player.getEyeLocation().toVector().subtract(from.toVector());
        double length = Math.min(SONIC_RANGE, step.length());
        if (length < 0.5) return;
        step.normalize();
        for (double along = 1; along <= length; along += particle == Particle.SONIC_BOOM ? 2 : 0.8) {
            from.getWorld().spawnParticle(particle, from.clone().add(step.clone().multiply(along)), 1, 0, 0, 0, 0);
        }
    }

    // The player, if close enough, takes the blow and is thrown back
    private void blowAround(Player player, LivingEntity boss, Location at, double radius, double times, double throwPower, Fight fight) {
        if (!player.isOnline() || !player.getWorld().equals(at.getWorld()) || player.getLocation().distance(at) > radius) return;
        hit(player, boss, times, fight);
        throwBack(player, at, throwPower);
    }

    private void hit(Player player, LivingEntity boss, double times, Fight fight) {
        MobData mob = registry.mobOf(boss);
        double damage = mob == null ? 4.0 : MobDamage.dealt(mob, player, false) * times;
        damage = fight.incoming(player, damage);
        if (damage > 0) player.damage(damage);
    }

    private static void throwBack(Player player, Location from, double power) {
        Vector away = player.getLocation().toVector().subtract(from.toVector()).setY(0);
        if (away.lengthSquared() < 0.01) away = new Vector(0, 0, 1);
        player.setVelocity(away.normalize().multiply(power).setY(0.5 + power * 0.2));
    }

    private static void ring(Location centre, double radius) {
        for (int i = 0; i < 32; i++) {
            double angle = Math.PI * 2 * i / 32;
            Location dot = centre.clone().add(Math.cos(angle) * radius, 0.2, Math.sin(angle) * radius);
            centre.getWorld().spawnParticle(Particle.DUST, dot, 2, 0.1, 0, 0.1, 0, WARNING);
        }
    }

    private void later(long ticks, Runnable task) {
        plugin.getServer().getScheduler().runTaskLater(plugin, task, ticks);
    }
}
