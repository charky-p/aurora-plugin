package org.charky.aurora.features.gauntlet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.combat.CustomHealth;
import org.charky.aurora.features.combat.MobDamage;
import org.charky.aurora.features.spawner.MobData;
import org.charky.aurora.features.spawner.SpawnerManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// A floor's wave: its mutator, its mobs from floors.csv (more of them for a party, some
// elite, from floor 10 with an affix), a Shard Hoarder now and then, mobs hunting the
// nearest player standing, what an elite's affix does when it dies, and on boss floors
// the boss: tougher for a party, calling for help as it weakens, raging at the last
// third, with abilities of its own (bosses.csv)
final class GauntletWaves {
    // A mob this far from the arena's spawn has fallen off or wandered, it is brought back
    private static final double STRAY_DISTANCE = 80;
    private static final String HOARDER = "g_hoarder";
    private static final NamespacedKey ENRAGE = new NamespacedKey("aurora", "gauntlet_enrage");

    private final Aurora plugin;
    private final GauntletManager manager;
    private final SpawnerManager spawners;
    private final GauntletRunCombat combat;
    private final GauntletBossAbilities abilities;

    GauntletWaves(Aurora plugin, GauntletManager manager, SpawnerManager spawners, GauntletRunCombat combat) {
        this.plugin = plugin;
        this.manager = manager;
        this.spawners = spawners;
        this.combat = combat;
        this.abilities = new GauntletBossAbilities(plugin, manager.registry());
    }

    private GauntletData data() {
        return manager.data();
    }

    // A floor's mutator now and then from mutator_from, on every floor from there in the
    // Daily Tower; one that gives every mob an affix only from affix_from, like elites'
    GauntletData.Mutator rollMutator(GauntletRun run) {
        int floor = run.statFloor();
        if (data().mutators().isEmpty() || floor < data().setting("mutator_from", 6)) return null;
        Random random = run.floorRandom(1);
        if (run.towerDay == 0 && random.nextDouble(100) >= data().setting("mutator_chance", 25)) return null;
        boolean affixes = floor >= data().setting("affix_from", 10);
        List<GauntletData.Mutator> all = data().mutators().values().stream()
                .filter(mutator -> affixes || mutator.affix().isBlank()).toList();
        return all.isEmpty() ? null : all.get(random.nextInt(all.size()));
    }

    // Percent chance a mob of this floor is elite
    private double eliteChance(GauntletRun run) {
        GauntletData data = data();
        double from = data.setting("elite_from", 3);
        if (run.statFloor() < from) return 0;
        double chance = Math.min(data.setting("elite_chance_max", 15),
                data.setting("elite_chance", 4) + data.setting("elite_chance_per_floor", 0.2) * (run.statFloor() - from));
        return chance * (run.mutator == null ? 1 : run.mutator.elites());
    }

    // The floor's mobs at the arena's mob spawns, shuffled: party_mobs percent more for
    // every player past the first (and more on a Horde floor), the boss party_boss_health
    // percent tougher instead; now and then a Shard Hoarder
    void spawn(GauntletRun run) {
        GauntletData.Floor floor = data().floor(run.floor);
        GauntletArena arena = manager.arena();
        List<Location> spawns = new ArrayList<>(arena.mobSpawns().stream().map(spawn -> arena.inSlot(run.slot, spawn)).toList());
        Random random = run.floorRandom(2);
        Collections.shuffle(spawns, random);
        double eliteChance = eliteChance(run);
        run.floorScale = GauntletRun.partyScale(run.members.size(), data().setting("party_mobs", 50));
        double more = (run.mutator == null ? 1 : run.mutator.count()) * run.floorScale;
        int next = 0;
        int elites = 0;
        for (Map.Entry<String, Integer> entry : floor.mobs().entrySet()) {
            boolean boss = floor.isBoss(entry.getKey());
            int count = boss ? entry.getValue() : (int) Math.ceil(entry.getValue() * more);
            for (int i = 0; i < count; i++) {
                Location at = spawns.get(next++ % spawns.size()).clone()
                        .add(random.nextDouble(-1.5, 1.5), 0, random.nextDouble(-1.5, 1.5));
                boolean elite = !boss && random.nextDouble(100) < eliteChance;
                String affix = elite ? rollAffix(run, random) : null;
                if (spawnOne(run, entry.getKey(), boss, elite, affix, at) && elite) elites++;
            }
        }
        run.floorMobs = run.mobs.size();
        run.floorStarted = System.currentTimeMillis();
        if (!floor.boss() && run.floor >= data().setting("hoarder_from", 3)
                && random.nextDouble(100) < data().setting("hoarder_chance", 8)) {
            hoarder(run, spawns.get(random.nextInt(spawns.size())));
        }
        if (elites > 0) {
            run.sound(Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.4f, 1.4f);
            for (Player player : run.online()) {
                player.sendActionBar(TextUtils.parse("<gold><b>" + elites + " elite" + (elites > 1 ? "s" : "") + "</b> <gray>on this floor"));
            }
        }
    }

    // One of the floor's mobs: an elite with its affix, any other with the mutator's, or
    // the boss, tougher for a party. Whether it came
    private boolean spawnOne(GauntletRun run, String kind, boolean boss, boolean elite, String eliteAffix, Location at) {
        String affix = elite ? eliteAffix : boss || run.mutator == null || run.mutator.affix().isBlank() ? null : run.mutator.affix();
        String mutatorId = run.mutator == null ? null : run.mutator.id();
        LivingEntity mob = mob(run, new GauntletData.Variant(kind, run.statFloor(), elite, affix, run.heat, mutatorId).id(), at);
        if (mob == null) return false;
        if (boss) {
            run.boss = mob.getUniqueId();
            double tougher = GauntletRun.partyScale(run.members.size(), data().setting("party_boss_health", 60));
            if (tougher > 1) CustomHealth.scale(mob, tougher, manager.registry());
        }
        if (elite) run.elites.add(mob.getUniqueId());
        if (affix != null) {
            run.affixes.put(mob.getUniqueId(), affix);
            if (affix.equals("swift")) {
                AttributeInstance speed = mob.getAttribute(Attribute.MOVEMENT_SPEED);
                if (speed != null) speed.setBaseValue(speed.getBaseValue() * 1.4);
            }
        }
        return true;
    }

    // A Shard Hoarder: quick, glowing, harmless, not needed to clear the floor, and worth
    // hoarder_pay kills if caught before it gets away
    private void hoarder(GauntletRun run, Location at) {
        if (manager.registry().getMob(HOARDER) == null) return;
        LivingEntity hoarder = spawners.spawnGauntletMob(new GauntletData.Variant(HOARDER, run.statFloor(), false, run.heat, null).id(), at);
        if (hoarder == null) return;
        run.hoarder = hoarder.getUniqueId();
        int seconds = (int) data().setting("hoarder_seconds", 20);
        run.sound(Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 2f);
        run.tell("<gold><b>SHARD HOARDER</b> <gray>appeared! Catch it in <white>" + seconds + " seconds</white> for a pile of Shards");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!hoarder.isValid() || hoarder.isDead()) return;
            hoarder.getWorld().spawnParticle(Particle.POOF, hoarder.getLocation(), 20, 0.3, 0.3, 0.3, 0.05);
            hoarder.remove();
            if (!run.over && hoarder.getUniqueId().equals(run.hoarder)) {
                run.hoarder = null;
                run.tell("<gray>The Shard Hoarder got away");
            }
        }, seconds * 20L);
    }

    // An affix for an elite from affix_from on, null before or without any
    private String rollAffix(GauntletRun run, Random random) {
        if (data().affixes().isEmpty() || run.statFloor() < data().setting("affix_from", 10)) return null;
        List<String> ids = List.copyOf(data().affixes().keySet());
        return ids.get(random.nextInt(ids.size()));
    }

    // One mob of the run, hunting the nearest player standing, sped up by the floor's mutator
    private LivingEntity mob(GauntletRun run, String id, Location at) {
        LivingEntity mob = spawners.spawnGauntletMob(id, at);
        if (mob == null) return null;
        run.mobs.add(mob.getUniqueId());
        if (run.mutator != null && run.mutator.speed() != 1) {
            AttributeInstance speed = mob.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(speed.getBaseValue() * run.mutator.speed());
        }
        Player target = nearest(run.standing(), at);
        if (mob instanceof Mob hunter && target != null) hunter.setTarget(target);
        return mob;
    }

    // The player nearest a spot, null if there is nobody
    private static Player nearest(List<Player> players, Location at) {
        Player best = null;
        double distance = Double.MAX_VALUE;
        for (Player player : players) {
            if (!player.getWorld().equals(at.getWorld())) continue;
            double d = player.getLocation().distanceSquared(at);
            if (d < distance) {
                distance = d;
                best = player;
            }
        }
        return best;
    }

    // Every second: mobs that are gone count as dead, strays are brought back, and a mob
    // hunting nobody, or someone down or gone, hunts the nearest player standing instead
    void hunt(GauntletRun run, Location spawn) {
        List<Player> standing = run.standing();
        run.mobs.removeIf(id -> {
            Entity mob = Bukkit.getEntity(id);
            if (mob == null || !mob.isValid() || mob.isDead()) return true;
            if (spawn != null && (!mob.getWorld().equals(spawn.getWorld()) || mob.getLocation().distance(spawn) > STRAY_DISTANCE)) {
                mob.teleport(spawn);
            }
            if (mob instanceof Mob hunter && !(hunter.getTarget() instanceof Player target && standing.contains(target))) {
                Player near = nearest(standing, mob.getLocation());
                if (near != null) hunter.setTarget(near);
            }
            return false;
        });
    }

    // At two thirds and one third of its health a boss calls the floor's other mobs to
    // its side; at the last third it rages, moving faster
    void bossPhases(GauntletRun run) {
        if (run.boss == null || run.bossPhase >= 2 || !(Bukkit.getEntity(run.boss) instanceof LivingEntity boss) || !boss.isValid()) return;
        double left = CustomHealth.current(boss) / CustomHealth.max(boss);
        while (run.bossPhase < 2 && left <= (2 - run.bossPhase) / 3.0) {
            run.bossPhase++;
            callAdds(run, boss, (int) data().setting("boss_adds", 2) + run.statFloor() / 10);
            if (run.bossPhase == 2) {
                AttributeInstance speed = boss.getAttribute(Attribute.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.removeModifier(ENRAGE);
                    speed.addModifier(new AttributeModifier(ENRAGE, data().setting("boss_enrage_speed", 20) / 100,
                            AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY));
                }
                boss.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, boss.getLocation().add(0, boss.getHeight(), 0), 10, 0.5, 0.3, 0.5, 0);
                run.sound(Sound.ENTITY_RAVAGER_ROAR, 0.8f, 0.8f);
                run.tell("<dark_red><b>ENRAGED</b> <gray>The boss moves faster now");
            }
        }
    }

    // The boss's own abilities (bosses.csv), at the player standing nearest it
    void bossAbilities(GauntletRun run) {
        if (run.boss == null || !(Bukkit.getEntity(run.boss) instanceof LivingEntity boss) || !boss.isValid()) return;
        Player target = nearest(run.standing(), boss.getLocation());
        if (target == null) return;
        String mobId = boss.getPersistentDataContainer().get(DataKeys.MOB_KEY, PersistentDataType.STRING);
        List<GauntletData.BossAbility> list = data().bosses().getOrDefault(GauntletData.baseMobId(mobId), List.of());
        abilities.tick(target, boss, list, run.statFloor(), new GauntletBossAbilities.Fight() {
            @Override
            public boolean on() {
                return !run.over && run.state == GauntletRun.State.FIGHTING;
            }

            @Override
            public void summon(int count) {
                callAdds(run, boss, count);
            }

            @Override
            public double incoming(Player victim, double damage) {
                return combat.incoming(victim, boss, damage);
            }
        });
    }

    // A boss killed, or left behind by a run that ended: its abilities' state goes
    void bossGone(UUID boss) {
        abilities.forget(boss);
    }

    private void callAdds(GauntletRun run, LivingEntity boss, int adds) {
        if (spawnAdds(run, boss.getLocation(), adds) == 0) return;
        run.sound(Sound.EVENT_RAID_HORN, 0.5f, 1.4f);
        run.tell("<dark_red><b>REINFORCEMENTS</b> <gray>The boss calls <white>" + adds + "</white> to its side");
    }

    // The floor's other mobs, plain ones, around a spot. How many came
    private int spawnAdds(GauntletRun run, Location around, int adds) {
        GauntletData.Floor floor = data().floor(run.floor);
        List<String> kinds = floor.mobs().keySet().stream().filter(id -> !floor.isBoss(id)).toList();
        if (kinds.isEmpty() || adds <= 0) return 0;
        String mutatorId = run.mutator == null ? null : run.mutator.id();
        for (int i = 0; i < adds; i++) {
            String kind = kinds.get(ThreadLocalRandom.current().nextInt(kinds.size()));
            Location at = around.clone().add(ThreadLocalRandom.current().nextDouble(-2, 2), 0, ThreadLocalRandom.current().nextDouble(-2, 2));
            mob(run, new GauntletData.Variant(kind, run.statFloor(), false, run.heat, mutatorId).id(), at);
        }
        run.floorMobs += adds;
        return adds;
    }

    // An elite's affix when it dies: Explosive goes off a second later, Summoner calls
    // two of the floor's mobs
    void affixDeath(GauntletRun run, LivingEntity dead, String affix) {
        Location at = dead.getLocation();
        if (affix.equals("summoner")) {
            if (spawnAdds(run, at, 2) > 0) {
                at.getWorld().spawnParticle(Particle.SOUL, at.clone().add(0, 1, 0), 25, 0.6, 0.6, 0.6, 0.02);
                at.getWorld().playSound(at, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 0.8f, 1.2f);
            }
            return;
        }
        if (!affix.equals("explosive")) return;
        MobData mob = manager.registry().mobOf(dead);
        at.getWorld().spawnParticle(Particle.SMOKE, at.clone().add(0, 0.5, 0), 30, 0.4, 0.3, 0.4, 0.02);
        at.getWorld().playSound(at, Sound.ENTITY_CREEPER_PRIMED, 1f, 1f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (run.over) return;
            at.getWorld().spawnParticle(Particle.EXPLOSION, at, 3, 0.5, 0.3, 0.5, 0);
            at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.1f);
            for (Player player : run.standing()) {
                if (!player.getWorld().equals(at.getWorld()) || player.getLocation().distance(at) > 3.5) continue;
                double damage = mob == null ? 4 : MobDamage.dealt(mob, player, false) * 1.5;
                damage = combat.incoming(player, null, damage);
                if (damage > 0) player.damage(damage);
            }
        }, 20L);
    }
}
