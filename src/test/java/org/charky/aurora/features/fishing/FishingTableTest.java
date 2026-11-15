package org.charky.aurora.features.fishing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingTableTest {
    private static String beginner(int roll, int luck) {
        return FishingTable.roll(FishingTable.BEGINNER_ROD, roll, luck, 0, 1).itemId();
    }

    private static FishingTable.Catch novice(int roll, int luck, int fortune, int amountRoll) {
        return FishingTable.roll(FishingTable.NOVICE_ROD, roll, luck, fortune, amountRoll);
    }

    // Half junk, three in ten catfish, two in ten bass, and no tuna at all
    @Test
    void theBeginnerRodWithoutLuck() {
        assertEquals("fishing_junk", beginner(1, 0));
        assertEquals("fishing_junk", beginner(49, 0));
        assertEquals("catfish", beginner(50, 0));
        assertEquals("catfish", beginner(79, 0));
        assertEquals("black_bass", beginner(80, 0));
        assertEquals("black_bass", beginner(99, 0));
        assertEquals("blackfin_tuna", beginner(100, 0));
    }

    @Test
    void luckTradesJunkForBetterFish() {
        assertEquals("catfish", beginner(40, 10));
        assertEquals("blackfin_tuna", beginner(90, 10));
        assertTrue(FishingTable.roll(FishingTable.BEGINNER_ROD, 90, 10, 0, 1).rare());
    }

    // The novice rod's rare is out of reach until the rod has Luck
    @Test
    void theNoviceRodNeedsLuckForItsRare() {
        assertEquals("sea_trout", novice(100, 0, 0, 1).itemId());
        assertEquals("spotted_pufferfish", novice(100, 5, 0, 1).itemId());
        assertEquals("fishing_junk", novice(44, 0, 0, 1).itemId());
        assertEquals("pink_salmon", novice(45, 0, 0, 1).itemId());
    }

    @Test
    void fortuneMultipliesTheCommonFish() {
        assertEquals(1, novice(60, 0, 0, 3).amount());
        assertEquals(4, novice(60, 0, 1, 3).amount());
        assertEquals(7, novice(60, 0, 2, 3).amount());
        // Trout come one or two at a time
        assertEquals(5, novice(96, 0, 2, 3).amount());
        assertEquals(1, novice(100, 5, 3, 3).amount());
    }

    @Test
    void onlyTheRodsFish() {
        assertTrue(FishingTable.isRod(FishingTable.BEGINNER_ROD));
        assertTrue(FishingTable.isRod(FishingTable.NOVICE_ROD));
        assertFalse(FishingTable.isRod("starter_pick"));
        assertFalse(FishingTable.isRod(null));
        assertNull(FishingTable.roll("starter_pick", 100, 0, 0, 1));
    }
}
