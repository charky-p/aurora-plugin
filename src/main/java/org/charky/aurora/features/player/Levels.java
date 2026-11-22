package org.charky.aurora.features.player;

// The level curve and what a level pays. Pure numbers, no Bukkit, so the pacing is
// tested directly.
//
// XP comes from mining and kills: every item a block drops gives the block's xp
// in blocks.csv (1 for cobblestone, 2 for coal, and so on, the same for every block
// of a zone), so Fortune and later mines level faster, and a kill gives the mob's
// health. The first level needs XP_BASE, and each one
// after needs EARLY_GROWTH times the last up to level EARLY_LEVELS, then
// LATE_GROWTH times the last for good. The steep start is what makes the first
// minutes feel right: levels 1 to 5 in about ten seconds of mining, 5 to 10 in
// under a minute, 10 to 15 in about two. Level 50 is about 25,000 XP in all and
// level 100 about 3 million
public final class Levels {
    static final double XP_BASE = 1.0;
    static final double EARLY_GROWTH = 1.35;
    static final int EARLY_LEVELS = 15;
    static final double LATE_GROWTH = 1.1;
    // Every level pays 1 Essence, plus 1 more for each ten levels reached, all times
    // ESSENCE_MULTIPLIER
    static final int LEVELS_PER_BONUS = 10;
    static final int ESSENCE_MULTIPLIER = 3;
    // A level that ends in 0 pays this many times over
    static final int MILESTONE_EVERY = 10;
    static final int MILESTONE_MULTIPLIER = 5;
    // And every hundredth pays this flat, the monumental ones
    static final int MONUMENT_EVERY = 100;
    static final int MONUMENT_ESSENCE = 1_500;

    private Levels() {
    }

    // Where a player stands after gaining XP, and what the levels gained paid
    public record Gain(int level, long xp, int levelsGained, int essence) {
    }

    // XP to go from this level to the next
    public static long xpForNext(int level) {
        if (level < 0) return 1L;
        double required = XP_BASE * Math.pow(EARLY_GROWTH, Math.min(level, EARLY_LEVELS))
                * Math.pow(LATE_GROWTH, Math.max(0, level - EARLY_LEVELS));
        if (required >= Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.max(1L, Math.round(required));
    }

    // Essence paid for reaching this level
    public static int essenceFor(int level) {
        if (level <= 0) return 0;
        if (level % MONUMENT_EVERY == 0) return MONUMENT_ESSENCE;

        int base = (1 + level / LEVELS_PER_BONUS) * ESSENCE_MULTIPLIER;
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
