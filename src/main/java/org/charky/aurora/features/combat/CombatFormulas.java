package org.charky.aurora.features.combat;

// The pure maths of combat, no Bukkit, so it is tested directly against the
// numbers in scripts/balance/balance_sim.py. Keep the two in step
public final class CombatFormulas {
    // Every sword hits for this before Sharpness
    public static final double BASE_DAMAGE = 10.0;

    // Mob hits are dealt in real hearts, set by how many tiers the mob's attack
    // level sits above the player's protection level. Each zone spans this many
    // protection levels
    static final double LEVELS_PER_TIER = 4.0;
    private static final double LOG_A = 1 / Math.log(1.15);
    private static final double LOG_B = 0.15;

    private static final int BRACKET_SIZE = 5;
    private static final int[] BRACKET_BONUS = {1, 3, 5, 10, 15, 20, 25, 30, 35, 40};
    private static final int BRACKET_STEP = 5;

    private static final double FULL_CHARGE_MS = 625.0;

    private CombatFormulas() {
    }

    // Same tier is one heart. Above it grows like a log, 2, 2.9, 3.7, 4.4, 5, so
    // big hits break through armour. Below it tapers to half a heart, never less.
    // Both sides meet at exactly 1 so there is no jump at gap 0
    public static double heartsPerHit(double attackLevel, double protectionLevel) {
        double gap = (attackLevel - protectionLevel) / LEVELS_PER_TIER;
        if (gap >= 0) return 1 + LOG_A * Math.log(1 + LOG_B * gap);
        return 0.5 + 0.5 * Math.exp(2 * gap);
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

    // 20% damage on a spammed swing rising to 100% on a timed one. Squared so
    // half charge pays 40%, not 60%.
    public static double chargeMultiplier(long sinceLastSwingMs) {
        double charge = Math.min(1.0, sinceLastSwingMs / FULL_CHARGE_MS);
        return 0.2 + 0.8 * charge * charge;
    }

    private static int bonusForBracket(int bracket) {
        if (bracket < BRACKET_BONUS.length) return BRACKET_BONUS[bracket];
        return BRACKET_BONUS[BRACKET_BONUS.length - 1]
                + (bracket - BRACKET_BONUS.length + 1) * BRACKET_STEP;
    }
}
