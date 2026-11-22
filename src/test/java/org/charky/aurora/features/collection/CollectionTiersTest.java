package org.charky.aurora.features.collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionTiersTest {
    // Cobblestone, the first mine
    private static final long BLOCK = 250;

    // The steps of nice_round in scripts/price_calculator.py
    @ParameterizedTest
    @CsvSource({"1, 1", "13, 12", "100, 100", "720, 750", "1000, 1000", "1400, 1500", "3750, 4000"})
    void roundsToTheNearestCleanNumber(long exact, long nice) {
        assertEquals(nice, CollectionTiers.nearestNice(exact));
    }

    // The first tier is the base as it is, the rest are its multiples rounded clean,
    // so 48 x 15 = 720 needs 750
    @ParameterizedTest
    @CsvSource({"250", "48", "30"})
    void tiersAreMultiplesOfTheBase(long base) {
        assertEquals(base, CollectionTiers.threshold(base, 1));
        for (int tier = 2; tier <= CollectionTiers.count(); tier++) {
            long exact = base * CollectionTiers.MULTIPLIERS[tier - 1];
            assertEquals(CollectionTiers.nearestNice(exact), CollectionTiers.threshold(base, tier));
            assertTrue(CollectionTiers.threshold(base, tier) > CollectionTiers.threshold(base, tier - 1));
        }
        assertEquals(750, CollectionTiers.threshold(48, 3));
    }

    @Test
    void tiersAreReachedAtTheirTotals() {
        assertEquals(0, CollectionTiers.tier(BLOCK, 0));
        for (int tier = 1; tier <= CollectionTiers.count(); tier++) {
            long needed = CollectionTiers.threshold(BLOCK, tier);
            assertEquals(tier - 1, CollectionTiers.tier(BLOCK, needed - 1));
            assertEquals(tier, CollectionTiers.tier(BLOCK, needed));
        }
        assertEquals(CollectionTiers.count(), CollectionTiers.tier(BLOCK, Long.MAX_VALUE / 2));
    }

    // Each tier passed pays once, nothing between, and a big Blast swing can pass several
    @Test
    void passingATierPaysItOnce() {
        int[] rewards = CollectionTiers.REWARDS;
        long first = CollectionTiers.threshold(BLOCK, 1);
        long second = CollectionTiers.threshold(BLOCK, 2);
        long last = CollectionTiers.threshold(BLOCK, CollectionTiers.count());
        assertEquals(0, CollectionTiers.reward(BLOCK, 0, first - 1));
        assertEquals(0, CollectionTiers.reward(BLOCK, first, second - 1));
        assertEquals(rewards[0], CollectionTiers.reward(BLOCK, first - 1, first));
        assertEquals(rewards[1], CollectionTiers.reward(BLOCK, second - 1, second + 1));
        assertEquals(rewards[0] + rewards[1], CollectionTiers.reward(BLOCK, 0, second));

        int all = 0;
        for (int reward : rewards) all += reward;
        assertEquals(all, CollectionTiers.reward(BLOCK, 0, last));
        assertEquals(0, CollectionTiers.reward(BLOCK, last, last * 2));
    }

    @Test
    void theNextTierAndWhatItPays() {
        long second = CollectionTiers.threshold(BLOCK, 2);
        assertEquals(CollectionTiers.threshold(BLOCK, 3), CollectionTiers.nextThreshold(BLOCK, second));
        assertEquals(CollectionTiers.REWARDS[2], CollectionTiers.nextReward(BLOCK, second));
        long last = CollectionTiers.threshold(BLOCK, CollectionTiers.count());
        assertEquals(-1, CollectionTiers.nextThreshold(BLOCK, last));
        assertEquals(0, CollectionTiers.nextReward(BLOCK, last));
    }
}
