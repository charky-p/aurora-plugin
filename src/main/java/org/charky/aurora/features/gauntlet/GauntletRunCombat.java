package org.charky.aurora.features.gauntlet;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.charky.aurora.features.combat.CombatHooks;
import org.charky.aurora.features.combat.CustomHealth;
import org.charky.aurora.features.gauntlet.GauntletRun.Member;

import java.util.concurrent.ThreadLocalRandom;

// A run's side of every Aurora hit (CombatHooks), each player by their own boons and
// perks: what is added to the hits they land, Cleave's splash, and what dodging,
// guarding and the attacking elite's affix do to the hits they take
final class GauntletRunCombat {
    private static final double CLEAVE_RANGE = 3;
    private static final double EXECUTE_BELOW = 0.3;
    // Vanilla Strength does nothing to Aurora hits, in a run each level adds this much
    private static final double STRENGTH_PER_LEVEL = 0.3;

    private final GauntletManager manager;
    private final GauntletRuns runs;
    private final GauntletBonuses bonuses;

    GauntletRunCombat(GauntletManager manager, GauntletRuns runs, GauntletBonuses bonuses) {
        this.manager = manager;
        this.runs = runs;
        this.bonuses = bonuses;
        CombatHooks.set(this::outgoing, this::incoming, this::afterHit);
    }

    // Might, First Strike, Executioner and Giant Slayer add up; Keen Eye doubles; then
    // Strength potions and the weapon's own share, in a run only
    private double outgoing(Player attacker, LivingEntity victim, boolean ranged) {
        GauntletRun run = runs.runOf(attacker);
        Member member = runs.memberOf(attacker);
        if (run == null || member == null) return 1.0;
        double percent = bonuses.boonTotal(member, "damage") + bonuses.perkTotal(attacker, "damage");
        double health = CustomHealth.current(victim) / Math.max(1, CustomHealth.max(victim));
        if (health >= 0.999) percent += bonuses.boonTotal(member, "firststrike") + bonuses.perkTotal(attacker, "firststrike");
        if (health < EXECUTE_BELOW) percent += bonuses.boonTotal(member, "execute") + bonuses.perkTotal(attacker, "execute");
        if (victim.getUniqueId().equals(run.boss) || run.elites.contains(victim.getUniqueId())) {
            percent += bonuses.boonTotal(member, "slayer") + bonuses.perkTotal(attacker, "slayer");
        }
        double multiplier = 1 + percent / 100;
        if ("shielded".equals(run.affixes.get(victim.getUniqueId())) && health >= 0.5) multiplier *= 0.5;
        if (ThreadLocalRandom.current().nextDouble(100) < bonuses.boonTotal(member, "crit") + bonuses.perkTotal(attacker, "crit")) multiplier *= 2;
        PotionEffect strength = attacker.getPotionEffect(PotionEffectType.STRENGTH);
        if (strength != null) multiplier *= 1 + STRENGTH_PER_LEVEL * (strength.getAmplifier() + 1);
        if (!ranged) {
            GauntletData.Gear gear = manager.data().gear().get(GauntletGear.gearId(attacker.getInventory().getItemInMainHand()));
            if (gear != null) multiplier *= gear.damage() / 100.0;
        }
        return multiplier;
    }

    // Cleave: a share of every melee hit to the run's other mobs close to the one struck
    private void afterHit(Player attacker, LivingEntity victim, double dealt, boolean ranged) {
        GauntletRun run = runs.runOf(attacker);
        Member member = runs.memberOf(attacker);
        if (run == null || member == null || ranged || dealt <= 0 || !run.mobs.contains(victim.getUniqueId())) return;
        double cleave = bonuses.boonTotal(member, "cleave") + bonuses.perkTotal(attacker, "cleave");
        if (cleave <= 0) return;
        double splash = Math.round(dealt * cleave / 100);
        if (splash <= 0) return;
        for (Entity near : victim.getNearbyEntities(CLEAVE_RANGE, CLEAVE_RANGE, CLEAVE_RANGE)) {
            if (near instanceof LivingEntity other && run.mobs.contains(other.getUniqueId()) && !other.isDead()) {
                CustomHealth.apply(other, splash, attacker, manager.registry());
                other.getWorld().spawnParticle(Particle.SWEEP_ATTACK, other.getLocation().add(0, 1, 0), 1);
            }
        }
    }

    // Evasion and Iron Skin, and the attacking elite's affix: Vampiric heals, Frenzied
    // hits harder below half health. Nothing reaches a player who is down. Also for the
    // bosses' abilities and Explosive elites, with no source for those
    double incoming(Player victim, LivingEntity source, double damage) {
        GauntletRun run = runs.runOf(victim);
        Member member = runs.memberOf(victim);
        if (run == null || member == null) return damage;
        if (member.down) return 0;
        String affix = source == null ? null : run.affixes.get(source.getUniqueId());
        if ("frenzied".equals(affix) && CustomHealth.current(source) < CustomHealth.max(source) / 2) damage *= 1.3;
        if ("vampiric".equals(affix)) {
            CustomHealth.heal(source, CustomHealth.max(source) * 0.1, manager.registry());
            source.getWorld().spawnParticle(Particle.HEART, source.getLocation().add(0, source.getHeight(), 0), 2, 0.3, 0.2, 0.3, 0);
        }
        if (ThreadLocalRandom.current().nextDouble(100) < bonuses.boonTotal(member, "dodge")) {
            victim.playSound(victim.getLocation(), Sound.ENTITY_BREEZE_JUMP, 0.6f, 1.6f);
            return 0;
        }
        return damage * (1 - Math.min(0.8, (bonuses.boonTotal(member, "guard") + bonuses.setTotal(manager.profile(victim), "guard")) / 100));
    }
}
