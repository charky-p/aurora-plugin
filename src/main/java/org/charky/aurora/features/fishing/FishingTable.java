package org.charky.aurora.features.fishing;

// What a cast brings up, by rod. A catch is one roll of 1 to 100 plus the rod's
// Luck, read off the rod's table from the top down, so Luck trades junk for better
// fish and a rare needs a lucky rod as well as a lucky roll. Fortune multiplies
// the common fish of the better rod. Pure numbers, tested directly
public final class FishingTable {
    public static final String BEGINNER_ROD = "beginner_rod";
    public static final String NOVICE_ROD = "novice_rod";

    // One in 2000 casts also brings up treasure, whatever the rod
    public static final double TREASURE_CHANCE = 0.0005;
    public static final String TREASURE = "fishing_treasure";

    private FishingTable() {
    }

    public record Catch(String itemId, int amount, boolean rare) {
    }

    public static boolean isRod(String itemId) {
        return BEGINNER_ROD.equals(itemId) || NOVICE_ROD.equals(itemId);
    }

    // roll is 1 to 100, amountRoll is 1 to 3 and only matters for fish that come
    // in numbers. Null for an item that is not one of the rods
    public static Catch roll(String rodId, int roll, int luck, int fortune, int amountRoll) {
        int score = roll + Math.max(0, luck);

        if (BEGINNER_ROD.equals(rodId)) {
            if (score >= 100) return new Catch("blackfin_tuna", 1, true);
            if (score >= 80) return new Catch("black_bass", 1, false);
            if (score >= 50) return new Catch("catfish", 1, false);
            return new Catch("fishing_junk", 1, false);
        }

        if (NOVICE_ROD.equals(rodId)) {
            int extra = Math.max(0, fortune);
            if (score >= 105) return new Catch("spotted_pufferfish", 1, true);
            // One or two a cast, one to three for the salmon, times Fortune, plus one
            if (score >= 95) return new Catch("sea_trout", Math.min(2, amountRoll) * extra + 1, false);
            if (score >= 45) return new Catch("pink_salmon", amountRoll * extra + 1, false);
            return new Catch("fishing_junk", 1, false);
        }

        return null;
    }
}
