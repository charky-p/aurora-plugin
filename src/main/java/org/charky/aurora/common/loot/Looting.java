package org.charky.aurora.common.loot;

import java.util.random.RandomGenerator;

// Looting is Fortune for mobs. Level is the Looting past the mob's density: below 0 the
// resource does not drop at all, then it is multiplied by the same curve, 1, 2, 3.5, 5.5,
// 8, 11 ... for 0, 1, 2, 3, 4, 5, and a rare drop (under 100%) by its cube root. Only
// stackable resources are boosted, never gear or keys
public final class Looting {
    private Looting() {
    }

    public static int apply(int amount, int level, boolean rare, RandomGenerator random) {
        if (level < 0) return 0;
        if (level == 0) return amount;
        return rare
                ? Fortune.scaleRare(amount, Fortune.rareMultiplier(level, 0), random)
                : Fortune.scale(amount, Fortune.multiplier(level, 0));
    }
}
