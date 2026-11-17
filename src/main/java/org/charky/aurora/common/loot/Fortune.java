package org.charky.aurora.common.loot;

import java.util.random.RandomGenerator;

// How many times over a mined block drops. fortune() in scripts/price_calculator.py is the same
public final class Fortune {
    private Fortune() {
    }

    // Nothing below the block's density, then 1, 2, 3.5, 5.5, 8, 11 ... for every level past it
    public static double multiplier(int fortune, int density) {
        int level = fortune - density;
        if (level < 0) return 0;
        return 1.0 + 0.75 * level + 0.25 * level * level;
    }

    // Rare drops grow with the cube root of it, so 116x on a common drop is about 5x on a rare one
    public static double rareMultiplier(int fortune, int density) {
        return Math.cbrt(multiplier(fortune, density));
    }

    public static int scale(int amount, double multiplier) {
        return Math.max(1, (int) Math.round(amount * multiplier));
    }

    // The part after the point is a chance of one more, so a rare drop at 1.3x still
    // gives 2 now and then instead of always rounding back down to 1
    public static int scaleRare(int amount, double multiplier, RandomGenerator random) {
        double scaled = amount * multiplier;
        int whole = (int) scaled;
        if (random.nextDouble() < scaled - whole) whole++;
        return Math.max(1, whole);
    }
}
