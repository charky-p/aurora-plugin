package org.charky.aurora.features.boss;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// The Marsh Hydra's fight: a zoglin body with a stack of Hydra Heads, bogged that
// ride on its back and shoot.
//  - Guarded: while it has a head, the body takes no hits at all
//  - Heads: three to start, stacked one on another. The bottom one is in reach;
//    kill it and the ones above drop down a place
//  - Regrowth: every head killed grows back on top of the stack a while later. Kill
//    all three and the body is open until the first one is back
// Vanilla would let the first rider steer a mob it rides, the way a skeleton steers
// its spider, so the heads ride an invisible marker on its back instead and the
// zoglin keeps walking at players by itself. Heads go with the body
public class MarshHydra implements BossAbilities {
    public static final String BOSS_ID = "marsh_hydra";
    public static final String HEAD_ID = "hydra_head";

    private static final int HEADS = 3;
    // From a head dying to it growing back, 20 seconds
    private static final int REGROW_TICKS = 400;
    // A player hitting the guarded body is told why, at most this often
    private static final long GUARD_MESSAGE_MS = 1500;
    private static final double TARGET_RANGE = 48.0;

    private final Aurora plugin;
    // Bottom of the stack first
    private final List<UUID> heads = new ArrayList<>();
    // The fight tick each missing head died on, the first to grow back first
    private final Deque<Integer> regrowing = new ArrayDeque<>();
    private final Map<UUID, Long> told = new HashMap<>();
    private ArmorStand seat;
    private int age = 0;
    private boolean grown = false;
    private boolean warned = false;

    public MarshHydra(Aurora plugin) {
        this.plugin = plugin;
    }

    @Override
    public void tick(BossFight fight) {
        age += BossManager.TICKS;

        // Not in the constructor: the body is still being added to the world then
        if (!grown) {
            grown = true;
            for (int i = 0; i < HEADS; i++) grow(fight, false);
        }

        int before = heads.size();
        heads.removeIf(uuid -> {
            Entity head = plugin.getServer().getEntity(uuid);
            return head == null || !head.isValid() || head.isDead();
        });
        for (int i = heads.size(); i < before; i++) regrowing.add(age);

        if (before > 0 && heads.isEmpty()) {
            LivingEntity boss = fight.boss();
            boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ZOGLIN_ANGRY, 1.5f, 0.5f);
            for (Player player : fight.playersWithin(TARGET_RANGE)) {
                player.sendActionBar(TextUtils.parse("<green>The Hydra has no heads, strike the body!"));
            }
        }

        while (!regrowing.isEmpty() && heads.size() < HEADS && age - regrowing.peek() >= REGROW_TICKS) {
            regrowing.poll();
            if (!grow(fight, true)) break;
        }

        restack(fight);
    }

    // One more head on top of the stack. False if the head mob is not in the registry
    private boolean grow(BossFight fight, boolean regrown) {
        String spawner = fight.spawnerName();
        if (spawner == null) return false;

        LivingEntity boss = fight.boss();
        Location at = boss.getLocation().add(0, boss.getHeight(), 0);
        LivingEntity head = plugin.getSpawnerManager().spawnMob(HEAD_ID, at, spawner);
        if (head == null) {
            if (!warned) plugin.getLogger().warning("The Marsh Hydra has no '" + HEAD_ID + "' mob in the registry to grow");
            warned = true;
            return false;
        }

        heads.add(head.getUniqueId());
        List<Player> targets = fight.playersWithin(TARGET_RANGE);
        if (head instanceof Mob mob && !targets.isEmpty()) {
            mob.setTarget(targets.get(ThreadLocalRandom.current().nextInt(targets.size())));
        }
        if (regrown) {
            boss.getWorld().playSound(at, Sound.ENTITY_SLIME_SQUISH, 1.5f, 0.5f);
            boss.getWorld().spawnParticle(Particle.ITEM_SLIME, at, 30, 0.4, 0.4, 0.4, 0.05);
        }
        return true;
    }

    // Body, seat, then each head on the one below. A head that died took the ones
    // above it off with it; they climb back on a place lower
    private void restack(BossFight fight) {
        LivingEntity boss = fight.boss();
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

        Entity below = seat;
        for (UUID uuid : heads) {
            Entity head = plugin.getServer().getEntity(uuid);
            if (head == null) continue;

            mount(below, head);
            below = head;
        }
    }

    private static void mount(Entity vehicle, Entity rider) {
        if (rider.getVehicle() == vehicle) return;

        rider.leaveVehicle();
        vehicle.addPassenger(rider);
    }

    @Override
    public boolean shields(BossFight fight, Player attacker) {
        if (heads.isEmpty()) return false;

        long now = System.currentTimeMillis();
        Long last = told.get(attacker.getUniqueId());
        if (last == null || now - last >= GUARD_MESSAGE_MS) {
            told.put(attacker.getUniqueId(), now);
            attacker.sendActionBar(TextUtils.parse("<red>The heads guard the body, cut them off first"));
            attacker.playSound(fight.boss().getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.6f);
        }
        return true;
    }

    @Override
    public void end(BossFight fight) {
        for (UUID uuid : heads) {
            Entity head = plugin.getServer().getEntity(uuid);
            if (head == null || !head.isValid()) continue;

            head.getWorld().spawnParticle(Particle.ITEM_SLIME, head.getLocation(), 15, 0.3, 0.3, 0.3, 0.05);
            head.remove();
        }
        heads.clear();
        regrowing.clear();
        told.clear();
        if (seat != null) seat.remove();
        seat = null;
    }
}
