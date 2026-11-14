package org.charky.aurora.features.collection;

// What a collection pays and when. Every resource has five tiers, counted on every
// one of it a player has ever gathered. How many a tier needs depends on the
// resource: its base, the collection column of resources.csv, is the first tier and
// the rest are multiples of it. A base is small for mob drops, a kill is far more
// work than a block, and grows with every mine. Pure numbers, tested directly
public final class CollectionTiers {
    static final int[] MULTIPLIERS = {1, 4, 15, 50, 200};
    static final int[] REWARDS = {5, 25, 100, 400, 1_500};
    private static final String[] NUMERALS = {"I", "II", "III", "IV", "V"};

    private CollectionTiers() {
    }

    public static int count() {
        return MULTIPLIERS.length;
    }

    // The total a tier needs, tiers are 1 to count()
    public static long threshold(long base, int tier) {
        return base * MULTIPLIERS[tier - 1];
    }

    // How many tiers this total has reached, 0 to count()
    public static int tier(long base, long amount) {
        int tier = 0;
        while (tier < count() && amount >= threshold(base, tier + 1)) tier++;
        return tier;
    }

    // Essence for every tier passed going from one total to another
    public static int reward(long base, long before, long after) {
        int essence = 0;
        for (int tier = tier(base, before); tier < tier(base, after); tier++) {
            essence += REWARDS[tier];
        }
        return essence;
    }

    // The total the next tier needs, -1 once every tier is reached
    public static long nextThreshold(long base, long amount) {
        int tier = tier(base, amount);
        return tier >= count() ? -1 : threshold(base, tier + 1);
    }

    // Essence the next tier pays, 0 once every tier is reached
    public static int nextReward(long base, long amount) {
        int tier = tier(base, amount);
        return tier >= count() ? 0 : REWARDS[tier];
    }

    // "II" for tier 2, empty for tier 0
    public static String numeral(int tier) {
        return tier <= 0 ? "" : NUMERALS[Math.min(tier, NUMERALS.length) - 1];
    }
}
