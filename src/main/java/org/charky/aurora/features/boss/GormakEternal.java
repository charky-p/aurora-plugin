package org.charky.aurora.features.boss;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.combat.CustomHealth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// Gormak the Eternal's fight, on top of his slow heavy swing:
//  - Stomp: he hops, then slams. Everyone standing near him is thrown back, a
//    player in the air when he lands is not
//  - Ghost Husks: a wave each time his health passes a threshold. Fast, a hit or
//    two to kill, and their scythes take hunger instead of much health
//  - Totem: the first killing blow pops the totem he holds. He comes back with
//    part of his health, faster, stomping more often, with one more wave
public class GormakEternal implements BossAbilities {
    public static final String BOSS_ID = "gormak_eternal";
    public static final String GHOST_ID = "ghost_husk";

    // Seconds between slams while someone is in reach, and once the totem has popped
    private static final int STOMP_INTERVAL_TICKS = 120;
    private static final int ENRAGED_STOMP_INTERVAL_TICKS = 80;
    // The first one comes quickly, so every fight shows it
    private static final int FIRST_STOMP_TICKS = 60;
    // From the hop to the slam, the time a player has to jump
    private static final int STOMP_WINDUP_TICKS = 15;
    // He only bothers when someone is this close, and the slam reaches this far
    private static final double STOMP_RANGE = 6.0;
    private static final double STOMP_HOP = 0.55;
    // How hard the slam throws a player away, and up
    private static final double STOMP_KNOCKBACK = 1.6;
    private static final double STOMP_LIFT = 0.75;

    // How far from him a wave appears
    private static final double GHOST_MIN_DISTANCE = 3.0;
    private static final double GHOST_MAX_DISTANCE = 5.0;
    // A ghost nobody killed fades by itself
    private static final int GHOST_LIFETIME_TICKS = 600;
    // Invisible but for the scythe and the name over it
    private static final boolean GHOSTS_INVISIBLE = true;
    // What a scythe hit takes: food points straight away, and the Hunger effect
    private static final int SCYTHE_FOOD_LOSS = 2;
    private static final int SCYTHE_HUNGER_TICKS = 120;

    private static final double ENRAGED_SPEED = 1.2;
    // Who is told when the totem pops
    private static final double ANNOUNCE_RANGE = 64.0;

    private final Aurora plugin;
    // Each living ghost and the fight tick it was called on
    private final Map<UUID, Integer> ghosts = new HashMap<>();
    private int age = 0;
    private int nextStomp = FIRST_STOMP_TICKS;
    // The tick he lands on, -1 while he is not mid stomp
    private int slamAt = -1;
    private int wavesSent = 0;
    private boolean totemUsed = false;
    private boolean warnedNoGhost = false;

    public GormakEternal(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void tick(BossFight fight) {
        age += BossManager.TICKS;
        fadeOldGhosts(fight);

        if (!totemUsed) {
            int due = GormakPhases.wavesDue(fight.healthRatio());
            while (wavesSent < due) {
                summonWave(fight, GormakPhases.WAVE_SIZE);
                wavesSent++;
            }
        }

        if (slamAt >= 0) {
            if (age >= slamAt) slam(fight);
        } else if (age >= nextStomp && !fight.playersWithin(STOMP_RANGE).isEmpty()) {
            windUp(fight);
        }
    }

    // The hop is the warning, with a roar
    private void windUp(BossFight fight) {
        LivingEntity boss = fight.boss();
        boss.setVelocity(new Vector(0, STOMP_HOP, 0));
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.6f);
        slamAt = age + STOMP_WINDUP_TICKS;
    }

    private void slam(BossFight fight) {
        slamAt = -1;
        nextStomp = age + (totemUsed ? ENRAGED_STOMP_INTERVAL_TICKS : STOMP_INTERVAL_TICKS);

        LivingEntity boss = fight.boss();
        Location centre = boss.getLocation();
        World world = boss.getWorld();

        world.playSound(centre, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
        world.spawnParticle(Particle.EXPLOSION, centre, 3, 0.6, 0.1, 0.6, 0);
        for (int i = 0; i < 24; i++) {
            double angle = 2 * Math.PI * i / 24;
            world.spawnParticle(Particle.CLOUD,
                    centre.clone().add(Math.cos(angle) * STOMP_RANGE, 0.2, Math.sin(angle) * STOMP_RANGE), 2, 0.1, 0.05, 0.1, 0.01);
        }

        for (Player player : fight.playersWithin(STOMP_RANGE)) {
            // Jumped in time
            if (!standing(player)) continue;

            Vector away = player.getLocation().toVector().subtract(centre.toVector()).setY(0);
            if (away.lengthSquared() < 1.0E-4) away = player.getLocation().getDirection().multiply(-1).setY(0);
            if (away.lengthSquared() < 1.0E-4) away = new Vector(1, 0, 0);

            player.setVelocity(away.normalize().multiply(STOMP_KNOCKBACK).setY(STOMP_LIFT));
        }
    }

    // On the ground, or close enough to it that the shockwave reaches
    private static boolean standing(Player player) {
        return !player.getLocation().subtract(0, 0.25, 0).getBlock().isPassable();
    }

    private void summonWave(BossFight fight, int size) {
        LivingEntity boss = fight.boss();
        String spawner = fight.spawnerName();
        if (spawner == null) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Player> targets = fight.playersWithin(ANNOUNCE_RANGE);
        int count = GormakPhases.ghostsToSummon(size, ghosts.size());

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble(2 * Math.PI);
            double distance = random.nextDouble(GHOST_MIN_DISTANCE, GHOST_MAX_DISTANCE);
            Location at = boss.getLocation().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
            // Never inside a wall, beside him is always free
            if (!at.getBlock().isPassable() || !at.clone().add(0, 1, 0).getBlock().isPassable()) at = boss.getLocation();

            LivingEntity ghost = plugin.getSpawnerManager().spawnMob(GHOST_ID, at, spawner);
            if (ghost == null) {
                if (!warnedNoGhost) {
                    plugin.getLogger().warning("Gormak has no '" + GHOST_ID + "' mob in the registry to summon");
                    warnedNoGhost = true;
                }
                return;
            }

            if (GHOSTS_INVISIBLE) {
                ghost.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false));
            }
            if (ghost instanceof Mob mob && !targets.isEmpty()) {
                mob.setTarget(targets.get(random.nextInt(targets.size())));
            }

            ghosts.put(ghost.getUniqueId(), age);
            at.getWorld().spawnParticle(Particle.SOUL, at.clone().add(0, 1, 0), 20, 0.3, 0.6, 0.3, 0.02);
        }

        if (count > 0) boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.2f, 0.5f);
    }

    private void fadeOldGhosts(BossFight fight) {
        ghosts.entrySet().removeIf(entry -> {
            Entity ghost = plugin.getServer().getEntity(entry.getKey());
            if (ghost == null || !ghost.isValid()) return true;
            if (age - entry.getValue() < GHOST_LIFETIME_TICKS) return false;

            fade(ghost);
            return true;
        });
    }

    private static void fade(Entity ghost) {
        ghost.getWorld().spawnParticle(Particle.SOUL, ghost.getLocation().add(0, 1, 0), 10, 0.3, 0.6, 0.3, 0.02);
        ghost.remove();
    }

    @Override
    public boolean lastStand(BossFight fight) {
        if (totemUsed) return false;
        totemUsed = true;

        LivingEntity boss = fight.boss();
        CustomHealth.heal(boss, CustomHealth.max(boss) * GormakPhases.REVIVE_SHARE, plugin.getRegistry());
        if (boss.getEquipment() != null) boss.getEquipment().setItemInOffHand(null);

        AttributeInstance speed = boss.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(speed.getBaseValue() * ENRAGED_SPEED);

        Location centre = boss.getLocation();
        boss.getWorld().playSound(centre, Sound.ITEM_TOTEM_USE, 1.5f, 0.8f);
        boss.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, centre.clone().add(0, 2, 0), 120, 0.8, 1.5, 0.8, 0.4);
        fight.bar().color(BossBar.Color.PURPLE);

        Component message = TextUtils.parse("<dark_red><b>").append(fight.bar().name())
                .append(TextUtils.parse("<reset><gray> refuses to die"));
        for (Player player : fight.playersWithin(ANNOUNCE_RANGE)) player.sendMessage(message);

        // He comes back swinging: no waiting for the next stomp, and one more wave
        nextStomp = age;
        summonWave(fight, GormakPhases.LAST_STAND_WAVE_SIZE);
        return true;
    }

    // His ghosts go with him
    @Override
    public void end(BossFight fight) {
        for (UUID uuid : ghosts.keySet()) {
            Entity ghost = plugin.getServer().getEntity(uuid);
            if (ghost != null && ghost.isValid()) fade(ghost);
        }
        ghosts.clear();
    }

    // A scythe that lands takes food, a blocked one takes nothing
    public static void reap(Player player) {
        player.setFoodLevel(Math.max(0, player.getFoodLevel() - SCYTHE_FOOD_LOSS));
        player.setSaturation(0f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, SCYTHE_HUNGER_TICKS, 0));
    }
}
