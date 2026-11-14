package org.charky.aurora.features.collection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollectionTiersTest {
    @Test
    void tiersAreReachedAtTheirTotals() {
        assertEquals(0, CollectionTiers.tier(0));
        assertEquals(0, CollectionTiers.tier(249));
        assertEquals(1, CollectionTiers.tier(250));
        assertEquals(2, CollectionTiers.tier(2_500));
        assertEquals(3, CollectionTiers.tier(25_000));
        assertEquals(4, CollectionTiers.tier(250_000));
        assertEquals(4, CollectionTiers.tier(9_000_000));
    }

    @Test
    void nothingIsPaidBetweenTiers() {
        assertEquals(0, CollectionTiers.reward(0, 249));
        assertEquals(0, CollectionTiers.reward(250, 2_499));
    }

    @Test
    void passingATierPaysItOnce() {
        assertEquals(5, CollectionTiers.reward(249, 250));
        assertEquals(25, CollectionTiers.reward(2_000, 2_600));
        assertEquals(100, CollectionTiers.reward(24_999, 25_000));
        assertEquals(400, CollectionTiers.reward(249_999, 250_001));
        assertEquals(0, CollectionTiers.reward(250, 251));
    }

    // A big Blast swing at high Fortune can cross two at once
    @Test
    void oneDropCanPassSeveralTiers() {
        assertEquals(5 + 25, CollectionTiers.reward(0, 2_500));
        assertEquals(5 + 25 + 100 + 400, CollectionTiers.reward(0, 250_000));
    }

    @Test
    void theNextTierAndWhatItPays() {
        assertEquals(250, CollectionTiers.nextThreshold(0));
        assertEquals(5, CollectionTiers.nextReward(0));
        assertEquals(25_000, CollectionTiers.nextThreshold(2_500));
        assertEquals(100, CollectionTiers.nextReward(2_500));
        assertEquals(-1, CollectionTiers.nextThreshold(250_000));
        assertEquals(0, CollectionTiers.nextReward(250_000));
    }

    @Test
    void tiersAreShownAsNumerals() {
        assertEquals("", CollectionTiers.numeral(0));
        assertEquals("I", CollectionTiers.numeral(1));
        assertEquals("IV", CollectionTiers.numeral(4));
    }
}
