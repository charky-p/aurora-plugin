package org.charky.aurora.features.collection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollectionTiersTest {
    // Cobblestone, the first mine
    private static final long BLOCK = 250;
    // Feathers, a mob drop needs far fewer
    private static final long DROP = 30;

    @Test
    void thereAreFiveTiers() {
        assertEquals(5, CollectionTiers.count());
    }

    // 30 x 15 = 450 and 30 x 50 = 1,500 are clean already, 48 x 15 = 720 is not
    @Test
    void laterTiersAreRoundedToCleanNumbers() {
        assertEquals(750, CollectionTiers.threshold(48, 3));
        assertEquals(2_500, CollectionTiers.threshold(48, 4));
        assertEquals(10_000, CollectionTiers.threshold(48, 5));
        assertEquals(48, CollectionTiers.threshold(48, 1));
        assertEquals(1_500, CollectionTiers.nearestNice(1_400));
        assertEquals(12, CollectionTiers.nearestNice(13));
    }

    @Test
    void everyTierIsAboutAMultipleOfTheBase() {
        assertEquals(250, CollectionTiers.threshold(BLOCK, 1));
        assertEquals(1_000, CollectionTiers.threshold(BLOCK, 2));
        assertEquals(4_000, CollectionTiers.threshold(BLOCK, 3));
        assertEquals(12_000, CollectionTiers.threshold(BLOCK, 4));
        assertEquals(50_000, CollectionTiers.threshold(BLOCK, 5));
    }

    @Test
    void aSmallerBaseNeedsFewer() {
        assertEquals(30, CollectionTiers.threshold(DROP, 1));
        assertEquals(6_000, CollectionTiers.threshold(DROP, 5));
        assertEquals(2, CollectionTiers.tier(DROP, 120));
        assertEquals(0, CollectionTiers.tier(BLOCK, 120));
    }

    @Test
    void tiersAreReachedAtTheirTotals() {
        assertEquals(0, CollectionTiers.tier(BLOCK, 0));
        assertEquals(0, CollectionTiers.tier(BLOCK, 249));
        assertEquals(1, CollectionTiers.tier(BLOCK, 250));
        assertEquals(2, CollectionTiers.tier(BLOCK, 1_000));
        assertEquals(3, CollectionTiers.tier(BLOCK, 4_000));
        assertEquals(4, CollectionTiers.tier(BLOCK, 12_000));
        assertEquals(5, CollectionTiers.tier(BLOCK, 50_000));
        assertEquals(5, CollectionTiers.tier(BLOCK, 9_000_000));
    }

    @Test
    void nothingIsPaidBetweenTiers() {
        assertEquals(0, CollectionTiers.reward(BLOCK, 0, 249));
        assertEquals(0, CollectionTiers.reward(BLOCK, 250, 999));
    }

    @Test
    void passingATierPaysItOnce() {
        assertEquals(5, CollectionTiers.reward(BLOCK, 249, 250));
        assertEquals(25, CollectionTiers.reward(BLOCK, 900, 1_100));
        assertEquals(100, CollectionTiers.reward(BLOCK, 3_999, 4_000));
        assertEquals(400, CollectionTiers.reward(BLOCK, 11_999, 12_001));
        assertEquals(1_500, CollectionTiers.reward(BLOCK, 49_999, 50_000));
        assertEquals(0, CollectionTiers.reward(BLOCK, 250, 251));
    }

    // A big Blast swing at high Fortune can cross two at once
    @Test
    void oneDropCanPassSeveralTiers() {
        assertEquals(5 + 25, CollectionTiers.reward(BLOCK, 0, 1_000));
        assertEquals(5 + 25 + 100 + 400 + 1_500, CollectionTiers.reward(BLOCK, 0, 50_000));
    }

    @Test
    void theNextTierAndWhatItPays() {
        assertEquals(250, CollectionTiers.nextThreshold(BLOCK, 0));
        assertEquals(5, CollectionTiers.nextReward(BLOCK, 0));
        assertEquals(4_000, CollectionTiers.nextThreshold(BLOCK, 1_000));
        assertEquals(100, CollectionTiers.nextReward(BLOCK, 1_000));
        assertEquals(-1, CollectionTiers.nextThreshold(BLOCK, 50_000));
        assertEquals(0, CollectionTiers.nextReward(BLOCK, 50_000));
    }

    @Test
    void tiersAreShownAsNumerals() {
        assertEquals("", CollectionTiers.numeral(0));
        assertEquals("I", CollectionTiers.numeral(1));
        assertEquals("V", CollectionTiers.numeral(5));
    }
}
