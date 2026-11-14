package org.charky.aurora.features.player;

// The level curve and what a level pays. Pure numbers, no Bukkit, so the pacing is
// tested directly.
//
// XP comes from mining and kills: a block gives the number of its mine (1 for
// cobblestone, 2 for coal, and so on) and a kill gives the mob's health, so a kill is
// always worth far more than a block. Each level needs XP_PER_LEVEL more than the
// last. Tuned so a full Origin Isles and Gormak run ends around level 50, and level
// 100 takes about four of them
public final class Levels {
    // Level n to n + 1 costs this times n + 1
    static final int XP_PER_LEVEL = 60;
    // Every level pays 1 Essence, plus 1 more for each ten levels reached
    static final int LEVELS_PER_BONUS = 10;
    // A level that ends in 0 pays this many times over
    static final int MILESTONE_EVERY = 10;
    static final int MILESTONE_MULTIPLIER = 5;
    // And every hundredth pays this flat, the monumental ones
    static final int MONUMENT_EVERY = 100;
    static final int MONUMENT_ESSENCE = 500;

    private Levels() {
    }

    // Where a player stands after gaining XP, and what the levels gained paid
    public record Gain(int level, long xp, int levelsGained, int essence) {
    }

    // XP to go from this level to the next
    public static long xpForNext(int level) {
        return (long) XP_PER_LEVEL * (level + 1);
    }

    // Essence paid for reaching this level
    public static int essenceFor(int level) {
        if (level <= 0) return 0;
        if (level % MONUMENT_EVERY == 0) return MONUMENT_ESSENCE;

        int base = 1 + level / LEVELS_PER_BONUS;
        return level % MILESTONE_EVERY == 0 ? base * MILESTONE_MULTIPLIER : base;
    }

    public static boolean isMilestone(int level) {
        return level > 0 && level % MILESTONE_EVERY == 0;
    }

    // Adds XP to a level and the XP already into it, carrying over as many levels
    // as it covers
    public static Gain add(int level, long xp, long gained) {
        long total = xp + Math.max(0, gained);
        int levels = 0;
        int essence = 0;

        while (total >= xpForNext(level)) {
            total -= xpForNext(level);
            level++;
            levels++;
            essence += essenceFor(level);
        }
        return new Gain(level, total, levels, essence);
    }

    // 0 to 1, how far through the current level, for the XP bar
    public static float progress(int level, long xp) {
        return (float) Math.clamp((double) xp / xpForNext(level), 0.0, 0.999);
    }
}
