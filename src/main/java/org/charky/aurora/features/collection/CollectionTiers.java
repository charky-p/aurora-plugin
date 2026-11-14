package org.charky.aurora.features.collection;

// What a collection pays and when. Every resource has the same four tiers, counted
// on every one of it a player has ever gathered. Pure numbers, tested directly
public final class CollectionTiers {
    static final long[] THRESHOLDS = {250, 2_500, 25_000, 250_000};
    static final int[] REWARDS = {5, 25, 100, 400};
    private static final String[] NUMERALS = {"I", "II", "III", "IV"};

    private CollectionTiers() {
    }

    public static int count() {
        return THRESHOLDS.length;
    }

    // How many tiers this total has reached, 0 to count()
    public static int tier(long amount) {
        int tier = 0;
        for (long threshold : THRESHOLDS) {
            if (amount >= threshold) tier++;
        }
        return tier;
    }

    // Essence for every tier passed going from one total to another
    public static int reward(long before, long after) {
        int essence = 0;
        for (int tier = tier(before); tier < tier(after); tier++) {
            essence += REWARDS[tier];
        }
        return essence;
    }

    // The total the next tier needs, -1 once every tier is reached
    public static long nextThreshold(long amount) {
        int tier = tier(amount);
        return tier >= count() ? -1 : THRESHOLDS[tier];
    }

    // Essence the next tier pays, 0 once every tier is reached
    public static int nextReward(long amount) {
        int tier = tier(amount);
        return tier >= count() ? 0 : REWARDS[tier];
    }

    // "II" for tier 2, empty for tier 0
    public static String numeral(int tier) {
        return tier <= 0 ? "" : NUMERALS[Math.min(tier, NUMERALS.length) - 1];
    }
}
