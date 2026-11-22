package org.charky.aurora.features.collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollectionTiersTest {
    // Cobblestone, the first mine
    private static final long BLOCK = 250;

    // About a multiple of the base, later tiers rounded to clean numbers:
    // 30 x 15 = 450 is clean already, 48 x 15 = 720 is not
    @ParameterizedTest
    @CsvSource({"250, 1, 250", "250, 2, 1000", "250, 3, 4000", "250, 4, 12000", "250, 5, 50000",
            "30, 1, 30", "30, 5, 6000", "48, 1, 48", "48, 3, 750", "48, 4, 2500", "48, 5, 10000"})
    void thresholds(long base, int tier, long expected) {
        assertEquals(expected, CollectionTiers.threshold(base, tier));
    }

    @Test
    void tiersAreReachedAtTheirTotals() {
        assertEquals(0, CollectionTiers.tier(BLOCK, 249));
        assertEquals(1, CollectionTiers.tier(BLOCK, 250));
        assertEquals(4, CollectionTiers.tier(BLOCK, 12_000));
        assertEquals(5, CollectionTiers.tier(BLOCK, 9_000_000));
        assertEquals(2, CollectionTiers.tier(30, 120));
    }

    // Paid once per tier passed, nothing between, and a big Blast swing can pass several
    @ParameterizedTest
    @CsvSource({"0, 249, 0", "250, 999, 0", "250, 251, 0", "249, 250, 5", "900, 1100, 25",
            "49999, 50000, 1500", "0, 1000, 30", "0, 50000, 2030"})
    void passingATierPaysItOnce(long before, long after, long reward) {
        assertEquals(reward, CollectionTiers.reward(BLOCK, before, after));
    }

    @Test
    void theNextTierAndWhatItPays() {
        assertEquals(4_000, CollectionTiers.nextThreshold(BLOCK, 1_000));
        assertEquals(100, CollectionTiers.nextReward(BLOCK, 1_000));
        assertEquals(-1, CollectionTiers.nextThreshold(BLOCK, 50_000));
        assertEquals(0, CollectionTiers.nextReward(BLOCK, 50_000));
    }
}
