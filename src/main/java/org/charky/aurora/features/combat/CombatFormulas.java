package org.charky.aurora.features.combat;

// The pure maths of combat, no Bukkit, so it is tested directly against the
// numbers in scripts/balance/balance_sim.py. Keep the two in step
public final class CombatFormulas {
    // Every sword hits for this before Sharpness
    public static final double BASE_DAMAGE = 10.0;

    // A jump crit multiplies the hit, as in vanilla. The balance sim leaves crits out,
    // so its kill times are what a player gets without them
    public static final double CRIT_MULTIPLIER = 1.5;

    // Every mob hit is this share of heartsPerHit, tuned down after the first
    // playtest, and arrows and other projectiles take a share of that again since
    // they land from range and with no swing to dodge
    public static final double MOB_DAMAGE_SCALE = 0.6;
    public static final double RANGED_SCALE = 0.5;

    // Mob hits are dealt in real hearts, set by how many tiers the mob's attack
    // level sits above the player's protection level. Each zone spans this many
    // protection levels
    static final double LEVELS_PER_TIER = 4.0;
    private static final double LOG_A = 1 / Math.log(1.15);
    private static final double LOG_B = 0.15;
    // The least a hit can do to armour far ahead of the mob, in hearts
    static final double ARMOUR_FLOOR = 0.25;

    private static final int BRACKET_SIZE = 5;
    private static final int[] BRACKET_BONUS = {1, 3, 5, 10, 15, 20, 25, 30, 35, 40};
    private static final int BRACKET_STEP = 5;

    private static final double FULL_CHARGE_MS = 625.0;
    // What a swing with no charge at all still does, enough to finish off what is nearly dead
    private static final double SPAM_FLOOR = 0.02;

    // A raised shield fully covers attacks up to this level at Deflection 0, the
    // Abandoned City's, and each zone's 3 Deflection levels cover its 4 attack levels
    static final double DEFLECTION_BASE_ATTACK = 11.0;
    static final double ATTACK_PER_DEFLECTION = 4.0 / 3.0;
    // Tiers (about a zone each) above what the shield covers: up to FREE nothing gets
    // through, so a shield holds against mobs two zones ahead of it; from BREAK the hit
    // breaks through in full and the shield goes on cooldown
    static final double DEFLECT_FREE_TIERS = 2.0;
    static final double DEFLECT_BREAK_TIERS = 4.0;

    // Protection takes a share off every hit, up to REDUCTION_AT_MAX at
    // MAX_PROTECTION levels; levels past it add nothing. The share rises steeply at
    // first (level / max to the power PROTECTION_CURVE) so the low levels of the
    // early zones count: 27% off at 5, 39% at 12, 52% at 25, 68% at 50, 90% at 100
    public static final int MAX_PROTECTION = 100;
    static final double REDUCTION_AT_MAX = 0.9;
    static final double PROTECTION_CURVE = 0.4;

    private CombatFormulas() {
    }

    // What is left of a hit after Protection: 1 with none, 0.1 at the cap
    public static double protectionMultiplier(double protection) {
        double levels = Math.clamp(protection, 0, MAX_PROTECTION);
        return 1 - REDUCTION_AT_MAX * Math.pow(levels / MAX_PROTECTION, PROTECTION_CURVE);
    }

    // A hit before Protection, from the mob's attack level less the player's Defence.
    // At an attack of 0 it is one heart. Above it grows like a log, 2, 2.9, 3.7, 4.4,
    // 5 a tier at a time. Defence past the attack makes it fall fast, half a heart two
    // levels past, tapering to a quarter of a heart, never less. Both sides meet at
    // exactly 1 so there is no jump
    public static double heartsPerHit(double attackLevel, double defence) {
        double gap = (attackLevel - defence) / LEVELS_PER_TIER;
        if (gap >= 0) return 1 + LOG_A * Math.log(1 + LOG_B * gap);
        return ARMOUR_FLOOR + (1 - ARMOUR_FLOOR) * Math.exp(2 * gap);
    }

    // Levels 1-5 give 1 each, 6-10 give 3 each, 11-15 give 5, and so on. Past
    // the table the increment keeps rising by BRACKET_STEP so there is no cliff.
    public static double sharpnessBonus(int sharpness) {
        if (sharpness <= 0) return 0.0;

        double total = 0.0;
        int remaining = sharpness;
        int bracket = 0;

        while (remaining > 0) {
            int levels = Math.min(remaining, BRACKET_SIZE);
            total += (double) levels * bonusForBracket(bracket);
            remaining -= levels;
            bracket++;
        }

        return total;
    }

    // 2% damage on a spammed swing rising to 100% on a timed one, cubed so clicking fast
    // never pays: at ten clicks a second the hits add up to about a sixth of what timed
    // swings deal, half charge pays 14%. Only something nearly dead dies to spam
    public static double chargeMultiplier(long sinceLastSwingMs) {
        double charge = Math.min(1.0, sinceLastSwingMs / FULL_CHARGE_MS);
        return SPAM_FLOOR + (1 - SPAM_FLOOR) * charge * charge * charge;
    }

    // How many tiers a hit sits above what the shield covers, negative when below
    public static double deflectionGap(double attackLevel, int deflection) {
        double covered = DEFLECTION_BASE_ATTACK + deflection * ATTACK_PER_DEFLECTION;
        return (attackLevel - covered) / LEVELS_PER_TIER;
    }

    public static boolean breaksShield(double gap) {
        return gap >= DEFLECT_BREAK_TIERS;
    }

    // Share of the unblocked hit that gets through a raised shield: none for a
    // similar tier, rising evenly to all of it where the shield breaks
    public static double throughShield(double gap) {
        if (gap <= DEFLECT_FREE_TIERS) return 0.0;
        if (breaksShield(gap)) return 1.0;
        return (gap - DEFLECT_FREE_TIERS) / (DEFLECT_BREAK_TIERS - DEFLECT_FREE_TIERS);
    }

    private static int bonusForBracket(int bracket) {
        if (bracket < BRACKET_BONUS.length) return BRACKET_BONUS[bracket];
        return BRACKET_BONUS[BRACKET_BONUS.length - 1]
                + (bracket - BRACKET_BONUS.length + 1) * BRACKET_STEP;
    }
}
